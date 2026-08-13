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

/**
 * 管理员认证领域服务。
 *
 * <p>这个类负责串联账号、邀请、权限、数据范围、服务端会话和 JWT，核心设计是：</p>
 * <ul>
 *     <li>JWT 只负责携带并证明身份，会话是否仍有效以数据库中的 {@link AdminSession} 为准；</li>
 *     <li>权限和题库范围保存在独立关系表中，不塞进 JWT，避免授权变更后必须等待 Token 过期；</li>
 *     <li>邀请、密码修改等跨多张表的操作放在事务中，保证业务状态要么全部成功、要么全部回滚；</li>
 *     <li>密码只保存 BCrypt 哈希，邀请 Token 只保存 SHA-256 摘要，降低数据库泄露后的风险。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    // Mapper 只负责持久化；本类负责安排调用顺序并维护认证领域的不变量。
    private final AdminAccountMapper accountMapper;
    private final AdminInvitationMapper invitationMapper;
    private final AdminAccountPermissionMapper permissionMapper;
    private final AdminBankScopeMapper bankScopeMapper;
    private final AdminSessionMapper sessionMapper;

    // 安全能力分别封装，避免在业务流程中散落密码算法、JWT 细节和授权查询规则。
    private final BCryptPasswordEncoder passwordEncoder;
    private final AdminJwtService jwtService;
    private final TokenDigestService tokenDigestService;
    private final AdminAccessService accessService;
    private final AdminReauthService reauthService;
    private final ObjectMapper objectMapper;

    /**
     * 校验账号凭证并创建一个可由服务端主动撤销的登录会话。
     *
     * <p>登录同时写会话和最后登录时间，因此使用事务保持两项状态一致。</p>
     */
    @Transactional
    public AdminLoginVO login(AdminLoginDTO dto) {
        // 邮箱是登录账号：查询前统一大小写和首尾空白，避免同一邮箱出现多种表示。
        String email = dto.getEmail().trim().toLowerCase(Locale.ROOT);
        AdminAccount admin = accountMapper.selectOne(new LambdaQueryWrapper<AdminAccount>()
                .eq(AdminAccount::getEmail, email));

        // 账号不存在和密码错误返回同一种错误，避免攻击者据此枚举已注册的管理员邮箱。
        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPasswordHash())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_CREDENTIALS_INVALID);
        }
        // 凭证正确不代表允许登录；禁用或其他非 ACTIVE 状态仍应被拦截。
        if (admin.getStatus() != AdminStatus.ACTIVE) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_ACCOUNT_UNAVAILABLE);
        }

        /*
         * JWT 本身一经签发无法主动删除，所以额外建立服务端会话：
         * Token 中只携带 sessionKey，后续请求还必须通过拦截器确认该会话未过期、未撤销。
         */
        String sessionKey = UUID.randomUUID().toString();
        LocalDateTime expiresTime = LocalDateTime.now().plusSeconds(jwtService.getTtlSeconds());
        AdminSession session = new AdminSession();
        session.setSessionKey(sessionKey);
        session.setAdminId(admin.getId());
        session.setExpiresTime(expiresTime);
        sessionMapper.insert(session);

        admin.setLastLoginTime(LocalDateTime.now());
        accountMapper.updateById(admin);

        // 返回前端初始化后台所需的完整认证快照，避免登录后再发多次请求。
        AdminLoginVO vo = new AdminLoginVO();
        vo.setAccessToken(jwtService.createToken(
                admin.getId(),
                admin.getEmail(),
                sessionKey,
                // 版本号用于批量作废该账号此前签发的 Token，例如修改密码后令旧 Token 失效。
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

    /*
    AdminManagementService 接收邀请参数
            ↓
        生成随机邀请 Token
            ↓
    创建或更新 AdminInvitation（把邀请信息存入，不是一定要邀请成功，是所有邀请记录）
            ↓
    将操作权限和题库ID 转成 JSON 保存到 AdminInvitation
            ↓
        生成完整邀请链接
            ↓
    返回 AdminInvitationCreateVO
            ↓
    前端取得 invitationUrl
            ↓
        复制到剪贴板
            ↓
    超级管理员手动发给被邀请人

    点击 invitationUrl
            ↓
    进入 /admin/invitation?token=xxx
            ↓
    加载 InvitationView.vue
            ↓
    自动调用 previewInvitation(token)
            ↓
    邀请有效：展示设置密码表单
    邀请无效：展示“邀请无效或已过期”
            ↓
    填写密码，点击“激活管理员账号”
            ↓
    调用 acceptInvitation(token, form)
            ↓
    创建管理员账号和权限关系
            ↓
    跳转到 /login
            ↓
    使用邀请邮箱和新密码登录
     */
    public AdminInvitationPreviewVO previewInvitation(String token) {
        // 前端传来的rowToken，经过sha256计算之后，看看在 AdminInvitation 表中是否存在
        AdminInvitation invitation = invitationMapper.selectOne(new LambdaQueryWrapper<AdminInvitation>()
                .eq(AdminInvitation::getTokenDigest, tokenDigestService.sha256(token)));
        AdminInvitationPreviewVO vo = new AdminInvitationPreviewVO();
        if (invitation == null) {
            vo.setValid(false);
            return vo;
        }
        int at = invitation.getEmail().indexOf('@');

        String masked = at <= 1 ? "***" + invitation.getEmail().substring(Math.max(at, 0)) : invitation.getEmail().charAt(0) + "***" + invitation.getEmail().substring(at);
        vo.setEmailMasked(masked);
        vo.setDisplayName(invitation.getDisplayName());
        vo.setExpiresTime(invitation.getExpiresTime());
        vo.setValid(invitation.getAcceptedTime() == null && invitation.getExpiresTime().isAfter(LocalDateTime.now()));
        return vo;
    }

    /*
    创建邀请时
    List<String> / List<Long>
            ↓
        转成 JSON
            ↓
    暂存在 AdminInvitation
            ↓
    等待被邀请人接受
        接受邀请时
        读取 JSON
            ↓
    创建 AdminAccount，获得 adminId
            ↓
    权限写入 admin_account_permission
            ↓
    题库范围写入 admin_bank_scope
     */

    @Transactional
    public void acceptInvitation(String token, AdminInvitationAcceptDTO dto) {
        // DTO 校验负责格式和强度；两次输入的一致性属于本次业务操作的校验。
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 邀请必须同时满足：确实存在、尚未使用、尚未过期。
        AdminInvitation invitation = invitationMapper.selectOne(new LambdaQueryWrapper<AdminInvitation>()
                .eq(AdminInvitation::getTokenDigest, tokenDigestService.sha256(token)));
        if (invitation == null
                || invitation.getAcceptedTime() != null
                || invitation.getExpiresTime().isBefore(LocalDateTime.now())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_INVITATION_INVALID);
        }

        // 防止邀请发出后，同一邮箱已通过其他途径创建账号。
        Long existing = accountMapper.selectCount(new LambdaQueryWrapper<AdminAccount>()
                .eq(AdminAccount::getEmail, invitation.getEmail()));
        if (existing > 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_ACCOUNT_CONFLICT);
        }

        // 邀请只能创建普通管理员；超级管理员不能通过普通邀请链路产生。
        AdminAccount account = new AdminAccount();
        account.setEmail(invitation.getEmail());
        // 永远不落库明文密码，由 BCrypt 自动生成盐并保存不可逆哈希。
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
            /*
             * 邀请保存的是“发出邀请那一刻”的授权快照。接受时再拆成关系表，
             * 使后续鉴权可以按权限码、题库 ID 高效查询，也便于独立增删授权。
             */

            // 先把 permissionsJson 反序列化成 List<String>
            // 因为在 AdminPermissionCatalog 中，管理员权限是以 List<String> 形式存储到字段 ALL 中的
            List<String> permissions = objectMapper.readValue(
                    invitation.getPermissionsJson(),
                    new TypeReference<>() {
                    }
            );
            for (String permissionCode : permissions) {
                AdminAccountPermission permission = new AdminAccountPermission();
                permission.setAdminId(account.getId());
                permission.setPermissionCode(permissionCode);
                permissionMapper.insert(permission);
            }

            // 再把 bankIdsJson 反序列化成 List<Long>
            // 因为在 AdminInvitation 中，题库权限的选择是按照 bankId 装入 Json 的
            List<Long> bankIds = objectMapper.readValue(
                    invitation.getBankIdsJson(),
                    new TypeReference<>() {
                    }
            );
            for (Long bankId : bankIds) {
                AdminBankScope bankScope = new AdminBankScope();
                bankScope.setAdminId(account.getId());
                bankScope.setBankId(bankId);
                bankScopeMapper.insert(bankScope);
            }
        } catch (Exception exception) {
            // 统一转为领域异常；由于方法带事务，前面已插入的账号和关系也会一并回滚。
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR, exception);
        }

        // 最后标记已接受，使邀请具备一次性；若更新失败，事务会回滚前面的账号创建。
        invitation.setAcceptedTime(LocalDateTime.now());
        invitationMapper.updateById(invitation);
    }

    /**
     * 汇总当前请求对应的管理员身份、实时授权和会话到期时间。
     */
    public CurrentAdminVO currentAdmin() {
        // AdminAuthInterceptor 已验证 JWT 和服务端会话，并把账号、sessionKey 放入请求上下文。
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

    /**
     * 撤销当前设备对应的服务端会话；其他设备的会话不受影响。
     */
    public void logout() {
        AdminSession session = sessionMapper.selectOne(new LambdaQueryWrapper<AdminSession>()
                .eq(AdminSession::getSessionKey, AdminContext.getSessionKey()));
        // 保持操作幂等：重复退出不会报错，也不会重复改写撤销时间。
        if (session != null && session.getRevokedTime() == null) {
            session.setRevokedTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    /**
     * 修改当前管理员密码，撤销其他设备会话，并为当前会话签发新 Token。
     *
     * <p>保留当前会话能避免用户改密后立即被登出；版本号升级则保证当前旧 Token
     * 和所有其他旧 Token 都无法继续使用，只有返回的新 Token 匹配新版本。</p>
     */
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
        // 拦截器会比较 JWT 中的 ver 与账号版本；递增后，所有旧 JWT 立即失效。
        admin.setSessionVersion(admin.getSessionVersion() + 1);
        accountMapper.updateById(admin);

        // 显式撤销其他设备的服务端会话，既反映真实状态，也便于审计和会话管理。
        sessionMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AdminSession>()
                        .eq(AdminSession::getAdminId, admin.getId())
                        .ne(AdminSession::getSessionKey, AdminContext.getSessionKey())
                        .isNull(AdminSession::getRevokedTime)
                        .set(AdminSession::getRevokedTime, LocalDateTime.now())
        );

        // 当前 sessionKey 不变，但 Token 携带升级后的版本号，因此当前设备可以无缝续用。
        return jwtService.createToken(
                admin.getId(),
                admin.getEmail(),
                AdminContext.getSessionKey(),
                admin.getSessionVersion()
        );
    }

    /**
     * 用当前密码完成一次“升格认证”，签发五分钟内有效且绑定操作范围的一次性令牌。
     *
     * <p>登录态只能说明会话有效；删除、重置等高风险动作前再次验密，可以降低设备
     * 离席或登录态被短暂窃取时的风险。</p>
     */
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

    /** 将账号实体转换为对外暴露的安全摘要，避免直接返回含密码哈希等内部字段的实体。 */
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
