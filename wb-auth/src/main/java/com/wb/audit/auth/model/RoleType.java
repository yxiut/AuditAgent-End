package com.wb.audit.auth.model;

/**
 * 用户角色（本期写死映射）
 */
public enum RoleType {

    /** 审核人：可发布审核任务 */
    AUDITOR,
    /** 陪审员：仅转发，不可发布 */
    JUROR,
    /** 被审核人：叶子，不可发布 */
    AUDITEE
}
