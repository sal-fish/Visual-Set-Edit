package com.sal_fish.visual_set_edit.data.condition;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import com.sal_fish.visual_set_edit.util.ExpressionEvaluator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public class IronSpellCondition extends Condition {
    @Expose public String field;
    @Expose public String comparator;
    @Expose public double value;
    @Expose public String valueExpression = null; // 动态阈值表达式，含成对 %占位符% 时优先于 value

    public IronSpellCondition() { this.type = "iron_spell"; }

    @Override
    public boolean test(LivingEntity entity) {
        if (!IntegrationManager.isIronSpellsLoaded()) return false;
        var is = IntegrationManager.getIronSpells();
        double target = targetValue(entity);
        return switch (field) {
            case "MANA" -> compare(is.getMana(entity), comparator, target);
            case "MANA_PERCENT" -> compare(is.getManaPercent(entity), comparator, target);
            case "CASTING" -> is.isCasting(entity);
            default -> false;
        };
    }

    private double targetValue(LivingEntity entity) {
        if (valueExpression == null || valueExpression.isBlank()) return value;
        return ExpressionEvaluator.resolveThreshold(entity, valueExpression);
    }

    private boolean compare(double a, String comp, double b) {
        if (!Double.isFinite(b)) return false; // 含 NEQ，一并拦截 NaN
        return switch (comp) {
            case "EQ" -> a == b;
            case "GT" -> a > b;
            case "LT" -> a < b;
            case "GTE" -> a >= b;
            case "LTE" -> a <= b;
            case "NEQ" -> a != b;
            default -> false;
        };
    }

    @Override
    public String getDisplayText() {
        String fieldName = Component.translatable("visual_set_edit.condition.iron_spell.field." + field).getString();
        String compName = Component.translatable("visual_set_edit.condition.comparator." + comparator).getString();
        Object target = (valueExpression != null && !valueExpression.isBlank()) ? valueExpression : value;
        return Component.translatable("visual_set_edit.condition.iron_spell.display", fieldName, compName, target).getString();
    }
}