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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeEffectEntry extends EffectEntry {
    @Expose public String attributeId;
    @Expose public double amount;
    @Expose public AttributeModifier.Operation operation = AttributeModifier.Operation.ADDITION;
    @Expose public String uniqueId;
    @Expose public int durationSeconds = -1;
    @Expose public int cooldownSeconds = 0;

    private static final ConcurrentHashMap<UUID, List<AttributeEffectEntry>> ORPHANED_TIMED_ATTRS = new ConcurrentHashMap<>();

    public static void clearOrphans(UUID uuid) {
        ORPHANED_TIMED_ATTRS.remove(uuid);
    }

    public static List<AttributeEffectEntry> getOrphaned(UUID uuid) {
        return ORPHANED_TIMED_ATTRS.get(uuid);
    }

    private static void registerOrphan(LivingEntity entity, AttributeEffectEntry attr) {
        ORPHANED_TIMED_ATTRS.computeIfAbsent(entity.getUUID(), k -> new ArrayList<>()).add(attr);
    }

    private static void unregisterOrphan(UUID uuid, AttributeEffectEntry attr) {
        List<AttributeEffectEntry> list = ORPHANED_TIMED_ATTRS.get(uuid);
        if (list != null) {
            list.remove(attr);
            if (list.isEmpty()) ORPHANED_TIMED_ATTRS.remove(uuid);
        }
    }

    // 按 uniqueId 清除历史孤儿：保存/重穿重建会产生新的 deepCopy 实例（引用不同），
    // 旧孤儿对象引用比较清不掉，这里按 uniqueId 统一清掉，避免孤儿表泄漏与幽灵双跑。
    private static void purgeOrphans(UUID entityUuid, String uniqueId) {
        List<AttributeEffectEntry> list = ORPHANED_TIMED_ATTRS.get(entityUuid);
        if (list != null) {
            list.removeIf(o -> uniqueId.equals(o.uniqueId));
            if (list.isEmpty()) ORPHANED_TIMED_ATTRS.remove(entityUuid);
        }
    }

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
            purgeOrphans(entity.getUUID(), uniqueId); // 重新激活：清掉同 uniqueId 的历史孤儿（旧 deepCopy 实例）
            CompoundTag data = entity.getPersistentData();
            data.putBoolean("vse_attr_active_" + uniqueId, true);
            data.putLong("vse_attr_tick_" + uniqueId, entity.level().getGameTime());
        }
    }

    @Override
    public void remove(LivingEntity entity) {
        ensureUniqueId();
        if (durationSeconds > 0) {
            registerOrphan(entity, this);
            return;
        }
        removeModifierQuiet(entity);
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
                if (cooldownSeconds > 0) {
                    // 有冷却：摘除 modifier
                    removeModifierQuiet(entity);
                    data.putBoolean(activeKey, false);
                    data.putLong(tickKey, now);
                } else {
                    if (isManagedElsewhere(entity)) {
                        unregisterOrphan(entity.getUUID(), this);
                    } else if (isPhaseStillActive(entity)) {
                        data.putLong(tickKey, now);
                    } else {
                        removeModifierQuiet(entity);
                        data.putBoolean(activeKey, false);
                        unregisterOrphan(entity.getUUID(), this);
                    }
                }
            }
        } else {
            if (cooldownSeconds > 0 && lastTick > 0 && now - lastTick < cooldownSeconds * 20L) {
                return;
            }
            if (!isPhaseStillActive(entity)) {
                unregisterOrphan(entity.getUUID(), this);
                return;
            }
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

    private boolean isManagedElsewhere(LivingEntity entity) {
        for (var active : ActiveSetTracker.getActivePhases(entity)) {
            for (EffectEntry entry : active.phase().effects) {
                if (entry instanceof AttributeEffectEntry attr && attr != this && uniqueId.equals(attr.uniqueId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void applyInternal(LivingEntity entity) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributeId));
        if (attr == null) return;
        UUID id = UUID.fromString(uniqueId);
        boolean isMaxHealth = attr == Attributes.MAX_HEALTH;
        float oldMax = isMaxHealth ? entity.getMaxHealth() : 0;
        float oldHealth = isMaxHealth ? entity.getHealth() : 0;
        AttributeHelper.applyModifier(entity, attr, id, "VSE " + attributeId, amount, operation);
        if (isMaxHealth) {
            AttributeHelper.preserveHealthRatio(entity, oldMax, oldHealth);
        } else if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }

    private void removeModifierQuiet(LivingEntity entity) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributeId));
        if (attr != null) {
            UUID id = UUID.fromString(uniqueId);
            boolean isMaxHealth = attr == Attributes.MAX_HEALTH;
            float oldMax = isMaxHealth ? entity.getMaxHealth() : 0;
            float oldHealth = isMaxHealth ? entity.getHealth() : 0;
            AttributeHelper.removeModifier(entity, attr, id);
            if (isMaxHealth) {
                // 对称保比例：摘除后同样按新旧上限等比缩放，避免把健康玩家按失真比例压死、也避免残血白嫖回满
                AttributeHelper.preserveHealthRatio(entity, oldMax, oldHealth);
            } else if (entity.getHealth() > entity.getMaxHealth()) {
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
