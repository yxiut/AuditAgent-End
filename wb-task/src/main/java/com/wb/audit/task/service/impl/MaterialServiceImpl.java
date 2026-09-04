package com.wb.audit.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.dict.entity.DictClauseTree;
import com.wb.audit.dict.service.DictService;
import com.wb.audit.task.constant.TaskConstants;
import com.wb.audit.task.dto.ConfirmVo;
import com.wb.audit.task.dto.MaterialTaskVo;
import com.wb.audit.task.dto.PendingClauseVo;
import com.wb.audit.task.dto.PullPreviewVo;
import com.wb.audit.task.dto.UploadVo;
import com.wb.audit.task.entity.AuditTask;
import com.wb.audit.task.entity.DataPull;
import com.wb.audit.task.entity.Material;
import com.wb.audit.task.entity.TaskAssignment;
import com.wb.audit.task.entity.TaskClause;
import com.wb.audit.task.entity.TaskNodeLog;
import com.wb.audit.task.mapper.AuditTaskMapper;
import com.wb.audit.task.mapper.DataPullMapper;
import com.wb.audit.task.mapper.MaterialMapper;
import com.wb.audit.task.mapper.TaskAssignmentMapper;
import com.wb.audit.task.mapper.TaskClauseMapper;
import com.wb.audit.task.mapper.TaskNodeLogMapper;
import com.wb.audit.task.service.MaterialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.File;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 材料服务实现（场景二·提交材料 DEMO 简化版）
 */
@Service
public class MaterialServiceImpl implements MaterialService {

    private final DictService dictService;
    private final AuditTaskMapper auditTaskMapper;
    private final TaskClauseMapper taskClauseMapper;
    private final TaskAssignmentMapper taskAssignmentMapper;
    private final TaskNodeLogMapper taskNodeLogMapper;
    private final MaterialMapper materialMapper;
    private final DataPullMapper dataPullMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 上传目录（可配 app.upload-dir） */
    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public Map<String, Object> listTasks(Long userId) {
        List<Long> taskIds = taskAssignmentMapper.selectList(
                        new LambdaQueryWrapper<TaskAssignment>().eq(TaskAssignment::getAssigneeId, userId))
                .stream().map(TaskAssignment::getTaskId).distinct().toList();
        if (taskIds.isEmpty()) {
            return Map.of("tasks", List.of());
        }
        List<AuditTask> tasks = auditTaskMapper.selectList(new LambdaQueryWrapper<AuditTask>()
                .in(AuditTask::getId, taskIds)
                .eq(AuditTask::getGlobalState, "COLLECTING")
                .orderByDesc(AuditTask::getId));
        List<MaterialTaskVo> vos = new ArrayList<>();
        for (AuditTask t : tasks) {
            long pending = taskClauseMapper.selectCount(new LambdaQueryWrapper<TaskClause>()
                    .eq(TaskClause::getTaskId, t.getId())
                    .eq(TaskClause::getAssigneeId, userId)
                    .eq(TaskClause::getState, "TO_COLLECT"));
            MaterialTaskVo vo = new MaterialTaskVo();
            vo.setTaskId(t.getId());
            vo.setTaskNo(t.getTaskNo());
            vo.setFactoryName(TaskConstants.FACTORY_NAME.getOrDefault(t.getFactoryId(), String.valueOf(t.getFactoryId())));
            vo.setPeriod(t.getPeriodStart() + " ~ " + t.getPeriodEnd());
            vo.setRegion(t.getRegion());
            vo.setGlobalState(t.getGlobalState());
            vo.setPendingCount((int) pending);
            vos.add(vo);
        }
        return Map.of("tasks", vos);
    }

