package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.config.CuriosItemMappingManager;
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
                    ResourceLocation id = entry.getId();
                    String itemId = id.toString();
                    if (minecraft != null) {
                        minecraft.setScreen(new CuriosItemRegisterScreen(this, itemId));
                    }
                });
        addWidget(itemList);

        List<String> registeredItems = new ArrayList<>(CuriosItemMappingManager.getMappings().keySet());
        registeredItems.sort(Comparator.comparing(id -> {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) {
                    return Component.translatable(item.getDescriptionId()).getString();
                }
            }
            return id;
        }));

        for (String itemId : registeredItems) {
            ResourceLocation rl = ResourceLocation.tryParse(itemId);
            if (rl == null) continue;
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null) continue;
            List<String> slots = CuriosItemMappingManager.getSlotsForItem(itemId);
            String slotList = String.join(", ", slots);
            Component display = Component.literal(
                    Component.translatable(item.getDescriptionId()).getString()
                            + "  §7→ " + slotList);
            itemList.addEntry(new ScrollableSelectionList.Entry(display, rl, new ItemStack(item)));
        }

        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.back"),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(parent);
                }).pos(width / 2 - 100, height - 40).size(200, 20).build());
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
}