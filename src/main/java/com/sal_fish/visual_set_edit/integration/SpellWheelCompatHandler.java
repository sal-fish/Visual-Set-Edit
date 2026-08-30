package com.sal_fish.visual_set_edit.integration;

import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.effect.EffectEntry;
import com.sal_fish.visual_set_edit.data.effect.IronSpellEffectEntry;
import com.sal_fish.visual_set_edit.tooltip.TooltipRenderer;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SpellWheelCompatHandler {

    private static final Map<UUID, String> lastActiveSignature = new HashMap<>();
    private static final int CHECK_INTERVAL_TICKS = 20; // 每 1 秒检测一次套装状态变化

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new SpellWheelCompatHandler());
    }

    @SubscribeEvent
    public void onSpellSelection(SpellSelectionManager.SpellSelectionEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        int index = 0;
        for (Preset preset : PresetManager.clientPresets) {
            for (SetPhase phase : preset.phases) {
                if (!TooltipRenderer.isPhaseActiveClient(player, phase)) continue;
                for (EffectEntry entry : phase.effects) {
                    if (!(entry instanceof IronSpellEffectEntry spellEffect)) continue;
                    if (spellEffect.spellId == null || spellEffect.spellId.isEmpty()) continue;
                    ResourceLocation rl = ResourceLocation.tryParse(spellEffect.spellId);
                    if (rl == null) continue;
                    AbstractSpell spell = SpellRegistry.REGISTRY.get().getValue(rl);
                    if (spell == null) continue;
                    int level = Math.max(1, spellEffect.spellLevel);
                    event.addSelectionOption(new SpellData(spell, level), SpellSelectionManager.MAINHAND, index++);
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Player player)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player != player) return;
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0) return;

        String signature = computeActiveSetSignature(player);
        UUID uid = player.getUUID();
        String last = lastActiveSignature.get(uid);
        if (!Objects.equals(last, signature)) {
            lastActiveSignature.put(uid, signature);
            refreshSpellSelectionManager();
        }
    }

    private static void refreshSpellSelectionManager() {
        try {
            Class<?> clazz = Class.forName("io.redspace.ironsspellbooks.player.ClientMagicData");
            Method m = clazz.getMethod("updateSpellSelectionManager");
            m.invoke(null);
        } catch (Exception ignored) {
        }
    }

    private String computeActiveSetSignature(Player player) {
        StringBuilder sb = new StringBuilder();
        for (Preset preset : PresetManager.clientPresets) {
            for (int i = 0; i < preset.phases.size(); i++) {
                if (TooltipRenderer.isPhaseActiveClient(player, preset.phases.get(i))) {
                    sb.append(preset.id).append(':').append(i).append(';');
                }
            }
        }
        return sb.toString();
    }
}
