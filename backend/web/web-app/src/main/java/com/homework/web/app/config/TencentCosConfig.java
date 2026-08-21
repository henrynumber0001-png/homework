package com.homework.web.app.config;

import com.homework.common.storage.CosReadUrlSigner;
import com.homework.common.storage.TencentCosProperties;
import com.homework.common.storage.UserImageUrlResolver;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 创建 App 端使用最小权限子账号密钥的腾讯云 COS 客户端。 */
@Configuration
@EnableConfigurationProperties(TencentCosProperties.class)
public class TencentCosConfig {

    /** 创建通过 HTTPS 访问指定地域的 COS 客户端。 */
    @Bean(destroyMethod = "shutdown")
    //把这个方法返回的对象 COSClient 注册到 Spring IoC 容器
    //当 Spring 容器关闭的时候，调用这个 Bean 的 shutdown() 方法（Spring 最后帮你执行 cosClient.shutdown()）
    public COSClient cosClient(TencentCosProperties properties) { //传入配置文件，是 Spring 调用这个 @Bean 方法的时候自动提供参数。

        //创建腾讯云访问凭证： SecretId + SecretKey -> COSCredentials
        //COS SDK 请求腾讯云的时候，需要使用它们进行身份认证和签名。
        COSCredentials credentials = new BasicCOSCredentials(properties.getSecretId(), properties.getSecretKey());
        //告诉COS：我的 Bucket 位于哪个腾讯云地域
        ClientConfig clientConfig = new ClientConfig(new Region(properties.getRegion()));
        //强制使用 HTTPS：COSClient 与腾讯 COS 服务器通信的时候使用 HTTPS
        clientConfig.setHttpProtocol(HttpProtocol.https);

        //创建 COS 客户端
        return new COSClient(credentials, clientConfig);
    }

    /** 创建 App 端私有对象只读地址签发器。 */
    @Bean
    //先让Spring 创建腾讯官方的 COSClient，然后再利用这个 COSClient 创建你自己的 CosReadUrlSigner，最后把 CosReadUrlSigner 也交给Spring管理
    public CosReadUrlSigner cosReadUrlSigner(COSClient cosClient, TencentCosProperties properties) {
        return new CosReadUrlSigner(
                cosClient,
                properties.getBucket(),
                properties.getReadUrlTtlSeconds()
        );
    }
    //补充：@Bean 并不关心这个类是谁写的，关心的只有：这个方法返回了一个 CosReadUrlSigner 对象，我要把这个对象注册进 IoC
    //并不是说 @Bean 不能用于注册自己写的类到IoC容器，只是通常用 @Component
    /*
    也完全可以用 @Component
    @Component
    public class CosReadUrlSigner {

    private final COSClient cosClient;
    private final String bucket;
    private final long ttlSeconds;

    public CosReadUrlSigner(COSClient cosClient,TencentCosProperties properties) { //这里要把 String bucket, long ttlSeconds 改成 properties.getBucket() 和 properties.getReadUrlTtlSeconds()
        this.cosClient = cosClient;
        this.bucket = properties.getBucket();
        this.ttlSeconds = properties.getReadUrlTtlSeconds();
    }
}
     */

    @Bean
    public UserImageUrlResolver userImageUrlResolver(CosReadUrlSigner readUrlSigner) {
        return new UserImageUrlResolver(readUrlSigner);
    }
}
