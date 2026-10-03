package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.api.FieldSpec;
import com.sal_fish.visual_set_edit.api.PickerScreenFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

// FieldSpec 列表的渲染面板，持有字段值并生成控件
class FieldSpecPanel {

    // 面板宿主需要提供的能力
    interface Host {
        Font textFont();

        void attach(AbstractWidget widget);

        // 触发字段变化后重建界面
        void rebuild();

        // 打开选择屏时的父界面
        Screen self();
    }

    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, EditBox> textBoxes = new LinkedHashMap<>();
    private final Map<String, EditBox> numberBoxes = new LinkedHashMap<>();

    static Host hostOf(EffectEditScreen screen) {
        return new Host() {
            @Override public Font textFont() { return screen.fieldFont(); }

            @Override public void attach(AbstractWidget widget) { screen.attachField(widget); }

            @Override public void rebuild() { screen.init(); }

            @Override public Screen self() { return screen; }
        };
    }

    static Host hostOf(ConditionEditScreen screen) {
        return new Host() {
            @Override public Font textFont() { return screen.fieldFont(); }

            @Override public void attach(AbstractWidget widget) { screen.attachField(widget); }

            @Override public void rebuild() { screen.init(); }

            @Override public Screen self() { return screen; }
        };
    }

    void loadFrom(Map<String, Object> params) {
        values.clear();
        textBoxes.clear();
        numberBoxes.clear();
        if (params != null) values.putAll(params);
    }

    void clear() {
        values.clear();
        textBoxes.clear();
        numberBoxes.clear();
    }

    // 渲染字段，返回新的 y
    int render(Host host, List<FieldSpec> fields, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        textBoxes.clear();
        numberBoxes.clear();
        Font font = host.textFont();
        int x = centerX - totalWidth / 2;

        seedDefaults(fields);
        Set<String> triggers = collectTriggerKeys(fields);

        for (FieldSpec field : fields) {
            if (!isVisible(field)) continue;
            host.attach(new StringWidget(x, y, totalWidth, rowHeight, Component.translatable(field.label()), font));
            y += rowHeight;

            switch (field.kind()) {
                case TEXT -> {
                    EditBox box = new EditBox(font, x, y, totalWidth, rowHeight, Component.translatable(field.label()));
                    box.setMaxLength(5201314);
                    box.setValue(stringOf(values.get(field.key()), field.defaultValue()));
                    box.setResponder(s -> values.put(field.key(), s));
                    textBoxes.put(field.key(), box);
                    host.attach(box);
                }
                case NUMBER -> {
                    EditBox box = new EditBox(font, x, y, totalWidth, rowHeight, Component.translatable(field.label()));
                    box.setMaxLength(32);
                    box.setValue(stringOf(values.get(field.key()), field.defaultValue()));
                    box.setResponder(s -> values.put(field.key(), s));
                    numberBoxes.put(field.key(), box);
                    host.attach(box);
                }
                case TOGGLE -> {
                    boolean init = boolOf(values.get(field.key()), field.defaultValue());
                    values.put(field.key(), init);
                    boolean trigger = triggers.contains(field.key());
                    host.attach(CycleButton.booleanBuilder(
                                    Component.translatable("options.on"),
                                    Component.translatable("options.off"))
                            .withInitialValue(init)
                            .create(x, y, totalWidth, rowHeight, Component.translatable(field.label()),
                                    (button, value) -> {
                                        values.put(field.key(), value);
                                        if (trigger) host.rebuild();
                                    }));
                }
                case DROPDOWN -> {
                    List<String> options = field.options();
                    // 空选项会让 CycleButton 抛 IllegalStateException，这里只画标签
                    if (!options.isEmpty()) {
                        String init = stringOf(values.get(field.key()), field.defaultValue());
                        if (!options.contains(init)) init = options.get(0);
                        values.put(field.key(), init);
                        boolean trigger = triggers.contains(field.key());
                        host.attach(CycleButton.<String>builder(optionRendererOf(field))
                                .withValues(options)
                                .displayOnlyValue()
                                .withInitialValue(init)
                                .create(x, y, totalWidth, rowHeight, Component.translatable(field.label()),
                                        (button, value) -> {
                                            values.put(field.key(), value);
                                            if (trigger) host.rebuild();
                                        }));
                    }
                }
                case PICKER -> {
                    int editWidth = totalWidth - 22;
                    EditBox box = new EditBox(font, x, y, editWidth, rowHeight, Component.translatable(field.label()));
                    box.setMaxLength(5201314);
                    box.setValue(stringOf(values.get(field.key()), field.defaultValue()));
                    box.setResponder(s -> values.put(field.key(), s));
                    textBoxes.put(field.key(), box);
                    host.attach(box);

                    PickerScreenFactory factory = PickerScreenRegistry.get(field.pickerId());
                    if (factory != null) {
                        host.attach(Button.builder(Component.literal("📦"), b ->
                                Minecraft.getInstance().setScreen(factory.create(host.self(), picked -> {
                                    box.setValue(picked);
                                    values.put(field.key(), picked);
                                }))
                        ).pos(x + editWidth + 2, y).size(20, rowHeight).build());
                    }
                }
            }
            y += rowHeight + spacing;
        }
        return y;
    }

