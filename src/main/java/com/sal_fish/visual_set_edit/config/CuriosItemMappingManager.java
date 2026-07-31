package com.sal_fish.visual_set_edit.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sal_fish.visual_set_edit.VisualSetEdit;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class CuriosItemMappingManager {

    private static final Gson GSON = new Gson();
    private static final Path MAPPING_PATH = FMLPaths.CONFIGDIR.get().resolve("visual_set_edit/curios_item_mappings.json");

    // 每个物品ID对应一个注册记录列表，每条记录有独立的槽位、配置、NBT
    private static final Map<String, List<RegisteredEntry>> registry = new LinkedHashMap<>();

    // 数据模型

    public static class RegisteredEntry {
        public final List<String> slots;
        public final boolean canQuickEquip;
        public final boolean canRemove;
        public final String nbt; // null 或 "" 表示不限 NBT

        public RegisteredEntry(List<String> slots, boolean canQuickEquip, boolean canRemove, String nbt) {
            this.slots = slots != null ? new ArrayList<>(slots) : new ArrayList<>();
            this.canQuickEquip = canQuickEquip;
            this.canRemove = canRemove;
            this.nbt = (nbt != null && !nbt.isEmpty()) ? nbt : null;
        }

        public boolean matchesNbt(CompoundTag stackNbt) {
            if (nbt == null) return true;
            if (stackNbt == null) return false;
            try {
                CompoundTag savedTag = net.minecraft.nbt.TagParser.parseTag(nbt);
                return savedTag.equals(stackNbt);
            } catch (Exception e) {
                return stackNbt.toString().equals(nbt);
            }
        }

        // 用于在列表中显示时标识这个条目
        public String getNbtSummary() {
            if (nbt == null || nbt.isEmpty()) return null;
            // 提取简短的标识，避免太长
            if (nbt.length() > 30) {
                return nbt.substring(0, 27) + "...";
            }
            return nbt;
        }
    }

    // 公开查询方法

    /**
     * 获取指定物品 ID 的所有注册记录（不可修改）
     */
    public static List<RegisteredEntry> getEntries(String itemId) {
        List<RegisteredEntry> entries = registry.get(itemId);
        return entries != null ? Collections.unmodifiableList(entries) : Collections.emptyList();
    }

    /**
     * 获取与给定 NBT 匹配的所有注册记录
     */
    public static List<RegisteredEntry> getMatchingEntries(String itemId, CompoundTag stackNbt) {
        List<RegisteredEntry> result = new ArrayList<>();
        List<RegisteredEntry> entries = registry.get(itemId);
        if (entries != null) {
            for (RegisteredEntry entry : entries) {
                if (entry.matchesNbt(stackNbt)) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    /**
     * 获取该物品注册的所有槽位（不考虑 NBT，返回所有条目的槽位合并）
     * 用于向后兼容，例如工具提示中显示所有可能的槽位
     */
    public static List<String> getSlotsForItem(String itemId) {
        List<String> allSlots = new ArrayList<>();
        List<RegisteredEntry> entries = registry.get(itemId);
        if (entries != null) {
            for (RegisteredEntry entry : entries) {
                for (String slot : entry.slots) {
                    if (!allSlots.contains(slot)) {
                        allSlots.add(slot);
                    }
                }
            }
        }
        return allSlots;
    }

    /**
     * 获取与给定 NBT 匹配的所有槽位（合并）
     */
    public static List<String> getSlotsForItem(String itemId, CompoundTag stackNbt) {
        List<String> allSlots = new ArrayList<>();
        List<RegisteredEntry> entries = getMatchingEntries(itemId, stackNbt);
        for (RegisteredEntry entry : entries) {
            for (String slot : entry.slots) {
                if (!allSlots.contains(slot)) {
                    allSlots.add(slot);
                }
            }
        }
        return allSlots;
    }

    /**
     * 获取所有已注册的物品 ID（用于列表展示）
     */
    public static Set<String> getRegisteredItemIds() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    /**
     * 获取整个注册表（只读）
     */
    public static Map<String, List<RegisteredEntry>> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }

    /**
     * 添加一条注册记录。
     */
    public static void addEntry(String itemId, List<String> slots, boolean canQuickEquip, boolean canRemove, String nbt) {
        List<RegisteredEntry> entries = registry.computeIfAbsent(itemId, k -> new ArrayList<>());
        entries.add(new RegisteredEntry(slots, canQuickEquip, canRemove, nbt));
        save();
    }

    /**
     * 删除某个物品的特定记录（根据索引）
     */
    public static void removeEntry(String itemId, int index) {
        List<RegisteredEntry> entries = registry.get(itemId);
        if (entries != null && index >= 0 && index < entries.size()) {
            entries.remove(index);
            if (entries.isEmpty()) {
                registry.remove(itemId);
            }
            save();
        }
    }

    /**
     * 删除某个物品的所有记录
     */
    public static void removeAllEntries(String itemId) {
        registry.remove(itemId);
        save();
    }

    /**
     * 清空某个物品的旧记录并添加一条新记录（用于编辑界面直接覆盖）
     */
    public static void replaceEntries(String itemId, List<RegisteredEntry> newEntries) {
        if (newEntries == null || newEntries.isEmpty()) {
            registry.remove(itemId);
        } else {
            registry.put(itemId, new ArrayList<>(newEntries));
        }
        save();
    }

    // 持久化
    public static void load() {
        try {
            if (Files.notExists(MAPPING_PATH)) {
                registry.clear();
                return;
            }
            String json = Files.readString(MAPPING_PATH);

            // 1. 尝试解析新格式：Map<String, List<RegisteredEntryDTO>>
            try {
                Map<String, List<RegisteredEntryDTO>> loaded = GSON.fromJson(json,
                        new TypeToken<Map<String, List<RegisteredEntryDTO>>>(){}.getType());
                if (loaded != null) {
                    registry.clear();
                    for (Map.Entry<String, List<RegisteredEntryDTO>> itemEntry : loaded.entrySet()) {
                        String itemId = itemEntry.getKey();
                        List<RegisteredEntry> entries = new ArrayList<>();
                        for (RegisteredEntryDTO dto : itemEntry.getValue()) {
                            entries.add(new RegisteredEntry(
                                    dto.slots,
                                    dto.canQuickEquip == null || dto.canQuickEquip,
                                    dto.canRemove == null || dto.canRemove,
                                    dto.nbt
                            ));
                        }
                        if (!entries.isEmpty()) {
                            registry.put(itemId, entries);
                        }
                    }
                    VisualSetEdit.LOGGER.info("Loaded {} curios item mappings ({} items).", countAllEntries(), registry.size());
                    return;
                }
            } catch (Exception ignored) {
                // 不是新格式，尝试旧格式
            }

            // 2. 回退尝试旧格式
            try {
                Map<String, OldEntryDTO> oldLoaded = GSON.fromJson(json,
                        new TypeToken<Map<String, OldEntryDTO>>(){}.getType());
                registry.clear();
                if (oldLoaded != null) {
                    for (Map.Entry<String, OldEntryDTO> itemEntry : oldLoaded.entrySet()) {
                        String itemId = itemEntry.getKey();
                        OldEntryDTO dto = itemEntry.getValue();
                        List<RegisteredEntry> entries = new ArrayList<>();
                        entries.add(new RegisteredEntry(
                                dto.slots != null ? dto.slots : new ArrayList<>(),
                                dto.canQuickEquip == null || dto.canQuickEquip,
                                dto.canRemove == null || dto.canRemove,
                                dto.nbt
                        ));
                        registry.put(itemId, entries);
                    }
                }
                save(); // 保存为新格式
                VisualSetEdit.LOGGER.info("Converted old format to new, loaded {} curios item mappings ({} items).",
                        countAllEntries(), registry.size());
            } catch (Exception e) {
                VisualSetEdit.LOGGER.error("Failed to load curios item mappings as both old and new format", e);
                registry.clear();
            }
        } catch (Exception e) {
            VisualSetEdit.LOGGER.error("Failed to load curios item mappings", e);
            registry.clear();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(MAPPING_PATH.getParent());
            Map<String, List<RegisteredEntryDTO>> toSave = new LinkedHashMap<>();
            for (Map.Entry<String, List<RegisteredEntry>> entry : registry.entrySet()) {
                List<RegisteredEntryDTO> dtos = new ArrayList<>();
                for (RegisteredEntry reg : entry.getValue()) {
                    dtos.add(new RegisteredEntryDTO(reg.slots, reg.canQuickEquip, reg.canRemove, reg.nbt));
                }
                toSave.put(entry.getKey(), dtos);
            }
            String json = GSON.toJson(toSave);
            Files.writeString(MAPPING_PATH, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            VisualSetEdit.LOGGER.error("Failed to save curios item mappings", e);
        }
    }

    private static int countAllEntries() {
        int count = 0;
        for (List<RegisteredEntry> list : registry.values()) {
            count += list.size();
        }
        return count;
    }

    // 内部 DTO 用于 JSON 映射
    private static class RegisteredEntryDTO {
        List<String> slots;
        Boolean canQuickEquip;
        Boolean canRemove;
        String nbt;

        RegisteredEntryDTO(List<String> slots, Boolean canQuickEquip, Boolean canRemove, String nbt) {
            this.slots = slots;
            this.canQuickEquip = canQuickEquip;
            this.canRemove = canRemove;
            this.nbt = nbt;
        }
    }

    // 旧格式的单条记录（用于兼容转换）
    private static class OldEntryDTO {
        List<String> slots;
        Boolean canQuickEquip;
        Boolean canRemove;
        String nbt;
    }
}