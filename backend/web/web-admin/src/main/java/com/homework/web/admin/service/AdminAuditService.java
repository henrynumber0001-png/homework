package com.homework.web.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.model.entity.AdminAccount;
import com.homework.model.entity.AdminOperationLog;
import com.homework.web.admin.config.AdminRequestIdFilter;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.mapper.AdminOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 记录后台业务写操作的审计摘要。 */
@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminOperationLogMapper logMapper;
    private final ObjectMapper objectMapper;

    public void record(
            String module,
            String action,
            String targetType,
            Object targetId,
            String reason,
            Object before,
            Object after
    ) {
        AdminAccount admin = AdminContext.get();
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();

        AdminOperationLog log = new AdminOperationLog();
        log.setRequestId(request == null
                ? "internal"
                : String.valueOf(request.getAttribute(AdminRequestIdFilter.ATTRIBUTE)));
        log.setOperatorAdminId(admin == null ? 0L : admin.getId());
        log.setOperatorName(admin == null ? "system" : admin.getDisplayName());
        log.setModule(module);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId == null ? null : String.valueOf(targetId));
        log.setReason(reason);
        log.setBeforeSnapshot(toJson(before));
        log.setAfterSnapshot(toJson(after));
        log.setSuccess(true);
        log.setIp(request == null ? null : request.getRemoteAddr());
        log.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        logMapper.insert(log);
    }

    public void recordFailure(String failureMessage) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        if (request == null || "GET".equalsIgnoreCase(request.getMethod())) {
            return;
        }
        AdminAccount admin = AdminContext.get();
        AdminOperationLog log = new AdminOperationLog();
        log.setRequestId(String.valueOf(request.getAttribute(AdminRequestIdFilter.ATTRIBUTE)));
        log.setOperatorAdminId(admin == null ? 0L : admin.getId());
        log.setOperatorName(admin == null ? "anonymous" : admin.getDisplayName());
        log.setModule("REQUEST");
        log.setAction(request.getMethod());
        log.setTargetType("HTTP_ENDPOINT");
        log.setTargetId(request.getRequestURI());
        log.setSuccess(false);
        log.setFailureMessage(failureMessage == null
                ? "unknown"
                : failureMessage.substring(0, Math.min(failureMessage.length(), 500)));
        log.setIp(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        logMapper.insert(log);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{\"summary\":\"unavailable\"}";
        }
    }
}
