package com.sal_fish.visual_set_edit.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

public class BiomeListScreen extends Screen {
    private final Screen parent;
    private final Consumer<ResourceLocation> callback;
    private ScrollableSelectionList list;
    private EditBox searchField;
    private boolean requested;

    public BiomeListScreen(Screen parent, Consumer<ResourceLocation> callback) {
        super(Component.translatable("visual_set_edit.gui.select_biome"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int listWidth = width - 20;
        int listHeight = height - 60;
        list = new ScrollableSelectionList(minecraft, listWidth, listHeight, 30, 16, entry -> {
            callback.accept(entry.getId());
            assert minecraft != null;
            minecraft.setScreen(parent);
        });
        addWidget(list);

        searchField = new EditBox(font, 10, 10, listWidth, 16, Component.translatable("visual_set_edit.gui.search"));
        addRenderableWidget(searchField);
        searchField.setResponder(this::updateList);
        updateList("");
    }

    private void updateList(String filter) {
        list.clearAllEntries();
        String lowerFilter = filter.toLowerCase();

        List<ResourceLocation> cached = RegistryListHelper.getRegistryIds("biome");
        if (cached.isEmpty() && !requested) {
            RegistryListHelper.requestIfNeeded("biome");
            requested = true;
        }

        if (cached.isEmpty()) {
            list.addEntry(new ScrollableSelectionList.Entry(
                    Component.translatable("visual_set_edit.gui.loading"),
                    new ResourceLocation("vse", "loading"), null));
            return;
        }

        cached.stream()
                .sorted(ResourceLocation::compareNamespaced)
                .forEach(loc -> {
                    if (!filter.isEmpty() && !loc.toString().toLowerCase().contains(lowerFilter)) return;

                    String translationKey = "biome." + loc.getNamespace() + "." + loc.getPath();
                    Component displayName = Component.translatable(translationKey);
                    if (displayName.getString().equals(translationKey)) {
                        displayName = Component.literal(loc.toString());
                    }
                    list.addEntry(new ScrollableSelectionList.Entry(displayName, loc, null));
                });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (requested) {
            List<ResourceLocation> cached = RegistryListHelper.getRegistryIds("biome");
            if (!cached.isEmpty()) {
                requested = false;
                updateList(searchField.getValue());
            }
        }
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