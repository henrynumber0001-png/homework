package com.homework.web.admin.service;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.storage.CosReadUrlSigner;
import com.homework.common.storage.TencentCosProperties;
import com.homework.web.admin.vo.QuestionImageUploadVO;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 使用腾讯云 COS 保存和解析题目图片。 */
@Service
@RequiredArgsConstructor
public class QuestionImageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final COSClient cosClient; //腾讯COS客户端，负责上传、下载、删除、获取图片
    private final TencentCosProperties properties; //配置文件，配置我的腾讯云账号和桶地址
    private final CosReadUrlSigner readUrlSigner; //为私有 COS 对象生成临时只读地址

    //“临时图片必须在 24 小时内绑定到题目”
    public QuestionImageUploadVO upload(MultipartFile file) { //Spring 上传的图片都会变成 MultipartFile类型
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_SIZE || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        //得到后缀名称
        String extension = switch (file.getContentType()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        };

        /*
        Bucket
            │
            ├── objectKey1 // 也就是存储桶中一个具体对象的 键Key
            │      │
            │      ▼
            │   Object
            │     ├── Metadata
            │     └── Binary Content //实际的图片，存储于COS的服务器硬盘上
            │
            ├── objectKey2
            │      │
            │      ▼
            │   Object
            │     ├── Metadata
            │     └── Binary Content
            │
            └── ...
         */
        String objectKey = "admin-temp/questions/%s/%s.%s".formatted( //自主设计一个 Key的格式：admin-temp+questions+时间+随机数+扩展名
                LocalDate.now(),
                System.currentTimeMillis() + "-" + UUID.randomUUID().toString().toLowerCase(Locale.ROOT),
                extension);
        //System.currentTimeMillis() 是 毫秒时间戳。
        //objectKey就是腾讯COS里的 Object Key，不是URL，也不是文件名，它是一个字符串
        /** 更重要的来了：存储桶中存储的Map集合是 Map<objectKey, Object> */

        String previewUrl;
        try (InputStream inputStream = file.getInputStream()) { //打开输入流，结束后自动 close()
            ObjectMetadata metadata = new ObjectMetadata();

            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            //调用腾讯COS客户端，把前端传过来的图片都字节数、文件类型、objectKey存入指定的存储桶里的一个object中
            cosClient.putObject(new PutObjectRequest(
                    properties.getBucket(), //保存到哪个桶
                    objectKey, //桶里的键 输入什么
                    inputStream, //上传对象的字节输入流(真正的图片)
                    metadata //附加信息，如文件类型（扩展名）、字节数(KB/MB)
            ));
            //previewUrl = Predesign URL
            previewUrl = readUrlSigner.sign(objectKey);
        } catch (Exception exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }

        LocalDateTime now = LocalDateTime.now();
        QuestionImageUploadVO result = new QuestionImageUploadVO();

        //因为此时，objectKey 还没有最终确定被绑定到 题目的数据库表，也就是管理员还没点击“创建题目”按钮
        //所以要把这个临时的 objectKey 返回给前端，用于前端在创建题目时，调用bind方法，把 objectKey 绑定到数据库表
        result.setObjectKey(objectKey);
        //后端把这个图片在COS服务器中生成的 presignUrl 返回给前端，然后是前端拿着这个 previewUrl 自己再去请求 COS服务器，最终返回二进制图片给到浏览器（这一步不经过后端了）
        result.setPreviewUrl(previewUrl);
        result.setPreviewUrlExpiresTime(now.plusSeconds(properties.getReadUrlTtlSeconds()));
        result.setUploadExpiresTime(now.plusHours(24)); //过期时间设置为 24h
        return result;
    }
    /*
    前端接收到这个JSON:{objectKey, previewUrl, 过期时间}, 执行 imagePreview.value = result.previewUrl
    再次向COS服务器发出 GET请求，COS服务器收到previewUrl之后，校验 URL 中的签名以及是否过期，返回图片二进制内容，给到前端，最终返回给管理员
     */

    /** 将临时图片复制到正式目录、删除原对象，并返回正式对象 Key。 */

    public String bind(String objectKey) { //再把这个临时照片的 objectKey 传回来
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        //将临时 objectKey 替换为正式 objectKey 之前，要检查 图片从第一次上传到 COS 服务器开始，到这个方法 bind() 被调用的时间，是否超过了设定的24h
        validateObjectKey(objectKey);

        //设置正式图片的 objectKey
        String targetObjectKey = objectKey.replaceFirst("^admin-temp/questions/", "questions/");
        try {
            cosClient.copyObject(new CopyObjectRequest( //把临时照片的 object 复制一份，其他 metadata 和 binary content 不变
                    properties.getBucket(),
                    objectKey,
                    properties.getBucket(),
                    targetObjectKey
            )); //注意：此时存储桶中有两个 object
            cosClient.deleteObject(properties.getBucket(), objectKey); //把临时照片的 object 删除
        } catch (Exception exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }
        return targetObjectKey; //把正式照片的 objectKey 返回前端
    }

    /** 校验临时图片标识的目录和24小时有效期。 */
    public void validateObjectKey(String objectKey) {
        if (!objectKey.startsWith("admin-temp/questions/") || objectKey.contains("..")) { //防止 Path Traversal（路径穿越攻击）
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        String fileName = objectKey.substring(objectKey.lastIndexOf('/') + 1); //从 objectKey 中的最后一个 / 之后开始截取
        int separator = fileName.indexOf('-'); //找到 - 的索引
        try {
            long uploadedAt = Long.parseLong(fileName.substring(0, separator)); // 就是自主设计的规则中的 System.currentTimeMillis()，也就是上传时间
            long expiresAt = uploadedAt + java.time.Duration.ofHours(24).toMillis(); // + 24小时并转换到毫秒，然后校验临时图片过期了没
            if (System.currentTimeMillis() > expiresAt) {
                throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
            }
        } catch (HomeworkException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR, exception);
        }
    }
}


