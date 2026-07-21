package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class SlotSelectionScreen extends Screen {
    private final Screen parent;
    private final String currentSlot;
    private final Consumer<String> callback;
    private final boolean onlyCurios;                          // 是否仅显示 Curios 槽位
    private ScrollableSelectionList list;
    private EditBox searchField;

    // 记录条目对应的真实槽位字符串
    private final Map<ScrollableSelectionList.Entry, String> entrySlotMap = new HashMap<>();

    private static final String[] VANILLA_SLOTS = {"HEAD", "CHEST", "LEGS", "FEET", "MAINHAND", "OFFHAND"};

    // 原有构造函数，保持兼容，默认显示所有槽位
    public SlotSelectionScreen(Screen parent, String currentSlot, Consumer<String> callback) {
        this(parent, currentSlot, callback, false);
    }

    // 新构造函数，可指定是否仅显示 Curios 槽位
    public SlotSelectionScreen(Screen parent, String currentSlot, Consumer<String> callback, boolean onlyCurios) {
        super(Component.translatable("visual_set_edit.gui.select_slot"));
        this.parent = parent;
        this.currentSlot = currentSlot;
        this.callback = callback;
        this.onlyCurios = onlyCurios;
    }

    @Override
    protected void init() {
        int listWidth = width - 20;
        int listHeight = height - 60;
        list = new ScrollableSelectionList(minecraft, listWidth, listHeight, 30, 16, entry -> {
            String slot = entrySlotMap.get(entry);
            if (slot != null) {
                callback.accept(slot);
                assert minecraft != null;
                minecraft.setScreen(parent);
            }
        });
        addWidget(list);

        searchField = new EditBox(font, 10, 10, listWidth, 16, Component.translatable("visual_set_edit.gui.search"));
        addRenderableWidget(searchField);
        searchField.setMaxLength(5201314);
        searchField.setResponder(this::updateList);
        updateList("");
    }

    private void updateList(String filter) {
        list.clearAllEntries();
        entrySlotMap.clear();
        String lowerFilter = filter.toLowerCase();

        List<String> allSlots = new ArrayList<>();

        // 根据 onlyCurios 决定是否加入原版槽位
        if (!onlyCurios) {
            for (String s : VANILLA_SLOTS) {
                allSlots.add(s);
            }
        }

        if (IntegrationManager.isCuriosLoaded()) {
            for (String slotId : IntegrationManager.getCurios().getExtraSlots()) {
                allSlots.add("curios:" + slotId);
            }
        }

        allSlots.sort(Comparator.naturalOrder());

        for (String slot : allSlots) {
            if (!filter.isEmpty() && !slot.toLowerCase().contains(lowerFilter)) continue;
            Component slotName;
            if (slot.startsWith("curios:")) {
                slotName = Component.translatable("curios.identifier." + slot.substring(7));
            } else {
                slotName = Component.translatable("visual_set_edit.slot." + slot.toLowerCase());
            }
            String slotStr = slotName.getString();
            String displayText = slot.equals(currentSlot) ? "> " + slotStr + " <" : slotStr;
            ResourceLocation entryId = ResourceLocation.tryParse("vse:" + slot.toLowerCase().replace(':', '_'));
            if (entryId == null) {
                entryId = new ResourceLocation("vse", "unknown");
            }
            ScrollableSelectionList.Entry entry = new ScrollableSelectionList.Entry(
                    Component.literal(displayText),
                    entryId,
                    null
            );
            list.addEntry(entry);
            entrySlotMap.put(entry, slot);
        }
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
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
}