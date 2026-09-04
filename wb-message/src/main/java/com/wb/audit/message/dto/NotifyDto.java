package com.wb.audit.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * 通用发送入参
 */
public class NotifyDto {

    /** 关联任务ID（可选） */
    private Long taskId;

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    /** 接收人企微 userid 列表（如 YangXiuTian/DuoLeGeDuo） */
    @NotEmpty(message = "接收人不能为空")
    private List<String> toUsers;

    private Map<String, Object> vars;

    public Long getTaskId() { return taskId; }

    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTemplateCode() { return templateCode; }

    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public List<String> getToUsers() { return toUsers; }

    public void setToUsers(List<String> toUsers) { this.toUsers = toUsers; }

    public Map<String, Object> getVars() { return vars; }

    public void setVars(Map<String, Object> vars) { this.vars = vars; }

}


