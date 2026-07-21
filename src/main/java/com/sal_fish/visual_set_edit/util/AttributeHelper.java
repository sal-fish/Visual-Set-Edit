package com.sal_fish.visual_set_edit.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.UUID;

public class AttributeHelper {
    public static void applyModifier(LivingEntity entity, Attribute attribute, UUID id, String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            AttributeModifier old = instance.getModifier(id);
            if (old != null) instance.removeModifier(id);
            instance.addPermanentModifier(new AttributeModifier(id, name, amount, op));
        }
    }

    public static void removeModifier(LivingEntity entity, Attribute attribute, UUID id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }
}