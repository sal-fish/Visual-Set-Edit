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

    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '.' || c == '+' || c == '-';
            if (!allowed) return false;
        }
        return true;
    }

    public static boolean addObjective(String name) {
        if (!isValidName(name)) {
            VisualSetEdit.LOGGER.warn(
                    "Ignored invalid scoreboard objective name '{}' (only letters, digits, '_', '.', '+' and '-' are allowed)",
                    name);
            return false;
        }
        if (!objectives.contains(name)) {
            objectives.add(name);
            save();
        }
        return true;
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
            if (loaded == null) {
                objectives = new ArrayList<>();
                return;
            }
            // 过滤掉历史遗留/手改 JSON 产生的不合规名称，避免 GUI 列表构造
            // ResourceLocation 时崩溃；合规名去重保序。
            List<String> clean = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (String name : loaded) {
                if (name == null) continue;
                if (!isValidName(name)) {
                    VisualSetEdit.LOGGER.warn(
                            "Removed invalid scoreboard objective name '{}' from config (only letters, digits, '_', '.', '+' and '-' are allowed)",
                            name);
                    continue;
                }
                if (seen.add(name)) clean.add(name);
            }
            objectives = clean;
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
            // 防御性跳过（load/addObjective 已保证名单合规，此处双保险）
            if (!isValidName(name)) continue;
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