    @Override
    public Map<String, Object> listPending(Long taskId, Long userId) {
        List<TaskClause> clauses = taskClauseMapper.selectList(new LambdaQueryWrapper<TaskClause>()
                .eq(TaskClause::getTaskId, taskId)
                .eq(TaskClause::getAssigneeId, userId)
                .eq(TaskClause::getState, "TO_COLLECT")
                .orderByAsc(TaskClause::getId));
        List<PendingClauseVo> vos = new ArrayList<>();
        for (TaskClause tc : clauses) {
            PendingClauseVo vo = new PendingClauseVo();
            vo.setClauseId(tc.getClauseId());
            vo.setClauseName(tc.getClauseName());
            vo.setRegion(tc.getRegion());
            vo.setProject(tc.getProject());
            vo.setSubElement(tc.getSubElement());
            vo.setState(tc.getState());
            DictClauseTree c = dictService.getClauseById(tc.getClauseId());
            String label = c.getRequiredUploadLabel();
            vo.setRequiredUploadLabels(label == null || label.isBlank() ? List.of() : List.of(label));
            vo.setPullRuleId(c.getPullRuleId());
            vos.add(vo);
        }
        return Map.of("clauses", vos);
    }

    @Override
    public PullPreviewVo previewPull(Long taskId, String clauseId) {
        DictClauseTree c = dictService.getClauseById(clauseId);
        String ruleId = c.getPullRuleId();
        PullPreviewVo vo = new PullPreviewVo();
        vo.setRuleId(ruleId == null ? "" : ruleId);
        List<String> files = TaskConstants.PULL_RULE_FILES.getOrDefault(ruleId, List.of());
        vo.setFiles(files);
        int totalRows = 0;
        List<Map<String, String>> sample = new ArrayList<>();
        for (String f : files) {
            CsvData d = readCsv(f);
            totalRows += d.rows;
            if (sample.isEmpty()) {
                sample = d.sample;
            }
        }
        vo.setRows(totalRows);
        vo.setSample(sample);
        return vo;
    }

