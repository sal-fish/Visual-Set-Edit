package com.sal_fish.visual_set_edit.integration;

import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.effect.IronSpellEffectEntry;
import com.sal_fish.visual_set_edit.event.ActiveSetTracker;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IronSpellsIntegration implements IModIntegration {

    @Override
    public boolean isLoaded() { return true; }
    @Override
    public void onInitialize() {}

    @Override
    public double getMana(LivingEntity player) {
        if (player instanceof ServerPlayer sp)
            return MagicData.getPlayerMagicData(sp).getMana();
        return 0;
    }

    @Override
    public double getManaPercent(LivingEntity player) {
        if (player instanceof ServerPlayer sp) {
            MagicData data = MagicData.getPlayerMagicData(sp);
            return data.getMana() / (float) sp.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get());
        }
        return 0;
    }

    @Override
    public boolean isCasting(LivingEntity player) {
        if (player instanceof ServerPlayer sp)
            return MagicData.getPlayerMagicData(sp).isCasting();
        return false;
    }

    @Override
    public boolean tryCastActiveSpell(LivingEntity player) {
        if (!(player instanceof ServerPlayer sp)) return false;
        for (ActiveSetTracker.ActivePhase active : ActiveSetTracker.getActivePhases(sp)) {
            SetPhase phase = active.phase();
            for (var effect : phase.effects) {
                if (effect instanceof IronSpellEffectEntry spellEffect) {
                    ResourceLocation spellId = ResourceLocation.tryParse(spellEffect.spellId);
                    if (spellId == null) continue;
                    AbstractSpell spell = SpellRegistry.REGISTRY.get().getValue(spellId);
                    if (spell == null) {
                        sp.sendSystemMessage(Component.translatable("visual_set_edit.integration.spell_not_found", spellId));
                        return false;
                    }
                    // 使用效果中配置的等级
                    int level = Math.max(1, spellEffect.spellLevel);
                    return spell.attemptInitiateCast(ItemStack.EMPTY, level, sp.level(), sp, CastSource.SPELLBOOK, true, "mainhand");
                }
            }
        }
        return false;
    }

    // Curios 方法
    @Override public List<String> getExtraSlots() { return new ArrayList<>(); }
    @Override public ItemStack getSlotStack(LivingEntity entity, String slotId) { return ItemStack.EMPTY; }
    @Override public List<ItemStack> getSlotStacks(LivingEntity entity, String slotId) { return Collections.emptyList(); }
    @Override public boolean canItemGoInSlot(String slotId, ItemStack stack) { return false; }
}