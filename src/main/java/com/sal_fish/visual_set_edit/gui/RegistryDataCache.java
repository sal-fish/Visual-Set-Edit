package com.sal_fish.visual_set_edit.gui;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryDataCache {
    private static final Map<String, List<ResourceLocation>> cache = new ConcurrentHashMap<>();

    public static List<ResourceLocation> get(String type) {
        return cache.get(type);
    }

    public static void put(String type, List<ResourceLocation> list) {
        cache.put(type, list);
    }
}