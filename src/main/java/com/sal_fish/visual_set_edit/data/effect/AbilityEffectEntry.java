package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityEffectEntry extends EffectEntry {
    @Expose public String abilityId;

    private static final Map<UUID, Integer> FLIGHT_COUNTER = new ConcurrentHashMap<>();

    public static void clearFlightCounter(UUID uuid) {
        FLIGHT_COUNTER.remove(uuid);
    }

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
                int count = FLIGHT_COUNTER.merge(player.getUUID(), 1, Integer::sum);
                if (count == 1 && !player.getAbilities().mayfly) {
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
                int count = FLIGHT_COUNTER.merge(player.getUUID(), -1, Integer::sum);
                if (count <= 0) {
                    FLIGHT_COUNTER.remove(player.getUUID());
                    if (!player.isCreative() && !player.isSpectator()) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                    }
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