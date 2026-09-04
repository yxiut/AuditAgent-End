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
 * wb_dict_entity_alias 实体
 */
@Data
@NoArgsConstructor
@TableName("wb_dict_entity_alias")
public class DictEntityAlias {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 实体类型：base/region/project/sub_element/clause */
    private String entityType;

    /** 标准值 */
    private String standardValue;

    /** 口语别名 */
    private String alias;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
