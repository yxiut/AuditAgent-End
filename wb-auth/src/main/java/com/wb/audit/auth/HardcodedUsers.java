package com.wb.audit.auth;

import com.wb.audit.auth.model.AuthUser;
import com.wb.audit.auth.model.RoleType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本期用户鉴权：用户名/用户ID 写死映射（5=张伟·审核员[主]、4=陈志强·被审核人[主]）。
 * 预留：后续接真实组织数据时替换为 sys_user/sys_user_role 查询。
 */
public final class HardcodedUsers {

    private HardcodedUsers() {
    }

    /** userId → 用户（assigneeId 用） */
    public static final Map<Long, AuthUser> BY_ID = new LinkedHashMap<>();
    /** 用户名 → 用户（X-User-Name 用） */
    public static final Map<String, AuthUser> BY_NAME = new LinkedHashMap<>();

    static {
        // 企微 userid 与真实账号绑定（演示版本固定；生产按任务参与者动态解析）
        // 保留 id 4/5 与库中演示任务 210（owner=5, assignee=4）一致，不重新编号
        register(new AuthUser(4L, "陈志强", RoleType.AUDITEE, "YangXiuTian")); // 被审核人 → 企微 YangXiuTian
        register(new AuthUser(5L, "张伟", RoleType.AUDITOR, "DuoLeGeDuo"));    // 审核员 → 企微 DuoLeGeDuo
    }

    private static void register(AuthUser u) {
        BY_ID.put(u.getUserId(), u);
        BY_NAME.put(u.getName(), u);
    }

    public static AuthUser byName(String name) {
        return BY_NAME.get(name);
    }

    public static AuthUser byId(Long id) {
        return BY_ID.get(id);
    }

    public static List<AuthUser> all() {
        return List.copyOf(BY_ID.values());
    }
}
