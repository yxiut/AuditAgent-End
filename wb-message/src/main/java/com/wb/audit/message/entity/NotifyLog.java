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
 * wb_notify_log 实体
 */
@Data
@NoArgsConstructor
@TableName("wb_notify_log")
public class NotifyLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;

    /** 模板ID */
    private Long templateId;

    /** 目标用户ID */
    private Long targetUserId;

    /** 企微userid（接收人） */
    private String wecomUserid;

    /** 通知类型：MATERIAL_PENDING/TASK_FORWARD等 */
    private String notifyType;

    /** 渲染后消息内容 */
    private String content;

    /** 企微消息msgid */
    private String msgId;

    /** 渠道 */
    private String channel;

    /** 发送状态：SENT/FAILED/RETRY */
    private String status;

    /** 发送结果 */
    private String sendResult;

    /** 发送时间 */
    private LocalDateTime sentAt;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
