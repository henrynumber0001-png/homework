package com.homework.web.admin.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.model.entity.AdminAccount;
import com.homework.model.enums.AdminRole;
import com.homework.model.enums.AdminStatus;
import com.homework.model.enums.BankDataScope;
import com.homework.web.admin.mapper.AdminAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/** 首次启动时按环境变量初始化唯一内置超级管理员。 */
@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements CommandLineRunner {

    private final AdminFeatureProperties properties;
    private final AdminAccountMapper accountMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        AdminFeatureProperties.SuperAdmin config = properties.getSuperAdmin();
        if (!StringUtils.hasText(config.getEmail()) || !StringUtils.hasText(config.getPassword())) {
            return;
        }
        Long count = accountMapper.selectCount(new LambdaQueryWrapper<AdminAccount>()
                .eq(AdminAccount::getRole, AdminRole.SUPER_ADMIN));
        if (count > 0) {
            return;
        }
        AdminAccount account = new AdminAccount();
        account.setEmail(config.getEmail().trim().toLowerCase(Locale.ROOT));
        account.setPasswordHash(passwordEncoder.encode(config.getPassword()));
        account.setDisplayName(config.getDisplayName());
        account.setRole(AdminRole.SUPER_ADMIN);
        account.setStatus(AdminStatus.ACTIVE);
        account.setBankDataScope(BankDataScope.ALL_BANKS);
        account.setSessionVersion(0);
        account.setBuiltIn(true);
        account.setVersion(0);
        accountMapper.insert(account);
    }
}
