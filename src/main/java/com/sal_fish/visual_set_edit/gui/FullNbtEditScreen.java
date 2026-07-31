package com.sal_fish.visual_set_edit.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class FullNbtEditScreen extends Screen {
    private final Screen parent;
    private final String initialNbt;
    private final Consumer<String> onSave;
    private MultiLineEditBox textArea;

    public FullNbtEditScreen(Screen parent, String initialNbt, Consumer<String> onSave) {
        super(Component.translatable("visual_set_edit.gui.edit_full_nbt"));
        this.parent = parent;
        this.initialNbt = initialNbt;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        int margin = 10;
        int titleY = 5;
        int titleHeight = font.lineHeight + 5;
        int buttonAreaHeight = 30;
        int y = titleY + titleHeight;
        int width = this.width - margin * 2;
        int height = this.height - y - buttonAreaHeight - margin;

        textArea = new MultiLineEditBox(font, margin, y, width, height,
                Component.literal("NBT"),
                Component.translatable("visual_set_edit.gui.edit_full_nbt"));
        textArea.setValue(initialNbt != null ? initialNbt : "");
        addRenderableWidget(textArea);
        setInitialFocus(textArea);

        int buttonY = this.height - 25;

        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.save"), btn -> {
            String edited = textArea.getValue().trim();
            if (!edited.isEmpty()) {
                try {
                    TagParser.parseTag(edited);
                } catch (Exception e) {
                    return;
                }
            }
            onSave.accept(edited.isEmpty() ? null : edited);
            if (minecraft != null) minecraft.setScreen(parent);
        }).pos(this.width / 2 - 105, buttonY).size(100, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.cancel"), btn -> {
            onSave.accept(initialNbt);
            if (minecraft != null) minecraft.setScreen(parent);
        }).pos(this.width / 2 + 5, buttonY).size(100, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.edit_full_nbt"),
                width / 2, 5, 0xFFFFFF);
    }
}