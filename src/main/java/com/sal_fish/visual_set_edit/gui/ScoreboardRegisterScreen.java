package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.config.ScoreboardObjectiveManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardRegisterScreen extends Screen {
    private static final ResourceLocation ENTRY_ID = new ResourceLocation("vse", "objective");

    private static final int COLOR_VALID = 14737632;
    private static final int COLOR_INVALID = 0xFFFF5555;

    private final Screen parent;
    private final List<String> objectives = new ArrayList<>();
    private ScrollableSelectionList list;
    private EditBox nameEdit;

    public ScoreboardRegisterScreen(Screen parent) {
        super(Component.translatable("visual_set_edit.gui.scoreboard_register.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        objectives.clear();
        objectives.addAll(ScoreboardObjectiveManager.getObjectives());

        int centerX = width / 2 - 100;
        int y = 30;
        int listWidth = 200;
        int listHeight = Math.min(6 * 22, height - y - 100);

        nameEdit = new EditBox(font, centerX, y, 160, 20,
                Component.translatable("visual_set_edit.gui.scoreboard_register.name"));
        nameEdit.setMaxLength(5201314);
        //不合规时文字变红，合规或为空时恢复默认色
        nameEdit.setResponder(s -> {
            String v = s == null ? "" : s.trim();
            nameEdit.setTextColor(
                    v.isEmpty() || ScoreboardObjectiveManager.isValidName(v) ? COLOR_VALID : COLOR_INVALID);
        });
        addRenderableWidget(nameEdit);
        y += 22;

        Button addButton = Button.builder(
                Component.translatable("visual_set_edit.gui.add"),
                btn -> {
                    String name = nameEdit.getValue().trim();
                    if (ScoreboardObjectiveManager.isValidName(name)
                            && ScoreboardObjectiveManager.addObjective(name)) {
                        objectives.clear();
                        objectives.addAll(ScoreboardObjectiveManager.getObjectives());
                        nameEdit.setValue("");
                        if (list != null) updateList();
                    }
                }
        ).pos(centerX + 165, y - 22).size(35, 20).build();
        addRenderableWidget(addButton);

        // 创建列表，左键无操作，右键删除
        list = new ScrollableSelectionList(minecraft, listWidth, listHeight, y, 20,
                entry -> {}, // 左键：无操作
                entry -> {   // 右键：删除
                    String name = entry.getRawId();
                    ScoreboardObjectiveManager.removeObjective(name);
                    objectives.clear();
                    objectives.addAll(ScoreboardObjectiveManager.getObjectives());
                    updateList();
                }
        );
        addWidget(list);
        updateList();

        y += listHeight + 5;
        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.back"),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(parent);
                }
        ).pos(centerX, y).size(200, 20).build());
    }

    private void updateList() {
        list.clearAllEntries();
        for (String name : objectives) {
            list.addEntry(new ScrollableSelectionList.Entry(
                    Component.literal(name),
                    ENTRY_ID,
                    name,
                    null
            ));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0xCC000000);
        if (list != null) list.render(graphics, mouseX, mouseY, partial);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font,
                Component.translatable("visual_set_edit.gui.scoreboard_register.title"),
                width / 2, 10, 0xFFFFFF);
        // 鼠标悬停输入框时显示命名规则提示
        if (nameEdit != null && nameEdit.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(font,
                    Component.translatable("visual_set_edit.gui.scoreboard_register.name_tooltip"),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (list != null && list.isMouseOver(mouseX, mouseY)) {
            return list.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
    }
}