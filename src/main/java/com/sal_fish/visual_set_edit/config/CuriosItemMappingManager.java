package com.sal_fish.visual_set_edit.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sal_fish.visual_set_edit.VisualSetEdit;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class CuriosItemMappingManager {

    private static final Gson GSON = new Gson();
    private static final Path MAPPING_PATH = FMLPaths.CONFIGDIR.get().resolve("visual_set_edit/curios_item_mappings.json");

    private static Map<String, List<String>> mappings = new HashMap<>();
    private static Map<String, CuriosItemConfig> configs = new HashMap<>();

    public static Map<String, List<String>> getMappings() {
        return Collections.unmodifiableMap(mappings);
    }

    public static CuriosItemConfig getConfig(String itemId) {
        return configs.getOrDefault(itemId, CuriosItemConfig.DEFAULT);
    }

    public static void addMapping(String itemId, List<String> slotIds, boolean canQuickEquip, boolean canRemove) {
        mappings.put(itemId, new ArrayList<>(slotIds));
        configs.put(itemId, new CuriosItemConfig(canQuickEquip, canRemove));
        save();
    }

    public static void addMapping(String itemId, List<String> slotIds) {
        addMapping(itemId, slotIds, true, true);
    }

    public static void removeMapping(String itemId) {
        mappings.remove(itemId);
        configs.remove(itemId);
        save();
    }

    public static List<String> getSlotsForItem(String itemId) {
        return mappings.getOrDefault(itemId, Collections.emptyList());
    }

    public static void load() {
        try {
            if (Files.notExists(MAPPING_PATH)) {
                mappings = new HashMap<>();
                configs = new HashMap<>();
                return;
            }
            String json = Files.readString(MAPPING_PATH);
            Map<String, CuriosItemMapping> loaded = GSON.fromJson(json, new TypeToken<Map<String, CuriosItemMapping>>(){}.getType());
            mappings = new HashMap<>();
            configs = new HashMap<>();
            if (loaded != null) {
                for (var entry : loaded.entrySet()) {
                    CuriosItemMapping mapping = entry.getValue();
                    mappings.put(entry.getKey(), mapping.slots != null ? mapping.slots : List.of());
                    configs.put(entry.getKey(), new CuriosItemConfig(
                            mapping.canQuickEquip == null || mapping.canQuickEquip,
                            mapping.canRemove == null || mapping.canRemove
                    ));
                }
            }
            VisualSetEdit.LOGGER.info("Loaded {} curios item mappings.", mappings.size());
        } catch (Exception e) {
            VisualSetEdit.LOGGER.error("Failed to load curios item mappings", e);
            mappings = new HashMap<>();
            configs = new HashMap<>();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(MAPPING_PATH.getParent());
            Map<String, CuriosItemMapping> toSave = new HashMap<>();
            for (var entry : mappings.entrySet()) {
                CuriosItemConfig config = configs.get(entry.getKey());
                toSave.put(entry.getKey(), new CuriosItemMapping(
                        entry.getValue(),
                        config == null || config.canQuickEquip,
                        config == null || config.canRemove
                ));
            }
            String json = GSON.toJson(toSave);
            Files.writeString(MAPPING_PATH, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            VisualSetEdit.LOGGER.error("Failed to save curios item mappings", e);
        }
    }

    public static class CuriosItemConfig {
        public static final CuriosItemConfig DEFAULT = new CuriosItemConfig(true, true);
        public final boolean canQuickEquip;
        public final boolean canRemove;

        public CuriosItemConfig(boolean canQuickEquip, boolean canRemove) {
            this.canQuickEquip = canQuickEquip;
            this.canRemove = canRemove;
        }
    }

    private static class CuriosItemMapping {
        public List<String> slots;
        public Boolean canQuickEquip;
        public Boolean canRemove;

        public CuriosItemMapping(List<String> slots, Boolean canQuickEquip, Boolean canRemove) {
            this.slots = slots;
            this.canQuickEquip = canQuickEquip;
            this.canRemove = canRemove;
        }
    }
}