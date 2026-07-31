package com.sal_fish.visual_set_edit.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class FullNbtEditScreen extends Screen {
    private final String initialNbt;
    private final Consumer<String> onSave;
    private EditBox textArea;

    public FullNbtEditScreen(String initialNbt, Consumer<String> onSave) {
        super(Component.translatable("visual_set_edit.gui.edit_full_nbt"));
        this.initialNbt = initialNbt;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        int width = 200;
        int height = 120;
        int x = (this.width - width) / 2;
        int y = 30;

        textArea = new EditBox(font, x, y, width, height, Component.literal("NBT"));
        textArea.setMaxLength(65535);
        textArea.setValue(initialNbt != null ? initialNbt : "");
        addRenderableWidget(textArea);
        textArea.setFocused(true);

        y += height + 5;

        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.save"), btn -> {
            String edited = textArea.getValue().trim();
            // 简单验证：若用户输入非空，尝试解析为 Tag，避免非法 NBT
            if (!edited.isEmpty()) {
                try {
                    TagParser.parseTag(edited);
                } catch (Exception e) {
                    // 解析失败不保存，可提示（这里简化为不保存）
                    return;
                }
            }
            onSave.accept(edited.isEmpty() ? null : edited);
            if (minecraft != null) minecraft.setScreen(null);
        }).pos(x, y).size(95, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.cancel"), btn -> {
            onSave.accept(initialNbt); // 放弃修改
            if (minecraft != null) minecraft.setScreen(null);
        }).pos(x + 105, y).size(95, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.edit_full_nbt"),
                width / 2, 10, 0xFFFFFF);
    }
}