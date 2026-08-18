package com.sal_fish.visual_set_edit.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class TextInputScreen extends Screen {
    private final Screen parent;
    private final String initialText;
    private final Consumer<String> onSave;
    private EditBox editBox;

    public TextInputScreen(Screen parent, String initialText, Consumer<String> onSave) {
        super(Component.translatable("visual_set_edit.gui.edit_text"));
        this.parent = parent;
        this.initialText = initialText;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        int width = this.width / 2 - 100;
        editBox = new EditBox(font, width, 40, 200, 20, Component.literal(""));
        editBox.setMaxLength(256);
        editBox.setValue(initialText);
        addRenderableWidget(editBox);
        setInitialFocus(editBox);

        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.save"), btn -> {
            onSave.accept(editBox.getValue());
            if (minecraft != null) minecraft.setScreen(parent);
        }).pos(width, 70).size(95, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.cancel"), btn -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }).pos(width + 105, 70).size(95, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.edit_text"),
                this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
