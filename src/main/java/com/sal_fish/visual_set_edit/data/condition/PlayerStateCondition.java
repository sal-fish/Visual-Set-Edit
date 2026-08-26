package com.sal_fish.visual_set_edit.data.condition;

import com.google.gson.annotations.Expose;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public class PlayerStateCondition extends Condition {
    @Expose public String field;
    @Expose public String comparator;
    @Expose public String value;

    public PlayerStateCondition() { this.type = "player_state"; }

    @Override
    public boolean test(LivingEntity entity) {
        try {
            return switch (field) {
                case "HEALTH" -> comparePercentageOrAbsolute(
                        (int) entity.getHealth(), entity.getMaxHealth(), comparator, value);
                case "FOOD" -> entity instanceof Player player &&
                        comparePercentageOrAbsolute(
                                player.getFoodData().getFoodLevel(), 20, comparator, value);
                case "ARMOR" -> compareAbsolute(entity.getArmorValue(), comparator, value);
                case "XP_LEVEL" -> entity instanceof Player player &&
                        compareAbsolute(player.experienceLevel, comparator, value);
                case "HAS_EFFECT" -> {
                    MobEffect ef = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(value));
                    yield ef != null && entity.hasEffect(ef);
                }
                case "FALL_DISTANCE" -> compareAbsolute((int) entity.fallDistance, comparator, value);
                case "SUBMERGED" -> entity.isInWaterOrBubble();
                case "SNEAKING" -> entity.isCrouching();
                case "SPRINTING" -> entity.isSprinting();
                case "SWIMMING" -> entity.isSwimming();
                case "ON_GROUND" -> entity.onGround();
                case "ON_WALL" -> entity.onClimbable();
                case "FLYING" -> entity instanceof Player player && player.getAbilities().flying;
                case "SLEEPING" -> entity.isSleeping();
                case "RIDING" -> entity.isPassenger();
                case "TAG" -> {
                    boolean hasTag = entity.getTags().contains(value);
                    yield "NEQ".equals(comparator) != hasTag;
                }
                case "IS_HURT" -> {
                    int windowSec = 0;
                    float threshold = 0;
                    if (value != null && !value.isEmpty()) {
                        String[] parts = value.split(",");
                        if (parts.length >= 1 && !parts[0].isEmpty()) {
                            windowSec = Integer.parseInt(parts[0].trim());
                        }
                        if (parts.length >= 2 && !parts[1].isEmpty()) {
                            threshold = Float.parseFloat(parts[1].trim());
                        }
                    }

                    Long lastHurt = com.sal_fish.visual_set_edit.event.SetEventHandler.getLastHurtTick(entity);
                    if (lastHurt == null) yield false;

                    // 时间窗口检查
                    if (windowSec > 0) {
                        long currentTick = entity.level().getGameTime();
                        if ((currentTick - lastHurt) > windowSec * 20L) yield false;
                    }

                    // 伤害阈值检查
                    if (threshold > 0) {
                        Float amount = com.sal_fish.visual_set_edit.event.SetEventHandler.getLastHurtAmount(entity);
                        if (amount == null || amount < threshold) yield false;
                    }

                    yield true;
                }
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    //工具方法
    /** 支持 "50%" 格式的百分比比较，也支持纯数字的绝对值比较 */
    private boolean comparePercentageOrAbsolute(int current, float max, String comp, String val) {
        if (val.endsWith("%")) {
            int percent = Integer.parseInt(val.substring(0, val.length() - 1));
            float currentPercent = (max > 0) ? (current / max) * 100 : 0;
            return compareFloat(currentPercent, comp, percent);
        } else {
            return compareAbsolute(current, comp, val);
        }
    }

    /** 整数绝对值比较 */
    private boolean compareAbsolute(int actual, String comp, String val) {
        int target = Integer.parseInt(val);
        return switch (comp) {
            case "EQ" -> actual == target;
            case "NEQ" -> actual != target;
            case "GT" -> actual > target;
            case "LT" -> actual < target;
            case "GTE" -> actual >= target;
            case "LTE" -> actual <= target;
            default -> false;
        };
    }

    /** 浮点数比较，用于百分比 */
    private boolean compareFloat(float actual, String comp, float target) {
        return switch (comp) {
            case "EQ" -> actual == target;
            case "NEQ" -> actual != target;
            case "GT" -> actual > target;
            case "LT" -> actual < target;
            case "GTE" -> actual >= target;
            case "LTE" -> actual <= target;
            default -> false;
        };
    }

    @Override
    public String getDisplayText() {
        return field + " " + comparator + " " + value;
    }
}