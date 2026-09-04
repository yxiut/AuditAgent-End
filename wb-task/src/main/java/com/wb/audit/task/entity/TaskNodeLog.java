package com.wb.audit.task.entity;

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
 * wb_task_node_log 实体
 */
@Data
@NoArgsConstructor
@TableName("wb_task_node_log")
public class TaskNodeLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;

    /** 条款实例ID（任务级为空） */
    private String clauseId;

    /** 节点类型：TASK/CLAUSE */
    private String nodeType;

    /** 原状态 */
    private String fromState;

    /** 新状态 */
    private String toState;

    /** 操作人ID */
    private Long operatorId;

    /** 来源：AI/MANUAL/AUTOMATION/SYSTEM */
    private String source;

    /** 详情 */
    private String detailJson;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
