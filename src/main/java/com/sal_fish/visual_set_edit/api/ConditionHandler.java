package com.sal_fish.visual_set_edit.api;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

// 外部条件的行为实现
public interface ConditionHandler {

    // 判定条件是否成立；异常会被 VSE 捕获并视为不成立
    boolean test(LivingEntity entity, Map<String, Object> params);
}
