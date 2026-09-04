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
 * wb_task_assignment 实体
 */
@Data
@NoArgsConstructor
@TableName("wb_task_assignment")
public class TaskAssignment {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;

    /** 条款实例ID */
    private String clauseId;

    /** 分派人（审核员）ID */
    private Long assignerId;

    /** 承接人ID */
    private Long assigneeId;

    /** 节点类型：leaf/juror */
    private String nodeType;

    /** 分派时间 */
    private LocalDateTime assignedAt;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