    @Override
    public UploadVo upload(Long userId, Long taskId, String clauseId, MultipartFile file) {
        TaskClause clause = taskClauseMapper.selectOne(new LambdaQueryWrapper<TaskClause>()
                .eq(TaskClause::getTaskId, taskId)
                .eq(TaskClause::getClauseId, clauseId));
        if (clause == null) {
            throw new BizException(ResultCode.NOT_FOUND, "任务条款不存在: taskId=" + taskId + " clauseId=" + clauseId);
        }
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot + 1);
        }
        String storedName = taskId + "_" + clauseId + "_" + System.currentTimeMillis() + (dot >= 0 ? original.substring(dot) : "");
        Path dir = Paths.get(uploadDir, String.valueOf(taskId));
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName);
            file.transferTo(target.toAbsolutePath());
            log.info("材料已保存: {}", target.toAbsolutePath());
        } catch (IOException e) {
            throw new BizException(ResultCode.SERVER_ERROR, "文件保存失败: " + e.getMessage());
        }
        Material m = new Material();
        m.setTaskId(taskId);
        m.setClauseId(clauseId);
        m.setFileName(original);
        m.setFilePath(dir.resolve(storedName).toString());
        m.setFileSize(file.getSize());
        m.setFileType(ext);
        m.setUploaderId(userId);
        m.setState("UPLOADED");
        // xlsx 解析全文（规则执行 getMaterial 用；解析失败不阻断上传）
        if ("xlsx".equalsIgnoreCase(ext) || "xls".equalsIgnoreCase(ext)) {
            try {
                m.setParsedText(parseExcelText(m.getFilePath()));
            } catch (Exception ex) {
                log.warn("上传文件解析失败, 跳过全文: {}", ex.getMessage());
            }
        }
        materialMapper.insert(m);

        // 归类：文件名是否命中必传标签
        boolean classified = true;
        DictClauseTree c = dictService.getClauseById(clauseId);
        String label = c.getRequiredUploadLabel();
        if (label != null && !label.isBlank()) {
            classified = original.contains(label);
        }
        UploadVo vo = new UploadVo();
        vo.setMaterialId(m.getId());
        vo.setTaskId(taskId);
        vo.setClauseId(clauseId);
        vo.setFileName(original);
        vo.setFileSize(m.getFileSize());
        vo.setClassified(classified);
        vo.setStatus("UPLOADED");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConfirmVo confirm(Long taskId, Long userId, List<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "materialIds 不能为空");
        }
        List<Material> materials = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .in(Material::getId, materialIds)
                .eq(Material::getTaskId, taskId));
        if (materials.size() != materialIds.size()) {
            throw new BizException(ResultCode.PARAM_ERROR, "存在不属于该任务的材料ID");
        }
        Map<String, List<Material>> byClause = new LinkedHashMap<>();
        for (Material m : materials) {
            byClause.computeIfAbsent(m.getClauseId(), k -> new ArrayList<>()).add(m);
        }
        List<Map<String, String>> clauseStates = new ArrayList<>();
        List<Map<String, Object>> pulled = new ArrayList<>();
        for (Map.Entry<String, List<Material>> e : byClause.entrySet()) {
            String clauseId = e.getKey();
            TaskClause clause = taskClauseMapper.selectOne(new LambdaQueryWrapper<TaskClause>()
                    .eq(TaskClause::getTaskId, taskId)
                    .eq(TaskClause::getClauseId, clauseId));
            if (clause == null) {
                throw new BizException(ResultCode.NOT_FOUND, "条款不存在: " + clauseId);
            }
            if (!"COLLECTED".equals(clause.getState())) {
                clause.setState("COLLECTED");
                clause.setMaterialState("COLLECTED");
                taskClauseMapper.updateById(clause);
                taskNodeLogMapper.insert(nodeLog(taskId, clauseId, "CLAUSE", "TO_COLLECT", "COLLECTED", userId));
            }
            for (Material m : e.getValue()) {
                if (!"CONFIRMED".equals(m.getState())) {
                    m.setState("CONFIRMED");
                    materialMapper.updateById(m);
                    taskNodeLogMapper.insert(nodeLog(taskId, clauseId, "MATERIAL", "UPLOADED", "CONFIRMED", userId));
                }
            }
            // 正式取数落库（行数+摘要）
            DictClauseTree c = dictService.getClauseById(clauseId);
            String ruleId = c.getPullRuleId();
            if (ruleId != null && !ruleId.isBlank()) {
                int total = 0;
                for (String f : TaskConstants.PULL_RULE_FILES.getOrDefault(ruleId, List.of())) {
                    CsvData d = readCsv(f);
                    total += d.rows;
                    DataPull dp = new DataPull();
                    dp.setTaskId(taskId);
                    dp.setClauseId(clauseId);
                    dp.setRuleId(ruleId);
                    dp.setFileName(f);
                    dp.setRowCount(d.rows);
                    try {
                        dp.setSummaryJson(objectMapper.writeValueAsString(d.sample));
                    } catch (Exception ex) {
                        dp.setSummaryJson("[]");
                    }
                    dp.setFullContent(d.fullText);
                    dp.setPulledAt(LocalDateTime.now());
                    dataPullMapper.insert(dp);
                }
                pulled.add(Map.of("ruleId", ruleId, "rows", total));
            }
            clauseStates.add(Map.of("clauseId", clauseId, "state", clause.getState()));
        }
        // allCollected：该任务所有条款是否全部 COLLECTED
        long notCollected = taskClauseMapper.selectCount(new LambdaQueryWrapper<TaskClause>()
                .eq(TaskClause::getTaskId, taskId)
                .ne(TaskClause::getState, "COLLECTED"));
        boolean allCollected = notCollected == 0;
        AuditTask task = auditTaskMapper.selectById(taskId);
        if (task != null) {
            task.setMaterialAllCollected(allCollected ? 1 : 0);
            auditTaskMapper.updateById(task);
        }
        ConfirmVo vo = new ConfirmVo();
        vo.setAllCollected(allCollected);
        vo.setClauseStates(clauseStates);
        vo.setPulled(pulled);
        return vo;
    }

    /** 用 POI 把 xlsx 解析成文本：每个 sheet，行内制表符分隔、行间换行 */
    private String parseExcelText(String path) {
        StringBuilder sb = new StringBuilder();
        try (Workbook wb = WorkbookFactory.create(new File(path))) {
            DataFormatter fmt = new DataFormatter();
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                sb.append("### sheet: ").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    boolean first = true;
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        if (!first) {
                            sb.append('\t');
                        }
                        sb.append(fmt.formatCellValue(cell));
                        first = false;
                    }
                    sb.append('\n');
                }
            }
        } catch (Exception e) {
            throw new BizException(ResultCode.SERVER_ERROR, "xlsx 解析失败: " + e.getMessage());
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> content(Long taskId, String clauseId) {
        List<Map<String, Object>> materials = new ArrayList<>();
        List<DataPull> pulls = dataPullMapper.selectList(new LambdaQueryWrapper<DataPull>()
                .eq(DataPull::getTaskId, taskId)
                .eq(DataPull::getClauseId, clauseId)
                .orderByAsc(DataPull::getId));
        for (DataPull p : pulls) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("materialId", "PULL-" + p.getId());
            m.put("source", "system_pull");
            m.put("name", p.getFileName());
            m.put("ruleId", p.getRuleId());
            m.put("rowCount", p.getRowCount());
            m.put("fullText", p.getFullContent() == null ? "" : p.getFullContent());
            materials.add(m);
        }
        List<Material> ups = materialMapper.selectList(new LambdaQueryWrapper<Material>()
                .eq(Material::getTaskId, taskId)
                .eq(Material::getClauseId, clauseId)
                .orderByAsc(Material::getId));
        for (Material up : ups) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("materialId", "M-" + up.getId());
            m.put("source", "upload");
            m.put("name", up.getFileName());
            m.put("fileType", up.getFileType());
            m.put("filePath", up.getFilePath());
            m.put("fullText", up.getParsedText() == null ? "" : up.getParsedText());
            materials.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("taskId", taskId);
        out.put("clauseId", clauseId);
        out.put("materials", materials);
        return out;
    }

    private TaskNodeLog nodeLog(Long taskId, String clauseId, String nodeType,
                                String from, String to, Long operatorId) {
        TaskNodeLog log = new TaskNodeLog();
        log.setTaskId(taskId);
        log.setClauseId(clauseId);
        log.setNodeType(nodeType);
        log.setFromState(from);
        log.setToState(to);
        log.setOperatorId(operatorId);
        log.setSource("MANUAL");
        return log;
    }

    /** 读取 classpath:data/sim/{fileName}，返回行数、样例行与全文 */
    private CsvData readCsv(String fileName) {
        ClassPathResource res = new ClassPathResource("data/sim/" + fileName);
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder full = new StringBuilder();
            String headerLine = br.readLine();
            if (headerLine != null && headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1); // 去 UTF-8 BOM
            }
            if (headerLine != null) {
                full.append(headerLine).append('\n');
            }
            String[] cols = headerLine == null ? new String[0] : headerLine.split(",");
            List<Map<String, String>> sample = new ArrayList<>();
            int rows = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                rows++;
                full.append(line).append('\n');
                if (sample.size() < 2) {
                    String[] vals = line.split(",", -1);
                    Map<String, String> m = new LinkedHashMap<>();
                    for (int i = 0; i < cols.length; i++) {
                        m.put(cols[i], i < vals.length ? vals[i] : "");
                    }
                    sample.add(m);
                }
            }
            return new CsvData(rows, sample, full.toString());
        } catch (IOException e) {
            throw new BizException(ResultCode.SERVER_ERROR, "读取取数文件失败: " + fileName);
        }
    }

    /** CSV 读取结果 */
    private static final class CsvData {
        private final int rows;
        private final List<Map<String, String>> sample;
        private final String fullText;

        private CsvData(int rows, List<Map<String, String>> sample, String fullText) {
            this.rows = rows;
            this.sample = sample;
            this.fullText = fullText;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(MaterialServiceImpl.class);

    public MaterialServiceImpl(DictService dictService, AuditTaskMapper auditTaskMapper,
                               TaskClauseMapper taskClauseMapper, TaskAssignmentMapper taskAssignmentMapper,
                               TaskNodeLogMapper taskNodeLogMapper, MaterialMapper materialMapper,
                               DataPullMapper dataPullMapper) {
        this.dictService = dictService;
        this.auditTaskMapper = auditTaskMapper;
        this.taskClauseMapper = taskClauseMapper;
        this.taskAssignmentMapper = taskAssignmentMapper;
        this.taskNodeLogMapper = taskNodeLogMapper;
        this.materialMapper = materialMapper;
        this.dataPullMapper = dataPullMapper;
    }
}