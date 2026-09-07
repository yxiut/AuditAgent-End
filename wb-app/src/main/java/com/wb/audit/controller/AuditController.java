package com.wb.audit.controller;

import com.wb.audit.audit.dto.ConclusionVo;
import com.wb.audit.audit.dto.ConclusionWriteDto;
import com.wb.audit.audit.dto.IssueConfirmDto;
import com.wb.audit.audit.dto.ProgressVo;
import com.wb.audit.audit.dto.ReviewConfirmDto;
import com.wb.audit.audit.dto.RuleVo;
import com.wb.audit.audit.service.AuditConclusionService;
import com.wb.audit.audit.service.AuditRuleService;
import com.wb.audit.auth.model.AuthUser;
import com.wb.audit.auth.service.AuthService;
import com.wb.audit.common.context.UserContext;
import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 审核执行接口（场景三·规则执行，给 WorkBuddy 客户端 runTask / 审核员查进度调用）
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditRuleService auditRuleService;
    private final AuditConclusionService auditConclusionService;
    private final AuthService authService;

    /** getRule：取规则全文 */
    @GetMapping("/rules")
    public Result<RuleVo> rules(@RequestParam String clauseId) {
        return Result.ok(auditRuleService.getRule(clauseId));
    }

    /** listTasks：审核员（owner）名下任务（04 审核监控选任务） */
    @GetMapping("/tasks")
    public Result<Map<String, Object>> tasks() {
        String name = UserContext.currentUserName();
        AuthUser user = name == null ? null : authService.getUserByName(name);
        if (user == null) {
            throw new BizException(ResultCode.NO_PERMISSION, "无法识别当前用户");
        }
        return Result.ok(auditRuleService.ownerTasks(user.getUserId()));
    }

    /** queryReadyTasks：列「材料已收齐、待 AI 审核」的任务（定时任务轮询用） */
    @GetMapping("/ready-tasks")
    public Result<List<Map<String, Object>>> readyTasks() {
        return Result.ok(auditRuleService.readyTasks());
    }
    /** pullQueue：取待审条款（材料已收齐、audit_state=PENDING） */
    @GetMapping("/queue")
    public Result<Map<String, Object>> queue(@RequestParam Long taskId) {
        return Result.ok(auditRuleService.queue(taskId));
    }

    /** writeConclusion：AI 结论写回（scored/blocked + issues），任务态→HUMAN_REVIEW，通知审核员 */
    @PostMapping("/conclusion")
    public Result<ConclusionVo> conclusion(@Valid @RequestBody ConclusionWriteDto dto) {
        return Result.ok(auditConclusionService.write(dto));
    }

    /** getProgress：审核员查审核进度（结论摘要 + issues） */
    @GetMapping("/progress")
    public Result<ProgressVo> progress(@RequestParam Long taskId) {
        return Result.ok(auditConclusionService.progress(taskId));
    }

    /** 人工复审：审核员逐条确认/驳回，确认后算条款分 */
    @PostMapping("/issues/confirm")
    public Result<Map<String, Object>> confirm(@Valid @RequestBody IssueConfirmDto dto) {
        return Result.ok(auditConclusionService.confirm(dto));
    }

    /** 人工复审整表确认（confirmReview）：按 getProgress.bipRows 基线 diff 写回；成功后任务→REVIEWED */
    @PostMapping("/review/confirm")
    public Result<Map<String, Object>> reviewConfirm(@Valid @RequestBody ReviewConfirmDto dto) {
        return Result.ok(auditConclusionService.reviewConfirm(dto));
    }

    public AuditController(AuditRuleService auditRuleService, AuditConclusionService auditConclusionService, AuthService authService) {
        this.auditRuleService = auditRuleService;
        this.auditConclusionService = auditConclusionService;
        this.authService = authService;
    }
}