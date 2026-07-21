package com.sal_fish.visual_set_edit.tooltip;

import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.SlotCondition;
import com.sal_fish.visual_set_edit.data.condition.Condition;
import com.sal_fish.visual_set_edit.data.effect.EffectEntry;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class TooltipRenderer {

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        if (stack.isEmpty()) return;

        List<Preset> all = PresetManager.clientPresets;
        if (all == null) all = Collections.emptyList();

        boolean shiftDown = Screen.hasShiftDown();
        boolean altDown = Screen.hasAltDown();
        boolean ctrlDown = Screen.hasControlDown();

        for (Preset preset : all) {
            boolean belongs = false;
            for (SlotCondition cond : preset.getAllSlotConditions()) {
                if ((cond.itemId != null && !cond.itemId.isEmpty()) || (cond.tagId != null && !cond.tagId.isEmpty())) {
                    if (cond.matches(stack)) {
                        belongs = true;
                        break;
                    }
                }
            }
            if (!belongs) continue;

            // 背景故事
            if (!preset.backgroundStoryLines.isEmpty()) {
                for (String line : preset.backgroundStoryLines) {
                    event.getToolTip().add(parseFormattedText(line));
                }
            }

            if (!preset.showTooltip) continue;

            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable(
                    "visual_set_edit.tooltip.preset_name",
                    preset.fallbackName
            ).withStyle(ChatFormatting.GOLD));

            // Alt：槽位条件
            if (altDown) {
                for (SetPhase phase : preset.phases) {
                    event.getToolTip().add(Component.translatable(
                            "visual_set_edit.tooltip.phase_name",
                            phase.fallbackName
                    ).withStyle(ChatFormatting.YELLOW));

                    for (SlotCondition cond : phase.slotConditions) {
                        String slotName;
                        if (cond.slot.startsWith("curios:")) {
                            slotName = Component.translatable("curios.identifier." + cond.slot.substring(7)).getString();
                        } else {
                            slotName = Component.translatable("visual_set_edit.slot." + cond.slot.toLowerCase()).getString();
                        }
                        String desc = slotName + ": " + getConditionDescription(cond);
                        boolean matched = isSlotMatched(player, cond);
                        ChatFormatting color = matched ? ChatFormatting.GREEN : ChatFormatting.GRAY;
                        event.getToolTip().add(Component.literal("  " + desc).withStyle(color));
                    }
                }
            }
            // Shift 或 Ctrl：显示效果（Ctrl 额外显示条件）
            else if (shiftDown || ctrlDown) {
                for (SetPhase phase : preset.phases) {
                    int matched = countMatched(player, phase);
                    boolean active = matched >= phase.requiredCount;
                    ChatFormatting color = active ? ChatFormatting.GREEN : ChatFormatting.GRAY;
                    event.getToolTip().add(Component.translatable(
                            "visual_set_edit.tooltip.phase.detail",
                            phase.fallbackName,
                            matched,
                            phase.requiredCount
                    ).withStyle(color));

                    // 效果列表
                    for (EffectEntry effect : phase.effects) {
                        ChatFormatting effectColor = effect.getFinalColor();
                        event.getToolTip().add(Component.literal("   ▶ " + effect.getFinalDisplayText())
                                .withStyle(effectColor));
                    }

                    // 附加条件（Ctrl 时显示）
                    if (ctrlDown && !phase.additionalConditions.isEmpty()) {
                        event.getToolTip().add(Component.translatable("visual_set_edit.tooltip.conditions_header")
                                .withStyle(ChatFormatting.DARK_AQUA));
                        for (Condition cond : phase.additionalConditions) {
                            event.getToolTip().add(Component.literal("   ☑ " + cond.getDisplayText())
                                    .withStyle(ChatFormatting.AQUA));
                        }
                    }
                }
            }
            // 默认提示
            else {
                event.getToolTip().add(Component.translatable("visual_set_edit.tooltip.default_hint").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private MutableComponent parseFormattedText(String text) {
        MutableComponent result = Component.literal("");
        Style currentStyle = Style.EMPTY;
        StringBuilder currentText = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                if (!currentText.isEmpty()) {
                    result.append(Component.literal(currentText.toString()).withStyle(currentStyle));
                    currentText.setLength(0);
                }
                char code = text.charAt(i + 1);
                ChatFormatting format = ChatFormatting.getByCode(code);
                if (format != null) {
                    switch (format) {
                        case RESET -> currentStyle = Style.EMPTY;
                        case BOLD -> currentStyle = currentStyle.withBold(true);
                        case ITALIC -> currentStyle = currentStyle.withItalic(true);
                        case UNDERLINE -> currentStyle = currentStyle.withUnderlined(true);
                        case STRIKETHROUGH -> currentStyle = currentStyle.withStrikethrough(true);
                        case OBFUSCATED -> currentStyle = currentStyle.withObfuscated(true);
                        default -> {
                            TextColor textColor = TextColor.fromLegacyFormat(format);
                            currentStyle = currentStyle.withColor(textColor);
                        }
                    }
                } else if (code == 'r') {
                    currentStyle = Style.EMPTY;
                }
                i++;
            } else {
                currentText.append(c);
            }
        }
        if (!currentText.isEmpty()) {
            result.append(Component.literal(currentText.toString()).withStyle(currentStyle));
        }
        return result;
    }

    private String getConditionDescription(SlotCondition cond) {
        if (cond.tagId != null && !cond.tagId.isEmpty()) {
            return "tag:" + cond.tagId;
        } else if (cond.itemId != null && !cond.itemId.isEmpty()) {
            ResourceLocation rl = ResourceLocation.tryParse(cond.itemId);
            if (rl != null) {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) {
                    return Component.translatable(item.getDescriptionId()).getString();
                }
            }
            return cond.itemId;
        } else {
            return "*";
        }
    }

    private boolean isSlotMatched(Player player, SlotCondition cond) {
        if (player == null) return false;
        if (cond.slot.startsWith("curios:") && IntegrationManager.isCuriosLoaded()) {
            String realSlotId = cond.slot.substring(7);
            List<ItemStack> stacks = IntegrationManager.getCurios().getSlotStacks(player, realSlotId);
            for (ItemStack s : stacks) {
                if (cond.matches(s)) return true;
            }
            return false;
        } else {
            ItemStack s = getStackForSlot(player, cond.slot);
            return cond.matches(s);
        }
    }

    private ItemStack getStackForSlot(Player player, String slot) {
        return switch (slot) {
            case "HEAD" -> player.getInventory().getArmor(3);
            case "CHEST" -> player.getInventory().getArmor(2);
            case "LEGS" -> player.getInventory().getArmor(1);
            case "FEET" -> player.getInventory().getArmor(0);
            case "MAINHAND" -> player.getMainHandItem();
            case "OFFHAND" -> player.getOffhandItem();
            default -> ItemStack.EMPTY;
        };
    }

    private int countMatched(Player player, SetPhase phase) {
        if (player == null) return 0;
        int c = 0;
        Map<String, ItemStack> eq = new HashMap<>();
        eq.put("HEAD", player.getInventory().getArmor(3));
        eq.put("CHEST", player.getInventory().getArmor(2));
        eq.put("LEGS", player.getInventory().getArmor(1));
        eq.put("FEET", player.getInventory().getArmor(0));
        eq.put("MAINHAND", player.getMainHandItem());
        eq.put("OFFHAND", player.getOffhandItem());

        Map<String, Set<Integer>> usedIndices = new HashMap<>();

        for (SlotCondition cond : phase.slotConditions) {
            if (cond.slot.startsWith("curios:") && IntegrationManager.isCuriosLoaded()) {
                String realSlotId = cond.slot.substring(7);
                List<ItemStack> stacks = IntegrationManager.getCurios().getSlotStacks(player, realSlotId);
                boolean found = false;
                for (int i = 0; i < stacks.size(); i++) {
                    // 跳过已被其他条件占用的索引
                    if (usedIndices.containsKey(realSlotId) && usedIndices.get(realSlotId).contains(i)) {
                        continue;
                    }
                    ItemStack stack = stacks.get(i);
                    if (cond.matches(stack)) {
                        found = true;
                        usedIndices.computeIfAbsent(realSlotId, k -> new HashSet<>()).add(i);
                        break;
                    }
                }
                if (found) c++;
            } else {
                ItemStack s = eq.get(cond.slot);
                if (cond.matches(s)) c++;
            }
        }
        return c;
    }
}