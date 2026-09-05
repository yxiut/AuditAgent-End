package com.wb.audit.task.service;

import com.wb.audit.task.dto.TaskDto;
import com.wb.audit.task.dto.TaskVo;

/**
 * 任务服务：创建并下发（单事务）
 */
public interface TaskService {

    TaskVo createAndDispatch(String operatorUserName, TaskDto request);

    /** queryHistoryList：当前审核员历史任务（owner 最新在前、状态不限；可按条件过滤，只读） */
    java.util.List<java.util.Map<String, Object>> historyList(
            Long ownerId, String periodType, String periodStart, String periodEnd,
            Long factoryId, String region, String clauseId);
}

