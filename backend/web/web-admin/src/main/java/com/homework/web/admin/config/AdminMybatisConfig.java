package com.homework.web.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.homework.common.mybatisplus.MybatisMetaObjectHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** 管理端 MyBatis Plus 与密码编码配置。 */
@Configuration
public class AdminMybatisConfig {

    @Bean
    public MybatisPlusInterceptor adminMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MybatisMetaObjectHandler adminMetaObjectHandler() {
        return new MybatisMetaObjectHandler();
    }

    @Bean
    public BCryptPasswordEncoder adminPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
