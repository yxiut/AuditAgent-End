package com.wb.audit.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wb.audit.audit.dto.RunTaskVo;
import com.wb.audit.audit.entity.AuditRun;
import com.wb.audit.audit.mapper.AuditRunMapper;
import com.wb.audit.audit.service.AuditRunService;
import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.task.entity.AuditTask;
import com.wb.audit.task.entity.TaskNodeLog;
import com.wb.audit.task.mapper.AuditTaskMapper;
import com.wb.audit.task.mapper.TaskNodeLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI 审核执行实现（场景二·DEMO）：只做「受理」——
 * 写 wb_audit_run + 任务态 → AUDITING + node_log；不真审、不写结论、不企微。
 */
@Service
public class AuditRunServiceImpl implements AuditRunService {

    private final AuditRunMapper auditRunMapper;
    private final AuditTaskMapper auditTaskMapper;
    private final TaskNodeLogMapper taskNodeLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunTaskVo run(Long taskId) {
        AuditTask task = auditTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ResultCode.NOT_FOUND, "任务不存在: " + taskId);
        }
        if (!Integer.valueOf(1).equals(task.getMaterialAllCollected())) {
            throw new BizException(ResultCode.PARAM_ERROR, "材料未收齐，不能启动审核");
        }
        String runId = "RUN-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));

        AuditRun run = new AuditRun();
        run.setRunId(runId);
        run.setTaskId(taskId);
        run.setStatus("AUDITING");
        run.setStartedAt(LocalDateTime.now());
        auditRunMapper.insert(run);

        task.setGlobalState("AUDITING");
        auditTaskMapper.updateById(task);

        TaskNodeLog nodeLog = new TaskNodeLog();
        nodeLog.setTaskId(taskId);
        nodeLog.setNodeType("TASK");
        nodeLog.setFromState("COLLECTING");
        nodeLog.setToState("AUDITING");
        nodeLog.setOperatorId(task.getOwnerId());
        nodeLog.setSource("SYSTEM");
        taskNodeLogMapper.insert(nodeLog);

        log.info("AI 审核已受理 taskId={} runId={}", taskId, runId);
        RunTaskVo vo = new RunTaskVo();
        vo.setRunId(runId);
        vo.setTaskId(taskId);
        vo.setState("AUDITING");
        return vo;
    }

    private static final Logger log = LoggerFactory.getLogger(AuditRunServiceImpl.class);

    public AuditRunServiceImpl(AuditRunMapper auditRunMapper, AuditTaskMapper auditTaskMapper,
                               TaskNodeLogMapper taskNodeLogMapper) {
        this.auditRunMapper = auditRunMapper;
        this.auditTaskMapper = auditTaskMapper;
        this.taskNodeLogMapper = taskNodeLogMapper;
    }
}