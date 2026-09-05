package com.wb.audit.task.service.impl;

import com.wb.audit.auth.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wb.audit.auth.model.AuthUser;
import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.dict.entity.DictClauseTree;
import com.wb.audit.dict.service.DictService;
import com.wb.audit.message.dto.NotifyDto;
import com.wb.audit.message.service.NotifyService;
import com.wb.audit.task.constant.TaskConstants;
import com.wb.audit.task.dto.TaskDto;
import com.wb.audit.task.dto.TaskVo;
import com.wb.audit.task.entity.AuditTask;
import com.wb.audit.task.entity.TaskAssignment;
import com.wb.audit.task.entity.TaskClause;
import com.wb.audit.task.entity.TaskNodeLog;
import com.wb.audit.task.mapper.AuditTaskMapper;
import com.wb.audit.task.mapper.TaskAssignmentMapper;
import com.wb.audit.task.mapper.TaskClauseMapper;
import com.wb.audit.task.mapper.TaskNodeLogMapper;
import com.wb.audit.task.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 任务服务实现：创建并下发 = 单事务
 * 权限校验(用户名写死) → 条款校验(区域隔离/nodeType/叶子路径) → 落库 5 表 → 企微通知(聚合) → 回执
 * 任一步失败整体回滚；企微通知失败同样回滚。
 */
@Service
public class TaskServiceImpl implements TaskService {

    private final AuthService authService;
    private final DictService dictService;
    private final NotifyService notifyService;
    private final AuditTaskMapper auditTaskMapper;
    private final TaskClauseMapper taskClauseMapper;
    private final TaskAssignmentMapper taskAssignmentMapper;
    private final TaskNodeLogMapper taskNodeLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskVo createAndDispatch(String operatorUserName, TaskDto req) {
        // 1) 权限校验：X-User-Name/Id == 张伟/5（审核员）
        AuthUser operator = authService.getUserByName(operatorUserName);
        if (operator == null || !operator.canPublish()) {
            log.warn("无权限创建任务: operator={}", operatorUserName);
            throw new BizException(ResultCode.NO_PERMISSION);
        }

        // 2) 参数校验：周期起止合法
        if (req.getPeriod().getStart().isAfter(req.getPeriod().getEnd())) {
            throw new BizException(ResultCode.PARAM_ERROR, "周期起不能晚于周期止");
        }
        String factoryName = TaskConstants.FACTORY_NAME.get(req.getFactoryId());
        String factoryCode = TaskConstants.FACTORY_CODE.get(req.getFactoryId());
        if (factoryName == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "基地不存在: " + req.getFactoryId());
        }

        // 3) 条款校验（区域隔离 + nodeType 后端判定 + 叶子路径）
        // 3.1 收集所有 clauseId，检查跨区域重复
        Map<String, String> clauseRegion = new HashMap<>();
        List<TaskClause> clausesToInsert = new ArrayList<>();
        for (TaskDto.RegionClauses rc : req.getRegions()) {
            for (TaskDto.ClauseAssign ca : rc.getClauses()) {
                String prevRegion = clauseRegion.putIfAbsent(ca.getClauseId(), rc.getRegion());
                if (prevRegion != null && !prevRegion.equals(rc.getRegion())) {
                    throw new BizException(ResultCode.PARAM_ERROR,
                            "条款 " + ca.getClauseId() + " 跨区域重复（" + prevRegion + "/" + rc.getRegion() + "）");
                }
                // 条款必须存在于条款树 且 所属区域 == 所在分组区域（区域隔离）
                DictClauseTree c = dictService.getClauseById(ca.getClauseId());
                if (!rc.getRegion().equals(c.getRegion())) {
                    throw new BizException(ResultCode.PARAM_ERROR,
                            "条款 " + ca.getClauseId() + " 不属于区域 " + rc.getRegion() + "（实际属于 " + c.getRegion() + "）");
                }
                AuthUser assignee = authService.getUserById(ca.getAssigneeId()); // 分派人存在
                TaskClause tc = new TaskClause();
                tc.setClauseId(ca.getClauseId());
                tc.setClauseName(c.getClauseName());
                tc.setRegion(rc.getRegion());
                tc.setProject(c.getProject());
                tc.setSubElement(c.getSubElement());
                tc.setAssigneeId(ca.getAssigneeId());
                tc.setNodeType(assignee.nodeType()); // 后端判定，不信任前端
                tc.setState("TO_COLLECT");
                tc.setMaterialState("PENDING");
                tc.setAuditState("PENDING");
                tc.setReviewState("PENDING");
                tc.setVersion(1);
                clausesToInsert.add(tc);
            }
        }

