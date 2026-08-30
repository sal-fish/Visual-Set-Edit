package com.sal_fish.visual_set_edit.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sal_fish.visual_set_edit.VisualSetEdit;
import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.SlotCondition;
import com.sal_fish.visual_set_edit.data.condition.Condition;
import com.sal_fish.visual_set_edit.data.condition.ConditionAdapter;
import com.sal_fish.visual_set_edit.data.condition.EnvironmentCondition;
import com.sal_fish.visual_set_edit.data.effect.EffectEntry;
import com.sal_fish.visual_set_edit.data.effect.EffectEntryAdapter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class PresetManager {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(EffectEntry.class, new EffectEntryAdapter())
            .registerTypeAdapter(Condition.class, new ConditionAdapter())
            .serializeSpecialFloatingPointValues()
            .setPrettyPrinting()
            .create();

    private static final Path OLD_PRESETS_FILE = FMLPaths.CONFIGDIR.get().resolve("visual_set_edit/presets.json");
    private static final Path PRESETS_DIR = FMLPaths.CONFIGDIR.get().resolve("visual_set_edit/presets");

    private static List<Preset> presets = new ArrayList<>();
    public static List<Preset> clientPresets = new ArrayList<>();

    private static final Map<String, Set<String>> ITEM_TO_PRESETS = new HashMap<>();
    public static final Set<String> ZERO_COUNT_PRESET_IDS = new HashSet<>();
    public static final Set<String> ZERO_COUNT_PRESET_IDS_NON_PLAYER = new HashSet<>();
    private static final Map<String, Preset> PRESET_BY_ID = new HashMap<>();
    public static final Set<String> TIME_SENSITIVE_PHASE_IDS = new HashSet<>();

    public static List<Preset> getPresets() {
        return Collections.unmodifiableList(presets);
    }

    public static Preset getPresetById(String id) {
        return PRESET_BY_ID.get(id);
    }

    public static Set<String> getPresetIdsForItem(String itemId) {
        Set<String> ids = ITEM_TO_PRESETS.get(itemId);
        return ids != null ? ids : Collections.emptySet();
    }

    //加载
    public static void loadPresets() {
        List<Preset> loaded = new ArrayList<>();

        // 1. 优先从新目录加载
        if (Files.exists(PRESETS_DIR)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(PRESETS_DIR, "*.json")) {
                for (Path file : stream) {
                    try {
                        String json = Files.readString(file);
                        Preset preset = GSON.fromJson(json, Preset.class);
                        if (preset != null) {
                            preset.initAfterLoad();
                            loaded.add(preset);
                        }
                    } catch (Exception e) {
                        VisualSetEdit.LOGGER.error("Failed to load preset from {}", file, e);
                    }
                }
            } catch (IOException e) {
                VisualSetEdit.LOGGER.error("Failed to read presets directory", e);
            }
        }

        if (loaded.isEmpty() && Files.exists(OLD_PRESETS_FILE)) {
            try {
                String json = Files.readString(OLD_PRESETS_FILE);
                List<Preset> oldPresets = GSON.fromJson(json, new TypeToken<List<Preset>>(){}.getType());
                if (oldPresets != null) {
                    loaded = oldPresets;
                    presets = loaded;
                    savePresets(presets);
                    Files.delete(OLD_PRESETS_FILE);
                }
            } catch (Exception e) {
                VisualSetEdit.LOGGER.error("Failed to load old presets file", e);
            }
        }

        presets = loaded;
        for (Preset p : presets) p.initAfterLoad();
        rebuildIndex();
        VisualSetEdit.LOGGER.info("VSE loaded {} presets.", presets.size());
    }

    //保存
    public static void savePresets(List<Preset> newPresets) {
        try {
            Files.createDirectories(PRESETS_DIR);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(PRESETS_DIR, "*.json")) {
                for (Path file : stream) {
                    Files.delete(file);
                }
            } catch (IOException e) {
                VisualSetEdit.LOGGER.error("Failed to clean presets directory", e);
            }

            for (Preset preset : newPresets) {
                String json = GSON.toJson(preset);
                Path file = PRESETS_DIR.resolve(preset.id + ".json");
                Files.writeString(file, json, StandardCharsets.UTF_8);
            }
            /*
            String fullJson = GSON.toJson(newPresets);
            Files.writeString(OLD_PRESETS_FILE, fullJson, StandardCharsets.UTF_8);
            */

            presets = new ArrayList<>(newPresets);
            for (Preset p : presets) p.initAfterLoad();
            rebuildIndex();
            VisualSetEdit.LOGGER.info("VSE presets saved.");
        } catch (IOException e) {
            VisualSetEdit.LOGGER.error("VSE preset saving failed", e);
        }
    }

    public static void clientCachePresets(List<Preset> list) {
        clientPresets = list != null ? list : new ArrayList<>();
        for (Preset p : clientPresets) p.initAfterLoad();
        rebuildIndex();
    }

    //索引重建
    public static void rebuildIndex() {
        ITEM_TO_PRESETS.clear();
        ZERO_COUNT_PRESET_IDS.clear();
        ZERO_COUNT_PRESET_IDS_NON_PLAYER.clear();
        PRESET_BY_ID.clear();
        TIME_SENSITIVE_PHASE_IDS.clear();

        // 填充 PRESET_BY_ID 并检查零件套和时间敏感阶段
        for (Preset preset : presets) {
            PRESET_BY_ID.put(preset.id, preset);

            boolean isZeroCount = false;
            for (int i = 0; i < preset.phases.size(); i++) {
                SetPhase phase = preset.phases.get(i);
                if (phase.requiredCount <= 0) {
                    isZeroCount = true;
                }
                // 检查时间敏感条件
                boolean hasTimeCondition = false;
                for (Condition cond : phase.additionalConditions) {
                    if (cond instanceof EnvironmentCondition env && "TIME".equals(env.field)) {
                        hasTimeCondition = true;
                        break;
                    }
                }
                if (hasTimeCondition) {
                    TIME_SENSITIVE_PHASE_IDS.add(preset.id + ":" + i);
                }
            }
            if (isZeroCount) {
                ZERO_COUNT_PRESET_IDS.add(preset.id);
                boolean nonPlayerPossible = false;
                for (SetPhase phase : preset.phases) {
                    boolean blocked = false;
                    if (phase.additionalConditions != null) {
                        for (Condition cond : phase.additionalConditions) {
                            if (cond.requiresPlayer()) { blocked = true; break; }
                        }
                    }
                    if (!blocked) { nonPlayerPossible = true; break; }
                }
                if (nonPlayerPossible) {
                    ZERO_COUNT_PRESET_IDS_NON_PLAYER.add(preset.id);
                }
            }

            // 构建物品索引（原有逻辑）
            for (SlotCondition cond : preset.getAllSlotConditions()) {
                if (cond.itemId != null && !cond.itemId.isEmpty()
                        && (cond.tagId == null || cond.tagId.isEmpty())) {
                    ITEM_TO_PRESETS
                            .computeIfAbsent(cond.itemId, k -> new HashSet<>())
                            .add(preset.id);
                } else if (cond.tagId != null && !cond.tagId.isEmpty()) {
                    ResourceLocation tagRl = ResourceLocation.tryParse(cond.tagId);
                    if (tagRl != null) {
                        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagRl);
                        ITag<Item> tag = Objects.requireNonNull(ForgeRegistries.ITEMS.tags()).getTag(tagKey);
                        if (tag != null) {
                            tag.forEach(item -> {
                                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                                if (itemId != null) {
                                    ITEM_TO_PRESETS
                                            .computeIfAbsent(itemId.toString(), k -> new HashSet<>())
                                            .add(preset.id);
                                }
                            });
                        }
                    }
                }
            }
        }
    }
}