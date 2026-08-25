package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.SlotCondition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public class SlotConditionEditScreen extends Screen {
    private final SetPhase phase;
    private final int index;
    private final Screen parent;
    private EditBox minDurabilityEdit, maxDurabilityEdit, tagEdit;
    private Button itemSelectButton;
    private String selectedSlot;
    private ResourceLocation selectedItem;
    private String selectedTag;

    private static final String[] SLOTS = {"HEAD", "CHEST", "LEGS", "FEET", "MAINHAND", "OFFHAND"};

    public SlotConditionEditScreen(SetPhase phase, int index, Screen parent) {
        super(Component.translatable("visual_set_edit.gui.edit_slot_condition"));
        this.phase = phase;
        this.index = index;
        this.parent = parent;
    }

    private SlotCondition getCondition() {
        if (index < 0 || index >= phase.slotConditions.size()) return null;
        return phase.slotConditions.get(index);
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int totalWidth = 160;
        int rowHeight = 18;
        int spacing = 3;
        int y = 35;

        SlotCondition cond = getCondition();
        if (cond == null) { onClose(); return; }
        selectedSlot = cond.slot != null ? cond.slot : "HEAD";
        selectedItem = cond.itemId != null ? ResourceLocation.tryParse(cond.itemId) : null;
        selectedTag = cond.tagId != null ? cond.tagId : "";

        // 槽位选择（改为按钮 + 文字显示）
        StringWidget slotLabel = new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.slot"), font);
        addRenderableWidget(slotLabel);
        y += rowHeight;

        Button slotSelectButton = Button.builder(
                getSlotButtonText(),
                btn -> {
                    // 保存当前所有编辑内容到 SlotCondition（除 slot 外）
                    applyEditsToCondition(cond);
                    // 打开槽位选择界面
                    assert minecraft != null;
                    minecraft.setScreen(new SlotSelectionScreen(this, selectedSlot, newSlot -> {
                        // 更新槽位并重建编辑界面
                        cond.slot = newSlot;
                        minecraft.setScreen(new SlotConditionEditScreen(phase, index, parent));
                    }));
                }
        ).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
        addRenderableWidget(slotSelectButton);
        y += rowHeight + spacing;

        // 物品选择
        StringWidget itemLabel = new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.item_id"), font);
        addRenderableWidget(itemLabel);
        y += rowHeight;
        itemSelectButton = Button.builder(
                getItemButtonText(),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(new ItemListScreen(this, selectedSlot, rl -> {
                        selectedItem = rl;
                        itemSelectButton.setMessage(getItemButtonText());
                    }));
                }
        ).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
        itemSelectButton.active = selectedTag.isEmpty();
        addRenderableWidget(itemSelectButton);
        y += rowHeight + spacing;

        // Tag 编辑 + 选择按钮
        StringWidget tagLabel = new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.tag_id"), font);
        addRenderableWidget(tagLabel);
        y += rowHeight;

        int tagEditWidth = totalWidth - 22;
        tagEdit = new EditBox(font, centerX - totalWidth / 2, y, tagEditWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.tag_id"));
        tagEdit.setValue(selectedTag);
        tagEdit.setResponder(tag -> {
            selectedTag = tag.trim();
            itemSelectButton.active = selectedTag.isEmpty();
            if (!selectedTag.isEmpty()) {
                selectedItem = null;
                itemSelectButton.setMessage(Component.translatable("visual_set_edit.gui.click_select_item"));
            }
        });
        addRenderableWidget(tagEdit);

        Button tagSelectButton = Button.builder(
                Component.literal("📦"),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(new TagListScreen(this, tagId -> {
                        selectedTag = tagId;
                        tagEdit.setValue(selectedTag);
                        tagEdit.moveCursorToStart();
                        itemSelectButton.active = false;
                        selectedItem = null;
                        itemSelectButton.setMessage(Component.translatable("visual_set_edit.gui.click_select_item"));
                    }));
                }
        ).pos(centerX - totalWidth / 2 + tagEditWidth + 2, y).size(20, rowHeight).build();
        addRenderableWidget(tagSelectButton);
        y += rowHeight + spacing;

        // 耐久
        StringWidget minLabel = new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.min_durability"), font);
        addRenderableWidget(minLabel);
        y += rowHeight;
        minDurabilityEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.min_durability"));
        minDurabilityEdit.setValue(String.valueOf(cond.durabilityMinPercent));
        addRenderableWidget(minDurabilityEdit);
        y += rowHeight + spacing;

        StringWidget maxLabel = new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.max_durability"), font);
        addRenderableWidget(maxLabel);
        y += rowHeight;
        maxDurabilityEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.max_durability"));
        maxDurabilityEdit.setValue(String.valueOf(cond.durabilityMaxPercent));
        addRenderableWidget(maxDurabilityEdit);
        y += rowHeight + spacing + 6;

        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.back"),
                b -> {
                    applyEditsToCondition(cond);
                    assert minecraft != null;
                    minecraft.setScreen(parent);
                }
        ).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build());
    }

    /** 将当前界面的所有编辑值写回 SlotCondition 对象（但不改变屏幕） */
    private void applyEditsToCondition(SlotCondition cond) {
        cond.slot = selectedSlot;
        cond.tagId = selectedTag.isEmpty() ? null : selectedTag;
        cond.itemId = selectedTag.isEmpty() && selectedItem != null ? selectedItem.toString() : null;
        try { cond.durabilityMinPercent = Integer.parseInt(minDurabilityEdit.getValue()); } catch (Exception ignored) {}
        try { cond.durabilityMaxPercent = Integer.parseInt(maxDurabilityEdit.getValue()); } catch (Exception ignored) {}
    }

    private Component getSlotButtonText() {
        return Component.translatable("visual_set_edit.slot." + selectedSlot.toLowerCase());
    }

    private Component getItemButtonText() {
        if (selectedItem != null) {
            Item item = ForgeRegistries.ITEMS.getValue(selectedItem);
            if (item != null) {
                return Component.translatable(item.getDescriptionId());
            }
            return Component.literal(selectedItem.toString());
        }
        return Component.translatable("visual_set_edit.gui.click_select_item");
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.edit_slot_condition"),
                width / 2, 10, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        SlotCondition cond = getCondition();
        if (cond != null) {
            applyEditsToCondition(cond);
        }
        assert minecraft != null;
        minecraft.setScreen(parent);
    }
}