        // 3.2 叶子路径：每条条款必须在分派树里有归属（clauseId→assignee）
        Map<String, TaskDto.DispatchEntry> dispatchMap = new HashMap<>();
        for (TaskDto.DispatchEntry de : req.getDispatchTree()) {
            for (String clauseId : de.getClauseIds()) {
                if (dispatchMap.putIfAbsent(clauseId, de) != null) {
                    throw new BizException(ResultCode.PARAM_ERROR, "条款 " + clauseId + " 在分派树中重复归属");
                }
            }
        }
        for (TaskDto.RegionClauses rc : req.getRegions()) {
            for (TaskDto.ClauseAssign ca : rc.getClauses()) {
                if (!dispatchMap.containsKey(ca.getClauseId())) {
                    throw new BizException(ResultCode.TREE_INVALID, "条款 " + ca.getClauseId() + " 无叶子路径（未分派）");
                }
                authService.getUserById(dispatchMap.get(ca.getClauseId()).getAssigneeId()); // 分派人存在
            }
        }

        // 4) 生成 taskNo
        String taskNo = generateTaskNo(factoryCode);
        LocalDateTime now = LocalDateTime.now();

        // 5) 落库 audit_task（全局态=COLLECTING，无草稿态）
        AuditTask task = new AuditTask();
        task.setTaskNo(taskNo);
        task.setFactoryId(req.getFactoryId());
        task.setPeriodType(req.getPeriod().getType());
        task.setPeriodStart(req.getPeriod().getStart());
        task.setPeriodEnd(req.getPeriod().getEnd());
        task.setRegion(req.getRegions().stream().map(TaskDto.RegionClauses::getRegion).distinct().collect(Collectors.joining(",")));
        task.setOwnerId(operator.getUserId());
        task.setGlobalState("COLLECTING");
        task.setMaterialAllCollected(0);
        auditTaskMapper.insert(task);
        Long taskId = task.getId();

        // task_clause 批量（区域隔离已校验，region 写分组区域）
        for (TaskClause tc : clausesToInsert) {
            tc.setTaskId(taskId);
            taskClauseMapper.insert(tc);
        }

        // task_assignment（分派树，nodeType 后端判定）
        for (TaskDto.DispatchEntry de : req.getDispatchTree()) {
            AuthUser assignee = authService.getUserById(de.getAssigneeId());
            for (String clauseId : de.getClauseIds()) {
                TaskAssignment ta = new TaskAssignment();
                ta.setTaskId(taskId);
                ta.setClauseId(clauseId);
                ta.setAssignerId(operator.getUserId());
                ta.setAssigneeId(de.getAssigneeId());
                ta.setNodeType(assignee.nodeType()); // 后端判定
                ta.setAssignedAt(now);
                taskAssignmentMapper.insert(ta);
            }
        }

        // task_node_log：任务 无→COLLECTING；条款 无→TO_COLLECT
        taskNodeLogMapper.insert(nodeLog(taskId, null, "TASK", null, "COLLECTING", operator.getUserId()));
        for (TaskClause tc : clausesToInsert) {
            taskNodeLogMapper.insert(nodeLog(taskId, tc.getClauseId(), "CLAUSE", null, "TO_COLLECT", operator.getUserId()));
        }

        // 6) 企微通知（必须成功，同事务）：按责任节点聚合，叶子/陪审员各一套模板
        sendTaskCreatedNotifications(taskId, taskNo, factoryName, req, clausesToInsert);

