package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.data.Preset;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class TooltipEditScreen extends Screen {
    private final Preset preset;
    private final Screen parent;
    private final List<EditBox> lineEdits = new ArrayList<>();
    private static final int MAX_LINES = 10;
    private static final int LINE_HEIGHT = 20;
    private static final int LINE_SPACING = 2;
    private static final int EDIT_WIDTH = 200;
    private static final int MARGIN = 20;

    public TooltipEditScreen(Preset preset, Screen parent) {
        super(Component.translatable("visual_set_edit.gui.custom_text.edit.title"));
        this.preset = preset;
        this.parent = parent;
    }

    @Override
    protected void init() {
        lineEdits.clear();
        int startY = 30;

        // 标题帮助
        addRenderableWidget(new StringWidget(MARGIN, startY, width - MARGIN * 2, 18,
                Component.translatable("visual_set_edit.gui.custom_text.edit.help"), font));
        startY += 22;

        // 根据已有行数创建编辑框，至少显示 5 行
        List<String> lines = preset.backgroundStoryLines;
        int displayLines = Math.max(lines.isEmpty() ? 5 : lines.size(), 5);
        displayLines = Math.min(displayLines, MAX_LINES);

        for (int i = 0; i < displayLines; i++) {
            String initialText = i < lines.size() ? lines.get(i) : "";
            EditBox editBox = new EditBox(font, MARGIN, startY, width - MARGIN * 2, LINE_HEIGHT,
                    Component.literal("Line " + (i + 1)));
            editBox.setMaxLength(99999);
            editBox.setValue(initialText);
            editBox.setResponder(s -> {});
            int index = i;
            editBox.setFocused(false);
            editBox.setResponder(s -> { /* 不需要实时响应 */ });
            addRenderableWidget(editBox);
            lineEdits.add(editBox);
            startY += LINE_HEIGHT + LINE_SPACING;
        }

        // 预留一些空间后放置按钮
        startY += 4;

        // 样式按钮（颜色）
        int btnY = startY;
        int btnWidth = 20;
        int spacing = 2;
        int x = MARGIN;

        // 颜色按钮（显示带颜色的方块）
        String[] colorCodes = {"§0","§1","§2","§3","§4","§5","§6","§7","§8","§9","§a","§b","§c","§d","§e","§f"};
        ChatFormatting[] colorFormats = {
                ChatFormatting.BLACK, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GREEN,
                ChatFormatting.DARK_AQUA, ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE,
                ChatFormatting.GOLD, ChatFormatting.GRAY, ChatFormatting.DARK_GRAY,
                ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA,
                ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW,
                ChatFormatting.WHITE
        };

        for (int i = 0; i < colorCodes.length; i++) {
            final String code = colorCodes[i];
            ChatFormatting formatting = colorFormats[i];
            Component label = Component.literal("■").withStyle(formatting);
            addRenderableWidget(Button.builder(label, btn -> {
                OptionalInt focusIndex = getFocusLineIndex();
                if (focusIndex.isPresent()) {
                    EditBox box = lineEdits.get(focusIndex.getAsInt());
                    String txt = box.getValue();
                    int cursor = box.getCursorPosition();
                    String newTxt = txt.substring(0, cursor) + code + txt.substring(cursor);
                    box.setValue(newTxt);
                    box.moveCursorTo(cursor + code.length());
                    box.setFocused(true);
                }
            }).pos(x, btnY).size(btnWidth, LINE_HEIGHT).build());
            x += btnWidth + spacing;
            if (x + btnWidth > width - MARGIN) {
                x = MARGIN;
                btnY += LINE_HEIGHT + spacing;
            }
        }

        // 格式按钮
        btnY += LINE_HEIGHT + 4;
        x = MARGIN;
        String[] formats = {"§l","§o","§n","§m","§r"};
        String[] formatKeys = {
                "visual_set_edit.gui.format.bold",
                "visual_set_edit.gui.format.italic",
                "visual_set_edit.gui.format.underline",
                "visual_set_edit.gui.format.strikethrough",
                "visual_set_edit.gui.format.reset"
        };
        int formatBtnWidth = 50;
        for (int i = 0; i < formats.length; i++) {
            final String code = formats[i];
            Component label = Component.translatable(formatKeys[i]);
            addRenderableWidget(Button.builder(label, btn -> {
                OptionalInt focusIndex = getFocusLineIndex();
                if (focusIndex.isPresent()) {
                    EditBox box = lineEdits.get(focusIndex.getAsInt());
                    String txt = box.getValue();
                    int cursor = box.getCursorPosition();
                    String newTxt = txt.substring(0, cursor) + code + txt.substring(cursor);
                    box.setValue(newTxt);
                    box.moveCursorTo(cursor + code.length());
                    box.setFocused(true);
                }
            }).pos(x, btnY).size(formatBtnWidth, LINE_HEIGHT).build());
            x += formatBtnWidth + spacing;
        }

        // 清除格式按钮
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.format.clear"),
                btn -> {
                    OptionalInt focusIndex = getFocusLineIndex();
                    if (focusIndex.isPresent()) {
                        EditBox box = lineEdits.get(focusIndex.getAsInt());
                        String cleaned = box.getValue().replaceAll("§[0-9a-fk-or]", "");
                        box.setValue(cleaned);
                        box.setCursorPosition(Math.min(box.getCursorPosition(), cleaned.length()));
                        box.setFocused(true);
                    }
                }).pos(width - MARGIN - 60, btnY).size(60, LINE_HEIGHT).build());

        // 保存与返回
        btnY += LINE_HEIGHT + 6;
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.save"),
                btn -> {
                    List<String> newLines = new ArrayList<>();
                    for (EditBox edit : lineEdits) {
                        String text = edit.getValue();
                        if (!text.isEmpty()) {
                            newLines.add(text);
                        }
                    }
                    preset.backgroundStoryLines = newLines;
                    assert minecraft != null;
                    minecraft.setScreen(parent);
                }).pos(width / 2 - 105, btnY).size(100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.back"),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(parent);
                }).pos(width / 2 + 5, btnY).size(100, 20).build());
    }

    private OptionalInt getFocusLineIndex() {
        for (int i = 0; i < lineEdits.size(); i++) {
            if (lineEdits.get(i).isFocused()) {
                return OptionalInt.of(i);
            }
        }
        // 如果没有焦点，默认返回第一行
        if (!lineEdits.isEmpty()) {
            lineEdits.get(0).setFocused(true);
            return OptionalInt.of(0);
        }
        return OptionalInt.empty();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.custom_text.edit.title"),
                width / 2, 10, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }
}