package com.wb.audit.task.service;

import com.wb.audit.task.dto.TaskDto;
import com.wb.audit.task.dto.TaskVo;

/**
 * 任务服务：创建并下发（单事务）
 */
public interface TaskService {

    TaskVo createAndDispatch(String operatorUserName, TaskDto request);
}

