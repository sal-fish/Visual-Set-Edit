package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public class TagEffectEntry extends EffectEntry {
    @Expose public String tagName = "";

    public TagEffectEntry() {
        this.type = "tag";
    }

    @Override
    public void apply(LivingEntity entity) {
        if (!tagName.isEmpty() && !entity.getTags().contains(tagName)) {
            entity.addTag(tagName);
        }
    }

    @Override
    public void remove(LivingEntity entity) {
        if (!tagName.isEmpty() && entity.getTags().contains(tagName)) {
            entity.removeTag(tagName);
        }
    }

    @Override
    public String getDisplayText() {
        return Component.translatable("visual_set_edit.gui.effect.tag.display", tagName).getString();
    }
}
