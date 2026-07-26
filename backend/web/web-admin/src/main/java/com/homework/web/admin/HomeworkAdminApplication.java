package com.homework.web.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Homework 独立后台管理应用入口。 */
@MapperScan("com.homework.web.admin.mapper")
@SpringBootApplication(scanBasePackages = "com.homework.web.admin")
public class HomeworkAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeworkAdminApplication.class, args);
    }
}
