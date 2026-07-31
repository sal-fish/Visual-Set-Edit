package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.SlotCondition;
import com.sal_fish.visual_set_edit.data.NbtMatchRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PhaseEditScreen extends Screen {
    private final Preset preset;
    private final int phaseIndex;
    private final Screen parent;
    private EditBox nameEdit;
    private EditBox countEdit;
    private final List<SlotCondition> slotConditions = new ArrayList<>();
    private final Map<SlotCondition, EditBox> tagEditMap = new HashMap<>();

    private int scrollOffset = 0;
    private static final int MAX_VISIBLE_ROWS = 6;

    public PhaseEditScreen(Preset preset, int phaseIndex, Screen parent) {
        super(Component.translatable("visual_set_edit.gui.phase_edit.title"));
        this.preset = preset;
        this.phaseIndex = phaseIndex;
        this.parent = parent;
    }

    private SetPhase getPhase() { return preset.phases.get(phaseIndex); }

    private void saveNameAndCount() {
        if (nameEdit != null) {
            getPhase().fallbackName = nameEdit.getValue();
        }
        if (countEdit != null) {
            try {
                getPhase().requiredCount = Integer.parseInt(countEdit.getValue());
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    protected void init() {
        clearWidgets();
        tagEditMap.clear();
        SetPhase phase = getPhase();

        slotConditions.clear();
        slotConditions.addAll(phase.slotConditions);

        int leftX = 10;
        int y = 5;
        int rowHeight = 20;
        int smallSpacing = 2;
        int controlWidth = 240;

        // 固定头部
        addRenderableWidget(new StringWidget(leftX, y, 40, rowHeight,
                Component.translatable("visual_set_edit.gui.name"), font));
        nameEdit = new EditBox(font, leftX + 45, y, controlWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.name"));
        nameEdit.setValue(phase.fallbackName != null ? phase.fallbackName : "");
        addRenderableWidget(nameEdit);
        y += rowHeight + smallSpacing;

        addRenderableWidget(new StringWidget(leftX, y, 40, rowHeight,
                Component.translatable("visual_set_edit.gui.count"), font));
        countEdit = new EditBox(font, leftX + 45, y, 40, rowHeight,
                Component.translatable("visual_set_edit.gui.count"));
        countEdit.setValue(String.valueOf(phase.requiredCount));
        addRenderableWidget(countEdit);
        y += rowHeight + 6;

        // 可滚动槽位区域
        int listTop = y;
        int maxOffset = Math.max(0, slotConditions.size() - MAX_VISIBLE_ROWS);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;
        int startRow = scrollOffset;
        int endRow = Math.min(slotConditions.size(), startRow + MAX_VISIBLE_ROWS);
        int usedRows = endRow - startRow;
        int listHeight = usedRows * (rowHeight + smallSpacing);
        int listBottom = listTop + listHeight;

        // 为“捕获手持物品”按钮分配宽度
        int slotButtonWidth = 54;
        int deleteButtonWidth = 18;
        int tagButtonWidth = 18;
        int exactButtonWidth = 20;
        int captureButtonWidth = 20;
        int spacingBetween = 2;
        int tagEditWidth = 36;
        int itemButtonWidth = controlWidth - slotButtonWidth - deleteButtonWidth - tagEditWidth
                - tagButtonWidth - exactButtonWidth - captureButtonWidth - spacingBetween * 7;

        int currentY = listTop;
        for (int i = startRow; i < endRow; i++) {
            SlotCondition cond = slotConditions.get(i);
            int rowY = currentY;

            // 槽位按钮
            Button slotButton = Button.builder(
                    getSlotDisplayName(cond.slot),
                    btn -> {
                        saveNameAndCount();
                        assert minecraft != null;
                        minecraft.setScreen(new SlotSelectionScreen(this, cond.slot, newSlot -> {
                            cond.slot = newSlot;
                            minecraft.setScreen(this);
                        }));
                    }
            ).pos(leftX, rowY).size(slotButtonWidth, rowHeight).build();
            addRenderableWidget(slotButton);

            int curX = leftX + slotButtonWidth + spacingBetween;

            // 物品选择按钮
            Button itemButton = Button.builder(
                    getSlotItemText(cond),
                    btn -> {
                        saveNameAndCount();
                        assert minecraft != null;
                        minecraft.setScreen(new ItemListScreen(this, cond.slot, rl -> {
                            cond.itemId = rl.toString();
                            cond.tagId = null;
                            EditBox edit = tagEditMap.get(cond);
                            if (edit != null) edit.setValue("");
                            btn.setMessage(getSlotItemText(cond));
                        }));
                    }
            ).pos(curX, rowY).size(itemButtonWidth, rowHeight).build();
            itemButton.active = (cond.tagId == null || cond.tagId.isEmpty());
            addRenderableWidget(itemButton);

            curX += itemButtonWidth + spacingBetween;

            // Tag 输入框
            EditBox tagEdit = new EditBox(font, curX, rowY, tagEditWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.tag_id"));
            tagEdit.setValue(cond.tagId != null ? cond.tagId : "");
            tagEdit.setResponder(tag -> {
                cond.tagId = tag.trim().isEmpty() ? null : tag.trim();
                if (cond.tagId != null && !cond.tagId.isEmpty()) {
                    cond.itemId = null;
                }
                itemButton.active = (cond.tagId == null || cond.tagId.isEmpty());
                itemButton.setMessage(getSlotItemText(cond));
            });
            addRenderableWidget(tagEdit);
            tagEditMap.put(cond, tagEdit);

            curX += tagEditWidth + spacingBetween;

            // Tag 选择按钮
            Button tagSelectButton = Button.builder(
                    Component.literal("📦"),
                    btn -> {
                        saveNameAndCount();
                        assert minecraft != null;
                        minecraft.setScreen(new TagListScreen(this, tagId -> {
                            cond.tagId = tagId;
                            cond.itemId = null;
                            EditBox edit = tagEditMap.get(cond);
                            if (edit != null) edit.setValue(tagId);
                            itemButton.active = false;
                            itemButton.setMessage(getSlotItemText(cond));
                        }));
                    }
            ).pos(curX, rowY).size(tagButtonWidth, rowHeight).build();
            addRenderableWidget(tagSelectButton);

            curX += tagButtonWidth + spacingBetween;

            String nbtSymbol = switch (cond.nbtRule) {
                case IGNORE -> "✗";
                case EXACT -> "✓";
                case CUSTOM_KEYS -> "○";
            };
            Button exactButton = Button.builder(
                    Component.literal(nbtSymbol),
                    btn -> {
                        saveNameAndCount();
                        // 循环切换
                        switch (cond.nbtRule) {
                            case IGNORE -> cond.nbtRule = NbtMatchRule.EXACT;
                            case EXACT -> cond.nbtRule = NbtMatchRule.CUSTOM_KEYS;
                            case CUSTOM_KEYS -> cond.nbtRule = NbtMatchRule.IGNORE;
                        }
                        // 根据模式清理/保留数据
                        if (cond.nbtRule == NbtMatchRule.IGNORE) {
                            cond.exactNbt = null;
                            cond.nbtKeys = null;
                        }
                        init();
                    }
            ).pos(curX, rowY).size(exactButtonWidth, rowHeight).build();
            addRenderableWidget(exactButton);

            curX += exactButtonWidth + spacingBetween;

            // 捕获手持物品按钮
            Button captureButton = Button.builder(
                    Component.literal("📋"),
                    btn -> {
                        saveNameAndCount();
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            ItemStack held = mc.player.getMainHandItem();
                            if (!held.isEmpty()) {
                                ResourceLocation key = ForgeRegistries.ITEMS.getKey(held.getItem());
                                if (key != null) {
                                    cond.itemId = key.toString();
                                    cond.tagId = null;
                                    // 打开全 NBT 编辑界面
                                    String currentNbt = held.getTag() != null ? held.getTag().toString() : "";
                                    if (minecraft != null) {
                                        minecraft.setScreen(new FullNbtEditScreen(currentNbt, editedNbt -> {
                                            cond.exactNbt = editedNbt;
                                            // 自动解析并生成 nbtKeys
                                            if (editedNbt != null) {
                                                try {
                                                    CompoundTag tag = TagParser.parseTag(editedNbt);
                                                    Map<String, Object> keys = new HashMap<>();
                                                    for (String k : tag.getAllKeys()) {
                                                        keys.put(k, Objects.requireNonNull(tag.get(k)).getAsString());
                                                    }
                                                    cond.nbtKeys = keys.isEmpty() ? null : keys;
                                                } catch (Exception e) {
                                                    cond.nbtKeys = null;
                                                }
                                            } else {
                                                cond.nbtKeys = null;
                                            }
                                            minecraft.setScreen(this);
                                            init();
                                        }));
                                    }
                                }
                            }
                        }
                    }
            ).pos(curX, rowY).size(captureButtonWidth, rowHeight).build();
            addRenderableWidget(captureButton);

            curX += captureButtonWidth + spacingBetween;

            // 删除按钮
            final SlotCondition toRemove = cond;
            addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.delete"), btn -> {
                saveNameAndCount();
                phase.slotConditions.remove(toRemove);
                init();
            }).pos(curX, rowY).size(deleteButtonWidth, rowHeight).build());

            currentY += rowHeight + smallSpacing;
        }

        int addButtonY = listBottom + 4;
        // 添加槽位按钮（占主要宽度）
        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.add_slot"),
                btn -> {
                    saveNameAndCount();
                    SlotCondition newCond = new SlotCondition();
                    newCond.slot = "HEAD";
                    phase.slotConditions.add(newCond);
                    scrollOffset = Math.max(0, phase.slotConditions.size() - MAX_VISIBLE_ROWS);
                    init();
                }
        ).pos(leftX, addButtonY).size(controlWidth - 82, rowHeight).build());

        // 导入槽位按钮
        if (preset.phases.size() > 1) {
            addRenderableWidget(Button.builder(
                    Component.translatable("visual_set_edit.gui.import_slots"),
                    btn -> {
                        saveNameAndCount();
                        assert minecraft != null;
                        List<SetPhase> otherPhases = new ArrayList<>(preset.phases);
                        otherPhases.remove(getPhase());
                        if (!otherPhases.isEmpty()) {
                            minecraft.setScreen(new PhaseSelectScreen(this, otherPhases, selected -> {
                                // 深拷贝槽位条件并覆盖当前阶段的槽位条件
                                getPhase().slotConditions.clear();
                                for (SlotCondition cond : selected.slotConditions) {
                                    String json = PresetManager.GSON.toJson(cond);
                                    SlotCondition copy = PresetManager.GSON.fromJson(json, SlotCondition.class);
                                    getPhase().slotConditions.add(copy);
                                }
                                minecraft.setScreen(this);
                            }));
                        }
                    }
            ).pos(leftX + controlWidth - 80, addButtonY).size(80, rowHeight).build());
        }

        // 效果 / 条件按钮
        int bottomY = addButtonY + rowHeight + 4;
        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.effects"),
                btn -> {
                    saveNameAndCount();
                    assert minecraft != null;
                    minecraft.setScreen(new EffectListScreen(phase, preset, this));
                }
        ).pos(leftX, bottomY).size(80, rowHeight).build());
        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.conditions"),
                btn -> {
                    saveNameAndCount();
                    assert minecraft != null;
                    minecraft.setScreen(new ConditionListScreen(phase, preset, this));
                }
        ).pos(leftX + 85, bottomY).size(80, rowHeight).build());

        // 返回按钮
        bottomY += rowHeight + 4;
        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.back"),
                btn -> {
                    phase.fallbackName = nameEdit.getValue();
                    try {
                        phase.requiredCount = Integer.parseInt(countEdit.getValue());
                    } catch (NumberFormatException ignored) {}
                    assert minecraft != null;
                    minecraft.setScreen(parent);
                }
        ).pos(leftX, bottomY).size(controlWidth, rowHeight).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int listTop = 5 + 20 + 2 + 6 + 20 + 6;
        int listHeight = MAX_VISIBLE_ROWS * (20 + 2);
        int listBottom = listTop + listHeight;
        if (mouseY >= listTop && mouseY <= listBottom) {
            int maxOffset = Math.max(0, slotConditions.size() - MAX_VISIBLE_ROWS);
            int newOffset = scrollOffset - (int) Math.signum(scrollDelta);
            scrollOffset = Math.max(0, Math.min(maxOffset, newOffset));
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    private Component getSlotDisplayName(String slot) {
        if (slot.startsWith("curios:")) {
            return Component.translatable("curios.identifier." + slot.substring(7));
        }
        return Component.translatable("visual_set_edit.slot." + slot.toLowerCase());
    }

    private Component getSlotItemText(SlotCondition cond) {
        if (cond.itemId != null && !cond.itemId.isEmpty()) {
            ResourceLocation rl = ResourceLocation.tryParse(cond.itemId);
            if (rl != null) {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) {
                    return Component.translatable(item.getDescriptionId());
                }
            }
            return Component.literal(cond.itemId);
        }
        return Component.translatable("visual_set_edit.gui.click_select_item");
    }

    private static class PhaseSelectScreen extends Screen {
        private final Screen parent;
        private final List<SetPhase> phases;
        private final java.util.function.Consumer<SetPhase> onSelect;

        public PhaseSelectScreen(Screen parent, List<SetPhase> phases, java.util.function.Consumer<SetPhase> onSelect) {
            super(Component.translatable("visual_set_edit.gui.select_phase"));
            this.parent = parent;
            this.phases = phases;
            this.onSelect = onSelect;
        }

        @Override
        protected void init() {
            int y = 40;
            for (SetPhase phase : phases) {
                String name = phase.fallbackName != null ? phase.fallbackName : "Phase";
                addRenderableWidget(Button.builder(Component.literal(name), btn -> {
                    onSelect.accept(phase);
                    if (minecraft != null) minecraft.setScreen(parent);
                }).pos(width / 2 - 100, y).size(200, 20).build());
                y += 22;
            }
            addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.cancel"), btn -> {
                if (minecraft != null) minecraft.setScreen(parent);
            }).pos(width / 2 - 100, y + 10).size(200, 20).build());
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            renderBackground(graphics);
            super.render(graphics, mouseX, mouseY, partial);
            graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.select_phase"),
                    width / 2, 10, 0xFFFFFF);
        }
    }


    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.edit_phase"),
                width / 2, 5, 0xFFFFFF);

        // 滚动条
        if (slotConditions.size() > MAX_VISIBLE_ROWS) {
            int listTop = 5 + 20 + 2 + 6 + 20 + 6;
            int listHeight = MAX_VISIBLE_ROWS * (20 + 2);
            int scrollBarX = width - 5;
            int totalRows = slotConditions.size();
            float visibleRatio = (float) MAX_VISIBLE_ROWS / totalRows;
            int scrollBarHeight = Math.max(4, (int) (listHeight * visibleRatio));
            int scrollBarY = listTop + (int) ((listHeight - scrollBarHeight) *
                    ((float) scrollOffset / (totalRows - MAX_VISIBLE_ROWS)));
            graphics.fill(scrollBarX, listTop, scrollBarX + 3, listTop + listHeight, 0xFFAAAAAA);
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarHeight, 0xFFFFFFFF);
        }
    }
}