/*
用户选择图片
        │
        ▼
MultipartFile
        │
        ├── 文件名（cat.png）
        ├── Content-Type（image/png）
        ├── 大小（52 KB）
        └── 字节流（89 50 4E 47 ...）
                 │
                 ▼
file.getInputStream()
                 │
                 ▼
PutObjectRequest
├── bucket：homework
├── objectKey：admin-temp/questions/2026-07-27/abc.jpg
├── metadata：Content-Type、Content-Length
└── InputStream（真正图片内容）
                 │
                 ▼
腾讯 COS
├── Key（objectKey）
├── Metadata
└── Binary Content（真实图片字节）
                 │
                 ▼
readUrlSigner.sign(objectKey)
                 │
                 ▼
previewUrl
https://bucket.cos.../admin-temp/questions/2026-07-27/abc.jpg?signature=...&expires=...
 */


/*
私有读写，只携带COS Public URL + objectKey，无法读取服务器上的图片，因为权限校验失败：
   浏览器
     │
     ▼
Public URL + ObjectKey
     │
     ▼
 COS收到请求
     │
     ▼
① Bucket 是否 Public？（
     │
     ├── 是
     │      ▼
     │   返回资源
     │
     └── 否
            │
            ▼
② 有没有认证信息？（objectKey后面的一串查询参数）
            │
            ├── 没有
            │      ▼
            │    403
            │
            └── 有
                   │
                   ▼
            ③ AKID 是否存在？
                   │
                   ▼
            ④ 找到对应 SecretKey
                   │
                   ▼
            ⑤ 根据 q-sign-algorithm选择算法
                   │
                   ▼
            ⑥ q-sign-time 是否过期？
                   │
                   ▼
            ⑦ 用 SecretKey重新计算 Signature
                   │
                   ▼
            ⑧ 是否与 q-signature 一致？
                   │
          ├────────┴────────┐
          ▼                 ▼
         一致               不一致
          │                 │
          ▼                 ▼
      返回 Object        403 Forbidden
 */
