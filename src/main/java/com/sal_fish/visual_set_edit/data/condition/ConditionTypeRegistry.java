package com.sal_fish.visual_set_edit.data.condition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

// 条件类型注册表：type 名与实现类的唯一来源，双端可用
public final class ConditionTypeRegistry {

    public record Type(String id, Class<? extends Condition> clazz, BooleanSupplier available) {
        public boolean isAvailable() {
            return available == null || available.getAsBoolean();
        }
    }

    private static final String DEFAULT_ID = "environment";
    private static final Map<String, Type> BY_ID = new LinkedHashMap<>();

    // 注册顺序即下拉顺序
    static {
        register("environment", EnvironmentCondition.class, () -> true);
        register("player_state", PlayerStateCondition.class, () -> true);
        register("inventory", InventoryCondition.class, () -> true);
        register("iron_spell", IronSpellCondition.class, () -> true);
        register("attribute", AttributeCondition.class, () -> true);
        register("scoreboard", ScoreboardCondition.class, () -> true);
        register("composite", CompositeCondition.class, () -> true);
    }

    private ConditionTypeRegistry() {}

    public static void register(String id, Class<? extends Condition> clazz, BooleanSupplier available) {
        BY_ID.putIfAbsent(id, new Type(id, clazz, available));
    }

    public static Type get(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static Class<? extends Condition> classOf(String id) {
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
