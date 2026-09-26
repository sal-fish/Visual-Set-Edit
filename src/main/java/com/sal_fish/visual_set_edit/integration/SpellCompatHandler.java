package com.sal_fish.visual_set_edit.integration;

import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.effect.EffectEntry;
import com.sal_fish.visual_set_edit.data.effect.SpellLevelBoostEffectEntry;
import com.sal_fish.visual_set_edit.event.ActiveSetTracker;
import com.sal_fish.visual_set_edit.tooltip.TooltipRenderer;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpellCompatHandler {
    private static final Map<UUID, Long> CACHE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, List<SpellLevelBoostEffectEntry>> CACHE_BOOSTS = new ConcurrentHashMap<>();

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new SpellCompatHandler());
    }

    @SubscribeEvent
    public void onModifySpellLevel(ModifySpellLevelEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) return;

        List<SpellLevelBoostEffectEntry> boosts = entity instanceof Player player
                ? playerBoosts(player)
                : entityBoosts(entity);
        if (boosts.isEmpty()) return;

        String spellId = event.getSpell().getSpellId();
        for (SpellLevelBoostEffectEntry boost : boosts) {
            if (boost.spellId == null || boost.spellId.isEmpty() || boost.spellId.equals(spellId)) {
                event.setLevel(event.getLevel() + boost.boostAmount);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        CACHE_TICK.remove(id);
        CACHE_BOOSTS.remove(id);
    }

    private static List<SpellLevelBoostEffectEntry> playerBoosts(Player player) {
        UUID id = player.getUUID();
        Long tick = CACHE_TICK.get(id);
        if (tick != null && tick == player.tickCount) {
            return CACHE_BOOSTS.getOrDefault(id, List.of());
        }

        List<SpellLevelBoostEffectEntry> found = new ArrayList<>();
        List<Preset> source = player.level().isClientSide
                ? PresetManager.clientPresets
                : PresetManager.getPresets();
        for (Preset preset : source) {
            for (SetPhase phase : preset.phases) {
                if (!TooltipRenderer.isPhaseActiveClient(player, phase)) continue;
                for (EffectEntry entry : phase.effects) {
                    if (entry instanceof SpellLevelBoostEffectEntry boost) found.add(boost);
                }
            }
        }

        CACHE_TICK.put(id, (long) player.tickCount);
        CACHE_BOOSTS.put(id, found);
        return found;
    }

    private static List<SpellLevelBoostEffectEntry> entityBoosts(LivingEntity entity) {
        List<SpellLevelBoostEffectEntry> found = new ArrayList<>();
        for (ActiveSetTracker.ActivePhase active : ActiveSetTracker.getActivePhases(entity)) {
            for (EffectEntry entry : active.phase().effects) {
                if (entry instanceof SpellLevelBoostEffectEntry boost) found.add(boost);
            }
        }
        return found;
    }
}
