package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.condition.Condition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ConditionListScreen extends Screen {
    private final SetPhase phase;
    private final Preset preset;
    private final Screen parent;

    public ConditionListScreen(SetPhase phase, Preset preset, Screen parent) {
        super(Component.translatable("visual_set_edit.gui.conditions_title"));
        this.phase = phase;
        this.preset = preset;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = 40;

        // 添加条件按钮
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.add_condition"), b -> {
            assert minecraft != null;
            minecraft.setScreen(new ConditionEditScreen(cond -> {
                phase.additionalConditions.add(cond);
                minecraft.setScreen(this);
            }, this));
        }).pos(x, y).size(200, 20).build());
        y += 25;

        // 从其他阶段导入条件按钮（仅当存在其他阶段时显示）
        if (preset != null && preset.phases.size() > 1) {
            addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.import_conditions"), b -> {
                assert minecraft != null;
                List<SetPhase> otherPhases = new ArrayList<>(preset.phases);
                otherPhases.remove(phase);
                if (!otherPhases.isEmpty()) {
                    minecraft.setScreen(new PhaseSelectScreen(this, otherPhases, selected -> {
                        // 清空当前阶段的条件，导入选中阶段的条件（深拷贝）
                        phase.additionalConditions.clear();
                        for (Condition cond : selected.additionalConditions) {
                            phase.additionalConditions.add(copyCondition(cond));
                        }
                        minecraft.setScreen(this);
                    }));
                }
            }).pos(x, y).size(200, 20).build());
            y += 25;
        }

        // 已存在条件列表
        for (Condition cond : phase.additionalConditions) {
            addRenderableWidget(Button.builder(Component.literal(cond.getFinalDisplayText()), btn -> {
                assert minecraft != null;
                minecraft.setScreen(new ConditionEditScreen(edited -> {
                    int idx = phase.additionalConditions.indexOf(cond);
                    if (idx >= 0) phase.additionalConditions.set(idx, edited);
                    minecraft.setScreen(this);
                }, this, cond));
            }).pos(10, y).size(180, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.delete"), btn -> {
                phase.additionalConditions.remove(cond);
                init();
            }).pos(195, y).size(20, 20).build());
            y += 22;
        }

        // 返回按钮
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.back"), b -> {
            assert minecraft != null;
            minecraft.setScreen(parent);
        }).pos(x, height - 30).size(200, 20).build());
    }

    // 深拷贝
    private Condition copyCondition(Condition original) {
        String json = PresetManager.GSON.toJson(original);
        return PresetManager.GSON.fromJson(json, Condition.class);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.conditions_title"),
                width / 2, 10, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    // 阶段选择屏幕
    private static class PhaseSelectScreen extends Screen {
        private final Screen parent;
        private final List<SetPhase> phases;
        private final Consumer<SetPhase> onSelect;

        public PhaseSelectScreen(Screen parent, List<SetPhase> phases, Consumer<SetPhase> onSelect) {
            super(Component.translatable("visual_set_edit.gui.select_phase"));
            this.parent = parent;
            this.phases = phases;
            this.onSelect = onSelect;
        }

        @Override
        protected void init() {
            int y = 40;
            for (SetPhase phase : phases) {
                String name = phase.fallbackName != null ? phase.fallbackName : "Phase";
                addRenderableWidget(Button.builder(Component.literal(name), btn -> {
                    onSelect.accept(phase);
                    if (minecraft != null) minecraft.setScreen(parent);
                }).pos(width / 2 - 100, y).size(200, 20).build());
                y += 22;
            }
            addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.cancel"), btn -> {
                if (minecraft != null) minecraft.setScreen(parent);
            }).pos(width / 2 - 100, y + 10).size(200, 20).build());
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            renderBackground(graphics);
            super.render(graphics, mouseX, mouseY, partial);
            graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.select_phase"),
                    width / 2, 10, 0xFFFFFF);
        }
    }
}