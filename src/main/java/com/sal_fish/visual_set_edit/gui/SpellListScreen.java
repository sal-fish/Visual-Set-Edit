package com.sal_fish.visual_set_edit.gui;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Consumer;

public class SpellListScreen extends Screen {
    private final Screen parent;
    private final Consumer<ResourceLocation> callback;
    private ScrollableSelectionList list;
    private EditBox searchField;

    public SpellListScreen(Screen parent, Consumer<ResourceLocation> callback) {
        super(Component.translatable("visual_set_edit.gui.select_spell"));
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

        searchField = new EditBox(font, 20, 20, listWidth, 20,
                Component.translatable("visual_set_edit.gui.search"));
        addRenderableWidget(searchField);
        searchField.setMaxLength(5201314);
        searchField.setResponder(this::updateList);
        updateList("");
    }

    private void updateList(String filter) {
        list.clearAllEntries();
        String lowerFilter = filter.toLowerCase();

        IForgeRegistry<AbstractSpell> spellRegistry = SpellRegistry.REGISTRY.get();
        if (spellRegistry == null) return;

        spellRegistry.getValues().stream()
                .sorted(Comparator.comparing(AbstractSpell::getSpellName))
                .filter(spell -> {
                    if (filter.isEmpty()) return true;
                    String name = Component.translatable(spell.getComponentId()).getString().toLowerCase();
                    return name.contains(lowerFilter) || spell.getSpellName().toLowerCase().contains(lowerFilter);
                })
                .forEach(spell -> {
                    ResourceLocation id = spellRegistry.getKey(spell);
                    String displayName = Component.translatable(spell.getComponentId()).getString();
                    // 显示等级范围
                    int min = spell.getMinLevel();
                    int max = spell.getMaxLevel();
                    String levelInfo = (min == max) ? String.valueOf(min) : min + "~" + max;
                    String fullText = displayName + " (" + levelInfo + ") (" + id + ")";
                    list.addEntry(new ScrollableSelectionList.Entry(
                            Component.literal(fullText),
                            id,
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
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }
}