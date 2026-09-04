package com.wb.audit.auth.service;

import com.wb.audit.auth.model.AuthUser;

import java.util.List;
import java.util.Map;

/**
 * 认证与权限服务（本期写死映射）
 */
public interface AuthService {

    /** 发布权预检 */
    Map<String, Object> checkPublish(String userName);

    /** 按用户名取用户 */
    AuthUser getUserByName(String userName);

    /** 按用户ID取用户（assignee 解析） */
    AuthUser getUserById(Long userId);

    /** 全部写死用户（人员搜索用） */
    List<AuthUser> listUsers();
}
