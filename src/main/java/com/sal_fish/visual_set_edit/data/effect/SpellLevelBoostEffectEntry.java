package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;

public class SpellLevelBoostEffectEntry extends EffectEntry {
    @Expose public String spellId;   // null 或空字符串表示全部法术
    @Expose public int boostAmount = 1;

    public SpellLevelBoostEffectEntry() {
        this.type = "spell_level_boost";
    }

    @Override
    public void apply(net.minecraft.world.entity.LivingEntity entity) {

    }

    @Override
    public void remove(net.minecraft.world.entity.LivingEntity entity) {

    }

    @Override
    public String getDisplayText() {
        String target = (spellId == null || spellId.isEmpty()) ? "All spells" : spellId;
        return Component.translatable("visual_set_edit.gui.effect.spell_level_boost.display", target, boostAmount).getString();
    }
}