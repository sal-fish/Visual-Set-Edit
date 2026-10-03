package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

// 未知效果类型的占位：保留原始 type，不做任何事
public class MissingEffectEntry extends EffectEntry {

    @Expose public String originalType = "";

    public MissingEffectEntry() {
        this.type = "missing";
    }

    public MissingEffectEntry(String originalType) {
        this.type = originalType;
        this.originalType = originalType;
    }

    @Override
    public void apply(LivingEntity entity) {}

    @Override
    public void remove(LivingEntity entity) {}

    @Override
    public String getDisplayText() {
        return Component.translatable("visual_set_edit.effect.missing", originalType).getString();
    }
}
