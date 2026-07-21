package com.sal_fish.visual_set_edit.network;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RegistryDataServer {
    public static List<ResourceLocation> getRegistryIds(MinecraftServer server, String type) {
        List<ResourceLocation> ids = new ArrayList<>();
        switch (type) {
            case "dimension" -> {
                Registry<Level> reg = server.registryAccess().registryOrThrow(Registries.DIMENSION);
                ids.addAll(reg.keySet());
            }
            case "biome" -> {
                Registry<Biome> reg = server.registryAccess().registryOrThrow(Registries.BIOME);
                ids.addAll(reg.keySet());
            }
            case "structure" -> {
                ids.addAll(BuiltInRegistries.STRUCTURE_TYPE.keySet());
            }
        }
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        return ids;
    }
}