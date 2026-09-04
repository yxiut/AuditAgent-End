package com.wb.audit.controller;

import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.Result;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.message.dto.PushDto;
import com.wb.audit.message.wecom.WeComClient;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 企微直接推送（测试/联调用）：仅支持个人与多人。
 */
@RestController
@RequestMapping("/api/push")
public class PushController {

    private final WeComClient weComClient;

    /** 推送给单个人：{"userId":"DuoLeGeDuo","content":"..."} */
    @PostMapping("/user")
    public Result<Map<String, Object>> pushUser(@Valid @RequestBody PushDto dto) {
        if (dto.getUserId() == null || dto.getUserId().isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "userId 不能为空");
        }
        return Result.ok(weComClient.sendText(List.of(dto.getUserId().trim()), dto.getContent()));
    }

    /** 推送给多个人：{"userIds":["A","B"],"content":"..."} */
    @PostMapping("/users")
    public Result<Map<String, Object>> pushUsers(@Valid @RequestBody PushDto dto) {
        if (dto.getUserIds() == null || dto.getUserIds().isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "userIds 不能为空");
        }
        return Result.ok(weComClient.sendText(dto.getUserIds(), dto.getContent()));
    }

    public PushController(WeComClient weComClient) {
        this.weComClient = weComClient;
    }
}