    // 收集当前值，不可见字段不写入
    Map<String, Object> collect(List<FieldSpec> fields) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (FieldSpec field : fields) {
            if (!isVisible(field)) continue;
            Object value;
            switch (field.kind()) {
                case TEXT, PICKER -> {
                    EditBox box = textBoxes.get(field.key());
                    value = box != null ? box.getValue() : values.get(field.key());
                }
                case NUMBER -> {
                    EditBox box = numberBoxes.get(field.key());
                    value = box != null ? parseNumber(box.getValue(), field) : values.get(field.key());
                }
                default -> value = values.get(field.key());
            }
            if (value != null) out.put(field.key(), value);
        }
        return out;
    }

    boolean hasVisibleField(List<FieldSpec> fields) {
        for (FieldSpec field : fields) {
            if (isVisible(field)) return true;
        }
        return false;
    }

    // 先把各字段的初始值补进 values，显示条件才能不依赖字段声明顺序
    private void seedDefaults(List<FieldSpec> fields) {
        for (FieldSpec field : fields) {
            if (values.containsKey(field.key())) continue;
            values.put(field.key(), field.kind() == FieldSpec.Kind.TOGGLE
                    ? boolOf(null, field.defaultValue())
                    : stringOf(null, field.defaultValue()));
        }
    }

    // 被其他字段的显示条件引用的字段，改动后需要重建界面
    private static Set<String> collectTriggerKeys(List<FieldSpec> fields) {
        Set<String> keys = new HashSet<>();
        for (FieldSpec field : fields) {
            if (field.visibleWhenKey() != null) keys.add(field.visibleWhenKey());
        }
        return keys;
    }

    private boolean isVisible(FieldSpec field) {
        String key = field.visibleWhenKey();
        if (key == null) return true;
        Object current = values.get(key);
        return current != null && field.visibleWhenValues().contains(String.valueOf(current));
    }

    private static Function<String, Component> optionRendererOf(FieldSpec field) {
        Function<String, Component> renderer = field.optionRenderer();
        return renderer != null ? renderer : Component::literal;
    }

    private static Object parseNumber(String raw, FieldSpec field) {
        try {
            String text = raw.trim();
            if (field.integer()) return (long) Double.parseDouble(text);
            double v = Double.parseDouble(text);
            return Math.max(field.min(), Math.min(field.max(), v));
        } catch (Exception e) {
            return field.defaultValue();
        }
    }

    private static String stringOf(Object value, Object fallback) {
        Object v = value != null ? value : fallback;
        return v == null ? "" : String.valueOf(v);
    }

    private static boolean boolOf(Object value, Object fallback) {
        Object v = value != null ? value : fallback;
        if (v instanceof Boolean b) return b;
        return v != null && Boolean.parseBoolean(String.valueOf(v));
    }
}
