package com.sal_fish.visual_set_edit.data.condition;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public class IronSpellCondition extends Condition {
    @Expose public String field;
    @Expose public String comparator;
    @Expose public double value;

    public IronSpellCondition() { this.type = "iron_spell"; }

    @Override
    public boolean test(LivingEntity entity) {
        if (!IntegrationManager.isIronSpellsLoaded()) return false;
        var is = IntegrationManager.getIronSpells();
        return switch (field) {
            case "MANA" -> compare(is.getMana(entity), comparator, value);
            case "MANA_PERCENT" -> compare(is.getManaPercent(entity), comparator, value);
            case "CASTING" -> is.isCasting(entity);
            default -> false;
        };
    }

    private boolean compare(double a, String comp, double b) {
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
        return Component.translatable("visual_set_edit.condition.iron_spell.display", fieldName, compName, value).getString();
    }
}