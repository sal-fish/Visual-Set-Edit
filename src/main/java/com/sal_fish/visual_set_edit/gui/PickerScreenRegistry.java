package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.api.PickerScreenFactory;

import java.util.LinkedHashMap;
import java.util.Map;

// 选择屏注册表，仅客户端
public final class PickerScreenRegistry {

    private static final Map<String, PickerScreenFactory> FACTORIES = new LinkedHashMap<>();

    private PickerScreenRegistry() {}

    public static void register(String pickerId, PickerScreenFactory factory) {
        if (pickerId == null || pickerId.isEmpty() || factory == null) return;
        FACTORIES.putIfAbsent(pickerId, factory);
    }

    // 未注册时返回 null，调用方按普通文本框渲染
    public static PickerScreenFactory get(String pickerId) {
        return pickerId == null ? null : FACTORIES.get(pickerId);
    }
}
