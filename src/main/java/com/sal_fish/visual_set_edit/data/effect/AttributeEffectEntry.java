package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.event.ActiveSetTracker;
import com.sal_fish.visual_set_edit.util.AttributeHelper;
import net.minecraft.nbt.CompoundTag;
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
    @Expose public int durationSeconds = -1;
    @Expose public int cooldownSeconds = 0;

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
        if (cooldownSeconds < 0) cooldownSeconds = 0;
    }

    @Override
    public void resetUniqueId() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    @Override
    public void apply(LivingEntity entity) {
        ensureUniqueId();
        applyInternal(entity);
        if (durationSeconds > 0) {
            CompoundTag data = entity.getPersistentData();
            data.putBoolean("vse_attr_active_" + uniqueId, true);
            data.putLong("vse_attr_tick_" + uniqueId, entity.level().getGameTime());
        }
    }

    @Override
    public void remove(LivingEntity entity) {
        ensureUniqueId();
        removeModifierQuiet(entity);
        if (durationSeconds > 0) {
            CompoundTag data = entity.getPersistentData();
            data.remove("vse_attr_active_" + uniqueId);
            data.remove("vse_attr_tick_" + uniqueId);
        }
    }

    // 限时属性周期检查（由 SetEventHandler 每 20 tick 调用）
    public void updateTimed(LivingEntity entity) {
        if (durationSeconds <= 0) return;
        ensureUniqueId();
        CompoundTag data = entity.getPersistentData();
        String activeKey = "vse_attr_active_" + uniqueId;
        String tickKey = "vse_attr_tick_" + uniqueId;
        long now = entity.level().getGameTime();
        boolean active = data.getBoolean(activeKey);
        long lastTick = data.getLong(tickKey);

        if (active) {
            // 生效中：到期则移除
            if (lastTick > 0 && now - lastTick >= durationSeconds * 20L) {
                removeModifierQuiet(entity);
                data.putBoolean(activeKey, false);
                data.putLong(tickKey, now);
            }
        } else {
            // 冷却中：跳过
            if (cooldownSeconds > 0 && lastTick > 0 && now - lastTick < cooldownSeconds * 20L) {
                return;
            }
            // 仅当所属阶段仍激活时重新触发（防止脱装后仍周期性加属性）
            if (!isPhaseStillActive(entity)) return;
            applyInternal(entity);
            data.putBoolean(activeKey, true);
            data.putLong(tickKey, now);
        }
    }

    private boolean isPhaseStillActive(LivingEntity entity) {
        for (var active : ActiveSetTracker.getActivePhases(entity)) {
            for (EffectEntry entry : active.phase().effects) {
                if (entry == this) return true;
            }
        }
        return false;
    }

    private void applyInternal(LivingEntity entity) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributeId));
        if (attr == null) return;
        UUID id = UUID.fromString(uniqueId);
        AttributeHelper.applyModifier(entity, attr, id, "VSE " + attributeId, amount, operation);
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }

    private void removeModifierQuiet(LivingEntity entity) {
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
        if (durationSeconds > 0) {
            String cdStr = cooldownSeconds > 0
                    ? Component.translatable("visual_set_edit.gui.effect.attribute.timed.cd", durationSeconds, cooldownSeconds).getString()
                    : Component.translatable("visual_set_edit.gui.effect.attribute.timed.duration", durationSeconds).getString();
            return Component.translatable("visual_set_edit.gui.effect.attribute.display_timed", name, valueStr, cdStr).getString();
        }
        return Component.translatable("visual_set_edit.gui.effect.attribute.display", name, valueStr).getString();
    }

    private String formatValue(double amount, AttributeModifier.Operation op) {
        String opSymbol = switch (op) {
            case ADDITION -> (amount >= 0) ? "+" : "";
            case MULTIPLY_BASE -> "× base ";
            case MULTIPLY_TOTAL -> "× total ";
        };
        return opSymbol + amount;
    }
}
