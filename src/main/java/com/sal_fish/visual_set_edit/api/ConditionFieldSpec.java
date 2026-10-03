package com.sal_fish.visual_set_edit.api;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

// 条件字段规格：外部用它给已有条件类型（environment / player_state）追加一个字段
public final class ConditionFieldSpec {

    // 取值控件形态
    public enum ValueForm {
        // 比较符 + 文本框，默认形态
        VALUE,
        // 无比较符的固定选项下拉
        OPTIONS,
        // 比较符 + 文本框 + 选择屏按钮
        PICKER
    }

    private final String conditionType;
    private final String id;
    private final ValueForm valueForm;
    private final List<String> options;
    private final Function<String, Component> optionRenderer;
    private final String pickerId;
    private final ConditionFieldHandler handler;
    private final Supplier<Component> displayText;
    private final BiFunction<String, String, String> describer;
    private final boolean requiresPlayer;

    private ConditionFieldSpec(Builder builder) {
        this.conditionType = builder.conditionType;
        this.id = builder.id;
        this.valueForm = builder.valueForm;
        this.options = List.copyOf(builder.options);
        this.optionRenderer = builder.optionRenderer;
        this.pickerId = builder.pickerId;
        this.handler = builder.handler;
        this.displayText = builder.displayText;
        this.describer = builder.describer;
        this.requiresPlayer = builder.requiresPlayer;
    }

    public static Builder create(String conditionType, String id) {
        return new Builder(conditionType, id);
    }

    public String conditionType() { return conditionType; }

    public String id() { return id; }

    public ValueForm valueForm() { return valueForm; }

    public List<String> options() { return options; }

    public Function<String, Component> optionRenderer() { return optionRenderer; }

    public String pickerId() { return pickerId; }

    public ConditionFieldHandler handler() { return handler; }

    public Supplier<Component> displayText() { return displayText; }

    // 条件描述生成器，入参为比较符与值，为 null 时用 "<id> <comparator> <value>"
    public BiFunction<String, String, String> describer() { return describer; }

    // 该字段是否只在有玩家时成立，影响零件套能否作用于非玩家实体
    public boolean requiresPlayer() { return requiresPlayer; }

    public static final class Builder {
        private final String conditionType;
        private final String id;
        private ValueForm valueForm = ValueForm.VALUE;
        private List<String> options = List.of();
        private Function<String, Component> optionRenderer;
        private String pickerId;
        private ConditionFieldHandler handler;
        private Supplier<Component> displayText;
        private BiFunction<String, String, String> describer;
        private boolean requiresPlayer;

        private Builder(String conditionType, String id) {
            this.conditionType = conditionType;
            this.id = id;
        }

        // 判定实现，必填
        public Builder handler(ConditionFieldHandler handler) {
            this.handler = handler;
            return this;
        }

        // 取值控件改为固定选项下拉
        public Builder options(List<String> options) {
            this.valueForm = ValueForm.OPTIONS;
            this.options = options;
            return this;
        }

        // 取值控件改为固定选项下拉，选项显示文本由 optionRenderer 决定
        public Builder options(List<String> options, Function<String, Component> optionRenderer) {
            this.valueForm = ValueForm.OPTIONS;
            this.options = options;
            this.optionRenderer = optionRenderer;
            return this;
        }

        // 取值控件改为带选择屏的文本框；pickerId 由 VseClientApi.registerPicker 注册
        public Builder picker(String pickerId) {
            this.valueForm = ValueForm.PICKER;
            this.pickerId = pickerId;
            return this;
        }

        // 下拉里的显示名，不设时用 lang key visual_set_edit.gui.condition.field.<conditionType>.<id>
        public Builder displayText(Supplier<Component> displayText) {
            this.displayText = displayText;
            return this;
        }

        // tooltip 里该条件那一行的文本；入参为比较符与值
        public Builder describe(BiFunction<String, String, String> describer) {
            this.describer = describer;
            return this;
        }

        // 标记该字段只在有玩家时成立
        public Builder requiresPlayer(boolean requiresPlayer) {
            this.requiresPlayer = requiresPlayer;
            return this;
        }

        public ConditionFieldSpec build() {
            return new ConditionFieldSpec(this);
        }
    }
}
