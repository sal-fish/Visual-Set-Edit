package com.sal_fish.visual_set_edit.data.effect;

import com.sal_fish.visual_set_edit.integration.IntegrationManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

// 效果类型注册表：type 名与实现类的唯一来源，双端可用
public final class EffectTypeRegistry {

    public record Type(String id, Class<? extends EffectEntry> clazz, BooleanSupplier available) {
        public boolean isAvailable() {
            return available == null || available.getAsBoolean();
        }
    }

    private static final String DEFAULT_ID = "potion";
    private static final Map<String, Type> BY_ID = new LinkedHashMap<>();

    static {
        register("potion", PotionEffectEntry.class, () -> true);
        register("attribute", AttributeEffectEntry.class, () -> true);
        register("ability", AbilityEffectEntry.class, () -> true);
        register("command", CommandEffectEntry.class, () -> true);
        register("dynamic_attribute", DynamicAttributeEffectEntry.class, () -> true);
        register("tag", TagEffectEntry.class, () -> true);
        register("iron_spell", IronSpellEffectEntry.class, IntegrationManager::isIronSpellsLoaded);
        register("spell_level_boost", SpellLevelBoostEffectEntry.class, IntegrationManager::isIronSpellsLoaded);
        register("slot_count", SlotCountEffectEntry.class, IntegrationManager::isCuriosLoaded);
        register("l2hostility_trait", L2HostilityTraitEffectEntry.class, IntegrationManager::isL2HostilityLoaded);
        register("l2_difficulty_mod", L2DifficultyModEffectEntry.class, IntegrationManager::isL2HostilityLoaded);
    }

    private EffectTypeRegistry() {}

    public static void register(String id, Class<? extends EffectEntry> clazz, BooleanSupplier available) {
        BY_ID.putIfAbsent(id, new Type(id, clazz, available));
    }

    public static Type get(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static Class<? extends EffectEntry> classOf(String id) {
        Type t = get(id);
        return t != null ? t.clazz() : null;
    }

    public static boolean contains(String id) {
        return id != null && BY_ID.containsKey(id);
    }

    // 下拉候选：按注册顺序返回当前可用的类型
    public static List<String> availableIds() {
        List<String> ids = new ArrayList<>();
        for (Type t : BY_ID.values()) {
            if (t.isAvailable()) ids.add(t.id());
        }
        return ids;
    }

    public static String defaultId() {
        return DEFAULT_ID;
    }
}
