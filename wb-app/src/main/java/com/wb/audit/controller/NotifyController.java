package com.wb.audit.controller;

import com.wb.audit.common.result.Result;
import com.wb.audit.message.dto.NotifyDto;
import com.wb.audit.message.service.NotifyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 企微通知接口（通用发送）
 */
@RestController
@RequestMapping("/api/notify")
public class NotifyController {

    private final NotifyService notifyService;

    /** ⑦ 企微通用发送（本期模拟） */
    @PostMapping("/send")
    public Result<Map<String, Object>> send(@Valid @RequestBody NotifyDto request) {
        return Result.ok(notifyService.send(request));
    }

    public NotifyController(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

}




