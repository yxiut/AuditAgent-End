package com.wb.audit.audit.service;

import com.wb.audit.audit.dto.ConclusionVo;
import com.wb.audit.audit.dto.ConclusionWriteDto;
import com.wb.audit.audit.dto.IssueConfirmDto;
import com.wb.audit.audit.dto.ProgressVo;

import java.util.Map;

/**
 * 规则执行：写结论 / 查进度 / 人工确认（闭环到 HUMAN_REVIEW + 通知审核员）
 */
public interface AuditConclusionService {

    /** writeConclusion：落结论+问题明细，任务态→HUMAN_REVIEW，通知审核员 */
    ConclusionVo write(ConclusionWriteDto dto);

    /** getProgress：审核员查进度看 issues */
    ProgressVo progress(Long taskId);

    /** 人工复审：审核员逐条确认/驳回，确认后算条款分（已确认问题最低分，无问题10） */
    Map<String, Object> confirm(IssueConfirmDto dto);
}