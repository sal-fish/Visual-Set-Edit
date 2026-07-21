package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.SlotCondition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class SlotConditionListScreen extends Screen {
    private final SetPhase phase;
    private final Screen parent;

    public SlotConditionListScreen(SetPhase phase, Screen parent) {
        super(Component.translatable("visual_set_edit.gui.slot_conditions_title"));
        this.phase = phase;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.add_slot"), b -> {
            SlotCondition newCond = new SlotCondition();
            newCond.slot = "HEAD";
            phase.slotConditions.add(newCond);
            assert minecraft != null;
            minecraft.setScreen(new SlotConditionEditScreen(phase, phase.slotConditions.size() - 1, this));
        }).pos(x, 40).size(200, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.back"), b -> {
            assert minecraft != null;
            minecraft.setScreen(parent);
        }).pos(x, height - 30).size(200, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.slot_conditions_title"), width / 2, 10, 0xFFFFFF);
        int y = 70;
        for (int i = 0; i < phase.slotConditions.size(); i++) {
            SlotCondition cond = phase.slotConditions.get(i);
            // 优先显示 Tag，否则显示 itemId 或 "*"
            String matchDesc;
            if (cond.tagId != null && !cond.tagId.isEmpty()) {
                matchDesc = "tag:" + cond.tagId;
            } else if (cond.itemId != null && !cond.itemId.isEmpty()) {
                matchDesc = cond.itemId;
            } else {
                matchDesc = "*";
            }
            String info = cond.slot + ": " + matchDesc;
            graphics.drawString(font, info, 10, y, 0xFFFFFF);
            y += 12;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int y = 70;
        for (int i = 0; i < phase.slotConditions.size(); i++) {
            if (mouseX >= 10 && mouseX <= 210 && mouseY >= y && mouseY <= y + 10) {
                assert minecraft != null;
                minecraft.setScreen(new SlotConditionEditScreen(phase, i, this));
                return true;
            }
            y += 12;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }
}