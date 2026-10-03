package com.sal_fish.visual_set_edit.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

// 额外槽位来源：外部 mod 实现它把自定义槽位接入槽位条件
public interface SlotProvider {

    // 来源标识，同时作为槽位键前缀；不能与其他来源重复，也不能用 curios
    String id();

    // 该来源的槽位 id 列表；entity 可为 null
    List<String> slotIds(LivingEntity entity);

    // 取指定槽位的物品，只返回非空的；下标会被用作去重依据，顺序需稳定
    List<ItemStack> stacks(LivingEntity entity, String slotId);

    // 槽位显示名，返回 lang key；返回 null 时使用原始键
    default String slotDisplayName(String slotId) {
        return null;
    }
}
