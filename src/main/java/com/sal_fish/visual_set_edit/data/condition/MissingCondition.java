package com.sal_fish.visual_set_edit.data.condition;

import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

// 未知条件类型的占位：保留原始 type，恒不满足
public class MissingCondition extends Condition {

    @Expose public String originalType = "";

    public MissingCondition() {
        this.type = "missing";
    }

    public MissingCondition(String originalType) {
        this.type = originalType;
        this.originalType = originalType;
    }

    @Override
    public boolean test(LivingEntity entity) {
        return false;
    }

    @Override
    public String getDisplayText() {
        return Component.translatable("visual_set_edit.condition.missing", originalType).getString();
    }
}
