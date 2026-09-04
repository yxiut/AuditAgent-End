package com.wb.audit.dict.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 口语归一入参（字典 normalize）
 */
@Data
public class NormalizeDto {

    /** 口语串（基地/区域/项目/子要素等） */
    @NotBlank(message = "不能为空")
    private String text;
}