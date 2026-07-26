package com.homework.common.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;

import java.time.Instant;
import java.util.Date;

/** 为私有 COS 对象生成临时只读地址。 */
public class CosReadUrlSigner {

    private final COSClient cosClient;
    private final String bucket;
    private final long ttlSeconds;

    public CosReadUrlSigner(COSClient cosClient, String bucket, long ttlSeconds) {
        this.cosClient = cosClient;
        this.bucket = bucket;
        this.ttlSeconds = ttlSeconds;
    }

    /** 为对象 Key 生成带 GET 权限的临时签名地址。 */
    public String sign(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        Date expiresTime = Date.from(Instant.now().plusSeconds(ttlSeconds));
        return cosClient.generatePresignedUrl(
                bucket,
                objectKey,
                expiresTime,
                HttpMethodName.GET
        ).toString();
    }
}
