package com.sal_fish.visual_set_edit.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;

public class AttributeListScreen extends Screen {
    private final Screen parent;
    private final Consumer<ResourceLocation> callback;
    private ScrollableSelectionList list;
    private EditBox searchField;

    public AttributeListScreen(Screen parent, Consumer<ResourceLocation> callback) {
        super(Component.translatable("visual_set_edit.gui.select_attribute"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int listWidth = width - 40;
        int listHeight = height - 80;
        list = new ScrollableSelectionList(minecraft, listWidth, listHeight, 40, 18, entry -> {
            callback.accept(entry.getId());
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        });
        addWidget(list);

        searchField = new EditBox(font, 20, 20, listWidth, 20, Component.translatable("visual_set_edit.gui.search"));
        addRenderableWidget(searchField);
        searchField.setMaxLength(5201314);
        searchField.setResponder(this::updateList);
        updateList("");
    }

    private void updateList(String filter) {
        list.clearAllEntries();
        String lowerFilter = filter.toLowerCase();
        Language lang = Language.getInstance();

        ForgeRegistries.ATTRIBUTES.getValues().stream()
                .sorted(Comparator.comparing(attr -> Objects.requireNonNull(ForgeRegistries.ATTRIBUTES.getKey(attr)).toString()))
                .forEach(attr -> {
                    ResourceLocation rl = ForgeRegistries.ATTRIBUTES.getKey(attr);
                    String registeredName = null;
                    if (rl != null) {
                        registeredName = rl.toString();
                    }
                    String translatedName = Component.translatable(attr.getDescriptionId()).getString();

                    if (registeredName != null && (filter.isEmpty() || registeredName.toLowerCase().contains(lowerFilter) || translatedName.toLowerCase().contains(lowerFilter))) {
                        list.addEntry(new ScrollableSelectionList.Entry(
                                Component.literal(translatedName + " (" + registeredName + ")"),
                                rl, null
                        ));
                    }
                });
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        list.render(graphics, mouseX, mouseY, partial);
        searchField.render(graphics, mouseX, mouseY, partial);
        super.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (searchField != null && !searchField.isMouseOver(x, y)) {
            searchField.setFocused(false);
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }
}