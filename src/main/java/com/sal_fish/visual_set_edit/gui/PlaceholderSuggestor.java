package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.util.SearchUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 数值框的 %占位符% 候选：输入 % 弹属性列表，Tab 补全，上/下切换，Esc 关闭
public class PlaceholderSuggestor {
    private static final int ROW_H = 12;
    private static final int MAX_ROWS = 8;
    private static final int PAD = 4;
    private static final int MIN_W = 180;
    private static final int MAX_W = 380;
    private static final int BG = 0xFF101010;
    private static final float Z_LAYER = 400.0F;
    private static final int BORDER = 0xFF8A8A8A;
    private static final int TEXT = 0xFFD8D8D8;
    private static final int TEXT_SEL = 0xFFFFF070;
    private static final int HOVER_BG = 0x40FFFFFF;
    private static final int SEL_BG = 0x50FFFFFF;

    private final EditBox box;
    private final List<ResourceLocation> matches = new ArrayList<>();

    private int replaceFrom = -1;
    private int selected;
    private int scroll;
    private String lastText = "";
    private int lastCaret = -1;
    private boolean lastFocused;

    private int panelX, panelY, panelW, panelH;

    public PlaceholderSuggestor(EditBox box) {
        this.box = box;
    }

    // 文本、光标或焦点变化时重算候选
    private void refresh() {
        String text = box.getValue();
        int caret = box.getCursorPosition();
        boolean focused = box.isFocused();
        if (text.equals(lastText) && caret == lastCaret && focused == lastFocused) return;
        lastText = text;
        lastCaret = caret;
        lastFocused = focused;
        replaceFrom = -1;
        matches.clear();
        if (!focused) return;
        if (caret <= 0 || caret > text.length()) return;

        int pct = text.lastIndexOf('%', caret - 1);
        if (pct < 0) return;
        // 前面紧跟数字说明是百分比写法
        if (pct > 0 && Character.isDigit(text.charAt(pct - 1))) return;
        String prefix = text.substring(pct + 1, caret);
        if (prefix.indexOf('%') >= 0) return;
        // 落在已闭合的 %..% 之后则不提示
        int open = 0;
        for (int i = 0; i < pct; i++) {
            if (text.charAt(i) == '%') open++;
        }
        if ((open & 1) == 1) return;

        replaceFrom = pct;
        String q = prefix.trim();
        for (ResourceLocation id : ForgeRegistries.ATTRIBUTES.getKeys()) {
            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(id);
            String name = attr != null ? Component.translatable(attr.getDescriptionId()).getString() : "";
            if (SearchUtil.contains(id.toString(), q) || SearchUtil.contains(name, q)) {
                matches.add(id);
            }
        }
        matches.sort(Comparator.comparing(ResourceLocation::toString));
        selected = 0;
        scroll = 0;
    }

    private void apply(ResourceLocation id) {
        if (replaceFrom < 0) return;
        String text = box.getValue();
        int caret = Math.min(box.getCursorPosition(), text.length());
        if (caret < replaceFrom) return;
        String ins = id.toString();
        String result = text.substring(0, replaceFrom) + "%" + ins + "%" + text.substring(caret);
        box.setHighlightPos(replaceFrom);
        box.setValue(result);
        box.setCursorPosition(replaceFrom + ins.length() + 2);
        lastText = result;
        lastCaret = box.getCursorPosition();
        replaceFrom = -1;
        matches.clear();
    }

    private void close() {
        replaceFrom = -1;
        matches.clear();
    }

    public boolean keyPressed(int keyCode) {
        if (replaceFrom < 0 || matches.isEmpty()) return false;
        switch (keyCode) {
            case GLFW.GLFW_KEY_TAB -> apply(matches.get(selected));
            case GLFW.GLFW_KEY_DOWN -> move(1);
            case GLFW.GLFW_KEY_UP -> move(-1);
            case GLFW.GLFW_KEY_ESCAPE -> close();
            default -> {
                return false;
            }
        }
        return true;
    }

    private void move(int delta) {
        selected = Math.floorMod(selected + delta, matches.size());
        if (selected < scroll) scroll = selected;
        if (selected >= scroll + MAX_ROWS) scroll = selected - MAX_ROWS + 1;
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (replaceFrom < 0 || matches.isEmpty() || panelH <= 0) return false;
        if (mouseX < panelX || mouseX >= panelX + panelW) return false;
        if (mouseY < panelY || mouseY >= panelY + panelH) return false;
        int row = (int) ((mouseY - panelY - PAD) / ROW_H);
        int idx = scroll + row;
        if (row < 0 || idx < 0 || idx >= matches.size()) return false;
        apply(matches.get(idx));
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (replaceFrom < 0 || panelH <= 0) return false;
        if (mouseX < panelX || mouseX >= panelX + panelW) return false;
        if (mouseY < panelY || mouseY >= panelY + panelH) return false;
        int maxScroll = matches.size() - MAX_ROWS;
        if (maxScroll > 0) {
            scroll = (int) Math.max(0, Math.min(maxScroll, scroll - delta));
            if (selected < scroll) selected = scroll;
            if (selected >= scroll + MAX_ROWS) selected = scroll + MAX_ROWS - 1;
        }
        return true;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        refresh();
        panelH = 0;
        if (replaceFrom < 0 || matches.isEmpty()) return;

        var mc = Minecraft.getInstance();
        var font = mc.font;
        int rows = Math.min(matches.size(), MAX_ROWS);
        int w = MIN_W;
        for (int i = scroll; i < scroll + rows && i < matches.size(); i++) {
            w = Math.max(w, font.width(label(matches.get(i))) + PAD * 2 + 6);
        }
        w = Math.min(w, MAX_W);
        int h = rows * ROW_H + PAD * 2;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int x = Math.max(2, Math.min(box.getX(), screenW - w - 2));
        int below = box.getY() + box.getHeight() + 2;
        int y = below + h <= screenH ? below : Math.max(2, box.getY() - h - 2);

        panelX = x;
        panelY = y;
        panelW = w;
        panelH = h;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0, 0.0, Z_LAYER);

        graphics.fill(x, y, x + w, y + h, BG);
        graphics.fill(x, y, x + w, y + 1, BORDER);
        graphics.fill(x, y + h - 1, x + w, y + h, BORDER);
        graphics.fill(x, y, x + 1, y + h, BORDER);
        graphics.fill(x + w - 1, y, x + w, y + h, BORDER);

        for (int i = 0; i < rows; i++) {
            int idx = scroll + i;
            int ry = y + PAD + i * ROW_H;
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + ROW_H;
            if (idx == selected) {
                graphics.fill(x + 1, ry, x + w - 1, ry + ROW_H, SEL_BG);
            } else if (hover) {
                graphics.fill(x + 1, ry, x + w - 1, ry + ROW_H, HOVER_BG);
            }
            String text = font.plainSubstrByWidth(label(matches.get(idx)), w - PAD * 2 - 6);
            graphics.drawString(font, text, x + PAD, ry + 2, idx == selected ? TEXT_SEL : TEXT, false);
        }
        if (matches.size() > MAX_ROWS) {
            int barH = Math.max(8, h * MAX_ROWS / matches.size());
            int barY = y + (h - barH) * scroll / Math.max(1, matches.size() - MAX_ROWS);
            graphics.fill(x + w - 3, barY, x + w - 1, barY + barH, BORDER);
        }

        graphics.pose().popPose();
    }

    private String label(ResourceLocation id) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(id);
        String name = attr != null ? Component.translatable(attr.getDescriptionId()).getString() : "";
        return name.isEmpty() ? id.toString() : name + "  " + id;
    }
}
