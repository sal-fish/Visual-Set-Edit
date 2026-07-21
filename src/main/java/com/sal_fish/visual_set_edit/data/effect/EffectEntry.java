package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;

public abstract class EffectEntry {
    @Expose public String type;
    @Expose public String customDisplayText = "";
    @Expose public String customColor = "white";

    public abstract void apply(LivingEntity entity);
    public abstract void remove(LivingEntity entity);
    public abstract String getDisplayText();

    public void initAfterLoad() {}

    public void resetUniqueId() {}

    public String getFinalDisplayText() {
        return customDisplayText.isEmpty() ? getDisplayText() : customDisplayText;
    }

    public ChatFormatting getFinalColor() {
        return parseColor(customColor);
    }

    private static ChatFormatting parseColor(String name) {
        for (ChatFormatting f : ChatFormatting.values()) {
            if (f.getName().equalsIgnoreCase(name)) return f;
        }
        return ChatFormatting.GRAY;
    }
}