package com.wb.audit.controller;

import com.wb.audit.audit.dto.RunTaskDto;
import com.wb.audit.audit.dto.RunTaskVo;
import com.wb.audit.audit.service.AuditRunService;
import com.wb.audit.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 执行接口（场景二·audit_execute）
 */
@RestController
@RequestMapping("/api/execute")
public class ExecuteController {

    private final AuditRunService auditRunService;

    /** ⑥ 启动 AI 审核（异步受理） */
    @PostMapping("/tasks/run")
    public Result<RunTaskVo> run(@Valid @RequestBody RunTaskDto dto) {
        return Result.ok(auditRunService.run(dto.getTaskId()));
    }

    public ExecuteController(AuditRunService auditRunService) {
        this.auditRunService = auditRunService;
    }
}