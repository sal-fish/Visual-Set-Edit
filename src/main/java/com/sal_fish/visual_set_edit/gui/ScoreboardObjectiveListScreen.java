package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.config.ScoreboardObjectiveManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class ScoreboardObjectiveListScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> callback;
    private ScrollableSelectionList list;
    private EditBox searchField;

    public ScoreboardObjectiveListScreen(Screen parent, Consumer<String> callback) {
        super(Component.translatable("visual_set_edit.gui.select_scoreboard_objective"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int listWidth = width - 20;
        int listHeight = height - 60;
        list = new ScrollableSelectionList(minecraft, listWidth, listHeight, 30, 16, entry -> {
            callback.accept(entry.getId().getPath());
            if (minecraft != null) minecraft.setScreen(parent);
        });
        addWidget(list);

        searchField = new EditBox(font, 10, 10, listWidth, 16,
                Component.translatable("visual_set_edit.gui.search"));
        searchField.setMaxLength(5201314);
        addRenderableWidget(searchField);
        searchField.setResponder(this::updateList);
        updateList("");
    }

    private void updateList(String filter) {
        list.clearAllEntries();
        String lowerFilter = filter.toLowerCase();
        List<String> objectives = ScoreboardObjectiveManager.getObjectives();
        for (String name : objectives) {
            if (!filter.isEmpty() && !name.toLowerCase().contains(lowerFilter)) continue;
            list.addEntry(new ScrollableSelectionList.Entry(
                    Component.literal(name),
                    new ResourceLocation("vse", name),
                    null
            ));
        }
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