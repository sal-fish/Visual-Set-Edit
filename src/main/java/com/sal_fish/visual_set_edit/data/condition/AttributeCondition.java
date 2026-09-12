package com.sal_fish.visual_set_edit.data.condition;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.util.ExpressionEvaluator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.registries.ForgeRegistries;

public class AttributeCondition extends Condition {
    @Expose public String attributeId;   // 属性ID
    @Expose public String comparator;    // EQ, GT, LT, GTE, LTE
    @Expose public double value;         // 比较目标值
    @Expose public String valueExpression = null; // 动态阈值表达式，含成对 %占位符% 时优先于 value

    public AttributeCondition() {
        this.type = "attribute";
        this.comparator = "GTE";
        this.value = 0;
    }

    @Override
    public boolean test(LivingEntity entity) {
        if (attributeId == null || attributeId.isEmpty()) return false;
        ResourceLocation rl = ResourceLocation.tryParse(attributeId);
        if (rl == null) return false;
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(rl);
        if (attribute == null) return false;
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return false;
        double actual = instance.getValue();
        return compare(actual, comparator, targetValue(entity));
    }

    private double targetValue(LivingEntity entity) {
        if (valueExpression == null || valueExpression.isBlank()) return value;
        return ExpressionEvaluator.resolveThreshold(entity, valueExpression);
    }

    private boolean compare(double actual, String comp, double target) {
        if (!Double.isFinite(target)) return false; // 含 NEQ，一并拦截 NaN
        return switch (comp) {
            case "EQ" -> actual == target;
            case "GT" -> actual > target;
            case "LT" -> actual < target;
            case "GTE" -> actual >= target;
            case "LTE" -> actual <= target;
            case "NEQ" -> actual != target;
            default -> false;
        };
    }

    @Override
    public String getDisplayText() {
        if (attributeId == null) return "Attribute";
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(attributeId));
        String name = attribute != null ? net.minecraft.network.chat.Component.translatable(attribute.getDescriptionId()).getString() : attributeId;
        String target = (valueExpression != null && !valueExpression.isBlank()) ? valueExpression : String.valueOf(value);
        return name + " " + comparator + " " + target;
    }
}