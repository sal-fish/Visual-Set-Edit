package com.sal_fish.visual_set_edit.integration;

import com.sal_fish.visual_set_edit.VisualSetEdit;
import com.sal_fish.visual_set_edit.data.effect.L2HostilityTraitEffectEntry;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.AttributeTrait;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.content.traits.base.SelfEffectTrait;
import dev.xkmc.l2hostility.init.data.LHConfig;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.IForgeRegistry;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class L2CompatHandler {
    private static final Map<UUID, List<ScheduledRemoval>> scheduledRemovals = new HashMap<>();

    public static void applyAttackTarget(LivingEntity attacker, LivingEntity target, L2HostilityTraitEffectEntry entry) {
        if (attacker.level().isClientSide || target.level().isClientSide) return;
        if (!IntegrationManager.isL2HostilityLoaded()) return;

        MobTrait trait = getTrait(entry.traitId);
        if (trait == null) return;

        if (MobTraitCap.HOLDER.isProper(target)) {
            MobTraitCap cap = MobTraitCap.HOLDER.get(target);
            cap.setTrait(trait, entry.level);

            long expireTick = target.level().getGameTime() + 20 * 5;
            scheduledRemovals.computeIfAbsent(target.getUUID(), k -> new ArrayList<>())
                    .add(new ScheduledRemoval(trait, expireTick));
        }
    }

    public static void tickTemporaryTraits(LivingEntity entity) {
        if (!IntegrationManager.isL2HostilityLoaded()) return;
        if (!MobTraitCap.HOLDER.isProper(entity)) return;

        List<ScheduledRemoval> list = scheduledRemovals.get(entity.getUUID());
        if (list == null || list.isEmpty()) return;

        long currentTick = entity.level().getGameTime();
        Iterator<ScheduledRemoval> iter = list.iterator();
        while (iter.hasNext()) {
            ScheduledRemoval s = iter.next();
            if (currentTick >= s.expireTick) {
                removeTraitEffects(entity, s.trait);
                MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
                cap.removeTrait(s.trait);
                iter.remove();
            }
        }
        if (list.isEmpty()) scheduledRemovals.remove(entity.getUUID());
    }

    //难度读取
    public static int getChunkDifficulty(LivingEntity entity) {
        if (!IntegrationManager.isL2HostilityLoaded()) return 0;
        try {
            Class<?> chunkDiffClass = Class.forName("dev.xkmc.l2hostility.content.capability.chunk.ChunkDifficulty");
            Object optional = chunkDiffClass.getMethod("at", Level.class, BlockPos.class)
                    .invoke(null, entity.level(), entity.blockPosition());
            if (optional instanceof java.util.Optional<?> opt && opt.isPresent()) {
                Object chunkDiff = opt.get();
                // 获取 sections[0].difficulty.level
                Field sectionsField = chunkDiffClass.getDeclaredField("sections");
                sectionsField.setAccessible(true);
                Object[] sections = (Object[]) sectionsField.get(chunkDiff);
                if (sections.length > 0) {
                    Object section = sections[0];
                    Field diffField = section.getClass().getDeclaredField("difficulty");
                    diffField.setAccessible(true);
                    Object diffLevel = diffField.get(section);
                    return diffLevel.getClass().getField("level").getInt(diffLevel);
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public static int getPlayerDifficulty(LivingEntity entity) {
        if (!IntegrationManager.isL2HostilityLoaded() || !(entity instanceof Player player)) return 0;
        try {
            Class<?> playerDiffClass = Class.forName("dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty");
            Object holder = playerDiffClass.getField("HOLDER").get(null);
            Object playerDiff = holder.getClass().getMethod("get", Player.class).invoke(holder, player);
            if (playerDiff != null) {
                Field diffField = playerDiffClass.getDeclaredField("difficulty");
                diffField.setAccessible(true);
                Object diffLevel = diffField.get(playerDiff);
                return diffLevel.getClass().getField("level").getInt(diffLevel);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    //难度修改
    public static void modifyPlayerDifficulty(LivingEntity entity, int amount) {
        if (!IntegrationManager.isL2HostilityLoaded() || !(entity instanceof ServerPlayer sp)) return;
        try {
            Class<?> playerDiffClass = Class.forName("dev.xkmc.l2hostility.content.capability.player.PlayerDifficulty");
            Object holder = playerDiffClass.getField("HOLDER").get(null);
            Object playerDiff = holder.getClass().getMethod("get", Player.class).invoke(holder, sp);
            if (playerDiff == null) {
                VisualSetEdit.LOGGER.error("[VSE] PlayerDifficulty not found for {}", sp.getName().getString());
                return;
            }

            Field diffField = playerDiffClass.getDeclaredField("difficulty");
            diffField.setAccessible(true);
            Object difficultyLevel = diffField.get(playerDiff);

            Field levelField = difficultyLevel.getClass().getField("level");
            int current = levelField.getInt(difficultyLevel);
            int max = LHConfig.COMMON.maxPlayerLevel.get();
            int newVal = Math.max(0, Math.min(max, current + amount));
            levelField.setInt(difficultyLevel, newVal);

            playerDiffClass.getMethod("sync").invoke(playerDiff);
            //VisualSetEdit.LOGGER.info("[VSE] Player difficulty changed: {} -> {} for {}", current, newVal, sp.getName().getString());
        } catch (Exception e) {
            //VisualSetEdit.LOGGER.error("[VSE] Failed to modify player difficulty", e);
        }
    }

    //辅助方法
    private static void removeTraitEffects(LivingEntity entity, MobTrait trait) {
        if (trait instanceof AttributeTrait attrTrait) {
            try {
                Field entriesField = AttributeTrait.class.getDeclaredField("entries");
                entriesField.setAccessible(true);
                Object[] entries = (Object[]) entriesField.get(attrTrait);
                for (Object entry : entries) {
                    Field nameField = entry.getClass().getDeclaredField("name");
                    nameField.setAccessible(true);
                    String name = (String) nameField.get(entry);

                    Field attrField = entry.getClass().getDeclaredField("attribute");
                    attrField.setAccessible(true);
                    Attribute attribute = (Attribute) ((java.util.function.Supplier<?>) attrField.get(entry)).get();

                    UUID id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
                    var instance = entity.getAttribute(attribute);
                    if (instance != null) instance.removeModifier(id);
                }
            } catch (Exception ignored) {}
        }
        if (trait instanceof SelfEffectTrait selfTrait) {
            var effect = selfTrait.effect.get();
            if (entity.hasEffect(effect)) entity.removeEffect(effect);
        }
    }

    public static MobTrait getTrait(String traitId) {
        if (traitId == null || traitId.isEmpty()) return null;
        IForgeRegistry<MobTrait> registry = LHTraits.TRAITS.get();
        ResourceLocation rl = ResourceLocation.tryParse(traitId);
        return rl != null ? registry.getValue(rl) : null;
    }

    private static class ScheduledRemoval {
        final MobTrait trait;
        final long expireTick;
        ScheduledRemoval(MobTrait trait, long expireTick) {
            this.trait = trait;
            this.expireTick = expireTick;
        }
    }
}