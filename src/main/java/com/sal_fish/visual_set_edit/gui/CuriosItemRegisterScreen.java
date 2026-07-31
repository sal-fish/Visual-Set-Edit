package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.config.CuriosItemMappingManager;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CuriosItemRegisterScreen extends Screen {

    private final Screen parent;
    private final String initialItemId;
    private String editingNbt; // 当前编辑的条目 NBT，null 表示不限 NBT

    private String selectedItemId = null;
    private final List<String> selectedSlots = new ArrayList<>();
    private ScrollableSelectionList slotList;

    private boolean canQuickEquip = true;
    private boolean canRemove = true;

    private String capturedNbt = "";

    // 原有构造函数（新建）
    public CuriosItemRegisterScreen(Screen parent) {
        this(parent, null, null);
    }

    public CuriosItemRegisterScreen(Screen parent, String initialItemId) {
        this(parent, initialItemId, null);
    }

    // 新增：可传入编辑的 NBT
    public CuriosItemRegisterScreen(Screen parent, String initialItemId, String editingNbt) {
        super(Component.translatable("visual_set_edit.gui.curios_register.title"));
        this.parent = parent;
        this.initialItemId = initialItemId;
        this.editingNbt = editingNbt;
        if (initialItemId != null) {
            this.selectedItemId = initialItemId;
            loadEntryForEditing(initialItemId, editingNbt);
        }
    }

    /**
     * 根据 itemId 和 nbt 加载对应的注册条目，若找不到则按默认新建。
     */
    private void loadEntryForEditing(String itemId, String nbt) {
        selectedSlots.clear();
        List<CuriosItemMappingManager.RegisteredEntry> entries =
                CuriosItemMappingManager.getEntries(itemId);

        CuriosItemMappingManager.RegisteredEntry target = null;
        if (nbt != null) {
            // 精确匹配 nbt
            for (CuriosItemMappingManager.RegisteredEntry e : entries) {
                if (Objects.equals(e.nbt, nbt)) {
                    target = e;
                    break;
                }
            }
        } else {
            // 无 nbt 要求，取第一个（或 nbt 为 null 的）
            for (CuriosItemMappingManager.RegisteredEntry e : entries) {
                if (e.nbt == null) {
                    target = e;
                    break;
                }
            }
            if (target == null && !entries.isEmpty()) {
                target = entries.get(0); // 兜底
            }
        }

        if (target != null) {
            selectedSlots.addAll(target.slots);
            canQuickEquip = target.canQuickEquip;
            canRemove = target.canRemove;
            capturedNbt = target.nbt != null ? target.nbt : "";
            editingNbt = target.nbt; // 保持同步
        } else {
            // 新建条目，使用传入的 nbt（如果有）作为初始 NBT
            capturedNbt = (nbt != null) ? nbt : "";
            editingNbt = (nbt != null && !nbt.isEmpty()) ? nbt : null;
            // 新建时槽位为空，开关默认 true
            canQuickEquip = true;
            canRemove = true;
        }
    }

    @Override
    protected void init() {
        clearWidgets();

        int centerX = width / 2 - 100;
        int y = 30;
        int halfWidth = 99;

        // 物品选择按钮
        Button selectItemButton = Button.builder(
                getItemButtonText(),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(new ItemListScreen(this, "", rl -> {
                        selectedItemId = rl.toString();
                        // 重新选择物品后，清除编辑状态，加载第一个条目
                        editingNbt = null;
                        loadEntryForEditing(selectedItemId, null);
                        minecraft.setScreen(this);
                    }));
                }
        ).pos(centerX, y).size(halfWidth, 20).build();
        addRenderableWidget(selectItemButton);

        // 手持物品 NBT 获取 → 现在打开编辑界面
        Button captureNbtButton = Button.builder(
                Component.translatable("visual_set_edit.gui.curios_register.capture_nbt"),
                btn -> {
                    assert minecraft != null;
                    String initialNbt = capturedNbt; // 默认使用已保存的 NBT
                    // 尝试从手持物品获取 NBT 作为初始编辑内容
                    if (minecraft.player != null) {
                        ItemStack held = minecraft.player.getMainHandItem();
                        if (!held.isEmpty()) {
                            ResourceLocation key = ForgeRegistries.ITEMS.getKey(held.getItem());
                            if (key != null) {
                                selectedItemId = key.toString();
                                CompoundTag tag = held.getTag();
                                initialNbt = tag != null ? tag.toString() : "";
                                selectItemButton.setMessage(getItemButtonText());
                            }
                        }
                    }
                    // 打开 NBT 编辑界面
                    minecraft.setScreen(new FullNbtEditScreen(this, initialNbt, editedNbt -> {
                        capturedNbt = (editedNbt != null) ? editedNbt : "";
                        editingNbt = capturedNbt.isEmpty() ? null : capturedNbt;
                        loadEntryForEditing(selectedItemId, editingNbt);
                        minecraft.setScreen(this);
                        init(); // 刷新界面（槽位勾选等）
                    }));
                }
        ).pos(centerX + halfWidth + 2, y).size(halfWidth, 20).build();
        addRenderableWidget(captureNbtButton);

        y += 22;

        // 可滚动的槽位列表
        if (selectedItemId != null && IntegrationManager.isCuriosLoaded()) {
            int listWidth = 200;
            int listHeight = Math.min(6 * 22, height - y - 100);
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
                            // 删除对应条目
                            if (editingNbt != null) {
                                List<CuriosItemMappingManager.RegisteredEntry> entries =
                                        CuriosItemMappingManager.getEntries(selectedItemId);
                                for (int i = 0; i < entries.size(); i++) {
                                    if (Objects.equals(entries.get(i).nbt, editingNbt)) {
                                        CuriosItemMappingManager.removeEntry(selectedItemId, i);
                                        break;
                                    }
                                }
                            } else {
                                CuriosItemMappingManager.removeAllEntries(selectedItemId);
                            }
                        } else {
                            // 添加或更新条目
                            if (editingNbt != null) {
                                // 先移除旧条目（如果存在）
                                List<CuriosItemMappingManager.RegisteredEntry> entries =
                                        new ArrayList<>(CuriosItemMappingManager.getEntries(selectedItemId));
                                for (int i = 0; i < entries.size(); i++) {
                                    if (Objects.equals(entries.get(i).nbt, editingNbt)) {
                                        CuriosItemMappingManager.removeEntry(selectedItemId, i);
                                        break;
                                    }
                                }
                                CuriosItemMappingManager.addEntry(
                                        selectedItemId,
                                        new ArrayList<>(selectedSlots),
                                        canQuickEquip,
                                        canRemove,
                                        editingNbt.isEmpty() ? null : editingNbt
                                );
                            } else {
                                // 不限 NBT 的新条目
                                CuriosItemMappingManager.addEntry(
                                        selectedItemId,
                                        new ArrayList<>(selectedSlots),
                                        canQuickEquip,
                                        canRemove,
                                        null
                                );
                            }
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