package com.wb.audit.controller;

import com.wb.audit.auth.model.AuthUser;
import com.wb.audit.auth.service.AuthService;
import com.wb.audit.common.context.UserContext;
import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.Result;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.task.dto.ConfirmVo;
import com.wb.audit.task.dto.MaterialConfirmDto;
import com.wb.audit.task.dto.PullPreviewDto;
import com.wb.audit.task.dto.PullPreviewVo;
import com.wb.audit.task.dto.UploadVo;
import com.wb.audit.task.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 材料接口（场景二·提交材料）
 */
@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService materialService;
    private final AuthService authService;

    /** ① 列待办任务 */
    @GetMapping("/tasks")
    public Result<Map<String, Object>> listTasks() {
        return Result.ok(materialService.listTasks(currentUserId()));
    }

    /** ② 列待交条款 */
    @GetMapping("/tasks/pending")
    public Result<Map<String, Object>> listPending(@RequestParam Long taskId) {
        return Result.ok(materialService.listPending(taskId, currentUserId()));
    }

    /** ③ 材料全文（getMaterial：系统取数 CSV 全文 + 上传解析文本，供规则执行） */
    @GetMapping("/content")
    public Result<Map<String, Object>> content(@RequestParam Long taskId, @RequestParam String clauseId) {
        return Result.ok(materialService.content(taskId, clauseId));
    }

    /** ④ 预览取数（不落库） */
    @PostMapping("/tasks/pullPreview")
    public Result<PullPreviewVo> previewPull(@Valid @RequestBody PullPreviewDto dto) {
        return Result.ok(materialService.previewPull(dto.getTaskId(), dto.getClauseId()));
    }

    /** ④ 上传材料（multipart） */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadVo> upload(@RequestParam Long taskId,
                                   @RequestParam String clauseId,
                                   @RequestParam("file") MultipartFile file) {
        return Result.ok(materialService.upload(currentUserId(), taskId, clauseId, file));
    }

    /** ⑤ 确认提交 */
    @PostMapping("/tasks/confirm")
    public Result<ConfirmVo> confirm(@Valid @RequestBody MaterialConfirmDto dto) {
        return Result.ok(materialService.confirm(dto.getTaskId(), currentUserId(), dto.getMaterialIds()));
    }

    private Long currentUserId() {
        String name = UserContext.currentUserName();
        AuthUser user = name == null ? null : authService.getUserByName(name);
        if (user == null) {
            throw new BizException(ResultCode.NO_PERMISSION, "无法识别当前用户");
        }
        return user.getUserId();
    }

    public MaterialController(MaterialService materialService, AuthService authService) {
        this.materialService = materialService;
        this.authService = authService;
    }
}