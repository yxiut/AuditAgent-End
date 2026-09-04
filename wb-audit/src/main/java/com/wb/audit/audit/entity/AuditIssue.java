package com.wb.audit.audit.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * wb_audit_issue AI审核问题明细（人工复审确认后算条款分）
 */
@Data
@NoArgsConstructor
@TableName("wb_audit_issue")
public class AuditIssue {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 结论ID */
    private Long conclusionId;

    /** 任务ID */
    private Long taskId;

    /** 条款实例ID */
    private String clauseId;

    /** 规则ID（Word 审核点短名） */
    private String ruleId;

    /** 问题描述（进 BIP 问题描述列） */
    private String problemDesc;

    /** 依据（材料ID/月份/行号，不进 BIP） */
    private String evidence;

    /** 问题类型（标准类/执行类等） */
    private String problemType;

    /** 问题得分（0/2/4/6/8/10） */
    private Integer score;

    /** 引用材料ID列表（JSON 数组字符串） */
    private String refMaterials;

    /** 判定建议：不符合/待澄清 */
    private String suggestJudgment;

    /** 置信度：高/中/低 */
    private String confidence;

    /** 人工复审：PENDING/CONFIRMED/REJECTED */
    private String confirmStatus;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}