package com.homework.web.admin.auth;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.admin.context.AdminContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 保存五分钟内可使用一次的高风险操作二次认证令牌。 */
@Service
public class AdminReauthService {

    private final Map<String, ReauthEntry> entries = new ConcurrentHashMap<>();

    public String create(String scope) {
        String token = UUID.randomUUID().toString();
        entries.put(token, new ReauthEntry(AdminContext.getAdminId(), scope, LocalDateTime.now().plusMinutes(5)));
        return token;
    }

    public void consume(String token, String scope) {
        ReauthEntry entry = entries.remove(token);
        if (entry == null
                || !entry.adminId().equals(AdminContext.getAdminId())
                || !entry.scope().equals(scope)
                || entry.expiresTime().isBefore(LocalDateTime.now())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_REAUTH_INVALID);
        }
    }

    private record ReauthEntry(Long adminId, String scope, LocalDateTime expiresTime) {
    }
}
