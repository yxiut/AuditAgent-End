package com.wb.audit.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wb.audit.audit.dto.RuleVo;
import com.wb.audit.audit.entity.RuleDoc;
import com.wb.audit.audit.mapper.RuleDocMapper;
import com.wb.audit.audit.service.AuditRuleService;
import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.task.entity.TaskClause;
import com.wb.audit.task.constant.TaskConstants;
import com.wb.audit.task.entity.AuditTask;
import com.wb.audit.task.mapper.AuditTaskMapper;
import com.wb.audit.task.mapper.TaskClauseMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 规则执行服务实现：getRule / pullQueue
 */
@Service
public class AuditRuleServiceImpl implements AuditRuleService {

    private final RuleDocMapper ruleDocMapper;
    private final TaskClauseMapper taskClauseMapper;
    private final AuditTaskMapper auditTaskMapper;

    @Override
    public RuleVo getRule(String clauseId) {
        RuleDoc doc = ruleDocMapper.selectOne(new LambdaQueryWrapper<RuleDoc>()
                .eq(RuleDoc::getClauseId, clauseId)
                .eq(RuleDoc::getStatus, 1)
                .last("LIMIT 1"));
        if (doc == null) {
            throw new BizException(ResultCode.NOT_FOUND, "规则文档不存在: clauseId=" + clauseId);
        }
        RuleVo vo = new RuleVo();
        vo.setClauseId(doc.getClauseId());
        vo.setRuleCode(doc.getRuleCode());
        vo.setRuleTitle(doc.getRuleTitle());
        vo.setVersion(doc.getVersion());
        vo.setDocText(doc.getDocText());
        return vo;
    }

    @Override
    public Map<String, Object> ownerTasks(Long ownerId) {
        List<AuditTask> tasks = auditTaskMapper.selectList(new LambdaQueryWrapper<AuditTask>()
                .eq(AuditTask::getOwnerId, ownerId)
                .orderByDesc(AuditTask::getId));
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (AuditTask t : tasks) {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("taskId", t.getId());
            m.put("taskNo", t.getTaskNo());
            m.put("factoryName", TaskConstants.FACTORY_NAME.getOrDefault(t.getFactoryId(), String.valueOf(t.getFactoryId())));
            m.put("period", t.getPeriodStart() + " ~ " + t.getPeriodEnd());
            m.put("region", t.getRegion());
            m.put("phase", t.getGlobalState());
            list.add(m);
        }
        return Map.of("tasks", list);
    }

    @Override
    public Map<String, Object> queue(Long taskId) {
        List<String> clauseIds = taskClauseMapper.selectList(new LambdaQueryWrapper<TaskClause>()
                        .eq(TaskClause::getTaskId, taskId)
                        .eq(TaskClause::getState, "COLLECTED")
                        .eq(TaskClause::getAuditState, "PENDING"))
                .stream().map(TaskClause::getClauseId).toList();
        return Map.of("taskId", taskId, "clauseIds", clauseIds);
    }

    public AuditRuleServiceImpl(RuleDocMapper ruleDocMapper, TaskClauseMapper taskClauseMapper, AuditTaskMapper auditTaskMapper) {
        this.ruleDocMapper = ruleDocMapper;
        this.taskClauseMapper = taskClauseMapper;
        this.auditTaskMapper = auditTaskMapper;
    }
}