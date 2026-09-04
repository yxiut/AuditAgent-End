package com.wb.audit.auth.service.impl;

import com.wb.audit.auth.HardcodedUsers;
import com.wb.audit.auth.model.AuthUser;
import com.wb.audit.auth.model.RoleType;
import com.wb.audit.auth.service.AuthService;
import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.ResultCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 认证与权限实现：按用户名匹配写死映射，非审核人不可发布。
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Override
    public Map<String, Object> checkPublish(String userName) {
        AuthUser user = getUserByName(userName);
        boolean canPublish = user != null && user.canPublish();
        return Map.of(
                "canPublish", canPublish,
                "level", user != null && user.getRole() == RoleType.AUDITOR ? "company" : "none",
                "factories", user != null && user.canPublish() ? List.of("龙兴工厂", "两江工厂", "扬帆工厂") : List.of()
        );
    }

    @Override
    public AuthUser getUserByName(String userName) {
        return HardcodedUsers.byName(userName);
    }

    @Override
    public AuthUser getUserById(Long userId) {
        AuthUser user = HardcodedUsers.byId(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "分派人不存在: userId=" + userId);
        }
        return user;
    }

    @Override
    public List<AuthUser> listUsers() {
        return HardcodedUsers.all();
    }
}
