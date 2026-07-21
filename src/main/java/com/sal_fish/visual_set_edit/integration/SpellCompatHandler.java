package com.sal_fish.visual_set_edit.integration;

import com.sal_fish.visual_set_edit.data.effect.SpellLevelBoostEffectEntry;
import com.sal_fish.visual_set_edit.event.ActiveSetTracker;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.world.entity.LivingEntity;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;

public class SpellCompatHandler {
    public static void init() {
        MinecraftForge.EVENT_BUS.register(new SpellCompatHandler());
    }

    @SubscribeEvent
    public void onModifySpellLevel(ModifySpellLevelEvent event) {
        LivingEntity entity = event.getEntity();
        AbstractSpell spell = event.getSpell();
        String spellId = spell.getSpellId();

        if (entity != null) {
            for (var active : ActiveSetTracker.getActivePhases(entity)) {
                for (var entry : active.phase().effects) {
                    if (entry instanceof SpellLevelBoostEffectEntry boost) {
                        if (boost.spellId == null || boost.spellId.isEmpty() || boost.spellId.equals(spellId)) {
                            event.setLevel(event.getLevel() + boost.boostAmount);
                        }
                    }
                }
            }
        }
    }
}