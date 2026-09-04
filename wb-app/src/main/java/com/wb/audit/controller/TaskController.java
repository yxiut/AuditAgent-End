package com.wb.audit.controller;

import com.wb.audit.common.context.UserContext;
import com.wb.audit.common.result.Result;
import com.wb.audit.task.dto.TaskDto;
import com.wb.audit.task.dto.TaskVo;
import com.wb.audit.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务接口：发起审核-创建并下发
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    /** ⑥ 创建并下发（单事务） */
    @PostMapping("/create")
    public Result<TaskVo> create(@Valid @RequestBody TaskDto request) {
        String operator = UserContext.currentUserName();
        return Result.ok(taskService.createAndDispatch(operator, request));
    }

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

}




