package com.wb.audit.common.context;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 用户上下文：从 WorkBuddy 客户端请求头取当前用户名（不走第三方认证）。
 * 本期用户为写死映射：4=陈志强·被审核人、5=张伟·审核员。
 * 身份来源（二选一）：
 *  1) X-User-Id（ASCII，4/5）——demo 联调可靠路径
 *  2) X-User-Name（UTF-8；若为 %XX 编码则自动解码）
 */
public final class UserContext {

    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_ID = "X-User-Id";

    private static final Map<String, String> ID_TO_NAME = Map.of(
            "4", "陈志强", "5", "张伟");

    private UserContext() {
    }

    public static String currentUserName() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        String userId = attrs.getRequest().getHeader(HEADER_USER_ID);
        if (userId != null && !userId.isBlank()) {
            return ID_TO_NAME.get(userId.trim());
        }
        String name = attrs.getRequest().getHeader(HEADER_USER_NAME);
        if (name == null || name.isBlank()) {
            return null;
        }
        name = name.trim();
        if (name.contains("%")) {
            try {
                return URLDecoder.decode(name, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return name;
            }
        }
        return name;
    }
}
