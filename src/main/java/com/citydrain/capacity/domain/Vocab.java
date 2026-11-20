package com.citydrain.capacity.domain;

/**
 * 开放词表常量：观测类型、目标类型、外部引用类型、覆盖账本条目、限制来源标记。
 * 使用 String 常量而非枚举，便于外部系统扩展取值。
 */
public final class Vocab {

    private Vocab() {}

    // 观测类型
    public static final String OBS_FLOW = "FLOW";                       // 流量
    public static final String OBS_WATER_LEVEL = "WATER_LEVEL";         // 水位（节点/管段）
    public static final String OBS_RAINFALL = "RAINFALL";               // 降雨
    public static final String OBS_DOWNSTREAM_LEVEL = "DOWNSTREAM_LEVEL"; // 下游边界水位
    public static final String OBS_GATE_STATE = "GATE_STATE";           // 闸门状态

    // 观测目标类型
    public static final String TARGET_SEGMENT = "SEGMENT";
    public static final String TARGET_NODE = "NODE";
    public static final String TARGET_GATE = "GATE";
    public static final String TARGET_BOUNDARY = "BOUNDARY";

    // 外部引用类型（只读接入）
    public static final String REF_DEFECT = "DEFECT_OBSERVATION";       // 缺陷观测
    public static final String REF_INSPECTION = "INSPECTION_COVERAGE";  // 检查覆盖
    public static final String REF_REPAIR = "REPAIR_ACTION";            // 维修作用范围/验收结果

    // 覆盖账本条目
    public static final String ITEM_PARAM_VERSION = "PARAM_VERSION";
    public static final String ITEM_PARAM_GEOMETRY = "PARAM_GEOMETRY";
    public static final String ITEM_PARAM_SLOPE = "PARAM_SLOPE";
    public static final String ITEM_PARAM_ROUGHNESS = "PARAM_ROUGHNESS";
    public static final String ITEM_PARAM_SILTATION = "PARAM_SILTATION";
    public static final String ITEM_OBS_DOWNSTREAM_LEVEL = "OBS_DOWNSTREAM_LEVEL";
    public static final String ITEM_OBS_GATE_STATE = "OBS_GATE_STATE";
    public static final String ITEM_OBS_FLOW = "OBS_FLOW";
    public static final String ITEM_CALIBRATION_SAMPLES = "CALIBRATION_SAMPLES";
    public static final String ITEM_EXT_DEFECT_REF = "EXT_DEFECT_REF";
    public static final String ITEM_EXT_INSPECTION_REF = "EXT_INSPECTION_COVERAGE_REF";

    // 覆盖账本状态
    public static final String ST_AVAILABLE = "AVAILABLE";
    public static final String ST_ASSUMED = "ASSUMED";
    public static final String ST_MISSING = "MISSING";
    public static final String ST_CONFLICTED = "CONFLICTED";

    // 限制来源标记（写入 capacity_result.limitations）
    public static final String LIM_ROUGHNESS_MISSING = "ROUGHNESS_MISSING";
    public static final String LIM_DOWNSTREAM_LEVEL_MISSING = "DOWNSTREAM_LEVEL_MISSING";
    public static final String LIM_SILTATION_MISSING = "SILTATION_MISSING";
    public static final String LIM_BACKWATER_DOMINATED = "BACKWATER_DOMINATED";
    public static final String LIM_GATE_CLOSED = "GATE_CLOSED";
    public static final String LIM_GATE_PARTIAL = "GATE_PARTIAL";
    public static final String LIM_CALIBRATION_INSUFFICIENT = "CALIBRATION_INSUFFICIENT";

    // 不可判定原因
    public static final String REASON_NO_PARAM_VERSION = "NO_PARAMETER_VERSION_AT_AS_OF";
    public static final String REASON_GEOMETRY_MISSING = "GEOMETRY_MISSING";
    public static final String REASON_SLOPE_MISSING = "SLOPE_MISSING";
}
