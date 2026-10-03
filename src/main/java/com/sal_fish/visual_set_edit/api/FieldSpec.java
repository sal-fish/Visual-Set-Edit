package com.sal_fish.visual_set_edit.api;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Function;

// 字段描述：外部用它声明一个可编辑字段，VSE 按 kind 生成对应控件
public final class FieldSpec {
    // 控件类型
    public enum Kind {TEXT, NUMBER, TOGGLE, DROPDOWN, PICKER}

    private final String key;
    private final String label;
    private final Kind kind;
    private final List<String> options;
    private final Function<String, Component> optionRenderer;
    private final String pickerId;
    private final String visibleWhenKey;
    private final List<String> visibleWhenValues;
    private final double min;
    private final double max;
    private final boolean integer;
    private final Object defaultValue;

    private FieldSpec(String key, String label, Kind kind, List<String> options,
                      Function<String, Component> optionRenderer, String pickerId,
                      String visibleWhenKey, List<String> visibleWhenValues,
                      double min, double max, boolean integer, Object defaultValue) {
        this.key = key;
        this.label = label;
        this.kind = kind;
        this.options = options;
        this.optionRenderer = optionRenderer;
        this.pickerId = pickerId;
        this.visibleWhenKey = visibleWhenKey;
        this.visibleWhenValues = visibleWhenValues;
        this.min = min;
        this.max = max;
        this.integer = integer;
        this.defaultValue = defaultValue;
    }

    // 文本框
    public static FieldSpec text(String key, String label) {
        return new FieldSpec(key, label, Kind.TEXT, List.of(), null, null, null, null, 0, 0, false, "");
    }

    // 小数框
    public static FieldSpec number(String key, String label, double min, double max) {
        return new FieldSpec(key, label, Kind.NUMBER, List.of(), null, null, null, null, min, max, false, 0.0);
    }

    // 整数框
    public static FieldSpec integer(String key, String label, int min, int max, int def) {
        return new FieldSpec(key, label, Kind.NUMBER, List.of(), null, null, null, null, min, max, true, def);
    }

    // 开关
    public static FieldSpec toggle(String key, String label, boolean def) {
        return new FieldSpec(key, label, Kind.TOGGLE, List.of(), null, null, null, null, 0, 0, false, def);
    }

    // 下拉，默认选中第一项；选项按字面量显示
    public static FieldSpec dropdown(String key, String label, List<String> options) {
        return new FieldSpec(key, label, Kind.DROPDOWN, options, null, null, null, null, 0, 0, false,
                options.isEmpty() ? "" : options.get(0));
    }

    // 下拉，选项显示文本由 optionRenderer 决定，存进 params 的仍是选项本身
    public static FieldSpec dropdown(String key, String label, List<String> options,
                                     Function<String, Component> optionRenderer) {
        return new FieldSpec(key, label, Kind.DROPDOWN, options, optionRenderer, null, null, null, 0, 0, false,
                options.isEmpty() ? "" : options.get(0));
    }

    // 选择框：文本框加一个按钮，点按钮打开 pickerId 对应的选择屏并把结果写回
    public static FieldSpec picker(String key, String label, String pickerId) {
        return new FieldSpec(key, label, Kind.PICKER, List.of(), null, pickerId, null, null, 0, 0, false, "");
    }

    // 仅当 key 字段的当前值在 values 中时才显示本字段；可对任意 kind 调用
    public FieldSpec visibleWhen(String key, String... values) {
        return new FieldSpec(this.key, this.label, this.kind, this.options, this.optionRenderer,
                this.pickerId, key, List.of(values), this.min, this.max, this.integer, this.defaultValue);
    }

    // 存入 params 的键名
    public String key() { return key; }

    // 界面上显示的标签文本；lang key 与普通文本都行，是 key 时按当前语言翻译
    public String label() { return label; }

    public Kind kind() { return kind; }

    public List<String> options() { return options; }

    // 下拉选项的显示文本生成器，为 null 时按字面量显示
    public Function<String, Component> optionRenderer() { return optionRenderer; }

    // 选择屏标识，由 VseClientApi.registerPicker 注册
    public String pickerId() { return pickerId; }

    // 显示条件所依赖的字段键，为 null 时恒显示
    public String visibleWhenKey() { return visibleWhenKey; }

    // 显示条件允许的取值
    public List<String> visibleWhenValues() { return visibleWhenValues; }

    public double min() { return min; }

    public double max() { return max; }

    public boolean integer() { return integer; }

    public Object defaultValue() { return defaultValue; }
}
