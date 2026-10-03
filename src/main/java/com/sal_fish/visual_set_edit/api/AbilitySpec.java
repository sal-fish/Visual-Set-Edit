package com.sal_fish.visual_set_edit.api;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// 能力规格：外部用它往"能力"板块追加一个子能力
public final class AbilitySpec {

    private final String id;
    private final AbilityHandler handler;
    private final List<FieldSpec> fields;
    private final Supplier<Component> displayText;

    private AbilitySpec(Builder builder) {
        this.id = builder.id;
        this.handler = builder.handler;
        this.fields = List.copyOf(builder.fields);
        this.displayText = builder.displayText;
    }

    public static Builder create(String id) {
        return new Builder(id);
    }

    public String id() { return id; }

    public AbilityHandler handler() { return handler; }

    public List<FieldSpec> fields() { return fields; }

    public Supplier<Component> displayText() { return displayText; }

    // 下拉里的显示名，未自定义时用 lang key visual_set_edit.gui.effect.ability.<id>
    public Component displayName() {
        return displayText != null
                ? displayText.get()
                : Component.translatable("visual_set_edit.gui.effect.ability." + id);
    }

    public static final class Builder {
        private final String id;
        private final List<FieldSpec> fields = new ArrayList<>();
        private AbilityHandler handler;
        private Supplier<Component> displayText;

        private Builder(String id) {
            this.id = id;
        }

        // 生效与失效实现，必填
        public Builder handler(AbilityHandler handler) {
            this.handler = handler;
            return this;
        }

        // 追加一个可编辑字段，可多次调用
        public Builder field(FieldSpec field) {
            this.fields.add(field);
            return this;
        }

        // 自定义显示名，不设时用 lang key
        public Builder displayText(Supplier<Component> displayText) {
            this.displayText = displayText;
            return this;
        }

        public AbilitySpec build() {
            return new AbilitySpec(this);
        }
    }
}
