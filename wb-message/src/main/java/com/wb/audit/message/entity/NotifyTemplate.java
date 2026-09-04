package com.wb.audit.message.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * wb_notify_template 实体
 */
@Data
@NoArgsConstructor
@TableName("wb_notify_template")
public class NotifyTemplate {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板编码：MATERIAL_PENDING(叶子)/TASK_FORWARD(陪审员) */
    private String code;

    /** 模板标题 */
    private String title;

    /** 正文，含占位符 {taskNo}/{factory}/{period}/{region}/{clauseCount} */
    private String content;

    /** 企微模板ID */
    private String wecomTemplateId;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 版本号 */
    private Integer version;

    /** 更新人ID */
    private Long updatedBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
