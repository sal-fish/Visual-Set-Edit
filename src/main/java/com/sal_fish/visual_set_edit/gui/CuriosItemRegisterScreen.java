package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.config.CuriosItemMappingManager;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CuriosItemRegisterScreen extends Screen {

    private final Screen parent;
    private final String initialItemId;

    private String selectedItemId = null;
    private final List<String> selectedSlots = new ArrayList<>();
    private ScrollableSelectionList slotList;

    private boolean canQuickEquip = true;
    private boolean canRemove = true;

    public CuriosItemRegisterScreen(Screen parent) {
        this(parent, null);
    }

    public CuriosItemRegisterScreen(Screen parent, String initialItemId) {
        super(Component.translatable("visual_set_edit.gui.curios_register.title"));
        this.parent = parent;
        this.initialItemId = initialItemId;
        if (initialItemId != null) {
            this.selectedItemId = initialItemId;
            this.selectedSlots.addAll(CuriosItemMappingManager.getSlotsForItem(initialItemId));
        }
    }

    @Override
    protected void init() {
        clearWidgets();

        int centerX = width / 2 - 100;
        int y = 30;

        // 物品选择按钮
        Button selectItemButton = Button.builder(
                getItemButtonText(),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(new ItemListScreen(this, "", rl -> {
                        selectedItemId = rl.toString();
                        selectedSlots.clear();
                        selectedSlots.addAll(CuriosItemMappingManager.getSlotsForItem(selectedItemId));
                        minecraft.setScreen(this);
                    }));
                }
        ).pos(centerX, y).size(200, 20).build();
        addRenderableWidget(selectItemButton);
        y += 30;

        // 可滚动的槽位列表（居中显示）
        if (selectedItemId != null && IntegrationManager.isCuriosLoaded()) {
            int listWidth = 200;
            int listHeight = Math.min(6 * 22, height - y - 100); // 最多6行
            slotList = new ScrollableSelectionList(minecraft, listWidth, listHeight, y, 20,
                    entry -> {
                        if (entry instanceof SlotEntry slotEntry) {
                            String fullSlotId = slotEntry.fullSlotId;
                            if (selectedSlots.contains(fullSlotId)) {
                                selectedSlots.remove(fullSlotId);
                            } else {
                                selectedSlots.add(fullSlotId);
                            }
                            slotList.setSelected(null);
                        }
                    });
            slotList.setLeftPos(centerX);
            addWidget(slotList);

            List<String> allSlots = IntegrationManager.getCurios().getExtraSlots();
            for (String slotId : allSlots) {
                String fullSlotId = "curios:" + slotId;
                slotList.addEntry(new SlotEntry(fullSlotId));
            }
            y += listHeight + 5;
        }

        // 可快捷放入
        Button quickEquipButton = Button.builder(
                Component.translatable("visual_set_edit.gui.curios_register.quick_equip").append(": ")
                        .append(Component.translatable(canQuickEquip ? "options.on" : "options.off")),
                btn -> {
                    canQuickEquip = !canQuickEquip;
                    btn.setMessage(Component.translatable("visual_set_edit.gui.curios_register.quick_equip").append(": ")
                            .append(Component.translatable(canQuickEquip ? "options.on" : "options.off")));
                }
        ).pos(centerX, y).size(200, 20).build();
        addRenderableWidget(quickEquipButton);
        y += 22;

        // 是否可取下
        Button canRemoveButton = Button.builder(
                Component.translatable("visual_set_edit.gui.curios_register.can_remove").append(": ")
                        .append(Component.translatable(canRemove ? "options.on" : "options.off")),
                btn -> {
                    canRemove = !canRemove;
                    btn.setMessage(Component.translatable("visual_set_edit.gui.curios_register.can_remove").append(": ")
                            .append(Component.translatable(canRemove ? "options.on" : "options.off")));
                }
        ).pos(centerX, y).size(200, 20).build();
        addRenderableWidget(canRemoveButton);
        y += 22;

        // 保存按钮
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.save"),
                btn -> {
                    if (selectedItemId != null) {
                        if (selectedSlots.isEmpty()) {
                            CuriosItemMappingManager.removeMapping(selectedItemId);
                        } else {
                            CuriosItemMappingManager.addMapping(
                                    selectedItemId,
                                    new ArrayList<>(selectedSlots),
                                    canQuickEquip,
                                    canRemove
                            );
                        }
                    }
                    assert minecraft != null;
                    minecraft.setScreen(parent);
                }).pos(centerX, y).size(200, 20).build());
        y += 25;

        // 查看已注册列表按钮
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.curios_registered.button"),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(new RegisteredItemsScreen(this));
                }).pos(centerX, y).size(200, 20).build());
        y += 25;

        // 返回按钮
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.back"),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(parent);
                }).pos(centerX, y).size(200, 20).build());
    }

    private Component getItemButtonText() {
        if (selectedItemId != null && !selectedItemId.isEmpty()) {
            ResourceLocation rl = ResourceLocation.tryParse(selectedItemId);
            if (rl != null) {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) {
                    return Component.translatable(item.getDescriptionId());
                }
            }
            return Component.literal(selectedItemId);
        }
        return Component.translatable("visual_set_edit.gui.click_select_item");
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        if (slotList != null) {
            slotList.render(graphics, mouseX, mouseY, partial);
        }
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font,
                Component.translatable("visual_set_edit.gui.curios_register.title"),
                width / 2, 10, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (slotList != null && slotList.isMouseOver(mouseX, mouseY)) {
            if (slotList.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (slotList != null && slotList.isMouseOver(mouseX, mouseY)) {
            return slotList.mouseScrolled(mouseX, mouseY, scrollDelta);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    public String getInitialItemId() {
        return initialItemId;
    }

    private class SlotEntry extends ScrollableSelectionList.Entry {
        final String fullSlotId;

        SlotEntry(String fullSlotId) {
            super(Component.literal(fullSlotId), new ResourceLocation("vse", "slot"), null);
            this.fullSlotId = fullSlotId;
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(fullSlotId);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            String slotId = fullSlotId.startsWith("curios:") ? fullSlotId.substring(7) : fullSlotId;
            boolean checked = selectedSlots.contains(fullSlotId);
            String prefix = checked ? "[✔] " : "[ ] ";
            String text = prefix + Component.translatable("curios.identifier." + slotId).getString();
            graphics.drawString(font, text, left, top + (rowHeight - 8) / 2, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            return true;
        }
    }
}