package com.sal_fish.visual_set_edit.client;

import com.sal_fish.visual_set_edit.config.CuriosItemMappingManager;
import com.sal_fish.visual_set_edit.config.CuriosItemMappingManager.RegisteredEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class CuriosTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return;
        String itemId = key.toString();

        // 获取所有与物品 NBT 匹配的注册条目
        List<RegisteredEntry> matchingEntries = CuriosItemMappingManager.getMatchingEntries(itemId, stack.getTag());
        if (matchingEntries.isEmpty()) return;

        // 收集所有匹配条目的槽位（去重，保持顺序）
        List<String> slotIds = new ArrayList<>();
        for (RegisteredEntry entry : matchingEntries) {
            for (String slot : entry.slots) {
                String cleaned = slot.startsWith("curios:") ? slot.substring(7) : slot;
                if (!slotIds.contains(cleaned)) {
                    slotIds.add(cleaned);
                }
            }
        }
        if (slotIds.isEmpty()) return;

        MutableComponent slotsTooltip = Component.translatable("curios.tooltip.slot")
                .append(" ")
                .withStyle(ChatFormatting.GOLD);

        for (int i = 0; i < slotIds.size(); i++) {
            String id = slotIds.get(i);
            MutableComponent type = Component.translatable("curios.identifier." + id);
            if (i < slotIds.size() - 1) {
                type = type.append(", ");
            }
            type = type.withStyle(ChatFormatting.YELLOW);
            slotsTooltip.append(type);
        }

        event.getToolTip().add(1, slotsTooltip);
    }
}