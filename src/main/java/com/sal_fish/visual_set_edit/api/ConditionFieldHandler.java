package com.sal_fish.visual_set_edit.api;

import net.minecraft.world.entity.LivingEntity;

// 条件字段处理器：外部用它给出字段的判定逻辑
@FunctionalInterface
public interface ConditionFieldHandler {

    // comparator 为比较符，OPTIONS 形态下为空串；value 为原始字符串；numeric 为 value 的数值解析，解析不出为 NaN
    boolean test(LivingEntity entity, String comparator, String value, double numeric);
}
