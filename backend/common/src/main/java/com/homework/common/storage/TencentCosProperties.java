package com.homework.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 腾讯云 COS 对象存储及签名地址配置。 */
@Data
@ConfigurationProperties(prefix = "tencent-cos")
public class TencentCosProperties {

    /** 存储桶所在地域，例如 ap-guangzhou。 */
    private String region;

    /** 当前服务使用的腾讯云子账号 SecretId。 */
    private String secretId;

    /** 与 SecretId 对应的腾讯云子账号 SecretKey。 */
    private String secretKey;

    /** 包含 APPID 后缀的完整存储桶名称。 */
    private String bucket;

    /** 私有对象签名地址的有效秒数。 */
    private long readUrlTtlSeconds = 3600;
}
