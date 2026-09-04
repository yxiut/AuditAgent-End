package com.wb.audit.dict.service;

import com.wb.audit.dict.entity.DictClauseTree;

import java.util.List;
import java.util.Map;

/**
 * 字典服务：归一 / 工厂 / 条款树
 */
public interface DictService {

    /** 口语归一：唯一→standardValue；多候选→candidates；无→空 */
    Map<String, Object> normalize(String text);

    /** 列可选工厂（entity_type=base 的标准名去重） */
    List<String> listFactories();

    /** 可选条款范围（区域→项目→子要素→条款） */
    Map<String, Object> clauseTree(String factory, String region);

    /** 按 clauseId 查条款（区域隔离校验用） */
    DictClauseTree getClauseById(String clauseId);
}
