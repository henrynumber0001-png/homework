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
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    //对应 “管理员” 菜单
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
            query.and(wrapper -> wrapper
                    .like(AdminAccount::getEmail, keyword.trim())
                    .or()
                    .like(AdminAccount::getDisplayName, keyword.trim()));
        }

        query.eq(status != null, AdminAccount::getStatus, status);
        query.orderByAsc(AdminAccount::getRole).orderByDesc(AdminAccount::getCreatedTime);

        //这里使用了分页查询
        Page<AdminAccount> page = accountMapper.selectPage(new Page<>(normalizedPage, normalizedSize), query);
        PageResult<AdminRowVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(this::toRow).toList()); //page.getRecords() 就是 List<AdminAccount>
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

        //先把 超级管理员勾选的 管理员账户权限 以字符串集合的形式 输出第一版
        List<String> permissions = new ArrayList<>(new LinkedHashSet<>(dto.getPermissions()));

        if (!AdminPermissionCatalog.ALL.containsAll(permissions)
                || permissions.stream().anyMatch(AdminPermissionCatalog.SUPER_ONLY::contains)) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_PERMISSION_DENIED);
        }
        BankDataScope bankDataScope = dto.getBankDataScope();
        List<Long> bankIds = dto.getAssignedBankIds() == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(dto.getAssignedBankIds()));

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
            //然后把 字符串集合 序列化为 Json字符串 存入 admin_invitation 表
            //用的是 writeValue
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

        //生成邮件链接
        //get invitation 字段，但是 invitation 字段的类型是一个内部类，也就是 getInvitation() 的返回值类型是 Invitation类的对象
        //然后再利用这个对象，调用 getter 方法获取 publicBaseUrl 字段的返回值，这个返回值就是从 application.yml 配置文件中读取的值
        String baseUrl = featureProperties.getInvitation().getPublicBaseUrl();
        String invitationUrl = baseUrl + (baseUrl.contains("?") ? "&token=" : "?token=") + rawToken;
        AdminInvitationCreateVO result = new AdminInvitationCreateVO();
        result.setEmail(email);
        result.setInvitationUrl(invitationUrl);
        result.setExpiresTime(expiresTime);

        auditService.record("ADMIN", "INVITE", "ADMIN_INVITATION", invitation.getId(), dto.getReason(), null, invitation);

        /*
         * 此处只发布应用事件，不直接调用 SES。监听器使用 AFTER_COMMIT，只有邀请和审计日志
         * 都成功提交后才会异步发送邮件；事务回滚时事件不会触发，收件人也不会收到无效链接。
         */

        //AdminManagementService 不再直接调用邮件服务，而是发布一个“管理员邀请已创建”的事件：ApplicationEventPublisher eventPublisher

        /*  监听逻辑
            invite()
              ↓
            保存邀请
              ↓
            保存审计失败
              ↓
            数据库回滚
              ↓
            Spring 不调用监听器
              ↓
            try 和 catch 都不执行
         */
        /*
        数据库成功，邮件进入进入 try
               ↓
            执行邮件发送
               ↓
            是否抛出异常？
               ├─ 否 → 跳过 catch，方法结束
               └─ 是 → 停止 try 的剩余代码，进入 catch
         */
        AdminInvitationCreatedEvent adminInvitationCreatedEvent = new AdminInvitationCreatedEvent(
                invitation.getId(), email, dto.getDisplayName().trim(), rawToken, expiresTime);

        //Spring 使用类似 ThreadLocal 的机制，把事务资源绑定到执行请求的线程。
        //执行eventPublisher.publishEvent() 时，仍处于 invite() 方法内，也仍然使用原来的请求线程。
        //把监听器注册为 当前线程 的 afterCommit 回调
        //线程提交成功，执行监听中的回调
        eventPublisher.publishEvent(adminInvitationCreatedEvent);
        return result;
    }
    /*
        事务 T1 开始
          ↓
        执行 invite()
          ↓
        publishEvent(event)
          ↓
        发现当前线程绑定着 T1
          ↓
        把监听器注册为 T1 的 afterCommit 回调
          ↓
        invite() 返回
          ↓
        Spring 提交 T1
          ↓
        T1 提交成功
          ↓
        执行该回调
     */

    @Transactional
    public AdminRowVO updateAccess(Long adminId, AdminAccessUpdateDTO dto) {
        AdminAccount target = accountMapper.selectById(adminId);
        if (target == null || target.getRole() == AdminRole.SUPER_ADMIN || target.getStatus() == AdminStatus.ARCHIVED) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_ACCOUNT_NOT_FOUND);
        }
        if (!target.getVersion().equals(dto.getVersion())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }
        List<String> permissions = new ArrayList<>(new LinkedHashSet<>(dto.getPermissions()));
        if (!AdminPermissionCatalog.ALL.containsAll(permissions) || permissions.stream().anyMatch(AdminPermissionCatalog.SUPER_ONLY::contains)) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_PERMISSION_DENIED);
        }
        BankDataScope scope = dto.getBankDataScope();
        List<Long> bankIds = dto.getAssignedBankIds() == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(dto.getAssignedBankIds()));
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
        if (target == null || target.getRole() == AdminRole.SUPER_ADMIN || !target.getVersion().equals(dto.getVersion())) {
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
        vo.setPermissions(account.getRole() == AdminRole.SUPER_ADMIN ? AdminPermissionCatalog.ALL : accessService.listPermissions(account.getId()));
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
