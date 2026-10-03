package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.data.condition.Condition;
import com.sal_fish.visual_set_edit.data.condition.ConditionTypeRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

// 条件编辑界面注册表
public final class ConditionEditorRegistry {

    public interface Editor {
        void buildFields(ConditionEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing);

        void loadFrom(ConditionEditScreen screen, Condition condition);

        Condition create(ConditionEditScreen screen);
    }

    private static final Map<String, Supplier<Editor>> EDITORS = new LinkedHashMap<>();
    private static final Map<String, Editor> INSTANCES = new LinkedHashMap<>();
    private static boolean initialized;

    private ConditionEditorRegistry() {}

    public static void register(String id, Editor editor) {
        EDITORS.putIfAbsent(id, () -> editor);
    }

    // 延迟创建
    public static void register(String id, Supplier<Editor> factory) {
        EDITORS.putIfAbsent(id, factory);
    }

    public static Editor get(String id) {
        ensureInitialized();
        if (id == null) return null;
        Supplier<Editor> factory = EDITORS.get(id);
        if (factory == null) return null;
        return INSTANCES.computeIfAbsent(id, k -> factory.get());
    }

    public static Editor getOrDefault(String id) {
        Editor editor = get(id);
        return editor != null ? editor : get(ConditionTypeRegistry.defaultId());
    }

    // 首次取用时才加载界面注册
    private static void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        ConditionEditScreen.registerEditors();
    }
}
