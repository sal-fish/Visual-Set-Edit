package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public class L2DifficultyModEffectEntry extends EffectEntry {
    @Expose public int amount = 1; // 可为负数

    public L2DifficultyModEffectEntry() {
        this.type = "l2_difficulty_mod";
    }

    @Override
    public void apply(LivingEntity entity) {
        IntegrationManager.modifyL2PlayerDifficulty(entity, amount);
    }

    @Override
    public void remove(LivingEntity entity) {
        IntegrationManager.modifyL2PlayerDifficulty(entity, -amount);
    }

    @Override
    public String getDisplayText() {
        String name = Component.translatable("visual_set_edit.gui.effect.l2_difficulty_mod.player").getString();
        String sign = amount >= 0 ? "+" : "";
        return Component.translatable("visual_set_edit.gui.effect.l2_difficulty_mod.display", name, sign + amount).getString();
    }
}