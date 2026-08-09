package com.homework.web.app.service.impl;

import com.homework.common.exception.HomeworkException;
import com.homework.common.storage.CosReadUrlSigner;
import com.homework.common.storage.TencentCosProperties;
import com.homework.model.entity.UserInfo;
import com.homework.model.enums.UserInfoStatus;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.model.enums.UserImageType;
import com.homework.web.app.vo.UserImageVO;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserImageServiceTest {

    @Mock private COSClient cosClient;
    @Mock private CosReadUrlSigner readUrlSigner;
    @Mock private UserInfoMapper userInfoMapper;

    private UserImageService service;

    @BeforeEach
    void setUp() {
        TencentCosProperties properties = new TencentCosProperties();
        properties.setBucket("homework-test");
        service = new UserImageService(cosClient, properties, readUrlSigner, userInfoMapper);
    }

    @Test
    void uploadUsesCurrentUsersPrefix() {
        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        };
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", png);
        when(readUrlSigner.sign(any())).thenReturn("https://signed.example/preview");

        UserImageVO result = service.upload(UserImageType.AVATAR, file, 7L);

        assertTrue(result.getImageObjectKey().startsWith("temp/user/image/avatar/"));
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(cosClient).putObject(request.capture());
        assertTrue(request.getValue().getKey().endsWith(".png"));
    }

    @Test
    void uploadRejectsFakeImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", "not an image".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(HomeworkException.class,
                () -> service.upload(UserImageType.AVATAR, file, 7L));
        verifyNoInteractions(cosClient);
    }

    @Test
    void updateRejectsNestedTemporaryKey() {
        assertThrows(HomeworkException.class, () -> service.updateImage(
                UserImageType.AVATAR, "temp/user/image/avatar/8/example.png", 7L
        ));

        verifyNoInteractions(cosClient);
    }

    @Test
    void updateRejectsWrongImageType() {
        assertThrows(HomeworkException.class, () -> service.updateImage(
                UserImageType.AVATAR, "temp/user/image/banner/7/example.png", 7L
        ));

        verifyNoInteractions(cosClient);
    }

    @Test
    void updateStoresOfficialKeyAndCleansOldImages() {
        UserInfo user = activeUser();
        user.setAvatarObjectKey("user/image/avatar/7/old.png");
        when(userInfoMapper.selectById(7L)).thenReturn(user);
        when(userInfoMapper.updateById(any(UserInfo.class))).thenReturn(1);
        String temporaryKey = "temp/user/image/avatar/new.png";

        service.updateImage(UserImageType.AVATAR, temporaryKey, 7L);

        verify(cosClient).copyObject(any(CopyObjectRequest.class));
        verify(cosClient).deleteObject("homework-test", temporaryKey);
        verify(cosClient).deleteObject("homework-test", "user/image/avatar/7/old.png");
        assertEquals("user/image/avatar/new.png", user.getAvatarObjectKey());
    }

    @Test
    void updateDeletesNewObjectWhenDatabaseUpdateFails() {
        when(userInfoMapper.selectById(7L)).thenReturn(activeUser());
        when(userInfoMapper.updateById(any(UserInfo.class))).thenReturn(0);

        assertThrows(HomeworkException.class, () -> service.updateImage(
                UserImageType.BANNER, "temp/user/image/banner/new.webp", 7L
        ));

        verify(cosClient).deleteObject("homework-test", "user/image/banner/new.webp");
        verify(cosClient, never()).deleteObject("homework-test", "temp/user/image/banner/new.webp");
    }

    private UserInfo activeUser() {
        UserInfo user = new UserInfo();
        user.setId(7L);
        user.setStatus(UserInfoStatus.ACTIVE);
        return user;
    }
}
