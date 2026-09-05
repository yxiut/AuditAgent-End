package com.wb.audit.controller;

import com.wb.audit.auth.model.AuthUser;
import com.wb.audit.auth.service.AuthService;
import com.wb.audit.common.context.UserContext;
import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.common.result.Result;
import com.wb.audit.task.dto.TaskDto;
import com.wb.audit.task.dto.TaskVo;
import com.wb.audit.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final AuthService authService;


    /** queryHistoryList：当前审核员历史任务（owner 最新在前、状态不限；只读） */
    @GetMapping("/queryHistoryList")
    public Result<java.util.List<java.util.Map<String, Object>>> queryHistoryList(
            @RequestParam(required = false) String periodType,
            @RequestParam(required = false) String periodStart,
            @RequestParam(required = false) String periodEnd,
            @RequestParam(required = false) Long factoryId,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String clauseId) {
        String name = UserContext.currentUserName();
        AuthUser user = name == null ? null : authService.getUserByName(name);
        if (user == null) {
            throw new BizException(ResultCode.NO_PERMISSION, "无法识别当前用户");
        }
        return Result.ok(taskService.historyList(user.getUserId(), periodType, periodStart, periodEnd, factoryId, region, clauseId));
    }    /** ⑥ 创建并下发（单事务） */
    @PostMapping("/create")
    public Result<TaskVo> create(@Valid @RequestBody TaskDto request) {
        String operator = UserContext.currentUserName();
        return Result.ok(taskService.createAndDispatch(operator, request));
    }

    public TaskController(TaskService taskService, AuthService authService) {
        this.taskService = taskService;
        this.authService = authService;
    }

}




