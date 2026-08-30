package com.sal_fish.visual_set_edit.data.condition;

import com.google.gson.annotations.Expose;
import net.minecraft.world.entity.LivingEntity;

public abstract class Condition {
    @Expose public String type;
    public abstract boolean test(LivingEntity entity);
    public abstract String getDisplayText();
    public boolean requiresPlayer() { return false; }
}