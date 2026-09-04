package com.wb.audit.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wb.audit.audit.entity.AuditRun;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI审核执行记录 Mapper
 */
@Mapper
public interface AuditRunMapper extends BaseMapper<AuditRun> {
}