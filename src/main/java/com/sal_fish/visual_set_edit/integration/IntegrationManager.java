package com.sal_fish.visual_set_edit.integration;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.util.function.Consumer;

public class IntegrationManager {
    private static IModIntegration curios;
    private static IModIntegration ironSpells;

    public static boolean isCuriosLoaded() { return ModList.get().isLoaded("curios"); }
    public static boolean isIronSpellsLoaded() { return ModList.get().isLoaded("irons_spellbooks"); }
    public static boolean isL2HostilityLoaded() { return ModList.get().isLoaded("l2hostility"); }

    public static IModIntegration getIronSpells() {
        if (ironSpells == null && isIronSpellsLoaded()) {
            try {
                ironSpells = (IModIntegration) Class.forName("com.sal_fish.visual_set_edit.integration.IronSpellsIntegration").getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                ironSpells = new IronSpellsIntegration();
            }
        }
        return ironSpells;
    }

    public static IModIntegration getCurios() {
        if (curios == null && isCuriosLoaded()) {
            try {
                curios = (IModIntegration) Class.forName("com.sal_fish.visual_set_edit.integration.CuriosIntegration").getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                curios = new CuriosIntegration();
            }
        }
        return curios;
    }

    // ========== 联动初始化 ==========
    public static void initCompat() {
        if (isIronSpellsLoaded()) {
            try {
                Class<?> handler = Class.forName("com.sal_fish.visual_set_edit.integration.SpellCompatHandler");
                handler.getMethod("init").invoke(null);
            } catch (Exception ignored) {}
        }
    }

    // ========== 莱特兰词条运行时调用 ==========
    public static void applyL2TraitToTarget(LivingEntity attacker, LivingEntity target, com.sal_fish.visual_set_edit.data.effect.EffectEntry entry) {
        if (!isL2HostilityLoaded()) return;
        try {
            Class<?> handler = Class.forName("com.sal_fish.visual_set_edit.integration.L2CompatHandler");
            handler.getMethod("applyAttackTarget", LivingEntity.class, LivingEntity.class,
                    entry.getClass()).invoke(null, attacker, target, entry);
        } catch (Exception ignored) {}
    }

    public static void tickL2Traits(LivingEntity entity) {
        if (!isL2HostilityLoaded()) return;
        try {
            Class<?> handler = Class.forName("com.sal_fish.visual_set_edit.integration.L2CompatHandler");
            handler.getMethod("tickTemporaryTraits", LivingEntity.class).invoke(null, entity);
        } catch (Exception ignored) {}
    }

    // ========== GUI 工厂方法 ==========
    public static Screen createSpellListScreen(Screen parent, Consumer<ResourceLocation> callback) {
        if (!isIronSpellsLoaded()) return null;
        try {
            return (Screen) Class.forName("com.sal_fish.visual_set_edit.gui.SpellListScreen")
                    .getConstructor(Screen.class, Consumer.class)
                    .newInstance(parent, callback);
        } catch (Exception e) {
            return null;
        }
    }

    public static Screen createL2TraitListScreen(Screen parent, Consumer<ResourceLocation> callback) {
        if (!isL2HostilityLoaded()) return null;
        try {
            return (Screen) Class.forName("com.sal_fish.visual_set_edit.gui.L2TraitListScreen")
                    .getConstructor(Screen.class, Consumer.class)
                    .newInstance(parent, callback);
        } catch (Exception e) {
            return null;
        }
    }

    // ========== 显示名称获取（反射，无硬依赖） ==========

    /**
     * 获取铁魔法法术的显示名称，未加载或找不到时返回 null
     */
    public static String getSpellDisplayName(String spellId) {
        if (!isIronSpellsLoaded() || spellId == null || spellId.isEmpty()) return null;
        try {
            ResourceLocation rl = ResourceLocation.tryParse(spellId);
            if (rl == null) return null;
            Class<?> spellRegistryClass = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
            Object registry = spellRegistryClass.getField("REGISTRY").get(null);
            Object forgeRegistry = registry.getClass().getMethod("get").invoke(registry);
            Object spell = forgeRegistry.getClass().getMethod("getValue", ResourceLocation.class).invoke(forgeRegistry, rl);
            if (spell != null) {
                String componentId = (String) spell.getClass().getMethod("getComponentId").invoke(spell);
                return Component.translatable(componentId).getString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 获取莱特兰词条的显示名称，未加载或找不到时返回 null
     */
    public static String getL2TraitDisplayName(String traitId) {
        if (!isL2HostilityLoaded() || traitId == null || traitId.isEmpty()) return null;
        try {
            ResourceLocation rl = ResourceLocation.tryParse(traitId);
            if (rl == null) return null;
            Class<?> lhTraitsClass = Class.forName("dev.xkmc.l2hostility.init.registrate.LHTraits");
            Object registry = lhTraitsClass.getField("TRAITS").get(null);
            Object forgeRegistry = registry.getClass().getMethod("get").invoke(registry);
            Object trait = forgeRegistry.getClass().getMethod("getValue", ResourceLocation.class).invoke(forgeRegistry, rl);
            if (trait != null) {
                Object desc = trait.getClass().getMethod("getDesc").invoke(trait);
                return ((Component) desc).getString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static int getL2ChunkDifficulty(LivingEntity entity) {
        if (!isL2HostilityLoaded()) return 0;
        try {
            Class<?> handler = Class.forName("com.sal_fish.visual_set_edit.integration.L2CompatHandler");
            return (int) handler.getMethod("getChunkDifficulty", LivingEntity.class).invoke(null, entity);
        } catch (Exception ignored) {}
        return 0;
    }

    public static int getL2PlayerDifficulty(LivingEntity entity) {
        if (!isL2HostilityLoaded()) return 0;
        try {
            Class<?> handler = Class.forName("com.sal_fish.visual_set_edit.integration.L2CompatHandler");
            return (int) handler.getMethod("getPlayerDifficulty", LivingEntity.class).invoke(null, entity);
        } catch (Exception ignored) {}
        return 0;
    }

    public static void modifyL2PlayerDifficulty(LivingEntity entity, int amount) {
        if (!isL2HostilityLoaded()) {
            //VisualSetEdit.LOGGER.warn("[VSE] L2Hostility not loaded");
            return;
        }
        try {
            Class<?> handler = Class.forName("com.sal_fish.visual_set_edit.integration.L2CompatHandler");
            //VisualSetEdit.LOGGER.info("[VSE] Calling L2CompatHandler.modifyPlayerDifficulty");
            handler.getMethod("modifyPlayerDifficulty", LivingEntity.class, int.class).invoke(null, entity, amount);
            //VisualSetEdit.LOGGER.info("[VSE] L2CompatHandler.modifyPlayerDifficulty returned successfully");
        } catch (Exception e) {
            //VisualSetEdit.LOGGER.error("[VSE] Failed to call modifyPlayerDifficulty", e);
        }
    }

    public static void cleanupCuriosSlotsOnClone(LivingEntity oldEntity, LivingEntity newEntity) {
        if (!isCuriosLoaded()) return;
        try {
            Class<?> curiosIntegration = Class.forName("com.sal_fish.visual_set_edit.integration.CuriosIntegration");
            curiosIntegration.getMethod("cleanupSlotModifiersOnClone", LivingEntity.class, LivingEntity.class)
                    .invoke(null, oldEntity, newEntity);
        } catch (Exception ignored) {}
    }
}