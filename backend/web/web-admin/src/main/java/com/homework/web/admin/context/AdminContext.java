package com.homework.web.admin.context;

import com.homework.model.entity.AdminAccount;

/** 保存当前请求已认证的管理员。 */
public final class AdminContext {

    private static final ThreadLocal<AdminAccount> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<String> SESSION_KEY = new ThreadLocal<>();

    private AdminContext() {
    }

    public static void set(AdminAccount admin, String sessionKey) {
        CURRENT.set(admin);
        SESSION_KEY.set(sessionKey);
    }

    public static AdminAccount get() {
        return CURRENT.get();
    }

    public static Long getAdminId() {
        AdminAccount admin = CURRENT.get();
        return admin == null ? null : admin.getId();
    }

    public static String getSessionKey() {
        return SESSION_KEY.get();
    }

    public static void clear() {
        CURRENT.remove();
        SESSION_KEY.remove();
    }
}
