package com.homework.web.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付 API v3 配置。
 *
 * <p>所有敏感信息均通过环境变量注入。这里的 appId 是已经与微信支付商户号绑定的
 * 应用 AppID，不一定等同于网站微信登录使用的 AppID。
 */
@Data
@Component
@ConfigurationProperties(prefix = "payment.wechat")
public class WechatPayProperties {

    private boolean enabled;

    private String appId;

    private String merchantId;

    private String merchantSerialNumber;

    private String merchantPrivateKeyPath;

    private String wechatPayPublicKeyId;

    private String wechatPayPublicKeyPath;

    private String apiV3Key;

    private String notifyUrl;

    private String zoneId = "Asia/Shanghai";

    private String productDescription = "Homework会员订阅";
}
