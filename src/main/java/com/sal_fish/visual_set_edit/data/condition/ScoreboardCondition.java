package com.sal_fish.visual_set_edit.data.condition;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.util.ExpressionEvaluator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

public class ScoreboardCondition extends Condition {
    @Expose public String objective = "";   // 计分板 objective 名
    @Expose public String comparator = "GTE"; // EQ, NEQ, GT, LT, GTE, LTE
    @Expose public double value = 0;        // 比较目标值
    @Expose public String valueExpression = null; // 动态阈值表达式，含成对 %占位符% 时优先于 value

    public ScoreboardCondition() {
        this.type = "scoreboard";
    }

    @Override
    public boolean test(LivingEntity entity) {
        if (objective == null || objective.isEmpty()) return false;
        if (comparator == null) return false;
        Scoreboard scoreboard = entity.level().getScoreboard();
        Objective obj = scoreboard.getObjective(objective);
        if (obj == null) return false;
        double actual = scoreboard.getOrCreatePlayerScore(entity.getScoreboardName(), obj).getScore();
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
        String objName = (objective == null || objective.isEmpty()) ? "-" : objective;
        String compName = Component.translatable("visual_set_edit.condition.comparator." + comparator).getString();
        Object target = (valueExpression != null && !valueExpression.isBlank()) ? valueExpression : value;
        return Component.translatable("visual_set_edit.condition.scoreboard.display", objName, compName, target).getString();
    }
}
