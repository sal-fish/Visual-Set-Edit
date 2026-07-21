package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.effect.EffectEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EffectListScreen extends Screen {
    private final SetPhase phase;
    private final Preset preset;          // 新增：用于访问其他阶段
    private final Screen parent;

    public EffectListScreen(SetPhase phase, Preset preset, Screen parent) {
        super(Component.translatable("visual_set_edit.gui.effects_title"));
        this.phase = phase;
        this.preset = preset;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = 40;

        // 添加效果按钮
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.add_effect"), b -> {
            assert minecraft != null;
            minecraft.setScreen(new EffectEditScreen(effect -> {
                phase.effects.add(effect);
                minecraft.setScreen(this);
            }, this));
        }).pos(x, y).size(200, 20).build());
        y += 25;

        // 从其他阶段导入按钮（仅当存在其他阶段时显示）
        if (preset != null && preset.phases.size() > 1) {
            addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.import_effects"), b -> {
                assert minecraft != null;
                List<SetPhase> otherPhases = new ArrayList<>(preset.phases);
                otherPhases.remove(phase); // 排除当前阶段
                if (!otherPhases.isEmpty()) {
                    minecraft.setScreen(new PhaseSelectScreen(this, otherPhases, selected -> {
                        // 清空当前阶段效果，导入选中阶段的效果（深拷贝）
                        phase.effects.clear();
                        for (EffectEntry entry : selected.effects) {
                            phase.effects.add(copyEffect(entry));
                        }
                        minecraft.setScreen(this);
                    }));
                }
            }).pos(x, y).size(200, 20).build());
            y += 25;
        }

        // 已有效果列表
        for (EffectEntry entry : phase.effects) {
            addRenderableWidget(Button.builder(Component.literal(entry.getDisplayText()), btn -> {
                assert minecraft != null;
                minecraft.setScreen(new EffectEditScreen(effect -> {
                    int idx = phase.effects.indexOf(entry);
                    if (idx >= 0) phase.effects.set(idx, effect);
                    minecraft.setScreen(this);
                }, this, entry));
            }).pos(10, y).size(180, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.delete"), btn -> {
                phase.effects.remove(entry);
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

    /**
     * 使用 Gson 序列化再反序列化，实现 EffectEntry 的深拷贝。
     */
    private EffectEntry copyEffect(EffectEntry original) {
        String json = PresetManager.GSON.toJson(original);
        return PresetManager.GSON.fromJson(json, EffectEntry.class);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.effects_title"),
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