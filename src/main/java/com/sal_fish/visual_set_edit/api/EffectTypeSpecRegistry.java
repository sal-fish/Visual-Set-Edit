package com.sal_fish.visual_set_edit.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 外部效果类型的运行时索引，仅 api 包内部使用
final class EffectTypeSpecRegistry {

    private static final Map<String, EffectTypeSpec> SPECS = new ConcurrentHashMap<>();

    private EffectTypeSpecRegistry() {}

    // 同 id 只保留先注册的
    static void put(EffectTypeSpec spec) {
        SPECS.putIfAbsent(spec.id(), spec);
    }

    static EffectTypeSpec get(String id) {
        return id == null ? null : SPECS.get(id);
    }

    // 取该类型的行为实现，未注册时返回 null
    static EffectHandler handlerOf(String id) {
        EffectTypeSpec spec = get(id);
        return spec != null ? spec.handler() : null;
    }
}
