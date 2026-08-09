package com.homework.common.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserImageUrlResolverTest {

    @Test
    void uploadedAvatarIsSigned() {
        CosReadUrlSigner signer = mock(CosReadUrlSigner.class);
        when(signer.sign("user/image/avatar/7/avatar.png")).thenReturn("https://signed.example/avatar");
        UserImageUrlResolver resolver = new UserImageUrlResolver(signer);

        String result = resolver.resolveAvatar("user/image/avatar/7/avatar.png");

        assertEquals("https://signed.example/avatar", result);
        verify(signer).sign("user/image/avatar/7/avatar.png");
    }

    @Test
    void missingObjectKeyReturnsNull() {
        CosReadUrlSigner signer = mock(CosReadUrlSigner.class);
        UserImageUrlResolver resolver = new UserImageUrlResolver(signer);

        assertNull(resolver.resolveAvatar(null));
        assertNull(resolver.resolveBanner(" "));
        verifyNoInteractions(signer);
    }
}
