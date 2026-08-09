package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.PageResult;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.AdminAccount;
import com.homework.model.entity.AdminAccountPermission;
import com.homework.model.entity.AdminBankScope;
import com.homework.model.entity.AdminInvitation;
import com.homework.model.entity.AdminSession;
import com.homework.model.enums.AdminRole;
import com.homework.model.enums.AdminStatus;
import com.homework.model.enums.BankDataScope;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.auth.TokenDigestService;
import com.homework.web.admin.config.AdminFeatureProperties;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.dto.AdminAccountActionDTO;
import com.homework.web.admin.dto.AdminAccessUpdateDTO;
import com.homework.web.admin.dto.AdminInvitationCreateDTO;
import com.homework.model.enums.AdminAccountAction;
import com.homework.web.admin.mapper.AdminAccountMapper;
import com.homework.web.admin.mapper.AdminAccountPermissionMapper;
import com.homework.web.admin.mapper.AdminBankScopeMapper;
import com.homework.web.admin.mapper.AdminInvitationMapper;
import com.homework.web.admin.mapper.AdminSessionMapper;
import com.homework.web.admin.mapper.QuestionBankMapper;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.AdminInvitationCreateVO;
import com.homework.web.admin.vo.AdminRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** 超级管理员维护普通管理员邀请、权限、题库范围和状态。 */
@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private final AdminAccountMapper accountMapper;
    private final AdminInvitationMapper invitationMapper;
    private final AdminAccountPermissionMapper permissionMapper;
    private final AdminBankScopeMapper bankScopeMapper;
    private final AdminSessionMapper sessionMapper;
    private final QuestionBankMapper bankMapper;
    private final AdminAccessService accessService;
    private final TokenDigestService tokenDigestService;
    private final AdminFeatureProperties featureProperties;
    private final ObjectMapper objectMapper;
    private final AdminAuditService auditService;

    public PageResult<AdminRowVO> list(
            String keyword,
            AdminStatus status,
            Integer pageNum,
            Integer pageSize
    ) {
        int normalizedPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int normalizedSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<AdminAccount> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper.like(AdminAccount::getEmail, keyword.trim())
                    .or().like(AdminAccount::getDisplayName, keyword.trim()));
        }
        query.eq(status != null, AdminAccount::getStatus, status);
        query.orderByAsc(AdminAccount::getRole).orderByDesc(AdminAccount::getCreatedTime);
        Page<AdminAccount> page = accountMapper.selectPage(new Page<>(normalizedPage, normalizedSize), query);
        PageResult<AdminRowVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(this::toRow).toList());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        return result;
    }

    @Transactional
    public AdminInvitationCreateVO invite(AdminInvitationCreateDTO dto) {
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        Long accountCount = accountMapper.selectCount(
                new LambdaQueryWrapper<AdminAccount>().eq(AdminAccount::getEmail, email));
        if (accountCount > 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_ACCOUNT_CONFLICT);
        }
        List<String> permissions = new ArrayList<>(new LinkedHashSet<>(dto.getPermissions()));
        if (!AdminPermissionCatalog.ALL.containsAll(permissions)
                || permissions.stream().anyMatch(AdminPermissionCatalog.SUPER_ONLY::contains)) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_PERMISSION_DENIED);
        }
        BankDataScope bankDataScope = dto.getBankDataScope();
        List<Long> bankIds = dto.getAssignedBankIds() == null
                ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(dto.getAssignedBankIds()));
        if (bankDataScope == BankDataScope.ASSIGNED_BANKS && bankIds.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        for (Long bankId : bankIds) {
            if (bankMapper.selectById(bankId) == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
            }
        }
        String rawToken = UUID.randomUUID().toString();
        LocalDateTime expiresTime = LocalDateTime.now().plusHours(24);
        AdminInvitation invitation = invitationMapper.selectOne(
                new LambdaQueryWrapper<AdminInvitation>()
                        .eq(AdminInvitation::getEmail, email)
                        .isNull(AdminInvitation::getAcceptedTime)
                        .gt(AdminInvitation::getExpiresTime, LocalDateTime.now())
                        .orderByDesc(AdminInvitation::getId)
                        .last("LIMIT 1"));
        if (invitation == null) {
            invitation = new AdminInvitation();
            invitation.setEmail(email);
        }
        invitation.setDisplayName(dto.getDisplayName().trim());
        invitation.setTokenDigest(tokenDigestService.sha256(rawToken));
        invitation.setBankDataScope(bankDataScope);
        invitation.setExpiresTime(expiresTime);
        invitation.setInvitedByAdminId(AdminContext.getAdminId());
        try {
            invitation.setPermissionsJson(objectMapper.writeValueAsString(permissions));
            invitation.setBankIdsJson(objectMapper.writeValueAsString(
                    bankDataScope == BankDataScope.ALL_BANKS ? List.of() : bankIds));
        } catch (JsonProcessingException exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }
        if (invitation.getId() == null) {
            invitationMapper.insert(invitation);
        } else {
            invitationMapper.updateById(invitation);
        }
        String baseUrl = featureProperties.getInvitation().getPublicBaseUrl();
        AdminInvitationCreateVO result = new AdminInvitationCreateVO();
        result.setEmail(email);
        result.setInvitationUrl(baseUrl + (baseUrl.contains("?") ? "&token=" : "?token=") + rawToken);
        result.setExpiresTime(expiresTime);
        auditService.record("ADMIN", "INVITE", "ADMIN_INVITATION", invitation.getId(), dto.getReason(), null, invitation);
        return result;
    }

    @Transactional
    public AdminRowVO updateAccess(Long adminId, AdminAccessUpdateDTO dto) {
        AdminAccount target = accountMapper.selectById(adminId);
        if (target == null || target.getRole() == AdminRole.SUPER_ADMIN
                || target.getStatus() == AdminStatus.ARCHIVED) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_ACCOUNT_NOT_FOUND);
        }
        if (!target.getVersion().equals(dto.getVersion())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        List<String> permissions = new ArrayList<>(new LinkedHashSet<>(dto.getPermissions()));
        if (!AdminPermissionCatalog.ALL.containsAll(permissions)
                || permissions.stream().anyMatch(AdminPermissionCatalog.SUPER_ONLY::contains)) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_PERMISSION_DENIED);
        }
        BankDataScope scope = dto.getBankDataScope();
        List<Long> bankIds = dto.getAssignedBankIds() == null
                ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(dto.getAssignedBankIds()));
        if (scope == BankDataScope.ASSIGNED_BANKS && bankIds.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        for (Long bankId : bankIds) {
            if (bankMapper.selectById(bankId) == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
            }
        }
        AdminRowVO before = toRow(target);
        target.setBankDataScope(scope);
        target.setSessionVersion(target.getSessionVersion() + 1);
        if (accountMapper.updateById(target) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        permissionMapper.delete(new LambdaQueryWrapper<AdminAccountPermission>()
                .eq(AdminAccountPermission::getAdminId, adminId));
        for (String permission : permissions) {
            AdminAccountPermission relation = new AdminAccountPermission();
            relation.setAdminId(adminId);
            relation.setPermissionCode(permission);
            permissionMapper.insert(relation);
        }
        bankScopeMapper.delete(new LambdaQueryWrapper<AdminBankScope>()
                .eq(AdminBankScope::getAdminId, adminId));
        if (scope == BankDataScope.ASSIGNED_BANKS) {
            for (Long bankId : bankIds) {
                AdminBankScope relation = new AdminBankScope();
                relation.setAdminId(adminId);
                relation.setBankId(bankId);
                bankScopeMapper.insert(relation);
            }
        }
        sessionMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AdminSession>()
                        .eq(AdminSession::getAdminId, adminId)
                        .isNull(AdminSession::getRevokedTime)
                        .set(AdminSession::getRevokedTime, LocalDateTime.now())
        );
        AdminRowVO updated = toRow(accountMapper.selectById(adminId));
        auditService.record("ADMIN", "UPDATE_ACCESS", "ADMIN_ACCOUNT", adminId, dto.getReason(), before, updated);
        return updated;
    }

    @Transactional
    public ActionResultVO action(Long adminId, AdminAccountActionDTO dto) {
        AdminAccount target = accountMapper.selectById(adminId);
        if (target == null || target.getRole() == AdminRole.SUPER_ADMIN
                || !target.getVersion().equals(dto.getVersion())) {
            if (target != null && target.getRole() != AdminRole.SUPER_ADMIN) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }
            throw new HomeworkException(ResultCodeEnum.ADMIN_ACCOUNT_NOT_FOUND);
        }
        AdminAccountAction action = dto.getAction();
        AdminStatus targetStatus;
        if (action == AdminAccountAction.DISABLE && target.getStatus() == AdminStatus.ACTIVE) {
            targetStatus = AdminStatus.DISABLED;
        } else if (action == AdminAccountAction.ACTIVATE && target.getStatus() == AdminStatus.DISABLED) {
            targetStatus = AdminStatus.ACTIVE;
        } else if (action == AdminAccountAction.ARCHIVE
                && (target.getStatus() == AdminStatus.ACTIVE || target.getStatus() == AdminStatus.DISABLED)) {
            targetStatus = AdminStatus.ARCHIVED;
        } else {
            throw new HomeworkException(ResultCodeEnum.ADMIN_ACCOUNT_CONFLICT);
        }
        AdminRowVO before = toRow(target);
        target.setStatus(targetStatus);
        target.setSessionVersion(target.getSessionVersion() + 1);
        if (accountMapper.updateById(target) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        sessionMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AdminSession>()
                        .eq(AdminSession::getAdminId, adminId)
                        .isNull(AdminSession::getRevokedTime)
                        .set(AdminSession::getRevokedTime, LocalDateTime.now())
        );
        AdminAccount updated = accountMapper.selectById(adminId);
        auditService.record("ADMIN", action.name(), "ADMIN_ACCOUNT", adminId, dto.getReason(), before, toRow(updated));
        ActionResultVO result = new ActionResultVO();
        result.setTargetId(adminId);
        result.setAction(action.getCode());
        result.setStatus(updated.getStatus().getCode());
        result.setVersion(updated.getVersion());
        result.setUpdatedTime(updated.getUpdatedTime());
        return result;
    }

    public AdminRowVO toRow(AdminAccount account) {
        AdminRowVO vo = new AdminRowVO();
        vo.setId(account.getId());
        vo.setEmail(account.getEmail());
        vo.setDisplayName(account.getDisplayName());
        vo.setRole(account.getRole());
        vo.setStatus(account.getStatus());
        vo.setPermissions(account.getRole() == AdminRole.SUPER_ADMIN
                ? AdminPermissionCatalog.ALL
                : accessService.listPermissions(account.getId()));
        vo.setBankDataScope(account.getBankDataScope());
        vo.setAssignedBankIds(account.getRole() == AdminRole.SUPER_ADMIN
                || account.getBankDataScope() == BankDataScope.ALL_BANKS
                ? List.of()
                : accessService.listAssignedBankIds(account.getId()));
        vo.setLastLoginTime(account.getLastLoginTime());
        vo.setVersion(account.getVersion());
        return vo;
    }
}
