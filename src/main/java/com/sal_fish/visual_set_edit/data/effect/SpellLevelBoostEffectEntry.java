package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;

public class SpellLevelBoostEffectEntry extends EffectEntry {
    @Expose public String spellId;   // null 或空表示不限单个法术
    @Expose public String schoolId;  // null 或空表示不限流派
    @Expose public int boostAmount = 1;  // 负数为降级

    public SpellLevelBoostEffectEntry() {
        this.type = "spell_level_boost";
    }

    // 判定是否限定流派
    public boolean hasSchool() {
        return schoolId != null && !schoolId.isEmpty();
    }

    // 判定是否限定单个法术
    public boolean hasSpell() {
        return spellId != null && !spellId.isEmpty();
    }

    @Override
    public void apply(net.minecraft.world.entity.LivingEntity entity) {

    }

    @Override
    public void remove(net.minecraft.world.entity.LivingEntity entity) {

    }

    @Override
    public String getDisplayText() {
        String target;
        if (hasSpell()) {
            target = spellId;
        } else if (hasSchool()) {
            target = Component.translatable("school." + schoolId.replace(':', '.')).getString();
        } else {
            target = Component.translatable("visual_set_edit.gui.effect.spell_level_boost.all_spells").getString();
        }
        String amount = boostAmount > 0 ? "+" + boostAmount : String.valueOf(boostAmount);
        return Component.translatable("visual_set_edit.gui.effect.spell_level_boost.display", target, amount).getString();
    }
}
