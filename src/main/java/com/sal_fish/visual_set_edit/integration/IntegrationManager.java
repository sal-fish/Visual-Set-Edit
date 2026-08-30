package com.sal_fish.visual_set_edit.integration;

import com.sal_fish.visual_set_edit.data.effect.EffectEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class IntegrationManager {
    private static IModIntegration curios;
    private static IModIntegration ironSpells;

    private static final ConcurrentHashMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    private static final Class<?> CLASS_MISS = IntegrationManager.class;

    private static Class<?> findClass(String name) {
        Class<?> cached = CLASS_CACHE.get(name);
        if (cached != null) return cached == CLASS_MISS ? null : cached;
        try {
            cached = Class.forName(name);
        } catch (Exception e) {
            cached = CLASS_MISS;
        }
        CLASS_CACHE.put(name, cached);
        return cached == CLASS_MISS ? null : cached;
    }

    private static Method findMethod(String key, Class<?> owner, String name, Class<?>... params) {
        Method cached = METHOD_CACHE.get(key);
        if (cached != null) return cached;
        if (owner == null) return null;
        try {
            cached = owner.getMethod(name, params);
        } catch (Exception ignored) {
            return null;
        }
        METHOD_CACHE.put(key, cached);
        return cached;
    }

    private static Method findMethod(String key, String className, String name, Class<?>... params) {
        return findMethod(key, findClass(className), name, params);
    }

    private static Field findField(String key, Class<?> owner, String name) {
        Field cached = FIELD_CACHE.get(key);
        if (cached != null) return cached;
        if (owner == null) return null;
        try {
            cached = owner.getField(name);
        } catch (Exception ignored) {
            return null;
        }
        FIELD_CACHE.put(key, cached);
        return cached;
    }

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

    //联动初始化
    public static void initCompat() {
        if (isIronSpellsLoaded()) {
            Method init = findMethod("spellCompatInit",
                    "com.sal_fish.visual_set_edit.integration.SpellCompatHandler", "init");
            if (init == null) return;
            try { init.invoke(null); } catch (Exception ignored) {}
        }
    }

    //莱特兰词条运行时调用
    public static void applyL2TraitToTarget(LivingEntity attacker, LivingEntity target, EffectEntry entry) {
        if (!isL2HostilityLoaded()) return;
        Method m = findMethod("l2Apply:" + entry.getClass().getName(),
                "com.sal_fish.visual_set_edit.integration.L2CompatHandler",
                "applyAttackTarget", LivingEntity.class, LivingEntity.class, entry.getClass());
        if (m == null) return;
        try { m.invoke(null, attacker, target, entry); } catch (Exception ignored) {}
    }

    public static void tickL2Traits(LivingEntity entity) {
        if (!isL2HostilityLoaded()) return;
        Method m = findMethod("l2Tick",
                "com.sal_fish.visual_set_edit.integration.L2CompatHandler",
                "tickTemporaryTraits", LivingEntity.class);
        if (m == null) return;
        try { m.invoke(null, entity); } catch (Exception ignored) {}
    }

    //GUI 工厂方法
    public static Screen createSpellListScreen(Screen parent, Consumer<ResourceLocation> callback) {
        if (!isIronSpellsLoaded()) return null;
        Class<?> screenClass = findClass("com.sal_fish.visual_set_edit.gui.SpellListScreen");
        if (screenClass == null) return null;
        try {
            return (Screen) screenClass.getConstructor(Screen.class, Consumer.class)
                    .newInstance(parent, callback);
        } catch (Exception e) {
            return null;
        }
    }

    public static Screen createL2TraitListScreen(Screen parent, Consumer<ResourceLocation> callback) {
        if (!isL2HostilityLoaded()) return null;
        Class<?> screenClass = findClass("com.sal_fish.visual_set_edit.gui.L2TraitListScreen");
        if (screenClass == null) return null;
        try {
            return (Screen) screenClass.getConstructor(Screen.class, Consumer.class)
                    .newInstance(parent, callback);
        } catch (Exception e) {
            return null;
        }
    }

    //获取铁魔法法术的显示名称
    public static String getSpellDisplayName(String spellId) {
        if (!isIronSpellsLoaded() || spellId == null || spellId.isEmpty()) return null;
        try {
            ResourceLocation rl = ResourceLocation.tryParse(spellId);
            if (rl == null) return null;
            Class<?> spellRegistryClass = findClass("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
            if (spellRegistryClass == null) return null;
            Field registryField = findField("issRegistry", spellRegistryClass, "REGISTRY");
            if (registryField == null) return null;
            Object registry = registryField.get(null);
            if (registry == null) return null;
            Method getMethod = findMethod("issGet:" + registry.getClass().getName(), registry.getClass(), "get");
            if (getMethod == null) return null;
            Object forgeRegistry = getMethod.invoke(registry);
            if (forgeRegistry == null) return null;
            Method getValueMethod = findMethod("issGetValue:" + forgeRegistry.getClass().getName(),
                    forgeRegistry.getClass(), "getValue", ResourceLocation.class);
            if (getValueMethod == null) return null;
            Object spell = getValueMethod.invoke(forgeRegistry, rl);
            if (spell != null) {
                Method componentIdMethod = findMethod("issComponentId:" + spell.getClass().getName(),
                        spell.getClass(), "getComponentId");
                if (componentIdMethod == null) return null;
                String componentId = (String) componentIdMethod.invoke(spell);
                return Component.translatable(componentId).getString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    //获取莱特兰词条的显示名称
    public static String getL2TraitDisplayName(String traitId) {
        if (!isL2HostilityLoaded() || traitId == null || traitId.isEmpty()) return null;
        try {
            ResourceLocation rl = ResourceLocation.tryParse(traitId);
            if (rl == null) return null;
            Class<?> lhTraitsClass = findClass("dev.xkmc.l2hostility.init.registrate.LHTraits");
            if (lhTraitsClass == null) return null;
            Field registryField = findField("l2TraitsRegistry", lhTraitsClass, "TRAITS");
            if (registryField == null) return null;
            Object registry = registryField.get(null);
            if (registry == null) return null;
            Method getMethod = findMethod("l2Get:" + registry.getClass().getName(), registry.getClass(), "get");
            if (getMethod == null) return null;
            Object forgeRegistry = getMethod.invoke(registry);
            if (forgeRegistry == null) return null;
            Method getValueMethod = findMethod("l2GetValue:" + forgeRegistry.getClass().getName(),
                    forgeRegistry.getClass(), "getValue", ResourceLocation.class);
            if (getValueMethod == null) return null;
            Object trait = getValueMethod.invoke(forgeRegistry, rl);
            if (trait != null) {
                Method descMethod = findMethod("l2GetDesc:" + trait.getClass().getName(),
                        trait.getClass(), "getDesc");
                if (descMethod == null) return null;
                Object desc = descMethod.invoke(trait);
                return ((Component) desc).getString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static int getL2ChunkDifficulty(LivingEntity entity) {
        if (!isL2HostilityLoaded()) return 0;
        Method m = findMethod("l2ChunkDifficulty",
                "com.sal_fish.visual_set_edit.integration.L2CompatHandler",
                "getChunkDifficulty", LivingEntity.class);
        if (m == null) return 0;
        try { return (int) m.invoke(null, entity); } catch (Exception ignored) {}
        return 0;
    }

    public static int getL2PlayerDifficulty(LivingEntity entity) {
        if (!isL2HostilityLoaded()) return 0;
        Method m = findMethod("l2PlayerDifficulty",
                "com.sal_fish.visual_set_edit.integration.L2CompatHandler",
                "getPlayerDifficulty", LivingEntity.class);
        if (m == null) return 0;
        try { return (int) m.invoke(null, entity); } catch (Exception ignored) {}
        return 0;
    }

    public static void modifyL2PlayerDifficulty(LivingEntity entity, int amount) {
        if (!isL2HostilityLoaded()) {
            return;
        }
        Method m = findMethod("l2ModifyPlayerDifficulty",
                "com.sal_fish.visual_set_edit.integration.L2CompatHandler",
                "modifyPlayerDifficulty", LivingEntity.class, int.class);
        if (m == null) return;
        try { m.invoke(null, entity, amount); } catch (Exception ignored) {}
    }

    public static void cleanupCuriosSlotsOnClone(LivingEntity oldEntity, LivingEntity newEntity) {
        if (!isCuriosLoaded()) return;
        Method m = findMethod("curiosCleanupOnClone",
                "com.sal_fish.visual_set_edit.integration.CuriosIntegration",
                "cleanupSlotModifiersOnClone", LivingEntity.class, LivingEntity.class);
        if (m == null) return;
        try { m.invoke(null, oldEntity, newEntity); } catch (Exception ignored) {}
    }
}
