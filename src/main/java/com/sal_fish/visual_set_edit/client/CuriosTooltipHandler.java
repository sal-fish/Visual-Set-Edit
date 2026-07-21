package com.sal_fish.visual_set_edit.client;

import com.sal_fish.visual_set_edit.config.CuriosItemMappingManager;
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

import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class CuriosTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return;
        List<String> slotIds = CuriosItemMappingManager.getSlotsForItem(key.toString());
        if (slotIds.isEmpty()) return;

        MutableComponent slotsTooltip = Component.translatable("curios.tooltip.slot")
                .append(" ")
                .withStyle(ChatFormatting.GOLD);

        for (int i = 0; i < slotIds.size(); i++) {
            String id = slotIds.get(i);
            if (id.startsWith("curios:")) {
                id = id.substring(7);
            }
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
