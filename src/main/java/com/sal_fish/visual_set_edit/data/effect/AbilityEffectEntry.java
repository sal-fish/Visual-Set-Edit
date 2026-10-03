package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.api.AbilitySpec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityEffectEntry extends EffectEntry {
    @Expose public String abilityId;
    // 外部注册能力的参数，内置两项不使用
    @Expose public Map<String, Object> params = new LinkedHashMap<>();

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
        switch (abilityId == null ? "" : abilityId) {
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
            default:
                // 外部注册的能力
                AbilitySpec spec = AbilityTypeRegistry.get(abilityId);
                if (spec != null) spec.handler().apply(player, params);
        }
    }

    @Override
    public void remove(LivingEntity entity) {
        if (entity instanceof Player player) {
            removeFromPlayer(player);
        }
    }

    private void removeFromPlayer(Player player) {
        switch (abilityId == null ? "" : abilityId) {
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
            default:
                // 外部注册的能力
                AbilitySpec spec = AbilityTypeRegistry.get(abilityId);
                if (spec != null) spec.handler().remove(player, params);
        }
    }

    @Override
    public String getDisplayText() {
        String abilityName = switch (abilityId == null ? "" : abilityId) {
            case "FLIGHT" -> Component.translatable("visual_set_edit.gui.effect.ability.flight").getString();
            case "FALL_IMMUNITY" ->
                    Component.translatable("visual_set_edit.gui.effect.ability.fall_immunity").getString();
            default -> {
                AbilitySpec spec = AbilityTypeRegistry.get(abilityId);
                yield spec != null ? spec.displayName().getString() : String.valueOf(abilityId);
            }
        };
        return Component.translatable("visual_set_edit.gui.effect.ability.display", abilityName).getString();
    }
}
