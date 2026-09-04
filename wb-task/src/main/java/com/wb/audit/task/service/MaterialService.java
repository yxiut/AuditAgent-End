package com.wb.audit.task.service;

import com.wb.audit.task.dto.ConfirmVo;
import com.wb.audit.task.dto.PullPreviewVo;
import com.wb.audit.task.dto.UploadVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 材料服务（场景二·提交材料）：待办/待交/取数/上传/确认
 */
public interface MaterialService {

    /** 列待办任务（owner=me） */
    Map<String, Object> listTasks(Long userId);

    /** 列待交条款（当前登录人负责、TO_COLLECT） */
    Map<String, Object> listPending(Long taskId, Long userId);

    /** 预览取数（不落库） */
    PullPreviewVo previewPull(Long taskId, String clauseId);

    /** 上传材料 */
    UploadVo upload(Long userId, Long taskId, String clauseId, MultipartFile file);

    /** 确认提交（取数落库 + 绑材料 → COLLECTED） */
    ConfirmVo confirm(Long taskId, Long userId, List<Long> materialIds);

    /** 材料全文（getMaterial：系统取数 CSV 全文 + 上传解析文本，供规则执行） */
    Map<String, Object> content(Long taskId, String clauseId);
}