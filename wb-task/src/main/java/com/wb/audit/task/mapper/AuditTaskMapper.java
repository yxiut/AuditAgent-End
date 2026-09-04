package com.wb.audit.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wb.audit.task.entity.AuditTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditTaskMapper extends BaseMapper<AuditTask> {
}
