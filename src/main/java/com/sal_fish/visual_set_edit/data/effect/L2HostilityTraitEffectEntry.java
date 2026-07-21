package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;

public class L2HostilityTraitEffectEntry extends EffectEntry {
    @Expose public String target = "ATTACK_TARGET";
    @Expose public String traitId;
    @Expose public int level = 1;

    public L2HostilityTraitEffectEntry() {
        this.type = "l2hostility_trait";
    }

    @Override
    public void apply(net.minecraft.world.entity.LivingEntity entity) {}

    @Override
    public void remove(net.minecraft.world.entity.LivingEntity entity) {}

    @Override
    public String getDisplayText() {
        return Component.translatable("visual_set_edit.gui.effect.l2hostility_trait.display", traitId != null ? traitId : "", level).getString()
                + " (Attack Target)";
    }
}