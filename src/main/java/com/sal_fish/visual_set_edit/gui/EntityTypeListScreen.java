package com.sal_fish.visual_set_edit.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Consumer;

public class EntityTypeListScreen extends Screen {
    private final Screen parent;
    private final Consumer<ResourceLocation> callback;
    private ScrollableSelectionList list;
    private EditBox searchField;

    public EntityTypeListScreen(Screen parent, Consumer<ResourceLocation> callback) {
        super(Component.translatable("visual_set_edit.gui.select_entity_type"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int listWidth = width - 20;
        int listHeight = height - 60;
        list = new ScrollableSelectionList(minecraft, listWidth, listHeight, 30, 16, entry -> {
            callback.accept(entry.getId());
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

        ForgeRegistries.ENTITY_TYPES.getKeys().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(rl -> {
                    EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(rl);
                    if (entityType == null) return;
                    if (!filter.isEmpty()) {
                        String registeredName = rl.toString();
                        String translatedName = Component.translatable(entityType.getDescriptionId()).getString();
                        if (!registeredName.toLowerCase().contains(lowerFilter)
                                && !translatedName.toLowerCase().contains(lowerFilter)) {
                            return;
                        }
                    }
                    list.addEntry(new ScrollableSelectionList.Entry(
                            Component.literal(Component.translatable(entityType.getDescriptionId()).getString()
                                    + " (" + rl + ")"),
                            rl,
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