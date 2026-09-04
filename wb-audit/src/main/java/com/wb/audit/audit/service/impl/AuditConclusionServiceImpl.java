package com.wb.audit.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wb.audit.audit.dto.ClauseProgressVo;
import com.wb.audit.audit.dto.ConclusionVo;
import com.wb.audit.audit.dto.ConclusionWriteDto;
import com.wb.audit.audit.dto.IssueConfirmDto;
import com.wb.audit.audit.dto.IssueVo;
import com.wb.audit.audit.dto.IssueWriteDto;
import com.wb.audit.audit.dto.ProgressVo;
import com.wb.audit.audit.entity.AuditConclusion;
import com.wb.audit.audit.entity.AuditIssue;
import com.wb.audit.audit.entity.AuditRun;
import com.wb.audit.audit.mapper.AuditConclusionMapper;
import com.wb.audit.audit.mapper.AuditIssueMapper;
import com.wb.audit.audit.mapper.AuditRunMapper;
import com.wb.audit.audit.service.AuditConclusionService;
import com.wb.audit.auth.service.AuthService;
import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.message.dto.NotifyDto;
import com.wb.audit.message.service.NotifyService;
import com.wb.audit.task.constant.TaskConstants;
import com.wb.audit.task.entity.AuditTask;
import com.wb.audit.task.entity.TaskClause;
import com.wb.audit.task.entity.TaskNodeLog;
import com.wb.audit.task.mapper.AuditTaskMapper;
import com.wb.audit.task.mapper.TaskClauseMapper;
import com.wb.audit.task.mapper.TaskNodeLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 写结论 / 查进度 / 人工确认 实现。
 * write：同事务落 wb_audit_conclusion + wb_audit_issue → 任务态 HUMAN_REVIEW → 企微通知审核员（必须成功）。
 */
@Service
public class AuditConclusionServiceImpl implements AuditConclusionService {

