package com.sal_fish.visual_set_edit.data.effect;

import com.sal_fish.visual_set_edit.api.AbilitySpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 外部注册的能力注册表，双端
public final class AbilityTypeRegistry {

    private static final Map<String, AbilitySpec> BY_ID = new LinkedHashMap<>();

    private AbilityTypeRegistry() {}

    public static void register(AbilitySpec spec) {
        if (spec == null || spec.id() == null || spec.id().isEmpty() || spec.handler() == null) return;
        BY_ID.putIfAbsent(spec.id(), spec);
    }

    // 未注册时返回 null
    public static AbilitySpec get(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    // 已注册的能力 id，按注册顺序
    public static List<String> ids() {
        return new ArrayList<>(BY_ID.keySet());
    }
}
