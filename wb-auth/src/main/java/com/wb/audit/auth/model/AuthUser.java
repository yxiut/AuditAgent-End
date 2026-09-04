package com.wb.audit.auth.model;


/**
 * 写死用户（本期不建用户表）
 */
public class AuthUser {

    private Long userId;
    private String name;
    private RoleType role;
    /** 企微 userid（模拟发送用） */
    private String wecomUserid;

    /** 是否可发布审核任务 */
    public boolean canPublish() {
        return role == RoleType.AUDITOR;
    }

    /** 任务内节点类型：被审核人→leaf，否则→juror（后端判定，不信任前端） */
    public String nodeType() {
        return role == RoleType.AUDITEE ? "leaf" : "juror";
    }

    public AuthUser(Long userId, String name, RoleType role, String wecomUserid) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.wecomUserid = wecomUserid;
    }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public RoleType getRole() { return role; }

    public void setRole(RoleType role) { this.role = role; }

    public String getWecomUserid() { return wecomUserid; }

    public void setWecomUserid(String wecomUserid) { this.wecomUserid = wecomUserid; }

}
