package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.data.TargetFilter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.function.Consumer;

public class TargetFilterEditScreen extends Screen {
    // 筛选类型枚举
    private enum FilterType {
        NONE,
        BLOCK_ID,
        BLOCK_TAG,
        ENTITY_ID,
        ENTITY_TAG,
        ENTITY_TAGS
    }

    private final Screen parent;
    private final Consumer<TargetFilter> onSave;
    private TargetFilter filter;
    private FilterType currentType = FilterType.NONE;

    private Button selectButton;
    private EditBox entityTagsEdit;

    public TargetFilterEditScreen(Screen parent, TargetFilter initialFilter, Consumer<TargetFilter> onSave) {
        super(Component.translatable("visual_set_edit.gui.target_filter.title"));
        this.parent = parent;
        this.filter = initialFilter != null ? initialFilter : new TargetFilter();
        this.onSave = onSave;
        // 根据已有字段推断当前类型
        if (filter.blockId != null) currentType = FilterType.BLOCK_ID;
        else if (filter.blockTag != null) currentType = FilterType.BLOCK_TAG;
        else if (filter.entityTypeId != null) currentType = FilterType.ENTITY_ID;
        else if (filter.entityTypeTag != null) currentType = FilterType.ENTITY_TAG;
        else if (filter.entityTags != null && !filter.entityTags.isEmpty()) currentType = FilterType.ENTITY_TAGS;
    }

    @Override
    protected void init() {
        clearWidgets();  // 关键：清除旧控件，防止残留

        int centerX = width / 2;
        int totalWidth = 200;
        int rowHeight = 20;
        int spacing = 3;
        int y = 30;

        // 筛选类型选择
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.target_filter.type"), font));
        y += rowHeight;
        // 重建界面
        CycleButton<FilterType> typeButton = CycleButton.<FilterType>builder(type -> Component.translatable(
                        "visual_set_edit.gui.target_filter.type." + type.name().toLowerCase()))
                .withValues(FilterType.values())
                .displayOnlyValue()
                .withInitialValue(currentType)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.target_filter.type"),
                        (btn, val) -> {
                            currentType = val;
                            filter = new TargetFilter();
                            init(); // 重建界面
                        });
        addRenderableWidget(typeButton);
        y += rowHeight + spacing;

        // 根据类型显示不同控件
        if (currentType == FilterType.ENTITY_TAGS) {
            // 显示实体 Tag 列表输入框
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.target_filter.entity_tags"), font));
            y += rowHeight;
            entityTagsEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.target_filter.entity_tags"));
            entityTagsEdit.setMaxLength(5201314);
            if (filter.entityTags != null) {
                entityTagsEdit.setValue(String.join(",", filter.entityTags));
            } else {
                entityTagsEdit.setValue("");
            }
            addRenderableWidget(entityTagsEdit);
            y += rowHeight + spacing;
            selectButton = null;
        } else if (currentType != FilterType.NONE) {
            // 显示选择按钮
            selectButton = Button.builder(
                    getSelectButtonText(),
                    btn -> openSelector()
            ).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
            addRenderableWidget(selectButton);
            y += rowHeight + spacing;
            entityTagsEdit = null;
        } else {
            selectButton = null;
            entityTagsEdit = null;
        }

        y += spacing;

        // 清空按钮
        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.target_filter.clear"),
                btn -> {
                    filter = new TargetFilter();
                    currentType = FilterType.NONE;
                    init(); // 重建界面
                }
        ).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build());
        y += rowHeight + spacing;

        // 保存按钮
        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.save"),
                btn -> {
                    // 保存前解析实体 Tag 输入
                    if (currentType == FilterType.ENTITY_TAGS && entityTagsEdit != null) {
                        String tagsStr = entityTagsEdit.getValue().trim();
                        if (tagsStr.isEmpty()) {
                            filter.entityTags = new ArrayList<>();
                        } else {
                            String[] parts = tagsStr.split(",");
                            filter.entityTags = new ArrayList<>();
                            for (String p : parts) {
                                String trimmed = p.trim();
                                if (!trimmed.isEmpty()) {
                                    filter.entityTags.add(trimmed);
                                }
                            }
                        }
                    }
                    onSave.accept(filter);
                    assert minecraft != null;
                    minecraft.setScreen(parent);
                }
        ).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build());
        y += rowHeight + spacing;

        // 取消按钮
        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.cancel"),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(parent);
                }
        ).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build());
    }

    private void openSelector() {
        assert minecraft != null;
        switch (currentType) {
            case BLOCK_ID -> minecraft.setScreen(new BlockListScreen(this, rl -> {
                filter.blockId = rl.toString();
                updateSelectButtonText();
            }));
            case BLOCK_TAG -> minecraft.setScreen(new BlockTagListScreen(this, tagId -> {
                filter.blockTag = tagId;
                updateSelectButtonText();
            }));
            case ENTITY_ID -> minecraft.setScreen(new EntityTypeListScreen(this, rl -> {
                filter.entityTypeId = rl.toString();
                updateSelectButtonText();
            }));
            case ENTITY_TAG -> minecraft.setScreen(new EntityTypeTagListScreen(this, tagId -> {
                filter.entityTypeTag = tagId;
                updateSelectButtonText();
            }));
            default -> {}
        }
    }

    private Component getSelectButtonText() {
        return switch (currentType) {
            case BLOCK_ID -> filter.blockId != null ? Component.literal(filter.blockId) : Component.translatable("visual_set_edit.gui.click_select_item");
            case BLOCK_TAG -> filter.blockTag != null ? Component.literal(filter.blockTag) : Component.translatable("visual_set_edit.gui.click_select_item");
            case ENTITY_ID -> filter.entityTypeId != null ? Component.literal(filter.entityTypeId) : Component.translatable("visual_set_edit.gui.click_select_item");
            case ENTITY_TAG -> filter.entityTypeTag != null ? Component.literal(filter.entityTypeTag) : Component.translatable("visual_set_edit.gui.click_select_item");
            default -> Component.literal("");
        };
    }

    private void updateSelectButtonText() {
        if (selectButton != null) {
            selectButton.setMessage(getSelectButtonText());
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.target_filter.title"),
                width / 2, 10, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }
}