    private final AuditConclusionMapper conclusionMapper;
    private final AuditIssueMapper issueMapper;
    private final AuditRunMapper auditRunMapper;
    private final AuditTaskMapper auditTaskMapper;
    private final TaskClauseMapper taskClauseMapper;
    private final TaskNodeLogMapper taskNodeLogMapper;
    private final AuthService authService;
    private final NotifyService notifyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConclusionVo write(ConclusionWriteDto dto) {
        String outcome = dto.getOutcome() == null ? "" : dto.getOutcome().trim();
        if (!"scored".equals(outcome) && !"blocked".equals(outcome)) {
            throw new BizException(ResultCode.PARAM_ERROR, "outcome 只能是 scored/blocked");
        }
        AuditTask task = auditTaskMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new BizException(ResultCode.NOT_FOUND, "任务不存在: " + dto.getTaskId());
        }
        TaskClause clause = taskClauseMapper.selectOne(new LambdaQueryWrapper<TaskClause>()
                .eq(TaskClause::getTaskId, dto.getTaskId())
                .eq(TaskClause::getClauseId, dto.getClauseId()));
        if (clause == null) {
            throw new BizException(ResultCode.NOT_FOUND, "任务条款不存在: " + dto.getTaskId() + "/" + dto.getClauseId());
        }
        if ("CONCLUDED".equals(clause.getAuditState()) || "BLOCKED".equals(clause.getAuditState())) {
            throw new BizException(ResultCode.PARAM_ERROR, "该条款已写结论，禁止重复写入: " + dto.getClauseId());
        }
        List<IssueWriteDto> issues = dto.getIssues() == null ? List.of() : dto.getIssues();
        if ("blocked".equals(outcome) && !issues.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "blocked 时 issues 必须为空");
        }
        if ("blocked".equals(outcome) && (dto.getBlockedReason() == null || dto.getBlockedReason().isBlank())) {
            throw new BizException(ResultCode.PARAM_ERROR, "blocked 时必须填 blockedReason");
        }

        // 结论主表
        AuditConclusion c = new AuditConclusion();
        c.setRunId(latestRunId(task.getId()));
        c.setTaskId(task.getId());
        c.setClauseId(clause.getClauseId());
        c.setOutcome(outcome);
        c.setBlockedReason("blocked".equals(outcome) ? dto.getBlockedReason() : null);
        c.setNotes(toJson(dto.getNotes() == null ? List.of() : dto.getNotes()));
        c.setIssueCount(issues.size());
        c.setModelRaw(toJson(dto));
        conclusionMapper.insert(c);

        // 问题明细
        for (IssueWriteDto i : issues) {
            AuditIssue ai = new AuditIssue();
            ai.setConclusionId(c.getId());
            ai.setTaskId(task.getId());
            ai.setClauseId(clause.getClauseId());
            ai.setRuleId(i.getRuleId());
            ai.setProblemDesc(i.getProblemDesc());
            ai.setEvidence(i.getEvidence());
            ai.setProblemType(i.getProblemType());
            ai.setScore(i.getScore());
            ai.setRefMaterials(toJson(i.getRefMaterials() == null ? List.of() : i.getRefMaterials()));
            ai.setSuggestJudgment(i.getSuggestJudgment());
            ai.setConfidence(i.getConfidence());
            ai.setConfirmStatus("PENDING");
            issueMapper.insert(ai);
        }

        // 条款态 + 任务态
        String fromClause = clause.getAuditState() == null ? "PENDING" : clause.getAuditState();
        clause.setAuditState("blocked".equals(outcome) ? "BLOCKED" : "CONCLUDED");
        taskClauseMapper.updateById(clause);
        String fromTask = task.getGlobalState();
        task.setGlobalState("HUMAN_REVIEW");
        auditTaskMapper.updateById(task);
        taskNodeLogMapper.insert(nodeLog(task.getId(), clause.getClauseId(), "CLAUSE", fromClause, clause.getAuditState(), task.getOwnerId()));
        taskNodeLogMapper.insert(nodeLog(task.getId(), null, "TASK", fromTask, "HUMAN_REVIEW", task.getOwnerId()));

        // 企微通知审核员（owner）：AI 已审完待人工复核
        notifyAuditor(task, clause, outcome, issues.size());

        ConclusionVo vo = new ConclusionVo();
        vo.setWritten(true);
        vo.setConclusionId(c.getId());
        return vo;
    }

    @Override
    public ProgressVo progress(Long taskId) {
        AuditTask task = auditTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ResultCode.NOT_FOUND, "任务不存在: " + taskId);
        }
        ProgressVo vo = new ProgressVo();
        vo.setTaskId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setFactoryName(TaskConstants.FACTORY_NAME.getOrDefault(task.getFactoryId(), String.valueOf(task.getFactoryId())));
        vo.setPeriod(task.getPeriodStart() + " ~ " + task.getPeriodEnd());
        vo.setGlobalState(task.getGlobalState());
        vo.setPhase(task.getGlobalState());

        List<TaskClause> clauses = taskClauseMapper.selectList(new LambdaQueryWrapper<TaskClause>()
                .eq(TaskClause::getTaskId, taskId)
                .orderByAsc(TaskClause::getId));
        List<ClauseProgressVo> cps = new ArrayList<>();
        List<Map<String, Object>> bipRows = new ArrayList<>();
        String periodText = task.getPeriodStart() + " ~ " + task.getPeriodEnd();
        String timeText = task.getPeriodStart() == null ? "" : task.getPeriodStart().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String factoryName = TaskConstants.FACTORY_NAME.getOrDefault(task.getFactoryId(), String.valueOf(task.getFactoryId()));
        int seq = 0;
        for (TaskClause tc : clauses) {
            ClauseProgressVo cp = new ClauseProgressVo();
            cp.setPath(tc.getRegion() + " › " + tc.getProject() + " › " + tc.getSubElement() + " › " + tc.getClauseName());
            cp.setClauseId(tc.getClauseId());
            cp.setClauseName(tc.getClauseName());
            cp.setAuditState(tc.getAuditState());
            cp.setReviewState(tc.getReviewState());
            cp.setClauseScore(tc.getClauseScore());
            AuditConclusion c = conclusionMapper.selectOne(new LambdaQueryWrapper<AuditConclusion>()
                    .eq(AuditConclusion::getTaskId, taskId)
                    .eq(AuditConclusion::getClauseId, tc.getClauseId())
                    .orderByDesc(AuditConclusion::getId)
                    .last("LIMIT 1"));
            if (c != null) {
                cp.setOutcome(c.getOutcome());
                cp.setBlockedReason(c.getBlockedReason());
                List<AuditIssue> issues = issueMapper.selectList(new LambdaQueryWrapper<AuditIssue>()
                        .eq(AuditIssue::getConclusionId, c.getId())
                        .orderByAsc(AuditIssue::getId));
                List<IssueVo> issueVos = issues.stream().map(this::toIssueVo).toList();
                cp.setIssues(issueVos);
                cp.setIssuesCount(issueVos.size());
                if ("scored".equals(c.getOutcome()) && issueVos.isEmpty()) {
                    cp.setSuggestedScore(10);
                } else if (!issueVos.isEmpty()) {
                    cp.setSuggestedScore(issueVos.stream().map(IssueVo::getScore)
                            .filter(java.util.Objects::nonNull).min(Integer::compareTo).orElse(null));
                }
                // 结果区 BIP 行（12 列；依据/ruleId/置信度/判定建议不进 BIP）
                if (!"blocked".equals(c.getOutcome())) {
                    for (IssueVo iv : issueVos) {
                        seq++;
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("时间", timeText);
                        row.put("制造基地", factoryName);
                        row.put("序号", seq);
                        row.put("区域", tc.getRegion());
                        row.put("项目", tc.getProject());
                        row.put("子要素", tc.getSubElement());
                        row.put("条款", tc.getClauseName());
                        row.put("问题描述", iv.getProblemDesc());
                        row.put("严重度（赋分）", iv.getScore());
                        row.put("问题属性", iv.getProblemType());
                        row.put("复核结论", "");
                        row.put("修改意见", "");
                        bipRows.add(row);
                    }
                }
            } else {
                cp.setIssues(List.of());
                cp.setIssuesCount(0);
            }
            cps.add(cp);
        }
        vo.setClauses(cps);
        vo.setBipRows(bipRows);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirm(IssueConfirmDto dto) {
        TaskClause clause = taskClauseMapper.selectOne(new LambdaQueryWrapper<TaskClause>()
                .eq(TaskClause::getTaskId, dto.getTaskId())
                .eq(TaskClause::getClauseId, dto.getClauseId()));
        if (clause == null) {
            throw new BizException(ResultCode.NOT_FOUND, "任务条款不存在: " + dto.getTaskId() + "/" + dto.getClauseId());
        }
        String target = Boolean.TRUE.equals(dto.getConfirmed()) ? "CONFIRMED" : "REJECTED";
        List<Long> ids = dto.getIssueIds() == null ? List.of() : dto.getIssueIds();
        List<AuditIssue> all = issueMapper.selectList(new LambdaQueryWrapper<AuditIssue>()
                .eq(AuditIssue::getTaskId, dto.getTaskId())
                .eq(AuditIssue::getClauseId, dto.getClauseId()));
        Map<Long, String> results = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            for (AuditIssue issue : all) {
                if (ids.contains(issue.getId())) {
                    issue.setConfirmStatus(target);
                    issueMapper.updateById(issue);
                    results.put(issue.getId(), target);
                }
            }
        }
        // 条款分：确认后 = 已确认问题最低分；无任何确认问题 → 10
        List<AuditIssue> confirmedIssues = all.stream()
                .filter(i -> "CONFIRMED".equals(i.getConfirmStatus())).toList();
        Integer clauseScore;
        if (confirmedIssues.isEmpty()) {
            clauseScore = 10;
        } else {
            clauseScore = confirmedIssues.stream().map(AuditIssue::getScore)
                    .filter(java.util.Objects::nonNull)
                    .min(Integer::compareTo).orElse(10);
        }
        boolean allResolved = all.stream().allMatch(i ->
                "CONFIRMED".equals(i.getConfirmStatus()) || "REJECTED".equals(i.getConfirmStatus()));
        clause.setClauseScore(clauseScore);
        if (allResolved && !"CONFIRMED".equals(clause.getReviewState())) {
            clause.setReviewState("CONFIRMED");
            Long operator = taskOwner(dto.getTaskId());
            taskNodeLogMapper.insert(nodeLog(dto.getTaskId(), clause.getClauseId(), "CLAUSE",
                    clause.getAuditState(), "REVIEWED", operator));
        }
        taskClauseMapper.updateById(clause);

        // 全部条款复审完 → 任务收口
        long pendingReview = taskClauseMapper.selectCount(new LambdaQueryWrapper<TaskClause>()
                .eq(TaskClause::getTaskId, dto.getTaskId())
                .ne(TaskClause::getReviewState, "CONFIRMED"));
        AuditTask task = auditTaskMapper.selectById(dto.getTaskId());
        if (task != null && pendingReview == 0 && !"COMPLETED".equals(task.getGlobalState())) {
            String prev = task.getGlobalState();
            task.setGlobalState("COMPLETED");
            auditTaskMapper.updateById(task);
            taskNodeLogMapper.insert(nodeLog(task.getId(), null, "TASK", prev, "COMPLETED", task.getOwnerId()));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("clauseId", clause.getClauseId());
        out.put("reviewState", clause.getReviewState());
        out.put("clauseScore", clauseScore);
        out.put("issueResults", results);
        return out;
    }

    /** 审核员（owner）企微通知 */
    private void notifyAuditor(AuditTask task, TaskClause clause, String outcome, int issueCount) {
        if (task.getOwnerId() == null) {
            return;
        }
        String wecomUserid = authService.getUserById(task.getOwnerId()).getWecomUserid();
        if (wecomUserid == null || wecomUserid.isBlank()) {
            return;
        }
        String outcomeDesc = "blocked".equals(outcome)
                ? "阻塞（缺材料），待人工确认"
                : (issueCount == 0 ? "已评分（无问题），待人工确认" : "已评分，" + issueCount + " 条问题待确认");
        NotifyDto notify = new NotifyDto();
        notify.setTaskId(task.getId());
        notify.setTemplateCode("REVIEW_PENDING");
        notify.setToUsers(List.of(wecomUserid));
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("taskNo", task.getTaskNo());
        vars.put("factory", TaskConstants.FACTORY_NAME.getOrDefault(task.getFactoryId(), ""));
        vars.put("period", task.getPeriodStart() + " ~ " + task.getPeriodEnd());
        vars.put("region", task.getRegion());
        vars.put("clauseName", clause.getClauseName());
        vars.put("outcomeDesc", outcomeDesc);
        notify.setVars(vars);
        notifyService.send(notify);
    }

    private Long taskOwner(Long taskId) {
        AuditTask t = auditTaskMapper.selectById(taskId);
        return t == null ? null : t.getOwnerId();
    }

    private String latestRunId(Long taskId) {
        AuditRun run = auditRunMapper.selectOne(new LambdaQueryWrapper<AuditRun>()
                .eq(AuditRun::getTaskId, taskId)
                .orderByDesc(AuditRun::getId)
                .last("LIMIT 1"));
        return run == null ? null : run.getRunId();
    }

    private IssueVo toIssueVo(AuditIssue i) {
        IssueVo vo = new IssueVo();
        vo.setId(i.getId());
        vo.setRuleId(i.getRuleId());
        vo.setProblemDesc(i.getProblemDesc());
        vo.setEvidence(i.getEvidence());
        vo.setProblemType(i.getProblemType());
        vo.setScore(i.getScore());
        vo.setRefMaterials(fromJsonList(i.getRefMaterials()));
        vo.setSuggestJudgment(i.getSuggestJudgment());
        vo.setConfidence(i.getConfidence());
        vo.setConfirmStatus(i.getConfirmStatus());
        return vo;
    }

    private String toJson(Object o) {
        try {
            return o == null ? "[]" : objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> l = objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
            return l == null ? List.of() : l;
        } catch (Exception e) {
            return List.of();
        }
    }

    private TaskNodeLog nodeLog(Long taskId, String clauseId, String nodeType,
                                String from, String to, Long operatorId) {
        TaskNodeLog log = new TaskNodeLog();
        log.setTaskId(taskId);
        log.setClauseId(clauseId);
        log.setNodeType(nodeType);
        log.setFromState(from);
        log.setToState(to);
        log.setOperatorId(operatorId);
        log.setSource("SYSTEM");
        return log;
    }

    private static final Logger log = LoggerFactory.getLogger(AuditConclusionServiceImpl.class);

    public AuditConclusionServiceImpl(AuditConclusionMapper conclusionMapper, AuditIssueMapper issueMapper,
                                      AuditRunMapper auditRunMapper, AuditTaskMapper auditTaskMapper,
                                      TaskClauseMapper taskClauseMapper, TaskNodeLogMapper taskNodeLogMapper,
                                      AuthService authService, NotifyService notifyService) {
        this.conclusionMapper = conclusionMapper;
        this.issueMapper = issueMapper;
        this.auditRunMapper = auditRunMapper;
        this.auditTaskMapper = auditTaskMapper;
        this.taskClauseMapper = taskClauseMapper;
        this.taskNodeLogMapper = taskNodeLogMapper;
        this.authService = authService;
        this.notifyService = notifyService;
    }
}