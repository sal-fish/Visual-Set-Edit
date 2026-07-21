package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class IronSpellEffectEntry extends EffectEntry {
    @Expose public String spellId;
    @Expose public int spellLevel = 1;    // 新增：法术等级，默认1

    public IronSpellEffectEntry() { this.type = "iron_spell"; }

    @Override
    public void apply(LivingEntity entity) {
        // 仅标记，实际施法通过按键触发
    }

    @Override
    public void remove(LivingEntity entity) {
        // 无需操作
    }

    @Override
    public String getDisplayText() {
        if (spellId != null && !spellId.isEmpty()) {
            ResourceLocation rl = ResourceLocation.tryParse(spellId);
            if (rl != null) {
                AbstractSpell spell = SpellRegistry.REGISTRY.get().getValue(rl);
                if (spell != null) {
                    return Component.translatable("visual_set_edit.gui.effect.iron_spell.display",
                            Component.translatable(spell.getComponentId()).getString(),
                            spellLevel).getString();
                }
            }
        }
        return Component.translatable("visual_set_edit.gui.effect.iron_spell.display", spellId, spellLevel).getString();
    }
}