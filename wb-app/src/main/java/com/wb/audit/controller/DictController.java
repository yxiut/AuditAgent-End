package com.wb.audit.controller;

import com.wb.audit.common.result.Result;
import jakarta.validation.Valid;
import com.wb.audit.dict.dto.NormalizeDto;
import com.wb.audit.dict.service.DictService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 字典接口
 */
@RestController
@RequestMapping("/api/dict")
public class DictController {

    private final DictService dictService;

    /** ② 口语实体归一 */
    @PostMapping("/normalize")
    public Result<Map<String, Object>> normalize(@Valid @RequestBody NormalizeDto dto) {
        return Result.ok(dictService.normalize(dto.getText()));
    }

    /** ③ 列可选工厂 */
    @GetMapping("/factories")
    public Result<Map<String, Object>> factories() {
        return Result.ok(Map.of("factories", dictService.listFactories()));
    }

    /** ④ 可选条款范围（四级树） */
    @GetMapping("/clauseTree")
    public Result<Map<String, Object>> clauseTree(@RequestParam(required = false) String factory,
                                                  @RequestParam(required = false) String region) {
        return Result.ok(dictService.clauseTree(factory, region));
    }

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

}



