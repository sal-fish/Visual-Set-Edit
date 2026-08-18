package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.event.ActiveSetTracker;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import com.sal_fish.visual_set_edit.util.AttributeHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public class DynamicAttributeEffectEntry extends EffectEntry {

    public enum VariableType {
        GAME_TIME, DAY_TIME, DISTANCE_TO_SPAWN, POS_Y,
        HEALTH, FOOD, XP_LEVEL, EQUIPPED_DURATION,
        KILL_COUNT_SINCE_EQUIP,
        L2H_CHUNK_DIFFICULTY,
        L2H_PLAYER_DIFFICULTY,
        ATTRIBUTE_VALUE
        }

    public enum FormulaType {
        LINEAR, QUADRATIC, EXPONENTIAL, LOGARITHMIC
    }

    @Expose public String attributeId;
    @Expose public AttributeModifier.Operation operation = AttributeModifier.Operation.ADDITION;
    @Expose public VariableType variableType = VariableType.GAME_TIME;
    @Expose public FormulaType formulaType = FormulaType.LINEAR;
    @Expose public double[] coefficients;
    @Expose public double base = 2.0;
    @Expose public double clipMinX = Double.NaN;
    @Expose public double clipMaxX = Double.NaN;
    @Expose public String uniqueId;
    @Expose public String sourceAttributeId = "";

    private transient Long startTick = null;

    public DynamicAttributeEffectEntry() {
        this.type = "dynamic_attribute";
        this.uniqueId = UUID.randomUUID().toString();
        this.coefficients = new double[]{0, 0};
    }

    public void ensureUniqueId() {
        if (uniqueId == null || uniqueId.isEmpty()) {
            uniqueId = UUID.randomUUID().toString();
        }
    }

    @Override
    public void initAfterLoad() {
        ensureUniqueId();
        if (coefficients == null || coefficients.length == 0) {
            coefficients = new double[]{0, 0};
        }
        if (sourceAttributeId == null) {
            sourceAttributeId = "";
        }
    }

    @Override
    public void resetUniqueId() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    @Override
    public void apply(LivingEntity entity) {
        ensureUniqueId();
        if (variableType == VariableType.EQUIPPED_DURATION) {
            String key = "vse_dyn_start_" + uniqueId;
            if (entity.getPersistentData().contains(key)) {
                startTick = entity.getPersistentData().getLong(key);
            } else {
                startTick = entity.level().getGameTime();
                entity.getPersistentData().putLong(key, startTick);
            }
        }
        if (variableType == VariableType.KILL_COUNT_SINCE_EQUIP) {
            String key = "vse_dynamic_kill_" + uniqueId;
            if (!entity.getPersistentData().contains(key)) {
                entity.getPersistentData().putInt(key, 0);
            }
        }
        updateModifier(entity);
    }

    @Override
    public void remove(LivingEntity entity) {
        ensureUniqueId();
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributeId));
        if (attr != null) {
            UUID id = UUID.fromString(uniqueId);
            AttributeHelper.removeModifier(entity, attr, id);
        }
        // 将当前起始游戏刻存入持久化，以便下次恢复（脱下、退出重进等）
        if (variableType == VariableType.EQUIPPED_DURATION && startTick != null) {
            entity.getPersistentData().putLong("vse_dyn_start_" + uniqueId, startTick);
        }
        startTick = null;
        // 击杀计数不删除，保持累积
    }

    public void updateModifier(LivingEntity entity) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributeId));
        if (attr == null) return;
        double x = getCurrentVariable(entity);
        double y = evaluate(x);
        UUID id = UUID.fromString(uniqueId);
        AttributeHelper.applyModifier(entity, attr, id, "VSE Dynamic " + attributeId, y, operation);
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
        if (variableType == VariableType.EQUIPPED_DURATION && startTick != null) {
            entity.getPersistentData().putLong("vse_dyn_start_" + uniqueId, startTick);
        }
    }

    public double getCurrentVariable(LivingEntity entity) {
        double raw;
        switch (variableType) {
            case GAME_TIME -> raw = entity.level().getGameTime() / 20.0;
            case DAY_TIME -> raw = (entity.level().getDayTime() % 24000) / 20.0;
            case DISTANCE_TO_SPAWN -> {
                var spawnPos = entity.level().getSharedSpawnPos();
                raw = Math.sqrt(entity.distanceToSqr(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()));
            }
            case POS_Y -> raw = entity.getY();
            case HEALTH -> raw = entity.getHealth();
            case FOOD -> raw = (entity instanceof Player player) ? player.getFoodData().getFoodLevel() : 0;
            case XP_LEVEL -> raw = (entity instanceof Player player) ? player.experienceLevel : 0;
            case EQUIPPED_DURATION -> {
                if (startTick == null) startTick = entity.level().getGameTime();
                raw = (entity.level().getGameTime() - startTick) / 20.0;
            }
            case KILL_COUNT_SINCE_EQUIP -> raw = entity.getPersistentData().getInt("vse_dynamic_kill_" + uniqueId);
            case L2H_CHUNK_DIFFICULTY -> raw = IntegrationManager.getL2ChunkDifficulty(entity);
            case L2H_PLAYER_DIFFICULTY -> raw = IntegrationManager.getL2PlayerDifficulty(entity);
            case ATTRIBUTE_VALUE -> {
                if (sourceAttributeId == null || sourceAttributeId.isEmpty()) {
                    raw = 0;
                } else {
                    Attribute sourceAttr = ForgeRegistries.ATTRIBUTES.getValue(
                            new ResourceLocation(sourceAttributeId));
                    if (sourceAttr == null) {
                        raw = 0;
                    } else {
                        AttributeInstance instance = entity.getAttribute(sourceAttr);
                        if (instance == null) {
                            raw = 0;
                        } else {
                            raw = instance.getValue();
                        }
                    }
                }
            }
            default -> raw = 0;
        }
        if (!Double.isNaN(clipMinX) && raw < clipMinX) raw = clipMinX;
        if (!Double.isNaN(clipMaxX) && raw > clipMaxX) raw = clipMaxX;
        return raw;
    }

    public double evaluate(double x) {
        if (coefficients == null) return 0;
        return switch (formulaType) {
            case LINEAR -> {
                double a = coefficients.length > 0 ? coefficients[0] : 0;
                double b = coefficients.length > 1 ? coefficients[1] : 0;
                yield a * x + b;
            }
            case QUADRATIC -> {
                double a = coefficients.length > 0 ? coefficients[0] : 0;
                double b = coefficients.length > 1 ? coefficients[1] : 0;
                double c = coefficients.length > 2 ? coefficients[2] : 0;
                yield a * x * x + b * x + c;
            }
            case EXPONENTIAL -> {
                double a = coefficients.length > 0 ? coefficients[0] : 1;
                double c = coefficients.length > 1 ? coefficients[1] : 0;
                yield a * Math.pow(base, x) + c;
            }
            case LOGARITHMIC -> {
                double a = coefficients.length > 0 ? coefficients[0] : 1;
                double c = coefficients.length > 1 ? coefficients[1] : 0;
                if (base <= 0 || base == 1) yield 0;
                yield a * (Math.log(x) / Math.log(base)) + c;
            }
        };
    }

    public static void incrementKillCount(LivingEntity entity, DynamicAttributeEffectEntry entry) {
        if (entry.variableType == VariableType.KILL_COUNT_SINCE_EQUIP) {
            String key = "vse_dynamic_kill_" + entry.uniqueId;
            int count = entity.getPersistentData().getInt(key);
            entity.getPersistentData().putInt(key, count + 1);
        }
    }

    public static void transferDynamicData(Player oldPlayer, Player newPlayer) {
        for (var active : ActiveSetTracker.getActivePhases(oldPlayer)) {
            for (EffectEntry entry : active.phase().effects) {
                if (entry instanceof DynamicAttributeEffectEntry dynEff) {
                    String startKey = "vse_dyn_start_" + dynEff.uniqueId;
                    if (oldPlayer.getPersistentData().contains(startKey)) {
                        long savedTick = oldPlayer.getPersistentData().getLong(startKey);
                        newPlayer.getPersistentData().putLong(startKey, savedTick);
                    }
                    if (dynEff.variableType == VariableType.KILL_COUNT_SINCE_EQUIP) {
                        String killKey = "vse_dynamic_kill_" + dynEff.uniqueId;
                        if (oldPlayer.getPersistentData().contains(killKey)) {
                            int count = oldPlayer.getPersistentData().getInt(killKey);
                            newPlayer.getPersistentData().putInt(killKey, count);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getDisplayText() {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attributeId));
        String name = attribute != null ? Component.translatable(attribute.getDescriptionId()).getString() : attributeId;

        if (Minecraft.getInstance().player != null) {
            AttributeInstance instance = null;
            if (attribute != null) {
                instance = Minecraft.getInstance().player.getAttribute(attribute);
            }
            if (instance != null) {
                UUID id = UUID.fromString(uniqueId);
                AttributeModifier mod = instance.getModifier(id);
                if (mod != null) {
                    double currentAmount = mod.getAmount();
                    return Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.current", name, String.format("%.2f", currentAmount)).getString();
                }
            }
        }

        String varName;
        if (variableType == VariableType.ATTRIBUTE_VALUE && !sourceAttributeId.isEmpty()) {
            Attribute sourceAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(sourceAttributeId));
            varName = sourceAttr != null
                    ? Component.translatable(sourceAttr.getDescriptionId()).getString()
                    : sourceAttributeId;
        } else {
            varName = Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.var." + variableType.name()).getString();
        }
        return Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.display", name, varName).getString();
    }
}