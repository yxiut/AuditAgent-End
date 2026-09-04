package com.wb.audit.message.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 企微直接推送入参（单人 userId / 多人 userIds 二选一）
 */
@Data
public class PushDto {

    /** 单人接收企微 userid（与 userIds 二选一） */
    private String userId;

    /** 多人接收企微 userid 列表（与 userId 二选一） */
    private List<String> userIds;

    /** 文本内容 */
    @NotBlank(message = "推送内容不能为空")
    private String content;
}