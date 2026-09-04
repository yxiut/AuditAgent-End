package com.wb.audit.controller;

import com.wb.audit.auth.service.AuthService;
import com.wb.audit.common.result.Result;
import lombok.val;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 认证鉴权接口（用户名写死映射，不走第三方认证）
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    /** ① 发布权预检 */
    @PostMapping("/auth/checkPublish")
    public Result<Map<String, Object>> checkPublish(@RequestParam(required = false) String userName) {
        val name = userName == null || userName.isBlank()
                ? com.wb.audit.common.context.UserContext.currentUserName()
                : userName;

        return Result.ok(authService.checkPublish(name));
    }

    /** 人员搜索（返回写死候选，不查用户表） */
    @GetMapping("/users/search")
    public Result<List<Map<String, Object>>> search(@RequestParam String q) {
        List<Map<String, Object>> hits = authService.listUsers().stream()
                .filter(u -> u.getName().contains(q) || String.valueOf(u.getUserId()).contains(q))
                .map(u -> Map.<String, Object>of(
                        "userId", u.getUserId(), "name", u.getName(),
                        "employeeNo", String.valueOf(u.getUserId()),
                        "post", u.getRole().name(), "role", u.getRole() == com.wb.audit.auth.model.RoleType.AUDITEE ? "leaf" : "juror"))
                .toList();
        return Result.ok(hits);
    }

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

}





