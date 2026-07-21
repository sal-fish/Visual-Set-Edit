package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.network.VsePacketHandler;
import com.sal_fish.visual_set_edit.network.C2SUpdatePresetPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PresetEditScreen extends Screen {
    private final Preset preset;
    private final List<Preset> allPresets;
    private final Screen parent;
    private EditBox nameEdit;
    private int selectedPhase = -1;
    private boolean showTooltip;

    public PresetEditScreen(Preset preset, List<Preset> allPresets, Screen parent) {
        super(Component.translatable("visual_set_edit.gui.preset_edit.title", preset.fallbackName));
        this.preset = preset;
        this.allPresets = allPresets;
        this.parent = parent;
        this.showTooltip = preset.showTooltip;
    }

    @Override
    protected void init() {
        int x = width / 2 - 120;
        nameEdit = new EditBox(font, x, 35, 240, 20, Component.translatable("visual_set_edit.gui.name"));
        nameEdit.setMaxLength(5201314);
        nameEdit.setValue(preset.fallbackName != null ? preset.fallbackName : "");
        addRenderableWidget(nameEdit);

        // 添加阶段
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.add_phase"), b -> {
            preset.fallbackName = nameEdit.getValue();
            SetPhase phase = new SetPhase();
            phase.fallbackName = Component.translatable("visual_set_edit.gui.phase_added", preset.phases.size() + 1).getString();
            preset.phases.add(phase);
            selectedPhase = preset.phases.size() - 1;
        }).pos(x, 60).size(120, 20).build());

        // 编辑选中阶段
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.edit_selected"), b -> {
            if (selectedPhase >= 0 && selectedPhase < preset.phases.size()) {
                preset.fallbackName = nameEdit.getValue();
                assert minecraft != null;
                minecraft.setScreen(new PhaseEditScreen(preset, selectedPhase, this));
            }
        }).pos(x + 125, 60).size(115, 20).build());

        // 删除选中阶段
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.delete_selected"), b -> {
            preset.fallbackName = nameEdit.getValue();
            if (selectedPhase >= 0 && selectedPhase < preset.phases.size()) {
                preset.phases.remove(selectedPhase);
                if (selectedPhase >= preset.phases.size()) selectedPhase = preset.phases.size() - 1;
            }
        }).pos(x, 85).size(120, 20).build());

        // 返回按钮
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.back"), b -> {
            preset.fallbackName = nameEdit.getValue();
            preset.showTooltip = showTooltip;
            assert minecraft != null;
            minecraft.setScreen(parent);
        }).pos(x + 125, 85).size(115, 20).build());

        // 保存到服务器
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.save_to_server"), b -> {
            preset.fallbackName = nameEdit.getValue();
            preset.showTooltip = showTooltip;
            VsePacketHandler.INSTANCE.sendToServer(new C2SUpdatePresetPacket(allPresets));
        }).pos(x, 110).size(240, 20).build());

        // 工具提示开关
        addRenderableWidget(CycleButton.<Boolean>builder(b ->
                        Component.translatable("visual_set_edit.gui.show_tooltip.state",
                                b ? Component.translatable("visual_set_edit.gui.tooltip.on").getString()
                                        : Component.translatable("visual_set_edit.gui.tooltip.off").getString()))
                .withValues(true, false)
                .displayOnlyValue()
                .withInitialValue(showTooltip)
                .create(x, 135, 240, 20,
                        Component.translatable("visual_set_edit.gui.show_tooltip"),
                        (btn, val) -> showTooltip = val));

        // 编辑自定义提示
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.edit_tooltip"),
                b -> {
                    preset.fallbackName = nameEdit.getValue();
                    assert minecraft != null;
                    minecraft.setScreen(new TooltipEditScreen(preset, this));
                }).pos(x, 160).size(240, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.preset_edit.title", preset.fallbackName), width / 2, 10, 0xFFFFFF);

        int y = 190;
        for (int i = 0; i < preset.phases.size(); i++) {
            SetPhase phase = preset.phases.get(i);
            int color = i == selectedPhase ? 0xFFAA00 : 0xAAAAAA;
            graphics.drawString(font, phase.fallbackName + " (" + phase.requiredCount + ")", 10, y, color);
            y += 12;
            if (y > height - 30) break;
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        int yStart = 190;
        for (int i = 0; i < preset.phases.size(); i++) {
            if (x >= 10 && x <= 300 && y >= yStart + i * 12 && y < yStart + i * 12 + 10) {
                selectedPhase = i;
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public void onClose() {
        preset.fallbackName = nameEdit.getValue();
        preset.showTooltip = showTooltip;
        assert minecraft != null;
        minecraft.setScreen(parent);
    }
}