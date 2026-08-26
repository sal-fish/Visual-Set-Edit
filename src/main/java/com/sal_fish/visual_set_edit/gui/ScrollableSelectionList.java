package com.sal_fish.visual_set_edit.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ScrollableSelectionList extends ObjectSelectionList<ScrollableSelectionList.Entry> {

    private final Consumer<Entry> onSelect;
    @Nullable
    private final Consumer<Entry> onRightClick;

    public ScrollableSelectionList(Minecraft minecraft, int width, int height, int y0, int itemHeight,
                                   Consumer<Entry> onSelect) {
        this(minecraft, width, height, y0, itemHeight, onSelect, null);
    }

    public ScrollableSelectionList(Minecraft minecraft, int width, int height, int y0, int itemHeight,
                                   Consumer<Entry> onSelect, @Nullable Consumer<Entry> onRightClick) {
        super(minecraft, width, height, y0, y0 + height, itemHeight);
        this.onSelect = onSelect;
        this.onRightClick = onRightClick;
    }

    public void clearAllEntries() {
        clearEntries();
    }

    @Override
    public int addEntry(@NotNull Entry entry) {
        return super.addEntry(entry);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY < this.y0 || mouseY > this.y1) {
            return false;
        }
        Entry entry = getEntryAtPosition(mouseX, mouseY);
        if (entry == null) return false;

        if (button == 1 && onRightClick != null) { // 右键
            onRightClick.accept(entry);
            return true;
        }
        if (button == 0 && onSelect != null) { // 左键
            onSelect.accept(entry);
            return true;
        }
        return false;
    }

    @Override
    protected int getScrollbarPosition() {
        return getRight() - 6;
    }

    @Override
    public int getRowWidth() {
        return getWidth() - 10;
    }

    public static class Entry extends ObjectSelectionList.Entry<Entry> {
        private final Component text;
        private final ResourceLocation id;
        @Nullable
        private final ItemStack icon;

        public Entry(Component text, ResourceLocation id, @Nullable ItemStack icon) {
            this.text = text;
            this.id = id;
            this.icon = icon;
        }

        public ResourceLocation getId() {
            return id;
        }

        @Override
        public @NotNull Component getNarration() {
            return text;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            Font font = Minecraft.getInstance().font;
            if (icon != null && !icon.isEmpty()) {
                graphics.renderItem(icon, left, top + (rowHeight - 16) / 2);
                graphics.drawString(font, text, left + 18, top + (rowHeight - 8) / 2, 0xFFFFFF);
            } else {
                graphics.drawString(font, text, left, top + (rowHeight - 8) / 2, 0xFFFFFF);
            }
        }
    }
}