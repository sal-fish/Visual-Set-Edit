package com.sal_fish.visual_set_edit.api;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.data.condition.Condition;
import com.sal_fish.visual_set_edit.util.VseLog;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;

// 外部注册的条件实例：字段值存在 params 里，判定转发给注册的 handler
public class DataDrivenCondition extends Condition {

    // 字段名 -> 值；数字回读时统一是 Double
    @Expose public Map<String, Object> params = new LinkedHashMap<>();
    // 序列化保存，载入期静态分析会读它
    @Expose public boolean needsPlayer = false;

    public DataDrivenCondition() {
        this.type = "data_driven";
    }

    @Override
    public boolean test(LivingEntity entity) {
        ConditionHandler handler = ConditionTypeSpecRegistry.handlerOf(type);
        if (handler == null) return false;
        try {
            return handler.test(entity, params);
        } catch (Throwable t) {
            VseLog.warnOnce("vse.condition.handler." + type, "[VSE] 条件处理器执行失败: " + type, t);
            return false;
        }
    }

    @Override
    public boolean requiresPlayer() {
        return needsPlayer;
    }

    @Override
    public String getDisplayText() {
        ConditionTypeSpec spec = ConditionTypeSpecRegistry.get(type);
        if (spec != null && spec.displayText() != null) {
            return spec.displayText().apply(this);
        }
        return Component.translatable("visual_set_edit.gui.condition.type." + type).getString();
    }
}
