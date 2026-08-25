package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;

public abstract class EffectEntry {
    @Expose public String type;
    @Expose public String customDisplayText = "";
    @Expose public String customColor = "white";
    @Expose public boolean showPointer = true;

    public abstract void apply(LivingEntity entity);
    public abstract void remove(LivingEntity entity);
    public abstract String getDisplayText();

    public void initAfterLoad() {}

    public void resetUniqueId() {}

    public String getFinalDisplayText() {
        return customDisplayText.isEmpty() ? getDisplayText() : customDisplayText;
    }

    public TextColor getFinalColor() {
        return parseColor(customColor);
    }

    private static TextColor parseColor(String color) {
        if (color == null || color.isEmpty()) {
            return TextColor.fromLegacyFormat(ChatFormatting.GRAY);
        }

        // 优先尝试解析为 Hex 颜色（如 #FF0000）
        if (color.startsWith("#")) {
            TextColor hex = TextColor.parseColor(color);
            if (hex != null) return hex;
        }

        // 兼容原版颜色名称（如 white, gold）
        ChatFormatting formatting = ChatFormatting.getByName(color.toLowerCase(Locale.ROOT));
        if (formatting != null && formatting.getColor() != null) {
            return TextColor.fromLegacyFormat(formatting);
        }

        // 最后尝试直接解析（可能包含其他格式）
        TextColor parsed = TextColor.parseColor(color);
        if (parsed != null) return parsed;

        return TextColor.fromLegacyFormat(ChatFormatting.GRAY);
    }
}