package com.sal_fish.visual_set_edit.data.condition;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.data.SlotCondition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class InventoryCondition extends Condition {
    @Expose public String slot;
    @Expose public SlotCondition itemCondition;

    public InventoryCondition() { this.type = "inventory"; }

    @Override
    public boolean test(LivingEntity entity) {
        ItemStack stack = getItemInSlot(entity, slot);
        return itemCondition != null && itemCondition.matches(stack);
    }

    private ItemStack getItemInSlot(LivingEntity entity, String s) {
        return switch (s) {
            case "HEAD" -> entity.getItemBySlot(EquipmentSlot.HEAD);
            case "CHEST" -> entity.getItemBySlot(EquipmentSlot.CHEST);
            case "LEGS" -> entity.getItemBySlot(EquipmentSlot.LEGS);
            case "FEET" -> entity.getItemBySlot(EquipmentSlot.FEET);
            case "MAINHAND" -> entity.getItemBySlot(EquipmentSlot.MAINHAND);
            case "OFFHAND" -> entity.getItemBySlot(EquipmentSlot.OFFHAND);
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public String getDisplayText() { return "Slot " + slot + " match"; }
}