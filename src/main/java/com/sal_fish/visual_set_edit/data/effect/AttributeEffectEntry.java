package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.util.AttributeHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.UUID;

public class AttributeEffectEntry extends EffectEntry {
    @Expose public String attributeId;
    @Expose public double amount;
    @Expose public AttributeModifier.Operation operation = AttributeModifier.Operation.ADDITION;
    @Expose public String uniqueId;

    public AttributeEffectEntry() {
        this.type = "attribute";
        this.uniqueId = UUID.randomUUID().toString();
    }

    public void ensureUniqueId() {
        if (uniqueId == null || uniqueId.isEmpty()) {
            uniqueId = UUID.randomUUID().toString();
        }
    }

    @Override
    public void initAfterLoad() {
        ensureUniqueId();
    }

    @Override
    public void resetUniqueId() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    @Override
    public void apply(LivingEntity entity) {
        ensureUniqueId();
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributeId));
        if (attr == null) return;
        UUID id = UUID.fromString(uniqueId);
        AttributeHelper.applyModifier(entity, attr, id, "VSE " + attributeId, amount, operation);
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }

    @Override
    public void remove(LivingEntity entity) {
        ensureUniqueId();
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributeId));
        if (attr != null) {
            UUID id = UUID.fromString(uniqueId);
            AttributeHelper.removeModifier(entity, attr, id);
            if (entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
        }
    }

    @Override
    public String getDisplayText() {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributeId));
        String name = attribute != null ? Component.translatable(attribute.getDescriptionId()).getString() : attributeId;
        String valueStr = formatValue(amount, operation);
        return Component.translatable("visual_set_edit.gui.effect.attribute.display", name, valueStr).getString();
    }

    private String formatValue(double amount, AttributeModifier.Operation op) {
        String opSymbol = switch (op) {
            case ADDITION -> (amount >= 0) ? "+" : "";
            case MULTIPLY_BASE -> "× base ";
            case MULTIPLY_TOTAL -> "× total ";
        };
        // 负数自带负号，不需额外前缀
        return opSymbol + amount;
    }
}