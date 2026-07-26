package com.homework.web.admin.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.AdminAccount;
import com.homework.model.entity.AdminSession;
import com.homework.model.enums.AdminStatus;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.mapper.AdminAccountMapper;
import com.homework.web.admin.mapper.AdminSessionMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/** 校验 Admin Token、管理员状态和接口权限。 */
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminJwtService jwtService;
    private final AdminAccountMapper accountMapper;
    private final AdminSessionMapper sessionMapper;
    private final AdminAccessService accessService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_NOT_AUTHENTICATED);
        }

        try {
            Claims claims = jwtService.parse(authorization.substring(7));
            Long adminId = Long.valueOf(claims.getSubject());
            String sessionKey = claims.get("sid", String.class);
            Integer sessionVersion = claims.get("ver", Integer.class);
            AdminAccount admin = accountMapper.selectById(adminId);
            AdminSession session = sessionMapper.selectOne(new LambdaQueryWrapper<AdminSession>()
                    .eq(AdminSession::getSessionKey, sessionKey)
                    .eq(AdminSession::getAdminId, adminId));
            if (admin == null || admin.getStatus() != AdminStatus.ACTIVE) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_ACCOUNT_UNAVAILABLE);
            }
            if (session == null
                    || session.getRevokedTime() != null
                    || session.getExpiresTime().isBefore(LocalDateTime.now())
                    || !admin.getSessionVersion().equals(sessionVersion)) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_SESSION_REVOKED);
            }
            AdminContext.set(admin, sessionKey);
        } catch (HomeworkException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_NOT_AUTHENTICATED, exception);
        }

        if (handler instanceof HandlerMethod handlerMethod) {
            AdminPermission permission = handlerMethod.getMethodAnnotation(AdminPermission.class);
            if (permission == null) {
                permission = handlerMethod.getBeanType().getAnnotation(AdminPermission.class);
            }
            if (permission != null) {
                accessService.requirePermission(permission.value());
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminContext.clear();
    }
}
