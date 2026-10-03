package com.sal_fish.visual_set_edit.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

// 条件类型规格：外部用它声明一个新条件类型
public final class ConditionTypeSpec {

    private final String id;
    private final ConditionHandler handler;
    private final List<FieldSpec> fields;
    private final BooleanSupplier available;
    private final Function<DataDrivenCondition, String> displayText;
    private final boolean requiresPlayer;

    private ConditionTypeSpec(Builder builder) {
        this.id = builder.id;
        this.handler = builder.handler;
        this.fields = List.copyOf(builder.fields);
        this.available = builder.available;
        this.displayText = builder.displayText;
        this.requiresPlayer = builder.requiresPlayer;
    }

    public static Builder create(String id) {
        return new Builder(id);
    }

    public String id() { return id; }

    public ConditionHandler handler() { return handler; }

    public List<FieldSpec> fields() { return fields; }

    public BooleanSupplier available() { return available; }

    public Function<DataDrivenCondition, String> displayText() { return displayText; }

    public boolean requiresPlayer() { return requiresPlayer; }

    public static final class Builder {
        private final String id;
        private final List<FieldSpec> fields = new ArrayList<>();
        private ConditionHandler handler;
        private BooleanSupplier available = () -> true;
        private Function<DataDrivenCondition, String> displayText;
        private boolean requiresPlayer;

        private Builder(String id) {
            this.id = id;
        }

        // 行为实现，必填
        public Builder handler(ConditionHandler handler) {
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

        // 自定义列表显示文本，不设时用 lang key visual_set_edit.gui.condition.type.<id>
        public Builder displayText(Function<DataDrivenCondition, String> displayText) {
            this.displayText = displayText;
            return this;
        }

        // 该条件是否只在有玩家时成立（影响零件套能否作用于非玩家实体）
        public Builder requiresPlayer(boolean requiresPlayer) {
            this.requiresPlayer = requiresPlayer;
            return this;
        }

        public ConditionTypeSpec build() {
            return new ConditionTypeSpec(this);
        }
    }
}
