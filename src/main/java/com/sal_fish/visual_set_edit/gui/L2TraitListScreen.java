package com.sal_fish.visual_set_edit.gui;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Consumer;

public class L2TraitListScreen extends Screen {
    private final Screen parent;
    private final Consumer<ResourceLocation> callback;
    private ScrollableSelectionList list;
    private EditBox searchField;

    public L2TraitListScreen(Screen parent, Consumer<ResourceLocation> callback) {
        super(Component.translatable("visual_set_edit.gui.select_l2trait"));
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int listWidth = width - 40, listHeight = height - 80;
        list = new ScrollableSelectionList(minecraft, listWidth, listHeight, 40, 18, entry -> {
            callback.accept(entry.getId());
            if (minecraft != null) minecraft.setScreen(parent);
        });
        addWidget(list);
        searchField = new EditBox(font, 20, 20, listWidth, 20, Component.translatable("visual_set_edit.gui.search"));
        searchField.setMaxLength(100);
        addRenderableWidget(searchField);
        searchField.setResponder(this::updateList);
        updateList("");
    }

    private void updateList(String filter) {
        list.clearAllEntries();
        IForgeRegistry<MobTrait> registry = LHTraits.TRAITS.get();
        registry.getValues().stream()
                .sorted(Comparator.comparing(t -> t.getDesc().getString()))
                .filter(trait -> filter.isEmpty() || trait.getDesc().getString().toLowerCase().contains(filter.toLowerCase()))
                .forEach(trait -> {
                    ResourceLocation id = registry.getKey(trait);
                    if (id != null) {
                        list.addEntry(new ScrollableSelectionList.Entry(
                                trait.getDesc().copy(), id, null));
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
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}