package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.AdminAccount;
import com.homework.model.entity.AdminAccountPermission;
import com.homework.model.entity.AdminBankScope;
import com.homework.model.entity.AdminInvitation;
import com.homework.model.entity.AdminSession;
import com.homework.model.enums.AdminRole;
import com.homework.model.enums.AdminStatus;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.auth.AdminJwtService;
import com.homework.web.admin.auth.AdminReauthService;
import com.homework.web.admin.auth.TokenDigestService;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.dto.AdminInvitationAcceptDTO;
import com.homework.web.admin.dto.AdminLoginDTO;
import com.homework.web.admin.dto.AdminPasswordChangeDTO;
import com.homework.web.admin.dto.AdminReauthDTO;
import com.homework.web.admin.mapper.AdminAccountMapper;
import com.homework.web.admin.mapper.AdminAccountPermissionMapper;
import com.homework.web.admin.mapper.AdminBankScopeMapper;
import com.homework.web.admin.mapper.AdminInvitationMapper;
import com.homework.web.admin.mapper.AdminSessionMapper;
import com.homework.web.admin.vo.AdminInvitationPreviewVO;
import com.homework.web.admin.vo.AdminLoginVO;
import com.homework.web.admin.vo.AdminReauthVO;
import com.homework.web.admin.vo.AdminSummaryVO;
import com.homework.web.admin.vo.CurrentAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** 管理员邀请接受、登录、会话和密码业务。 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminAccountMapper accountMapper;
    private final AdminInvitationMapper invitationMapper;
    private final AdminAccountPermissionMapper permissionMapper;
    private final AdminBankScopeMapper bankScopeMapper;
    private final AdminSessionMapper sessionMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AdminJwtService jwtService;
    private final TokenDigestService tokenDigestService;
    private final AdminAccessService accessService;
    private final AdminReauthService reauthService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AdminLoginVO login(AdminLoginDTO dto) {
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        AdminAccount admin = accountMapper.selectOne(new LambdaQueryWrapper<AdminAccount>()
                .eq(AdminAccount::getEmail, email));
        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPasswordHash())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_CREDENTIALS_INVALID);
        }
        if (admin.getStatus() != AdminStatus.ACTIVE) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_ACCOUNT_UNAVAILABLE);
        }

        String sessionKey = UUID.randomUUID().toString();
        LocalDateTime expiresTime = LocalDateTime.now().plusSeconds(jwtService.getTtlSeconds());
        AdminSession session = new AdminSession();
        session.setSessionKey(sessionKey);
        session.setAdminId(admin.getId());
        session.setExpiresTime(expiresTime);
        sessionMapper.insert(session);

        admin.setLastLoginTime(LocalDateTime.now());
        accountMapper.updateById(admin);

        AdminLoginVO vo = new AdminLoginVO();
        vo.setAccessToken(jwtService.createToken(
                admin.getId(),
                admin.getEmail(),
                sessionKey,
                admin.getSessionVersion()
        ));
        vo.setTokenType("Bearer");
        vo.setExpiresInSeconds(jwtService.getTtlSeconds());
        vo.setAdmin(toSummary(admin));
        vo.setPermissions(admin.getRole() == AdminRole.SUPER_ADMIN
                ? AdminPermissionCatalog.ALL
                : accessService.listPermissions(admin.getId()));
        vo.setBankDataScope(admin.getBankDataScope());
        return vo;
    }

    public AdminInvitationPreviewVO previewInvitation(String token) {
        AdminInvitation invitation = invitationMapper.selectOne(new LambdaQueryWrapper<AdminInvitation>()
                .eq(AdminInvitation::getTokenDigest, tokenDigestService.sha256(token)));
        AdminInvitationPreviewVO vo = new AdminInvitationPreviewVO();
        if (invitation == null) {
            vo.setValid(false);
            return vo;
        }
        int at = invitation.getEmail().indexOf('@');
        String masked = at <= 1
                ? "***" + invitation.getEmail().substring(Math.max(at, 0))
                : invitation.getEmail().charAt(0) + "***" + invitation.getEmail().substring(at);
        vo.setEmailMasked(masked);
        vo.setDisplayName(invitation.getDisplayName());
        vo.setExpiresTime(invitation.getExpiresTime());
        vo.setValid(invitation.getAcceptedTime() == null
                && invitation.getExpiresTime().isAfter(LocalDateTime.now()));
        return vo;
    }

    @Transactional
    public void acceptInvitation(String token, AdminInvitationAcceptDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        AdminInvitation invitation = invitationMapper.selectOne(new LambdaQueryWrapper<AdminInvitation>()
                .eq(AdminInvitation::getTokenDigest, tokenDigestService.sha256(token)));
        if (invitation == null
                || invitation.getAcceptedTime() != null
                || invitation.getExpiresTime().isBefore(LocalDateTime.now())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_INVITATION_INVALID);
        }
        Long existing = accountMapper.selectCount(new LambdaQueryWrapper<AdminAccount>()
                .eq(AdminAccount::getEmail, invitation.getEmail()));
        if (existing > 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_ACCOUNT_CONFLICT);
        }

        AdminAccount account = new AdminAccount();
        account.setEmail(invitation.getEmail());
        account.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        account.setDisplayName(invitation.getDisplayName());
        account.setRole(AdminRole.STANDARD_ADMIN);
        account.setStatus(AdminStatus.ACTIVE);
        account.setBankDataScope(invitation.getBankDataScope());
        account.setSessionVersion(0);
        account.setBuiltIn(false);
        account.setVersion(0);
        accountMapper.insert(account);

        try {
            List<String> permissions = objectMapper.readValue(
                    invitation.getPermissionsJson(),
                    new TypeReference<>() {
                    }
            );
            for (String permissionCode : permissions) {
                AdminAccountPermission relation = new AdminAccountPermission();
                relation.setAdminId(account.getId());
                relation.setPermissionCode(permissionCode);
                permissionMapper.insert(relation);
            }
            List<Long> bankIds = objectMapper.readValue(
                    invitation.getBankIdsJson(),
                    new TypeReference<>() {
                    }
            );
            for (Long bankId : bankIds) {
                AdminBankScope relation = new AdminBankScope();
                relation.setAdminId(account.getId());
                relation.setBankId(bankId);
                bankScopeMapper.insert(relation);
            }
        } catch (Exception exception) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR, exception);
        }

        invitation.setAcceptedTime(LocalDateTime.now());
        invitationMapper.updateById(invitation);
    }

    public CurrentAdminVO currentAdmin() {
        AdminAccount admin = AdminContext.get();
        AdminSession session = sessionMapper.selectOne(new LambdaQueryWrapper<AdminSession>()
                .eq(AdminSession::getSessionKey, AdminContext.getSessionKey()));
        CurrentAdminVO vo = new CurrentAdminVO();
        vo.setAdmin(toSummary(admin));
        vo.setPermissions(admin.getRole() == AdminRole.SUPER_ADMIN
                ? AdminPermissionCatalog.ALL
                : accessService.listPermissions(admin.getId()));
        vo.setBankDataScope(admin.getBankDataScope());
        vo.setAssignedBankIds(accessService.listAssignedBankIds(admin.getId()));
        vo.setSessionExpiresTime(session.getExpiresTime());
        return vo;
    }

    public void logout() {
        AdminSession session = sessionMapper.selectOne(new LambdaQueryWrapper<AdminSession>()
                .eq(AdminSession::getSessionKey, AdminContext.getSessionKey()));
        if (session != null && session.getRevokedTime() == null) {
            session.setRevokedTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    @Transactional
    public String changePassword(AdminPasswordChangeDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        AdminAccount admin = AdminContext.get();
        if (!passwordEncoder.matches(dto.getCurrentPassword(), admin.getPasswordHash())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_CREDENTIALS_INVALID);
        }
        admin.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        admin.setSessionVersion(admin.getSessionVersion() + 1);
        accountMapper.updateById(admin);
        sessionMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AdminSession>()
                        .eq(AdminSession::getAdminId, admin.getId())
                        .ne(AdminSession::getSessionKey, AdminContext.getSessionKey())
                        .isNull(AdminSession::getRevokedTime)
                        .set(AdminSession::getRevokedTime, LocalDateTime.now())
        );
        return jwtService.createToken(
                admin.getId(),
                admin.getEmail(),
                AdminContext.getSessionKey(),
                admin.getSessionVersion()
        );
    }

    public AdminReauthVO reauthenticate(AdminReauthDTO dto) {
        AdminAccount admin = AdminContext.get();
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPasswordHash())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_CREDENTIALS_INVALID);
        }
        AdminReauthVO vo = new AdminReauthVO();
        vo.setReauthToken(reauthService.create(dto.getActionScope()));
        vo.setExpiresTime(LocalDateTime.now().plusMinutes(5));
        return vo;
    }

    private AdminSummaryVO toSummary(AdminAccount admin) {
        AdminSummaryVO vo = new AdminSummaryVO();
        vo.setId(admin.getId());
        vo.setEmail(admin.getEmail());
        vo.setDisplayName(admin.getDisplayName());
        vo.setRole(admin.getRole());
        vo.setStatus(admin.getStatus());
        return vo;
    }
}
