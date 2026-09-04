package com.wb.audit.task.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * wb_data_pull 系统取数记录实体（demo：只存行数+摘要）
 */
@Data
@NoArgsConstructor
@TableName("wb_data_pull")
public class DataPull {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;

    /** 条款实例ID */
    private String clauseId;

    /** 取数规则ID：FTR等 */
    private String ruleId;

    /** 来源文件（sim_*.csv） */
    private String fileName;

    /** 行数 */
    private Integer rowCount;

    /** 取数摘要（demo：样例行/计数，不存明细） */
    private String summaryJson;

    /** 取数文件全文（规则执行 getMaterial 用） */
    private String fullContent;

    /** 取数时间 */
    private LocalDateTime pulledAt;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}