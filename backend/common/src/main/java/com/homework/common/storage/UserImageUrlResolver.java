package com.homework.common.storage;

/** 统一将用户图片的私有 COS object key 转换为临时可读 URL。 */
public class UserImageUrlResolver {

    private final CosReadUrlSigner readUrlSigner;

    public UserImageUrlResolver(CosReadUrlSigner readUrlSigner) {
        this.readUrlSigner = readUrlSigner;
    }

    public String resolveAvatar(String avatarObjectKey) {
        return resolve(avatarObjectKey);
    }

    /** Banner 只允许来自私有 COS。 */
    public String resolveBanner(String bannerObjectKey) {
        return resolve(bannerObjectKey);
    }

    private String resolve(String objectKey) {
        return hasText(objectKey) ? readUrlSigner.sign(objectKey) : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
