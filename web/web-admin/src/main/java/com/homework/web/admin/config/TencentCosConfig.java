package com.homework.web.admin.config;

import com.homework.common.storage.CosReadUrlSigner;
import com.homework.common.storage.TencentCosProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** 创建管理端共用的腾讯云 COS 客户端。 */
@Configuration
@EnableConfigurationProperties(TencentCosProperties.class)
public class TencentCosConfig {

    /** 创建通过 HTTPS 访问指定地域的 COS 客户端。 */
    @Bean(destroyMethod = "shutdown")
    public COSClient cosClient(TencentCosProperties properties) {
        COSCredentials credentials = new BasicCOSCredentials(
                properties.getSecretId(),
                properties.getSecretKey()
        );
        ClientConfig clientConfig = new ClientConfig(new Region(properties.getRegion()));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        return new COSClient(credentials, clientConfig);
    }

    /** 创建管理端题目图片只读地址签发器。 */
    @Bean
    public CosReadUrlSigner cosReadUrlSigner(COSClient cosClient, TencentCosProperties properties) {
        return new CosReadUrlSigner(
                cosClient,
                properties.getBucket(),
                properties.getReadUrlTtlSeconds()
        );
    }
}
