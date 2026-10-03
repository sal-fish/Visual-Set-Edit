package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.api.SlotProvider;
import com.sal_fish.visual_set_edit.data.SlotProviderRegistry;
import com.sal_fish.visual_set_edit.util.SearchUtil;
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
    private final boolean onlyCurios;
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

        List<String> allSlots = new ArrayList<>();

        if (!onlyCurios) {
            allSlots.addAll(Arrays.asList(VANILLA_SLOTS));
        }
        allSlots.addAll(SlotProviderRegistry.allSlotKeys(minecraft != null ? minecraft.player : null));

        allSlots.sort(Comparator.naturalOrder());

        for (String slot : allSlots) {
            Component slotName;
            if (SlotProviderRegistry.isAnyKey(slot)) {
                slotName = Component.translatable("visual_set_edit.slot.any");
            } else if (SlotProviderRegistry.isProviderKey(slot)) {
                SlotProvider provider = SlotProviderRegistry.get(SlotProviderRegistry.providerIdOf(slot));
                String displayKey = null;
                if (provider != null) {
                    displayKey = provider.slotDisplayName(SlotProviderRegistry.slotIdOf(slot));
                }
                if (displayKey != null) {
                    slotName = Component.translatable(displayKey);
                } else if ("curios".equals(provider.id())) {
                    slotName = Component.translatable("curios.identifier." + SlotProviderRegistry.slotIdOf(slot));
                } else {
                    slotName = Component.literal(slot);
                }
            } else {
                slotName = Component.translatable("visual_set_edit.slot." + slot.toLowerCase());
            }
            // 不同来源的槽位可能有相同翻译名，补上槽位键区分
            String slotStr = slotName.getString();
            if (!slotStr.equals(slot) && SlotProviderRegistry.isProviderKey(slot)
                    && !SlotProviderRegistry.isAnyKey(slot)) {
                slotStr = slotStr + " (" + slot + ")";
            }
            // 槽位键与显示名任一命中即可
            if (!filter.isEmpty() && !SearchUtil.contains(slot, filter)
                    && !SearchUtil.contains(slotStr, filter)) continue;
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