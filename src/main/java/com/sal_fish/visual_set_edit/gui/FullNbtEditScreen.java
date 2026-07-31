package com.sal_fish.visual_set_edit.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class FullNbtEditScreen extends Screen {
    private final Screen parent;
    private final String initialNbt;
    private final Consumer<String> onSave;
    private EditBox textArea;

    public FullNbtEditScreen(Screen parent, String initialNbt, Consumer<String> onSave) {
        super(Component.translatable("visual_set_edit.gui.edit_full_nbt"));
        this.parent = parent;
        this.initialNbt = initialNbt;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        int width = 200;
        int height = 20;
        int x = (this.width - width) / 2;
        int y = 30;

        textArea = new EditBox(font, x, y, width, height, Component.literal("NBT"));
        textArea.setMaxLength(65535);
        textArea.setValue(initialNbt != null ? initialNbt : "");
        addRenderableWidget(textArea);
        textArea.setFocused(true);

        y += height + 10;

        // 保存按钮
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.save"), btn -> {
            String edited = textArea.getValue().trim();
            if (!edited.isEmpty()) {
                try {
                    TagParser.parseTag(edited);  // 验证合法性
                } catch (Exception e) {
                    return; // 格式错误不保存
                }
            }
            onSave.accept(edited.isEmpty() ? null : edited);
            if (minecraft != null) minecraft.setScreen(parent);
        }).pos(x, y).size(95, 20).build());

        // 取消按钮
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.cancel"), btn -> {
            onSave.accept(initialNbt); // 放弃修改
            if (minecraft != null) minecraft.setScreen(parent);
        }).pos(x + 105, y).size(95, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.edit_full_nbt"),
                width / 2, 10, 0xFFFFFF);
    }
}