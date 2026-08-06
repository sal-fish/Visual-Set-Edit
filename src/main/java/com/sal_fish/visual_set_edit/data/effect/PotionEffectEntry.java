package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public class PotionEffectEntry extends EffectEntry {
    @Expose public String target = "SELF"; // SELF, ATTACK_TARGET, IMMUNE
    @Expose public String mobEffectId;
    @Expose public int amplifier = 0;
    @Expose public int durationSeconds = -1; // -1 = infinite
    @Expose public boolean showParticles = true;
    @Expose public int cooldownSeconds = 0;   // 冷却时间（秒），仅对有限时长有效
    @Expose public String uniqueId;           // 唯一标识符，用于冷却记录

    public PotionEffectEntry() {
        this.type = "potion";
        this.uniqueId = UUID.randomUUID().toString();
    }

    public void ensureUniqueId() {
        if (uniqueId == null || uniqueId.isEmpty()) {
            uniqueId = UUID.randomUUID().toString();
        }
    }

    @Override
    public void initAfterLoad() {
        ensureUniqueId();
    }

    @Override
    public void resetUniqueId() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    @Override
    public void apply(LivingEntity entity) {
        if ("IMMUNE".equals(target)) return;
        if (!"SELF".equals(target)) return;

        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(mobEffectId));
        if (effect == null) return;

        boolean isInfinite = (durationSeconds == -1);

        if (isInfinite) {
            if (entity.hasEffect(effect)) return;
        } else {
            MobEffectInstance current = entity.getEffect(effect);
            if (current != null) {
                if (current.getAmplifier() >= amplifier && current.getDuration() > 20) return;
                entity.removeEffect(effect);
            }

            if (cooldownSeconds > 0) {
                ensureUniqueId();
                long lastApplied = entity.getPersistentData().getLong("vse_cd_" + uniqueId);
                long now = entity.level().getGameTime();
                long cooldownTicks = cooldownSeconds * 20L;
                if (lastApplied > 0 && (now - lastApplied) < cooldownTicks) {
                    return;
                }
            }
        }

        int duration = isInfinite ? MobEffectInstance.INFINITE_DURATION
                : (durationSeconds <= 0 ? 60 : durationSeconds * 20);
        entity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, this.showParticles), null);

        if (!isInfinite && cooldownSeconds > 0) {
            ensureUniqueId();
            entity.getPersistentData().putLong("vse_cd_" + uniqueId, entity.level().getGameTime());
        }
    }

    @Override
    public void remove(LivingEntity entity) {
        if ("IMMUNE".equals(target)) return;
        if (!"SELF".equals(target)) return;

        if (durationSeconds == -1) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(mobEffectId));
            if (effect != null) {
                entity.removeEffect(effect);
            }
        }
    }

    @Override
    public String getDisplayText() {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(mobEffectId));
        String name = effect != null ? Component.translatable(effect.getDescriptionId()).getString() : mobEffectId;

        // 目标
        String targetText = switch (target) {
            case "SELF" -> Component.translatable("visual_set_edit.gui.effect.potion.target.self").getString();
            case "ATTACK_TARGET" -> Component.translatable("visual_set_edit.gui.effect.potion.target.attack_target").getString();
            case "IMMUNE" -> Component.translatable("visual_set_edit.gui.effect.potion.target.immune").getString();
            default -> target;
        };

        // 持续时间
        String durationText;
        if (durationSeconds == -1) {
            durationText = Component.translatable("visual_set_edit.gui.effect.potion.duration.infinite").getString();
        } else {
            durationText = Component.translatable("visual_set_edit.gui.effect.potion.duration.seconds", durationSeconds).getString();
        }

        // 冷却
        String cooldownText = "";
        if (cooldownSeconds > 0) {
            cooldownText = Component.translatable("visual_set_edit.gui.effect.potion.cooldown.seconds", cooldownSeconds).getString();
        }

        // 免疫模式特殊显示
        if ("IMMUNE".equals(target)) {
            return Component.translatable("visual_set_edit.gui.effect.potion.immune.display", name).getString();
        }

        // 普通模式完整显示
        if (cooldownText.isEmpty()) {
            return Component.translatable("visual_set_edit.gui.effect.potion.display.full",
                    name, amplifier + 1, targetText, durationText).getString();
        } else {
            return Component.translatable("visual_set_edit.gui.effect.potion.display.full_cooldown",
                    name, amplifier + 1, targetText, durationText, cooldownText).getString();
        }
    }
}