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
 * wb_task_clause 实体
 */
@Data
@NoArgsConstructor
@TableName("wb_task_clause")
public class TaskClause {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;

    /** 条款实例ID */
    private String clauseId;

    /** 条款名称 */
    private String clauseName;

    /** 区域（区域隔离：与分组区域一致） */
    private String region;

    /** 项目 */
    private String project;

    /** 子要素 */
    private String subElement;

    /** 当前责任节点ID（5=张伟/4=陈志强） */
    private Long assigneeId;

    /** 节点类型：leaf/juror（后端判定写入） */
    private String nodeType;

    /** 条款态：TO_COLLECT等 */
    private String state;

    /** 材料态：PENDING/COLLECTED/CHANGED */
    private String materialState;

    /** 审核态：PENDING/AUDITING/CONCLUDED/CLARIFY */
    private String auditState;

    /** 复审态：PENDING/CONFIRMED/REJECTED */
    private String reviewState;

    /** 条款分（人工确认后=已确认问题最低分，无问题10） */
    private Integer clauseScore;

    /** 版本号 */
    private Integer version;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
