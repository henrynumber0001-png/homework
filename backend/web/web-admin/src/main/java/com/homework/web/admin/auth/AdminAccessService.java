package com.homework.web.admin.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.AdminAccount;
import com.homework.model.entity.AdminAccountPermission;
import com.homework.model.entity.AdminBankScope;
import com.homework.model.enums.AdminRole;
import com.homework.model.enums.BankDataScope;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.mapper.AdminAccountPermissionMapper;
import com.homework.web.admin.mapper.AdminBankScopeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 统一执行管理端功能权限与题库数据范围校验。 */
@Service
@RequiredArgsConstructor
public class AdminAccessService {

    private final AdminAccountPermissionMapper permissionMapper;
    private final AdminBankScopeMapper bankScopeMapper;

    public void requirePermission(String permissionCode) {
        if (!hasPermission(permissionCode)) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_PERMISSION_DENIED);
        }
    }

    public boolean hasPermission(String permissionCode) {

        //先找到管理员账号
        AdminAccount admin = AdminContext.get();
        if (admin == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_NOT_AUTHENTICATED);
        }
        if (admin.getRole() == AdminRole.SUPER_ADMIN) {
            return true;
        }
        //查询 管理员权限表
        //count 大于0，说明有这个权限
        Long count = permissionMapper.selectCount(new LambdaQueryWrapper<AdminAccountPermission>()
                .eq(AdminAccountPermission::getAdminId, admin.getId())
                .eq(AdminAccountPermission::getPermissionCode, permissionCode));
        return count > 0;
    }

    public void requireAnyPermission(String... permissionCodes) {
        for (String permissionCode : permissionCodes) {
            if (hasPermission(permissionCode)) {
                return;
            }
        }
        throw new HomeworkException(ResultCodeEnum.ADMIN_PERMISSION_DENIED);
    }

    public void requireBank(Long bankId) {
        AdminAccount admin = AdminContext.get();
        if (admin == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_NOT_AUTHENTICATED);
        }
        if (admin.getRole() == AdminRole.SUPER_ADMIN || admin.getBankDataScope() == BankDataScope.ALL_BANKS) {
            return;
        }
        Long count = bankScopeMapper.selectCount(new LambdaQueryWrapper<AdminBankScope>()
                .eq(AdminBankScope::getAdminId, admin.getId())
                .eq(AdminBankScope::getBankId, bankId));
        if (count == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_SCOPE_DENIED);
        }
    }

    public List<String> listPermissions(Long adminId) {
        return permissionMapper.selectList(new LambdaQueryWrapper<AdminAccountPermission>()
                        .eq(AdminAccountPermission::getAdminId, adminId)
                        .orderByAsc(AdminAccountPermission::getPermissionCode))
                .stream()
                .map(AdminAccountPermission::getPermissionCode)
                .toList();
    }

    public List<Long> listAssignedBankIds(Long adminId) {
        return bankScopeMapper.selectList(new LambdaQueryWrapper<AdminBankScope>()
                        .eq(AdminBankScope::getAdminId, adminId)
                        .orderByAsc(AdminBankScope::getBankId))
                .stream()
                .map(AdminBankScope::getBankId)
                .toList();
    }
}
