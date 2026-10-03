package com.sal_fish.visual_set_edit.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

// 效果类型规格：外部用它声明一个新效果类型
public final class EffectTypeSpec {

    private final String id;
    private final EffectHandler handler;
    private final List<FieldSpec> fields;
    private final BooleanSupplier available;
    private final Function<DataDrivenEffectEntry, String> displayText;

    private EffectTypeSpec(Builder builder) {
        this.id = builder.id;
        this.handler = builder.handler;
        this.fields = List.copyOf(builder.fields);
        this.available = builder.available;
        this.displayText = builder.displayText;
    }

    public static Builder create(String id) {
        return new Builder(id);
    }

    public String id() { return id; }

    public EffectHandler handler() { return handler; }

    public List<FieldSpec> fields() { return fields; }

    public BooleanSupplier available() { return available; }

    public Function<DataDrivenEffectEntry, String> displayText() { return displayText; }

    public static final class Builder {
        private final String id;
        private final List<FieldSpec> fields = new ArrayList<>();
        private EffectHandler handler;
        private BooleanSupplier available = () -> true;
        private Function<DataDrivenEffectEntry, String> displayText;

        private Builder(String id) {
            this.id = id;
        }

        // 行为实现，必填
        public Builder handler(EffectHandler handler) {
            this.handler = handler;
            return this;
        }

        // 追加一个可编辑字段，可多次调用；字段的 label、选项文本、显示条件、选择屏都在 FieldSpec 上描述
        public Builder field(FieldSpec field) {
            this.fields.add(field);
            return this;
        }

        // 该类型是否出现在下拉里，默认恒真
        public Builder available(BooleanSupplier available) {
            this.available = available;
            return this;
        }

        // 自定义列表显示文本，不设时用 lang key visual_set_edit.gui.effect.type.<id>
        public Builder displayText(Function<DataDrivenEffectEntry, String> displayText) {
            this.displayText = displayText;
            return this;
        }

        public EffectTypeSpec build() {
            return new EffectTypeSpec(this);
        }
    }
}
