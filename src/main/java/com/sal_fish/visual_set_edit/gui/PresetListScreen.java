package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.SlotCondition;
import com.sal_fish.visual_set_edit.data.condition.Condition;
import com.sal_fish.visual_set_edit.data.effect.EffectEntry;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import com.sal_fish.visual_set_edit.network.C2SUpdatePresetPacket;
import com.sal_fish.visual_set_edit.network.VsePacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PresetListScreen extends Screen {
    private static final ResourceLocation ENTRY_ID = new ResourceLocation("vse", "preset");

    private List<Preset> presets;
    private Preset selectedPreset;
    private ScrollableSelectionList presetList;
    private String query = "";

    public PresetListScreen() {
        super(Component.translatable("visual_set_edit.gui.title"));
    }

    @Override
    protected void init() {
        presets = new ArrayList<>(PresetManager.clientPresets);
        selectedPreset = null;

        int buttonY = 35;
        int spacing = 25;
        int buttonWidth = 200;
        int centerX = width / 2 - buttonWidth / 2;

        // 搜索框与第一个按钮同一行，位于其左侧
        int searchWidth = Math.max(50, centerX - 20);
        EditBox searchField = new EditBox(font, 10, buttonY, searchWidth, 20,
                Component.translatable("visual_set_edit.gui.search"));
        searchField.setMaxLength(100);
        searchField.setValue(query);
        searchField.setResponder(s -> {
            query = s;
            updatePresetList();
        });
        addRenderableWidget(searchField);

        // 按钮区域（固定上方）
        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.new_preset"),
                b -> {
                    Preset p = new Preset();
                    p.id = "preset_" + System.currentTimeMillis();
                    p.fallbackName = Component.translatable("visual_set_edit.gui.unnamed").getString();
                    presets.add(p);
                    updatePresetList();
                }
        ).pos(centerX, buttonY).size(buttonWidth, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.copy_preset"),
                b -> {
                    if (selectedPreset != null) {
                        try {
                            // 通过 Gson 深拷贝，确保所有嵌套数据独立
                            String json = PresetManager.GSON.toJson(selectedPreset);
                            Preset copy = PresetManager.GSON.fromJson(json, Preset.class);
                            copy.resetAllUniqueIds();
                            copy.id = "preset_" + System.currentTimeMillis(); // 新 ID
                            copy.fallbackName = copy.fallbackName + Component.translatable("visual_set_edit.gui.copy_suffix").getString();
                            presets.add(copy);
                            updatePresetList();
                        } catch (Exception ex) {
                            // 复制失败时静默处理（可增加提示）
                        }
                    }
                }
        ).pos(centerX, buttonY + spacing).size(buttonWidth, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.save_to_server"),
                b -> VsePacketHandler.INSTANCE.sendToServer(new C2SUpdatePresetPacket(presets))
        ).pos(centerX, buttonY + spacing * 2).size(buttonWidth, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.edit_selected"),
                b -> {
                    if (selectedPreset != null) {
                        assert minecraft != null;
                        minecraft.setScreen(new PresetEditScreen(selectedPreset, presets, this));
                    }
                }
        ).pos(centerX, buttonY + spacing * 3).size(buttonWidth, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.delete_selected"),
                b -> {
                    if (selectedPreset != null) {
                        presets.remove(selectedPreset);
                        selectedPreset = null;
                        updatePresetList();
                    }
                }
        ).pos(centerX, buttonY + spacing * 4).size(buttonWidth, 20).build());

        // Curios 物品注册按钮（仅 Curios 加载时显示）
        int nextButtonIndex = 5; // 下一个按钮的倍数
        if (IntegrationManager.isCuriosLoaded()) {
            addRenderableWidget(Button.builder(
                    Component.translatable("visual_set_edit.gui.curios_register.button"),
                    b -> {
                        assert minecraft != null;
                        minecraft.setScreen(new CuriosItemRegisterScreen(this));
                    }
            ).pos(centerX, buttonY + spacing * nextButtonIndex).size(buttonWidth, 20).build());
            nextButtonIndex++;
        }

        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.scoreboard_register.button"),
                b -> {
                    assert minecraft != null;
                    minecraft.setScreen(new ScoreboardRegisterScreen(this));
                }
        ).pos(centerX, buttonY + spacing * nextButtonIndex).size(buttonWidth, 20).build());
        nextButtonIndex++;

        // 可滚动列表区域
        int listTop = buttonY + spacing * nextButtonIndex + 5;
        int listBottom = height - 10;
        presetList = new ScrollableSelectionList(
                minecraft, width, listBottom - listTop, listTop, 20,
                entry -> {
                    String id = entry.getRawId();
                    selectedPreset = presets.stream()
                            .filter(p -> p.id.equals(id))
                            .findFirst().orElse(null);
                    presetList.setSelected(entry);
                }
        );
        addWidget(presetList);
        updatePresetList();
    }

    private void updatePresetList() {
        if (presetList == null) return;
        presetList.clearAllEntries();
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);

        for (Preset p : presets) {
            if (!matchesQuery(p, q)) continue;
            String displayName = getPresetDisplayName(p);
            String fullDisplay = displayName + " §7(" + p.id + ")";
            presetList.addEntry(new ScrollableSelectionList.Entry(
                    Component.literal(fullDisplay),
                    ENTRY_ID,
                    p.id,
                    null
            ));
        }

        if (selectedPreset != null) {
            boolean visible = false;
            for (ScrollableSelectionList.Entry entry : presetList.children()) {
                if (selectedPreset.id.equals(entry.getRawId())) {
                    presetList.setSelected(entry);
                    visible = true;
                    break;
                }
            }
            // 选中项被搜索条件滤掉：同时清除列表选中与屏幕选中，避免误删不可见预设
            if (!visible) {
                presetList.setSelected(null);
                selectedPreset = null;
            }
        }
    }

    private boolean matchesQuery(Preset p, String q) {
        if (q.isEmpty()) return true;
        if (contains(getPresetDisplayName(p), q)) return true;
        if (contains(p.id, q)) return true;
        if (matchesAny(p.customTooltipLines, q)) return true;
        if (matchesAny(p.backgroundStoryLines, q)) return true;
        if (p.phases == null) return false;

        for (SetPhase phase : p.phases) {
            if (phase == null) continue;
            if (contains(phaseName(phase), q)) return true;
            if (contains(phase.fallbackName, q)) return true;

            if (phase.slotConditions != null) {
                for (SlotCondition sc : phase.slotConditions) {
                    if (sc == null) continue;
                    if (matchesItemRef(sc.itemId, q)) return true;
                    if (matchesTagRef(sc.tagId, q)) return true;
                    if (contains(sc.customDisplayText, q)) return true;
                }
            }
            if (phase.effects != null) {
                for (EffectEntry e : phase.effects) {
                    if (e == null) continue;
                    if (contains(e.type, q)) return true;
                    if (contains(e.customDisplayText, q)) return true;
                }
            }
            if (phase.additionalConditions != null) {
                for (Condition c : phase.additionalConditions) {
                    if (c == null) continue;
                    if (contains(c.type, q)) return true;
                    if (contains(c.customDisplayText, q)) return true;
                }
            }
        }
        return false;
    }

    private static boolean contains(String text, String q) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(q);
    }

    private static boolean matchesAny(List<String> lines, String q) {
        if (lines == null) return false;
        for (String s : lines) {
            if (contains(s, q)) return true;
        }
        return false;
    }

    //物品 id 同时匹配其本地化名称，便于直接搜"钻石"这类显示名
    private static boolean matchesItemRef(String itemId, String q) {
        if (contains(itemId, q)) return true;
        ResourceLocation rl = itemId == null ? null : ResourceLocation.tryParse(itemId);
        if (rl == null) return false;
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null) return false;
        return contains(Component.translatable(item.getDescriptionId()).getString(), q);
    }

    //标签 id 同时匹配其包含物品的本地化名称，便于直接搜"木板"这类显示名
    private static boolean matchesTagRef(String tagId, String q) {
        if (contains(tagId, q)) return true;
        ResourceLocation rl = tagId == null ? null : ResourceLocation.tryParse(tagId);
        if (rl == null) return false;
        ITagManager<Item> tagManager = ForgeRegistries.ITEMS.tags();
        if (tagManager == null) return false;
        ITag<Item> tag = tagManager.getTag(TagKey.create(Registries.ITEM, rl));
        if (tag == null) return false;
        for (Item item : tag) {
            if (item != null && contains(Component.translatable(item.getDescriptionId()).getString(), q)) return true;
        }
        return false;
    }

    private static String phaseName(SetPhase phase) {
        if (phase.translationKey != null && !phase.translationKey.isEmpty()) {
            return Component.translatable(phase.translationKey, phase.fallbackName).getString();
        }
        return phase.fallbackName != null ? phase.fallbackName : "";
    }

    private String getPresetDisplayName(Preset p) {
        if (p.translationKey != null && !p.translationKey.isEmpty()) {
            return Component.translatable(p.translationKey, p.fallbackName).getString();
        }
        return p.fallbackName != null ? p.fallbackName : Component.translatable("visual_set_edit.gui.unnamed").getString();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0x80000000);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.title"), width / 2, 10, 0xFFFFFF);
        if (presetList != null) {
            presetList.render(graphics, mouseX, mouseY, partial);
        }
        super.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (presetList != null && presetList.isMouseOver(mouseX, mouseY)) {
            return presetList.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
