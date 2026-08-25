package com.sal_fish.visual_set_edit.integration;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public interface IModIntegration {
    boolean isLoaded();
    void onInitialize();

    // 铁魔法
    double getMana(LivingEntity player);
    double getManaPercent(LivingEntity player);
    boolean isCasting(LivingEntity player);
    void tryCastActiveSpell(LivingEntity player);

    // Curios（所有实现类必须提供）
    List<String> getExtraSlots();
    ItemStack getSlotStack(LivingEntity entity, String slotId);
    boolean canItemGoInSlot(String slotId, ItemStack stack);
    List<ItemStack> getSlotStacks(LivingEntity entity, String slotId);
}