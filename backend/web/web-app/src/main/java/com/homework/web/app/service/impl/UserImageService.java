package com.homework.web.app.service.impl;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.storage.CosReadUrlSigner;
import com.homework.common.storage.TencentCosProperties;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.UserInfoStatus;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.model.enums.UserImageType;
import com.homework.web.app.vo.UserImageVO;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserImageService {

    private final COSClient cosClient;
    private final TencentCosProperties properties;
    private final CosReadUrlSigner readUrlSigner;
    private final UserInfoMapper userInfoMapper;

    /** 上传图片到当前用户的临时目录。 */
    public UserImageVO upload(UserImageType imageType,MultipartFile file,Long userId) {
        if (userId == null || imageType == null || file == null || file.isEmpty()
                || file.getSize() > imageType.getMaxSize()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        byte[] content;
        try {content = file.getBytes();}
        catch (IOException exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }

        //Magic Number：不相信文件名后缀，而是直接检查文件二进制内容的开头几个字节，判断它到底是 PNG、JPG 还是 WebP
        //获得文件后缀名，这个后缀名是用于拼接到 ObjectKey 的最后一个词缀
        String extension = detectExtension(content);
        //再转换回 contentType 格式
        String contentType = switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        };

        // contentType 是通过检查文件二进制 Magic Number 后推断出来的，代表文件的真实格式
        if (!contentType.equalsIgnoreCase(file.getContentType())) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //这里使用了一个非常好的方法：为了应对 file 既可能是 avatar，又可能是 banner，那么就把 avatar和banner 做成枚举类实例之一
        //然后两个类型，除了名字要求的MaxSize不同，上传图片的其他任何内容都是相同的
        String temporaryObjectKey = "temp/user/image/%s/%s.%s".formatted(
                imageType.getName().toLowerCase(),
                LocalDate.now() + "-" + UUID.randomUUID().toString().toLowerCase(Locale.ROOT),
                extension
        );

        // 装配存储桶中的 metaData
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        metadata.setContentType(contentType);

        try (InputStream inputStream = file.getInputStream()) {
            cosClient.putObject(
                    new PutObjectRequest(
                    properties.getBucket(),
                            temporaryObjectKey,
                            inputStream,
                            metadata
            ));
        } catch (Exception exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }

        UserImageVO result = new UserImageVO();
        result.setImageObjectKey(temporaryObjectKey);
        result.setPreviewUrl(readUrlSigner.sign(temporaryObjectKey));
        return result;
    }

    /** 将临时图片变为正式图片，并保存到 userInfo */
    public void updateImage(UserImageType imageType, String temporaryObjectKey,Long userId) {
        if (userId == null || imageType == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //校验格式
        validateTemporaryObjectKey(imageType, temporaryObjectKey);

        // userInfo 是 updateImage() 方法内的局部变量
        // 当updateImage() 方法结束，局部变量 userInfo 随着方法调用栈一起消失。只要没有其他地方保存这个对象的引用，该对象就无法再被访问，之后会被垃圾回收。
        // 因为一旦乐观锁并发，更新失败，数据库就不会update这次 userInfo 中的数据到数据库
        UserInfo userInfo = userInfoMapper.selectById(userId);

        if (userInfo == null || userInfo.getStatus() != UserInfoStatus.ACTIVE) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }

        String officialObjectKey = temporaryObjectKey.replaceFirst("temp/user/image/", "user/image/");
        String oldObjectKey = imageType == UserImageType.AVATAR ? userInfo.getAvatarObjectKey() : userInfo.getBannerObjectKey();

        try {
            cosClient.copyObject(
                    new CopyObjectRequest(
                    properties.getBucket(), temporaryObjectKey,
                    properties.getBucket(), officialObjectKey
            ));
        } catch (Exception exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }

        if (imageType == UserImageType.AVATAR) {
            userInfo.setAvatarObjectKey(officialObjectKey);
        } else {
            userInfo.setBannerObjectKey(officialObjectKey);
        }

        try {
            //如果乐观锁并发，更新失败
            if (userInfoMapper.updateById(userInfo) != 1) {
                throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
            }
        } catch (RuntimeException exception) {
            //就要删除这个已经存入到 COS存储桶中的 officialObjectKey，因为根本没使用它
            deleteObjectKey(officialObjectKey);
            // 继续抛出异常，结束 updateImage()
            throw exception;
        }

        //如果更新成功了，就不会走上面的catch模块了，那么存储桶就要使用正式的 objectKey，那么就要把临时 objectKey 删除
        deleteObjectKey(temporaryObjectKey);

        //如果这不是第一次设置头像，此前的 旧的 officialObjectKey 也要删除
        if (oldObjectKey != null && !oldObjectKey.equals(officialObjectKey)) {
            deleteObjectKey(oldObjectKey);
        }
        //对于那些最终未提交、重复上传产生的临时 objectKey，会被 COS 自动删除（比如用户反复尝试头像，但最终都没有提交的临时 objectKey）
        //在 COS 的存储桶中，通过基础配置 → 生命周期，设置规则开启“定时任务”，从而完成删除操作（不用在java中设置定时任务清理）
    }

    /** 确保临时 Key 的图片类型正确，并且文件名中没有额外目录。 */
    private void validateTemporaryObjectKey(UserImageType imageType, String objectKey) {
        String prefix = "temp/user/image/" + imageType.getName().toLowerCase() + "/";
        if (objectKey == null || !objectKey.startsWith(prefix)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        String fileName = objectKey.substring(prefix.length());
        if (fileName.isBlank() || fileName.contains("/") || fileName.contains("\\") //因为 \ 是转义字符，所以用 双\\表示 单纯的 \
                || !(fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".webp"))
        ) {throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);}
    }


    /** 根据文件头判断真实格式，不相信客户端传来的文件名。 */
    private String detectExtension(byte[] content) {

        //一个标准 PNG 文件开头固定是这 8 个字节：89 50 4E 47 0D 0A 1A 0A
        if (content.length >= 8
                && (content[0] & 0xff) == 0x89
                && content[1] == 0x50 && content[2] == 0x4e && content[3] == 0x47
                && content[4] == 0x0d && content[5] == 0x0a
                && content[6] == 0x1a && content[7] == 0x0a) {
            return "png";
        }
        if (content.length >= 3
                && (content[0] & 0xff) == 0xff
                && (content[1] & 0xff) == 0xd8
                && (content[2] & 0xff) == 0xff) {
            return "jpg";
        }
        if (content.length >= 12
                && content[0] == 'R' && content[1] == 'I'
                && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E'
                && content[10] == 'B' && content[11] == 'P') {
            return "webp";
        }
        throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
    }

    private void deleteObjectKey(String objectKey) {
        try {
            cosClient.deleteObject(properties.getBucket(), objectKey);
        } catch (Exception exception) {
            log.warn("清理 COS 图片失败: {}", objectKey, exception);
        }
    }
}
