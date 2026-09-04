package com.wb.audit.audit.service;

import com.wb.audit.audit.dto.RunTaskVo;

/**
 * AI 审核执行服务（场景二·执行工具 audit_execute）
 */
public interface AuditRunService {

    /** 异步受理开审：写 run 记录 + 任务态 AUDITING（demo 不真审） */
    RunTaskVo run(Long taskId);
}