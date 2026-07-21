package com.sal_fish.visual_set_edit.event;

import com.sal_fish.visual_set_edit.data.SetPhase;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveSetTracker {
    private static final Map<UUID, List<ActivePhase>> activeMap = new ConcurrentHashMap<>();

    public record ActivePhase(String presetId, int phaseIndex, SetPhase phase) {}

    public static void setActivePhases(LivingEntity entity, List<ActivePhase> phases) {
        if (phases.isEmpty()) {
            activeMap.remove(entity.getUUID());
        } else {
            activeMap.put(entity.getUUID(), new ArrayList<>(phases));
        }
    }

    public static List<ActivePhase> getActivePhases(LivingEntity entity) {
        return activeMap.getOrDefault(entity.getUUID(), Collections.emptyList());
    }

    public static void removeEntity(LivingEntity entity) {
        activeMap.remove(entity.getUUID());
    }

    public static void clearAll() {
        activeMap.clear();
    }
}