package com.homework.web.admin.service;

import java.util.List;
import java.util.Set;

/** 管理端允许分配的权限码目录。 */
public final class AdminPermissionCatalog {

    public static final List<String> ALL = List.of(
            "dashboard:view",
            "bank:view",
            "bank:create",
            "bank:update",
            "bank:publish",
            "bank:delete",
            "question:view",
            "question:create",
            "question:update",
            "question:publish",
            "question:delete",
            "question:sort",
            "question:import",
            "user:view",
            "user:manage",
            "user:ban",
            "community:moderate",
            "membership:view",
            "membership:manage",
            "membership:revoke",
            "membership:plan",
            "admin:manage",
            "audit:view"
    );

    public static final Set<String> SUPER_ONLY = Set.of(
            "user:ban",
            "membership:revoke",
            "membership:plan",
            "admin:manage"
    );

    private AdminPermissionCatalog() {
    }
}
