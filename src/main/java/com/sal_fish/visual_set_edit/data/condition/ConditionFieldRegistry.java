package com.sal_fish.visual_set_edit.data.condition;

import com.sal_fish.visual_set_edit.api.ConditionFieldSpec;
import com.sal_fish.visual_set_edit.util.VseLog;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

// 外部注册的条件字段注册表，双端
public final class ConditionFieldRegistry {

    private static final Map<String, ConditionFieldSpec> BY_KEY = new LinkedHashMap<>();

    private ConditionFieldRegistry() {}

    public static void register(ConditionFieldSpec spec) {
        if (spec == null || spec.conditionType() == null || spec.conditionType().isEmpty()) return;
        if (spec.id() == null || spec.id().isEmpty() || spec.handler() == null) return;
        BY_KEY.putIfAbsent(spec.conditionType() + ":" + spec.id(), spec);
    }

    // 未注册时返回 null
    public static ConditionFieldSpec get(String conditionType, String id) {
        if (conditionType == null || id == null) return null;
        return BY_KEY.get(conditionType + ":" + id);
    }

    // 某条件类型下已注册的字段 id，按注册顺序
    public static List<String> fieldIdsOf(String conditionType) {
        if (conditionType == null) return List.of();
        String prefix = conditionType + ":";
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, ConditionFieldSpec> entry : BY_KEY.entrySet()) {
            if (entry.getKey().startsWith(prefix)) ids.add(entry.getValue().id());
        }
        return ids;
    }

    // tooltip 里该字段那一行的文本，未自定义描述时用 "<id> <比较符> <值>"
    public static String describe(ConditionFieldSpec spec, String comparator, String value) {
        BiFunction<String, String, String> describer = spec.describer();
        return describer != null
                ? describer.apply(comparator, value)
                : spec.id() + " " + comparator + " " + value;
    }

    // 该字段是否只在有玩家时成立
    public static boolean requiresPlayer(ConditionFieldSpec spec) {
        return spec.requiresPlayer();
    }

    // 静态判定入口，value 解析成数值后一并交给处理器
    public static boolean test(String conditionType, LivingEntity entity, String id, String comparator, String value) {
        return run(conditionType, id, entity, comparator, value, parse(value));
    }

    // 表达式分支入口，value 已解析为数值
    public static boolean testDynamic(String conditionType, LivingEntity entity, String id, String comparator, double resolved) {
        return run(conditionType, id, entity, comparator, "", resolved);
    }

    private static boolean run(String conditionType, String id, LivingEntity entity,
                               String comparator, String value, double numeric) {
        ConditionFieldSpec spec = get(conditionType, id);
        if (spec == null) return false;
        try {
            return spec.handler().test(entity, comparator, value, numeric);
        } catch (Throwable t) {
            VseLog.warnOnce("vse.condition.field." + conditionType + "." + id,
                    "[VSE] 条件字段处理器执行失败: " + conditionType + "." + id, t);
            return false;
        }
    }

    private static double parse(String value) {
        if (value == null) return Double.NaN;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
