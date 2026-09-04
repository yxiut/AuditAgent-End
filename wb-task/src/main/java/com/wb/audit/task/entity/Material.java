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
 * wb_material 上传材料实体
 */
@Data
@NoArgsConstructor
@TableName("wb_material")
public class Material {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;

    /** 条款实例ID */
    private String clauseId;

    /** 原始文件名 */
    private String fileName;

    /** 存储路径（本地 uploads/ 下） */
    private String filePath;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 扩展名/类型：xlsx/pdf/jpg... */
    private String fileType;

    /** 上传文件解析文本（xlsx→文本，规则执行 getMaterial 用） */
    private String parsedText;

    /** 上传人ID（陈志强=4） */
    private Long uploaderId;

    /** 材料态：UPLOADED/CONFIRMED */
    private String state;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}