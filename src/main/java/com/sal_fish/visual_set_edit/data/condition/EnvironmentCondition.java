package com.sal_fish.visual_set_edit.data.condition;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class EnvironmentCondition extends Condition {
    @Expose public String field; // LIGHT_SKY,LIGHT_BLOCK,DIMENSION,BIOME,Y,WEATHER,MOON_PHASE,TIME,STRUCTURE,TEMPERATURE
    @Expose public String comparator; // EQ,GT,LT,GTE,LTE
    @Expose public String value;

    public EnvironmentCondition() { this.type = "environment"; }

    @Override
    public boolean test(LivingEntity entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        try {
        return switch (field) {
            case "LIGHT_SKY" -> compare(level.getBrightness(LightLayer.SKY, pos), comparator, Integer.parseInt(value));
            case "LIGHT_BLOCK" ->
                    compare(level.getBrightness(LightLayer.BLOCK, pos), comparator, Integer.parseInt(value));
            case "DIMENSION" -> level.dimension().location().toString().equals(value);
            case "BIOME" ->
                    level.getBiome(pos).unwrapKey().map(k -> k.location().toString().equals(value)).orElse(false);
            case "Y" -> compare(pos.getY(), comparator, Integer.parseInt(value));
            case "WEATHER" -> {
                boolean rain = level.isRaining();
                yield switch (value) {
                    case "RAIN" -> rain;
                    case "THUNDER" -> level.isThundering();
                    case "CLEAR" -> !rain;
                    default -> false;
                };
            }
            case "MOON_PHASE" -> compare(level.getMoonPhase(), comparator, Integer.parseInt(value));
            case "TIME" -> {
                int time = (int) (level.getDayTime() % 24000);
                yield compare(time, comparator, Integer.parseInt(value));
            }
            case "TEMPERATURE" -> {
                float temp = level.getBiome(pos).get().getBaseTemperature();
                yield compare((int) (temp * 100), comparator, Integer.parseInt(value));
            }
            case "L2H_CHUNK_DIFFICULTY" -> compare(IntegrationManager.getL2ChunkDifficulty(entity), comparator, Integer.parseInt(value));
            case "L2H_PLAYER_DIFFICULTY" -> compare(IntegrationManager.getL2PlayerDifficulty(entity), comparator, Integer.parseInt(value));
            default -> false;
        };
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean compare(int actual, String comp, int val) {
        return switch (comp) {
            case "EQ" -> actual == val;
            case "GT" -> actual > val;
            case "LT" -> actual < val;
            case "GTE" -> actual >= val;
            case "LTE" -> actual <= val;
            default -> false;
        };
    }

    @Override
    public String getDisplayText() { return field + " " + comparator + " " + value; }
}