        // 7) 回执
        List<Map<String, String>> clauseStates = clausesToInsert.stream()
                .map(tc -> Map.of("clauseId", tc.getClauseId(), "state", tc.getState()))
                .toList();
        TaskVo vo = new TaskVo();
        vo.setTaskId(taskId);
        vo.setTaskNo(taskNo);
        vo.setGlobalState(task.getGlobalState());
        vo.setClauseStates(clauseStates);
        return vo;
    }

    /** 企微通知：按 assignee 聚合，leaf→MATERIAL_PENDING，juror→TASK_FORWARD */
    private void sendTaskCreatedNotifications(Long taskId, String taskNo, String factoryName,
                                              TaskDto req, List<TaskClause> clauses) {
        Map<Long, List<TaskClause>> byAssignee = new LinkedHashMap<>();
        for (TaskClause tc : clauses) {
            byAssignee.computeIfAbsent(tc.getAssigneeId(), k -> new ArrayList<>()).add(tc);
        }
        String periodDesc = req.getPeriod().getStart() + " ~ " + req.getPeriod().getEnd();
        String regions = req.getRegions().stream().map(TaskDto.RegionClauses::getRegion).collect(Collectors.joining("、"));
        for (Map.Entry<Long, List<TaskClause>> e : byAssignee.entrySet()) {
            AuthUser assignee = authService.getUserById(e.getKey());
            boolean leaf = "leaf".equals(assignee.nodeType());
            String templateCode = leaf ? "MATERIAL_PENDING" : "TASK_FORWARD";
            NotifyDto notify = new NotifyDto();
            notify.setTaskId(taskId);
            notify.setTemplateCode(templateCode);
            notify.setToUsers(List.of(assignee.getWecomUserid()));
            notify.setVars(Map.of(
                    "taskNo", taskNo,
                    "factory", factoryName,
                    "period", periodDesc,
                    "region", regions,
                    "clauseCount", e.getValue().size()));
            // 模拟发送；失败抛异常 → 整个事务回滚
            notifyService.send(notify);
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

    private String generateTaskNo(String factoryCode) {
        String ym = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long count = auditTaskMapper.selectCount(null);
        return String.format("AUD-%s-%s-%03d", ym, factoryCode, count + 1);
    }

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Override
    public java.util.List<java.util.Map<String, Object>> historyList(
            Long ownerId, String periodType, String periodStart, String periodEnd,
            Long factoryId, String region, String clauseId) {
        LambdaQueryWrapper<AuditTask> qw = new LambdaQueryWrapper<AuditTask>()
                .eq(ownerId != null, AuditTask::getOwnerId, ownerId)
                .eq(factoryId != null, AuditTask::getFactoryId, factoryId)
                .eq(region != null && !region.isBlank(), AuditTask::getRegion, region)
                .eq(periodType != null && !periodType.isBlank(), AuditTask::getPeriodType, periodType)
                .orderByDesc(AuditTask::getId);
        if (periodStart != null && !periodStart.isBlank()) {
            qw.ge(AuditTask::getPeriodStart, java.time.LocalDate.parse(periodStart));
        }
        if (periodEnd != null && !periodEnd.isBlank()) {
            qw.le(AuditTask::getPeriodEnd, java.time.LocalDate.parse(periodEnd));
        }
        List<AuditTask> tasks = auditTaskMapper.selectList(qw);
        List<Map<String, Object>> out = new ArrayList<>();
        for (AuditTask t : tasks) {
            if (clauseId != null && !clauseId.isBlank()) {
                Long n = taskClauseMapper.selectCount(new LambdaQueryWrapper<TaskClause>()
                        .eq(TaskClause::getTaskId, t.getId())
                        .eq(TaskClause::getClauseId, clauseId));
                if (n == null || n == 0) {
                    continue;
                }
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", t.getId());
            m.put("taskNo", t.getTaskNo());
            m.put("factoryId", t.getFactoryId());
            m.put("factoryName", TaskConstants.FACTORY_NAME.getOrDefault(t.getFactoryId(), String.valueOf(t.getFactoryId())));
            m.put("periodType", t.getPeriodType());
            m.put("periodStart", String.valueOf(t.getPeriodStart()));
            m.put("periodEnd", String.valueOf(t.getPeriodEnd()));
            m.put("region", t.getRegion());
            m.put("globalState", t.getGlobalState());
            List<Map<String, Object>> cls = new ArrayList<>();
            for (TaskClause tc : taskClauseMapper.selectList(new LambdaQueryWrapper<TaskClause>()
                    .eq(TaskClause::getTaskId, t.getId())
                    .orderByAsc(TaskClause::getId))) {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("clauseId", tc.getClauseId());
                cm.put("clauseName", tc.getClauseName());
                cm.put("state", tc.getState());
                cls.add(cm);
            }
            m.put("clauses", cls);
            m.put("createTime", String.valueOf(t.getCreateTime()));
            out.add(m);
        }
        return out;
    }
    public TaskServiceImpl(AuthService authService, DictService dictService, NotifyService notifyService, AuditTaskMapper auditTaskMapper, TaskClauseMapper taskClauseMapper, TaskAssignmentMapper taskAssignmentMapper, TaskNodeLogMapper taskNodeLogMapper) {
        this.authService = authService;
        this.dictService = dictService;
        this.notifyService = notifyService;
        this.auditTaskMapper = auditTaskMapper;
        this.taskClauseMapper = taskClauseMapper;
        this.taskAssignmentMapper = taskAssignmentMapper;
        this.taskNodeLogMapper = taskNodeLogMapper;
    }

}





