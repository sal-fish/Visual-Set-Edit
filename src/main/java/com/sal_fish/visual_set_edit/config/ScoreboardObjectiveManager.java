package com.sal_fish.visual_set_edit.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sal_fish.visual_set_edit.VisualSetEdit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ScoreboardObjectiveManager {
    private static final Gson GSON = new Gson();
    private static final Path OBJECTIVES_FILE = FMLPaths.CONFIGDIR.get()
            .resolve("visual_set_edit/scoreboard_objectives.json");

    private static List<String> objectives = new ArrayList<>();

    public static List<String> getObjectives() {
        return Collections.unmodifiableList(objectives);
    }

    public static void addObjective(String name) {
        if (name != null && !name.isEmpty() && !objectives.contains(name)) {
            objectives.add(name);
            save();
        }
    }

    public static void removeObjective(String name) {
        if (objectives.remove(name)) {
            save();
        }
    }

    public static void load() {
        try {
            if (Files.notExists(OBJECTIVES_FILE)) {
                objectives.clear();
                return;
            }
            String json = Files.readString(OBJECTIVES_FILE);
            List<String> loaded = GSON.fromJson(json, new TypeToken<List<String>>(){}.getType());
            objectives = loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
            // 去重
            Set<String> set = new LinkedHashSet<>(objectives);
            objectives = new ArrayList<>(set);
        } catch (Exception e) {
            VisualSetEdit.LOGGER.error("Failed to load scoreboard objectives", e);
            objectives.clear();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(OBJECTIVES_FILE.getParent());
            String json = GSON.toJson(objectives);
            Files.writeString(OBJECTIVES_FILE, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            VisualSetEdit.LOGGER.error("Failed to save scoreboard objectives", e);
        }
    }

    //在服务器上注册所有计分板目标，若已存在则跳过。
    public static void registerObjectives(MinecraftServer server) {
        if (server == null) return;
        var scoreboard = server.getScoreboard();
        for (String name : objectives) {
            if (scoreboard.getObjective(name) == null) {
                scoreboard.addObjective(
                        name,
                        ObjectiveCriteria.DUMMY,
                        Component.literal(name),
                        ObjectiveCriteria.RenderType.INTEGER
                );
            }
        }
    }
}