package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.event.ActiveSetTracker;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import com.sal_fish.visual_set_edit.util.AttributeHelper;
import com.sal_fish.visual_set_edit.util.ExpressionEvaluator;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public class DynamicAttributeEffectEntry extends EffectEntry {

    public enum VariableType {
        GAME_TIME, DAY_TIME, DISTANCE_TO_SPAWN, POS_Y,
        HEALTH, HEALTH_PERCENT, FOOD, FOOD_PERCENT, XP_LEVEL, EQUIPPED_DURATION,
        KILL_COUNT_SINCE_EQUIP,
        L2H_CHUNK_DIFFICULTY,
        L2H_PLAYER_DIFFICULTY,
        ATTRIBUTE_VALUE,
        SCOREBOARD_VALUE,
        POTION_LEVEL
    }

    public enum FormulaType {
        LINEAR, QUADRATIC, EXPONENTIAL, LOGARITHMIC,
        POWER, STEP, SIGMOID,
        SINE, TRIANGLE, SQUARE, SAWTOOTH
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
    @Expose public String scoreboardObjective = "";
    @Expose public String sourcePotionId = "";
    // 各数值框原始输入串，支持 %属性id% 与算术；null/空 = 用对应数值字段
    @Expose public String[] coeffExpressions;
    @Expose public String baseExpression;
    @Expose public String clipMinExpression;
    @Expose public String clipMaxExpression;

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
        if (scoreboardObjective == null) {
            scoreboardObjective = "";
        }
        if (sourcePotionId == null) {
            sourcePotionId = "";
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
        double y = evaluate(entity, x);
        UUID id = UUID.fromString(uniqueId);
        boolean isMaxHealth = attr == Attributes.MAX_HEALTH;
        float oldMax = isMaxHealth ? entity.getMaxHealth() : 0;
        float oldHealth = isMaxHealth ? entity.getHealth() : 0;
        AttributeHelper.applyModifier(entity, attr, id, "VSE Dynamic " + attributeId, y, operation);
        if (isMaxHealth) {
            // 对称保比例：上限被改写时等比缩放血量
            AttributeHelper.preserveHealthRatio(entity, oldMax, oldHealth);
        } else if (entity.getHealth() > entity.getMaxHealth()) {
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
            case HEALTH_PERCENT -> {
                float max = entity.getMaxHealth();
                raw = max > 0 ? (entity.getHealth() / max) * 100.0 : 0;
            }
            case FOOD -> raw = (entity instanceof Player player) ? player.getFoodData().getFoodLevel() : 0;
            case FOOD_PERCENT -> {
                raw = (entity instanceof Player player) ? (player.getFoodData().getFoodLevel() / 20.0) * 100.0 : 0;
            }
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
            case SCOREBOARD_VALUE -> {
                if (scoreboardObjective == null || scoreboardObjective.isEmpty()) {
                    raw = 0;
                } else {
                    var scoreboard = entity.level().getScoreboard();
                    var objective = scoreboard.getObjective(scoreboardObjective);
                    if (objective == null) {
                        raw = 0;
                    } else {
                        raw = scoreboard.getOrCreatePlayerScore(entity.getScoreboardName(), objective).getScore();
                    }
                }
            }
            case POTION_LEVEL -> {
                if (sourcePotionId == null || sourcePotionId.isEmpty()) {
                    raw = 0;
                } else {
                    MobEffect sourceEffect = ForgeRegistries.MOB_EFFECTS.getValue(
                            ResourceLocation.tryParse(sourcePotionId));
                    if (sourceEffect == null) {
                        raw = 0;
                    } else {
                        MobEffectInstance instance = entity.getEffect(sourceEffect);
                        if (instance == null) {
                            raw = 0; // 玩家当前没有该药水效果，视为 0 级
                        } else {
                            raw = instance.getAmplifier() + 1; // amplifier 从 0 起，等级 = amplifier + 1
                        }
                    }
                }
            }
            default -> raw = 0;
        }
        double clipMin = resolveClip(entity, clipMinExpression, clipMinX);
        double clipMax = resolveClip(entity, clipMaxExpression, clipMaxX);
        if (!Double.isNaN(clipMin) && raw < clipMin) raw = clipMin;
        if (!Double.isNaN(clipMax) && raw > clipMax) raw = clipMax;
        return raw;
    }

    public double evaluate(LivingEntity entity, double x) {
        if (coefficients == null) return 0;
        return switch (formulaType) {
            case LINEAR -> {
                double a = resolveCoeff(entity, 0, 0);
                double b = resolveCoeff(entity, 1, 0);
                yield a * x + b;
            }
            case QUADRATIC -> {
                double a = resolveCoeff(entity, 0, 0);
                double b = resolveCoeff(entity, 1, 0);
                double c = resolveCoeff(entity, 2, 0);
                yield a * x * x + b * x + c;
            }
            case EXPONENTIAL -> {
                double a = resolveCoeff(entity, 0, 1);
                double c = resolveCoeff(entity, 1, 0);
                double b = resolveBase(entity);
                yield a * Math.pow(b, x) + c;
            }
            case LOGARITHMIC -> {
                double a = resolveCoeff(entity, 0, 1);
                double c = resolveCoeff(entity, 1, 0);
                double b = resolveBase(entity);
                if (b <= 0 || b == 1) yield 0;
                yield a * (Math.log(x) / Math.log(b)) + c;
            }
            case POWER -> {
                double a = resolveCoeff(entity, 0, 0);
                double k = resolveCoeff(entity, 1, 1);
                double c = resolveCoeff(entity, 2, 0);
                yield a * Math.pow(x, k) + c;
            }
            case STEP -> {
                double a = resolveCoeff(entity, 0, 0);
                double b = resolveCoeff(entity, 1, 1);
                double c = resolveCoeff(entity, 2, 0);
                if (b <= 0) yield c;
                yield a * Math.floor(x / b) + c;
            }
            case SIGMOID -> {
                double a = resolveCoeff(entity, 0, 1);
                double b = resolveCoeff(entity, 1, 1);
                double c = resolveCoeff(entity, 2, 0);
                double d = resolveCoeff(entity, 3, 0);
                yield a / (1 + Math.exp(-b * (x - c))) + d;
            }
            case SINE -> {
                double a = resolveCoeff(entity, 0, 0);
                double t = resolveCoeff(entity, 1, 1);
                double p = resolveCoeff(entity, 2, 0);
                double c = resolveCoeff(entity, 3, 0);
                if (t <= 0) yield c;
                yield a * Math.sin(2 * Math.PI * (x - p) / t) + c;
            }
            case TRIANGLE -> {
                double a = resolveCoeff(entity, 0, 0);
                double t = resolveCoeff(entity, 1, 1);
                double p = resolveCoeff(entity, 2, 0);
                double c = resolveCoeff(entity, 3, 0);
                if (t <= 0) yield c;
                yield a * (2 / Math.PI) * Math.asin(Math.sin(2 * Math.PI * (x - p) / t)) + c;
            }
            case SQUARE -> {
                double a = resolveCoeff(entity, 0, 0);
                double t = resolveCoeff(entity, 1, 1);
                double p = resolveCoeff(entity, 2, 0);
                double c = resolveCoeff(entity, 3, 0);
                if (t <= 0) yield c;
                double s = Math.sin(2 * Math.PI * (x - p) / t);
                yield (s >= 0 ? a : -a) + c;
            }
            case SAWTOOTH -> {
                double a = resolveCoeff(entity, 0, 0);
                double t = resolveCoeff(entity, 1, 1);
                double p = resolveCoeff(entity, 2, 0);
                double c = resolveCoeff(entity, 3, 0);
                if (t <= 0) yield c;
                double u = (x - p) / t;
                yield a * (2 * (u - Math.floor(u)) - 1) + c;
            }
        };
    }

    // 系数：优先表达式，未填/失败回落数值字段
    private double resolveCoeff(LivingEntity entity, int index, double def) {
        double legacy = coefficients != null && index < coefficients.length ? coefficients[index] : def;
        if (coeffExpressions == null || index >= coeffExpressions.length) return legacy;
        String expr = coeffExpressions[index];
        return ExpressionEvaluator.resolve(entity, expr, legacy);
    }

    // 底数：为空/失败回落 base
    private double resolveBase(LivingEntity entity) {
        return ExpressionEvaluator.resolve(entity, baseExpression, base);
    }

    // 截至值：未填用老数值字段；填了但失败 → NaN
    private double resolveClip(LivingEntity entity, String expression, double legacy) {
        if (expression == null || expression.isBlank()) return legacy;
        return ExpressionEvaluator.resolve(entity, expression, Double.NaN);
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
        } else if (variableType == VariableType.SCOREBOARD_VALUE) {
            varName = scoreboardObjective.isEmpty()
                    ? Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.var.SCOREBOARD_VALUE").getString()
                    : scoreboardObjective;
        } else if (variableType == VariableType.POTION_LEVEL) {
            MobEffect sourceEffect = null;
            if (!sourcePotionId.isEmpty()) {
                sourceEffect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(sourcePotionId));
            }
            varName = sourceEffect != null
                    ? Component.translatable(sourceEffect.getDescriptionId()).getString()
                    : Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.var.POTION_LEVEL").getString();
        } else {
            varName = Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.var." + variableType.name()).getString();
        }
        return Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.display", name, varName).getString();
    }
}