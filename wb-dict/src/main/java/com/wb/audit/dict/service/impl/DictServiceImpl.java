package com.wb.audit.dict.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wb.audit.common.exception.BizException;
import com.wb.audit.common.result.ResultCode;
import com.wb.audit.dict.entity.DictClauseTree;
import com.wb.audit.dict.entity.DictEntityAlias;
import com.wb.audit.dict.mapper.DictClauseTreeMapper;
import com.wb.audit.dict.mapper.DictEntityAliasMapper;
import com.wb.audit.dict.service.DictService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典服务实现
 */
@Service
public class DictServiceImpl implements DictService {

    private final DictClauseTreeMapper clauseTreeMapper;
    private final DictEntityAliasMapper aliasMapper;

    @Override
    public Map<String, Object> normalize(String text) {
        // 1) 精确别名命中
        List<DictEntityAlias> exact = aliasMapper.selectList(
                new LambdaQueryWrapper<DictEntityAlias>().eq(DictEntityAlias::getAlias, text));
        if (!exact.isEmpty()) {
            List<String> values = exact.stream().map(DictEntityAlias::getStandardValue).distinct().toList();
            if (values.size() == 1) {
                return Map.of("standardValue", values.get(0), "candidates", List.of());
            }
            return Map.of("standardValue", null,
                    "candidates", exact.stream().map(e -> Map.of("id", e.getId(), "label", e.getStandardValue())).toList());
        }
        // 2) 模糊候选（供消歧）
        List<DictEntityAlias> fuzzy = aliasMapper.selectList(
                new LambdaQueryWrapper<DictEntityAlias>().like(DictEntityAlias::getAlias, text).last("limit 7"));
        return Map.of("standardValue", null,
                "candidates", fuzzy.stream().map(e -> Map.of("id", e.getId(), "label", e.getStandardValue())).toList());
    }

    @Override
    public List<String> listFactories() {
        return aliasMapper.selectList(
                        new LambdaQueryWrapper<DictEntityAlias>().eq(DictEntityAlias::getEntityType, "base"))
                .stream().map(DictEntityAlias::getStandardValue).distinct().toList();
    }

    @Override
    public Map<String, Object> clauseTree(String factory, String region) {
        LambdaQueryWrapper<DictClauseTree> qw = new LambdaQueryWrapper<DictClauseTree>()
                .eq(DictClauseTree::getNodeType, "CLAUSE")
                .eq(DictClauseTree::getStatus, 1);
        if (region != null && !region.isBlank()) {
            qw.eq(DictClauseTree::getRegion, region);
        }
        List<DictClauseTree> clauses = clauseTreeMapper.selectList(qw);

        // 区域 → 项目 → 子要素 → 条款
        Map<String, Map<String, Map<String, List<Map<String, String>>>>> tree = new LinkedHashMap<>();
        for (DictClauseTree c : clauses) {
            tree.computeIfAbsent(c.getRegion(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(c.getProject(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(c.getSubElement(), k -> new ArrayList<>())
                    .add(Map.of("clauseId", c.getClauseId(), "clauseName", c.getClauseName()));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        tree.forEach((regionName, projects) -> {
            List<Map<String, Object>> projectList = new ArrayList<>();
            projects.forEach((projectName, subElements) -> {
                List<Map<String, Object>> subList = new ArrayList<>();
                subElements.forEach((subName, clausesList) -> subList.add(Map.of("subElement", subName, "clauses", clausesList)));
                projectList.add(Map.of("project", projectName, "subElements", subList));
            });
            out.add(Map.of("region", regionName, "projects", projectList));
        });
        return Map.of("clauseTree", out);
    }

    @Override
    public DictClauseTree getClauseById(String clauseId) {
        DictClauseTree c = clauseTreeMapper.selectOne(
                new LambdaQueryWrapper<DictClauseTree>().eq(DictClauseTree::getClauseId, clauseId));
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND, "条款不存在: " + clauseId);
        }
        return c;
    }

    private static final Logger log = LoggerFactory.getLogger(DictServiceImpl.class);

    public DictServiceImpl(DictClauseTreeMapper clauseTreeMapper, DictEntityAliasMapper aliasMapper) {
        this.clauseTreeMapper = clauseTreeMapper;
        this.aliasMapper = aliasMapper;
    }

}
