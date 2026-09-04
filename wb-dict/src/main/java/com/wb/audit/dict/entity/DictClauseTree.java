package com.wb.audit.dict.entity;

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
 * wb_dict_clause_tree 实体
 */
@Data
@NoArgsConstructor
@TableName("wb_dict_clause_tree")
public class DictClauseTree {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父节点ID */
    private Long parentId;

    /** 区域：零部件/过程（焊接）/过程（冲压） */
    private String region;

    /** 项目 */
    private String project;

    /** 子要素 */
    private String subElement;

    /** 条款实例ID：LJ-01 / HJ-GC-01 */
    private String clauseId;

    /** 条款名称 */
    private String clauseName;

    /** 必传材料标签（如 车间过程FTR问题跟踪管理表） */
    private String requiredUploadLabel;

    /** 取数规则ID（如 FTR；空=仅上传） */
    private String pullRuleId;

    /** 节点类型：REGION/PROJECT/SUB_ELEMENT/CLAUSE */
    private String nodeType;

    /** 层级 */
    private Integer levelNo;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
