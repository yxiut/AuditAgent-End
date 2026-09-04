package com.wb.audit.task.constant;

import java.util.List;
import java.util.Map;

/**
 * 任务相关常量（场景一/二共用）
 */
public final class TaskConstants {

    /** 制造基地ID → 基地名 */
    public static final Map<Long, String> FACTORY_NAME = Map.of(
            1L, "龙兴工厂", 2L, "两江工厂", 3L, "扬帆工厂");

    /** 制造基地ID → 基地码（taskNo 用） */
    public static final Map<Long, String> FACTORY_CODE = Map.of(
            1L, "LX", 2L, "LJ", 3L, "YF");

    /** 取数规则ID → CSV 文件列表（随包 classpath:data/sim/ 下） */
    public static final Map<String, List<String>> PULL_RULE_FILES = Map.of(
            "FTR", List.of(
                    "sim_月度指标.csv",
                    "sim_原子问题明细.csv",
                    "sim_系统TOP3问题.csv",
                    "sim_当班问题列表.csv",
                    "sim_0MIS问题列表.csv"));

    private TaskConstants() {
    }
}