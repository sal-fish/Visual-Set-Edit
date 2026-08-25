package com.sal_fish.visual_set_edit.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;

public class BlockTagListScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> callback;
    private ScrollableSelectionList list;
    private EditBox searchField;

    public BlockTagListScreen(Screen parent, Consumer<String> callback) {
        super(Component.translatable("visual_set_edit.gui.select_block_tag"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int listWidth = width - 20;
        int listHeight = height - 60;
        list = new ScrollableSelectionList(minecraft, listWidth, listHeight, 30, 16, entry -> {
            callback.accept(entry.getId().toString());
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        });
        addWidget(list);

        searchField = new EditBox(font, 10, 10, listWidth, 16, Component.translatable("visual_set_edit.gui.search"));
        searchField.setMaxLength(5201314);
        addRenderableWidget(searchField);
        searchField.setResponder(this::updateList);
        updateList("");
    }

    private void updateList(String filter) {
        list.clearAllEntries();
        String lowerFilter = filter.toLowerCase();

        Objects.requireNonNull(ForgeRegistries.BLOCKS.tags()).getTagNames()
                .sorted(Comparator.comparing(tagKey -> tagKey.location().toString()))
                .forEach(tagKey -> {
                    String tagId = tagKey.location().toString();
                    if (!filter.isEmpty() && !tagId.toLowerCase().contains(lowerFilter)) return;
                    list.addEntry(new ScrollableSelectionList.Entry(
                            Component.literal(tagId),
                            tagKey.location(),
                            null
                    ));
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