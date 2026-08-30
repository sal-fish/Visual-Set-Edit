package com.sal_fish.visual_set_edit.data.condition;

import com.google.gson.annotations.Expose;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

public class CompositeCondition extends Condition {
    @Expose public String op;
    @Expose public List<Condition> children;

    public CompositeCondition() { this.type = "composite"; }

    @Override
    public boolean requiresPlayer() {
        if (children == null || children.isEmpty()) return false;
        return switch (op) {
            case "AND" -> children.stream().anyMatch(Condition::requiresPlayer);
            case "OR" -> children.stream().allMatch(Condition::requiresPlayer);
            default -> false;
        };
    }

    @Override
    public boolean test(LivingEntity entity) {
        if (children == null || children.isEmpty()) return true;
        return switch (op) {
            case "AND" -> children.stream().allMatch(c -> c.test(entity));
            case "OR" -> children.stream().anyMatch(c -> c.test(entity));
            case "NOT" -> !children.get(0).test(entity);
            default -> false;
        };
    }

    @Override
    public String getDisplayText() { return "Composite (" + op + ")"; }
}