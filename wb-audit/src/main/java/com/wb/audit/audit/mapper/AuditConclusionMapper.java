package com.wb.audit.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wb.audit.audit.entity.AuditConclusion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 条款AI审核结论 Mapper
 */
@Mapper
public interface AuditConclusionMapper extends BaseMapper<AuditConclusion> {
}