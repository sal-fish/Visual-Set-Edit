package com.sal_fish.visual_set_edit.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 外部条件类型的运行时索引，仅 api 包内部使用
final class ConditionTypeSpecRegistry {

    private static final Map<String, ConditionTypeSpec> SPECS = new ConcurrentHashMap<>();

    private ConditionTypeSpecRegistry() {}

    // 同 id 只保留先注册的
    static void put(ConditionTypeSpec spec) {
        SPECS.putIfAbsent(spec.id(), spec);
    }

    static ConditionTypeSpec get(String id) {
        return id == null ? null : SPECS.get(id);
    }

    // 取该类型的行为实现，未注册时返回 null
    static ConditionHandler handlerOf(String id) {
        ConditionTypeSpec spec = get(id);
        return spec != null ? spec.handler() : null;
    }
}
