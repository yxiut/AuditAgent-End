package com.wb.audit.audit.dto;

/**
 * writeConclusion 出参
 */
public class ConclusionVo {

    /** 是否已写入 */
    private boolean written;

    /** 结论ID */
    private Long conclusionId;

    public boolean isWritten() { return written; }

    public void setWritten(boolean written) { this.written = written; }

    public Long getConclusionId() { return conclusionId; }

    public void setConclusionId(Long conclusionId) { this.conclusionId = conclusionId; }
}