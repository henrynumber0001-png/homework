package com.homework.web.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 管理员初始化、邀请链接和导入目录配置。 */
@Data
@Component
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
