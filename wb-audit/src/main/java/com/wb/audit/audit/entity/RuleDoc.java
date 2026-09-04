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
 * wb_rule_doc 条款规则文档（getRule 数据源，docx 转全文文本落库）
 */
@Data
@NoArgsConstructor
@TableName("wb_rule_doc")
public class RuleDoc {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 条款实例ID（HJ-GC-02） */
    private String clauseId;

    /** 知识库编号（GC-YY-01-01） */
    private String ruleCode;

    /** 规则文档标题 */
    private String ruleTitle;

    /** 适用区域 */
    private String region;

    /** 项目 */
    private String project;

    /** 子要素 */
    private String subElement;

    /** 版本号（V1.0） */
    private String version;

    /** 规则全文文本（docx 转文本） */
    private String docText;

    /** 原 docx 文件名 */
    private String fileName;

    /** 原 docx 留档路径 */
    private String filePath;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}