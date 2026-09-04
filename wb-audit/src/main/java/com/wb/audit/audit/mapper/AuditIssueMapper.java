package com.wb.audit.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wb.audit.audit.entity.AuditIssue;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI审核问题明细 Mapper
 */
@Mapper
public interface AuditIssueMapper extends BaseMapper<AuditIssue> {
}