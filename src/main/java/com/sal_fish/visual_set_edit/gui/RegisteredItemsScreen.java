package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.config.CuriosItemMappingManager;
import com.sal_fish.visual_set_edit.config.CuriosItemMappingManager.RegisteredEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RegisteredItemsScreen extends Screen {
    private final Screen parent;
    private ScrollableSelectionList itemList;
    private final Map<String, Item> itemCache = new HashMap<>();

    public RegisteredItemsScreen(Screen parent) {
        super(Component.translatable("visual_set_edit.gui.curios_registered.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        int listWidth = width - 40;
        int listHeight = height - 80;
        int yStart = 30;

        itemList = new ScrollableSelectionList(minecraft, listWidth, listHeight, yStart, 20,
                entry -> {
                    if (entry instanceof RegisteredEntryItem regEntry) {
                        if (minecraft != null) {
                            minecraft.setScreen(new CuriosItemRegisterScreen(this, regEntry.itemId, regEntry.nbt));
                        }
                    }
                });
        addWidget(itemList);

        List<RegisteredEntryItem> allItems = new ArrayList<>();
        Map<String, List<RegisteredEntry>> registry = CuriosItemMappingManager.getRegistry();
        if (registry != null) {
            for (Map.Entry<String, List<RegisteredEntry>> regEntry : registry.entrySet()) {
                String itemId = regEntry.getKey();
                Item item = getItem(itemId);
                if (item == null) continue;
                List<RegisteredEntry> entries = regEntry.getValue();
                for (int i = 0; i < entries.size(); i++) {
                    RegisteredEntry entry = entries.get(i);
                    allItems.add(new RegisteredEntryItem(itemId, i, entry, item));
                }
            }
        }

        allItems.sort(Comparator
                .<RegisteredEntryItem, String>comparing(
                        e -> Component.translatable(e.item.getDescriptionId()).getString())
                .thenComparing(e -> e.nbt == null ? "" : e.nbt));

        for (RegisteredEntryItem entryItem : allItems) {
            itemList.addEntry(entryItem);
        }

        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.back"),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(parent);
                }).pos(width / 2 - 100, height - 40).size(200, 20).build());
    }

    private Item getItem(String itemId) {
        return itemCache.computeIfAbsent(itemId, id -> {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            return rl != null ? ForgeRegistries.ITEMS.getValue(rl) : null;
        });
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        if (itemList != null) {
            itemList.render(graphics, mouseX, mouseY, partial);
        }
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font,
                Component.translatable("visual_set_edit.gui.curios_registered.title"),
                width / 2, 10, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (itemList != null && itemList.isMouseOver(mouseX, mouseY)) {
            if (itemList.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (itemList != null && itemList.isMouseOver(mouseX, mouseY)) {
            return itemList.mouseScrolled(mouseX, mouseY, scrollDelta);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    private class RegisteredEntryItem extends ScrollableSelectionList.Entry {
        final String itemId;
        final int entryIndex;
        final String nbt;
        final RegisteredEntry entry;
        final Item item;
        private final Component displayText;

        RegisteredEntryItem(String itemId, int index, RegisteredEntry entry, Item item) {
            super(buildDisplayText(item, entry),
                    ResourceLocation.tryParse(itemId),
                    new ItemStack(item));
            this.itemId = itemId;
            this.entryIndex = index;
            this.entry = entry;
            this.nbt = entry.nbt;
            this.item = item;
            this.displayText = buildDisplayText(item, entry);
        }

        private static Component buildDisplayText(Item item, RegisteredEntry entry) {
            String name = Component.translatable(item.getDescriptionId()).getString();
            String nbtPart = entry.getNbtSummary() != null ? " §8[" + entry.getNbtSummary() + "]" : "";
            String slotPart = String.join(", ", entry.slots);
            return Component.literal(name + nbtPart + " §7→ " + slotPart);
        }

        @Override
        public @NotNull Component getNarration() {
            return displayText;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (hovered) {
                graphics.fill(left, top, left + rowWidth, top + rowHeight, 0x40FFFFFF);
            }
            ItemStack icon = new ItemStack(item);
            graphics.renderFakeItem(icon, left + 2, top + (rowHeight - 16) / 2);
            graphics.drawString(font, displayText, left + 20, top + (rowHeight - 8) / 2, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }
    }
}