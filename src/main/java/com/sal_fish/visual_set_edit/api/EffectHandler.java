package com.sal_fish.visual_set_edit.api;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

// 外部效果的行为实现
public interface EffectHandler {

    // 套装生效时调用
    void apply(LivingEntity entity, Map<String, Object> params);

    // 套装失效时调用，需撤销 apply 的改动
    void remove(LivingEntity entity, Map<String, Object> params);
}
