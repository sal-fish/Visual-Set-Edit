package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.util.SearchUtil;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Consumer;

public class SchoolListScreen extends Screen {
    private static final ResourceLocation ALL = ResourceLocation.fromNamespaceAndPath("visual_set_edit", "all");

    private final Screen parent;
    private final Consumer<ResourceLocation> callback;
    private final boolean allOption;
    private ScrollableSelectionList list;
    private EditBox searchField;

    public SchoolListScreen(Screen parent, Consumer<ResourceLocation> callback) {
        this(parent, callback, false);
    }

    public SchoolListScreen(Screen parent, Consumer<ResourceLocation> callback, boolean allOption) {
        super(Component.translatable("visual_set_edit.gui.select_school"));
        this.parent = parent;
        this.callback = callback;
        this.allOption = allOption;
    }

    @Override
    protected void init() {
        int listWidth = width - 40;
        int listHeight = height - 80;
        list = new ScrollableSelectionList(minecraft, listWidth, listHeight, 40, 18, entry -> {
            callback.accept(ALL.equals(entry.getId()) ? null : entry.getId());
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
        if (allOption) {
            list.addEntry(new ScrollableSelectionList.Entry(
                    Component.translatable("visual_set_edit.gui.select_school.all"),
                    ALL,
                    null
            ));
        }
        IForgeRegistry<SchoolType> registry = SchoolRegistry.REGISTRY.get();
        if (registry == null) return;

        registry.getValues().stream()
                .sorted(Comparator.comparing(s -> s.getDisplayName().getString()))
                .filter(school -> {
                    if (filter.isEmpty()) return true;
                    ResourceLocation key = registry.getKey(school);
                    String path = key != null ? key.getPath() : "";
                    return SearchUtil.contains(school.getDisplayName().getString(), filter)
                            || SearchUtil.contains(path, filter)
                            || SearchUtil.contains(String.valueOf(key), filter);
                })
                .forEach(school -> {
                    ResourceLocation id = registry.getKey(school);
                    if (id == null) return;
                    String fullText = school.getDisplayName().getString() + " (" + id + ")";
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
