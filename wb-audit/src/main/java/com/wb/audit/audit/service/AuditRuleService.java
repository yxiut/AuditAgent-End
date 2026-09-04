package com.wb.audit.audit.service;

import com.wb.audit.audit.dto.RuleVo;

import java.util.List;
import java.util.Map;

/**
 * 规则执行：取规则 / 取待审队列（客户端 runTask 编排用）
 */
public interface AuditRuleService {

    /** getRule：按条款取规则全文 */
    RuleVo getRule(String clauseId);

    /** pullQueue：取某任务「材料已收齐、待 AI 审核」的条款ID列表 */
    Map<String, Object> queue(Long taskId);

    /** listTasks：审核员（owner）名下任务列表（04 审核监控选任务用） */
    Map<String, Object> ownerTasks(Long ownerId);
}