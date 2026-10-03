package com.sal_fish.visual_set_edit.api;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.data.effect.EffectEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;

// 外部注册的效果实例：字段值存在 params 里，行为转发给注册的 handler
public class DataDrivenEffectEntry extends EffectEntry {

    // 字段名 -> 值；数字回读时统一是 Double
    @Expose public Map<String, Object> params = new LinkedHashMap<>();

    public DataDrivenEffectEntry() {
        this.type = "data_driven";
    }

    @Override
    public void apply(LivingEntity entity) {
        EffectHandler handler = EffectTypeSpecRegistry.handlerOf(type);
        if (handler != null) handler.apply(entity, params);
    }

    @Override
    public void remove(LivingEntity entity) {
        EffectHandler handler = EffectTypeSpecRegistry.handlerOf(type);
        if (handler != null) handler.remove(entity, params);
    }

    @Override
    public String getDisplayText() {
        EffectTypeSpec spec = EffectTypeSpecRegistry.get(type);
        if (spec != null && spec.displayText() != null) {
            return spec.displayText().apply(this);
        }
        return Component.translatable("visual_set_edit.gui.effect.type." + type).getString();
    }
}
