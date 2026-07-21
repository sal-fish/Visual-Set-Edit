package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class AbilityEffectEntry extends EffectEntry {
    @Expose public String abilityId; // FLIGHT, FALL_IMMUNITY

    public AbilityEffectEntry() { this.type = "ability"; }

    @Override
    public void apply(LivingEntity entity) {
        if (entity instanceof Player player) {
            applyToPlayer(player);
        }
    }

    private void applyToPlayer(Player player) {
        switch (abilityId) {
            case "FLIGHT":
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }
                break;
            case "FALL_IMMUNITY":
                player.getPersistentData().putBoolean("vse_fallimmune", true);
                break;
        }
    }

    @Override
    public void remove(LivingEntity entity) {
        if (entity instanceof Player player) {
            removeFromPlayer(player);
        }
    }

    private void removeFromPlayer(Player player) {
        switch (abilityId) {
            case "FLIGHT":
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
                break;
            case "FALL_IMMUNITY":
                player.getPersistentData().remove("vse_fallimmune");
                break;
        }
    }

    @Override
    public String getDisplayText() {
        String abilityName = switch (abilityId) {
            case "FLIGHT" -> Component.translatable("visual_set_edit.gui.effect.ability.flight").getString();
            case "FALL_IMMUNITY" ->
                    Component.translatable("visual_set_edit.gui.effect.ability.fall_immunity").getString();
            default -> abilityId;
        };
        return Component.translatable("visual_set_edit.gui.effect.ability.display", abilityName).getString();
    }
}