package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import com.sal_fish.visual_set_edit.network.C2SUpdatePresetPacket;
import com.sal_fish.visual_set_edit.network.VsePacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

public class PresetListScreen extends Screen {
    private List<Preset> presets;
    private Preset selectedPreset;
    private ScrollableSelectionList presetList;

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
                    String id = entry.getId().getPath();
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
        for (Preset p : presets) {
            String displayName = getPresetDisplayName(p);
            String fullDisplay = displayName + " §7(" + p.id + ")";
            ResourceLocation entryId = new ResourceLocation("vse", p.id);
            presetList.addEntry(new ScrollableSelectionList.Entry(
                    Component.literal(fullDisplay),
                    entryId,
                    null
            ));
        }
        if (selectedPreset != null) {
            for (ScrollableSelectionList.Entry entry : presetList.children()) {
                if (entry.getId().getPath().equals(selectedPreset.id)) {
                    presetList.setSelected(entry);
                    break;
                }
            }
        }
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