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

        String presignedUrl = cosClient.generatePresignedUrl(bucket, objectKey, expiresTime, HttpMethodName.GET).toString();
        return presignedUrl;


        //generatePresignedUrl 方法把 GET请求方法 + 到期时间 + COS服务器的桶名 + objectKey 拼起来
        //然后再利用我的COS子账户的 SecretKey，使用HMAC-SHA1/SHA256算法，最终得到一个签名（比如2AE8F9C7D5....）
        //generatePresignedUrl 最终生成的不只是一个签名，而是一整个URL（Presigned URL），包含：COS Public URL + objectKey + 算法 + 签发AKID + 签名有效期（根据ttl）+ SecretKey生效时间 + 签名本身
        //Presigned URL 指向的是 COS 中的 Object（也就是说，URL实际上是Object的引用），而当浏览器通过这个 URL 发起 GET 请求时，COS 返回的是 Object 的 Binary Content（以及相关 Metadata）。
    }
}
