package com.homework.web.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 管理员初始化、邀请链接和导入目录配置。 */
@Data
@Component

/*
前缀只能是 admin，不能是 admin.invitation
因为AdminFeatureProperties 除了配置 public-base-url，还要配置 super-admin 和 import-config
如果你写成admin.invitation，其他配置将无法读取
 */

/*
Spring Boot 启动
    ↓
读取 application.yml
    ↓
发现 AdminFeatureProperties
    ↓
根据 @ConfigurationProperties 将配置文件的值绑定到 AdminFeatureProperties 对应的字段上
    ↓
创建并保存配置对象
    ↓
注入 AdminManagementService
    ↓
业务代码调用 Getter 取得已经填好的值
 */
@ConfigurationProperties(prefix = "admin")
public class AdminFeatureProperties {

    private final SuperAdmin superAdmin = new SuperAdmin();
    private final Invitation invitation = new Invitation();
    private final ImportConfig importConfig = new ImportConfig();

    /** 内置超级管理员配置。 */
    @Data
    public static class SuperAdmin {
        private String email;
        private String password;
        private String displayName;
    }

    /** 邀请链接配置。 */
    @Data
    public static class Invitation {
        private String publicBaseUrl;
    }

    /** Excel 导入临时文件配置。 */
    @Data
    public static class ImportConfig {
        private String tempDirectory;
    }
}
