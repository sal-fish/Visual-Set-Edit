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
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class EffectListScreen extends Screen {
    private final SetPhase phase;
    private final Preset preset;
    private final Screen parent;

    private static final int ITEM_HEIGHT = 22;
    private int scrollOffset = 0;
    private int listTop;
    private int listHeight;

    public EffectListScreen(SetPhase phase, Preset preset, Screen parent) {
        super(Component.translatable("visual_set_edit.gui.effects_title"));
        this.phase = phase;
        this.preset = preset;
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
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
                otherPhases.remove(phase);
                if (!otherPhases.isEmpty()) {
                    minecraft.setScreen(new PhaseSelectScreen(this, otherPhases, selected -> {
                        phase.effects.clear();
                        for (EffectEntry entry : selected.effects) {
                            EffectEntry copy = copyEffect(entry);
                            copy.resetUniqueId();
                            phase.effects.add(copy);
                        }
                        minecraft.setScreen(this);
                    }));
                }
            }).pos(x, y).size(200, 20).build());
            y += 25;
        }

        listTop = y;
        int maxVisible = Math.max(1, (height - listTop - 40) / ITEM_HEIGHT);
        int maxOffset = Math.max(0, phase.effects.size() - maxVisible);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));

        int currentY = listTop;
        int end = Math.min(phase.effects.size(), scrollOffset + maxVisible);
        for (int i = scrollOffset; i < end; i++) {
            EffectEntry entry = phase.effects.get(i);
            final int index = i;
            addRenderableWidget(Button.builder(Component.literal(entry.getDisplayText()), btn -> {
                assert minecraft != null;
                minecraft.setScreen(new EffectEditScreen(effect -> {
                    int idx = phase.effects.indexOf(entry);
                    if (idx >= 0) phase.effects.set(idx, effect);
                    minecraft.setScreen(this);
                }, this, entry));
            }).pos(10, currentY).size(140, 20).build());

            addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.delete"), btn -> {
                phase.effects.remove(index);
                int newMaxOffset = Math.max(0, phase.effects.size() - maxVisible);
                if (scrollOffset > newMaxOffset) scrollOffset = newMaxOffset;
                init();
            }).pos(153, currentY).size(20, 20).build());

            addRenderableWidget(Button.builder(Component.literal("↑"), btn -> {
                if (index > 0) {
                    Collections.swap(phase.effects, index, index - 1);
                    scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, phase.effects.size() - maxVisible)));
                    init();
                }
            }).pos(176, currentY).size(20, 20).build());

            addRenderableWidget(Button.builder(Component.literal("↓"), btn -> {
                if (index < phase.effects.size() - 1) {
                    Collections.swap(phase.effects, index, index + 1);
                    scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, phase.effects.size() - maxVisible)));
                    init();
                }
            }).pos(198, currentY).size(20, 20).build());

            currentY += ITEM_HEIGHT;
        }

        listHeight = maxVisible * ITEM_HEIGHT;

        // 返回按钮
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.back"), b -> {
            assert minecraft != null;
            minecraft.setScreen(parent);
        }).pos(x, height - 30).size(200, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (mouseY >= listTop && mouseY <= listTop + listHeight) {
            int maxVisible = listHeight / ITEM_HEIGHT;
            int maxOffset = Math.max(0, phase.effects.size() - maxVisible);
            int newOffset = scrollOffset - (int) Math.signum(scrollDelta);
            scrollOffset = Math.max(0, Math.min(maxOffset, newOffset));
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.effects_title"),
                width / 2, 10, 0xFFFFFF);

        // 绘制滚动条
        if (!phase.effects.isEmpty() && listHeight > 0 && phase.effects.size() > (listHeight / ITEM_HEIGHT)) {
            int maxVisible = listHeight / ITEM_HEIGHT;
            int scrollBarX = width - 5;
            int totalRows = phase.effects.size();
            int scrollBarHeight = Math.max(4, (int) ((float) maxVisible / totalRows * listHeight));
            int scrollBarY = listTop + (int) ((float) scrollOffset / (totalRows - maxVisible) * (listHeight - scrollBarHeight));
            graphics.fill(scrollBarX, listTop, scrollBarX + 3, listTop + listHeight, 0xFFAAAAAA);
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 3, scrollBarY + scrollBarHeight, 0xFFFFFFFF);
        }
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }

    private EffectEntry copyEffect(EffectEntry original) {
        String json = PresetManager.GSON.toJson(original);
        return PresetManager.GSON.fromJson(json, EffectEntry.class);
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