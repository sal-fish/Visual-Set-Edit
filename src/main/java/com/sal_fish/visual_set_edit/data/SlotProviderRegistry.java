package com.sal_fish.visual_set_edit.data;

import com.sal_fish.visual_set_edit.api.SlotProvider;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 额外槽位来源注册表，双端
public final class SlotProviderRegistry {

    public static final String ANY_SUFFIX = ":any";

    private static final Map<String, SlotProvider> PROVIDERS = new LinkedHashMap<>();

    private SlotProviderRegistry() {}

    public static void register(SlotProvider provider) {
        if (provider == null || provider.id() == null || provider.id().isEmpty()) return;
        PROVIDERS.putIfAbsent(provider.id(), provider);
    }

    public static List<SlotProvider> all() {
        return new ArrayList<>(PROVIDERS.values());
    }

    public static SlotProvider get(String id) {
        return id == null ? null : PROVIDERS.get(id);
    }

    public static String providerIdOf(String slotKey) {
        int i = slotKey == null ? -1 : slotKey.indexOf(':');
        return i <= 0 ? null : slotKey.substring(0, i);
    }

    public static String slotIdOf(String slotKey) {
        int i = slotKey == null ? -1 : slotKey.indexOf(':');
        return i <= 0 ? slotKey : slotKey.substring(i + 1);
    }

    // 是否属于某个已注册来源
    public static boolean isProviderKey(String slotKey) {
        return get(providerIdOf(slotKey)) != null;
    }

    public static boolean isAnyKey(String slotKey) {
        return slotKey != null && slotKey.endsWith(ANY_SUFFIX) && isProviderKey(slotKey);
    }

    // 指定来源的槽位键
    public static List<String> slotKeysOf(LivingEntity entity, String providerId) {
        SlotProvider provider = get(providerId);
        if (provider == null) return Collections.emptyList();
        List<String> ids = provider.slotIds(entity);
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        List<String> keys = new ArrayList<>(ids.size());
        for (String id : ids) {
            keys.add(providerId + ":" + id);
        }
        return keys;
    }

    // 全部来源的槽位键
    public static List<String> allSlotKeys(LivingEntity entity) {
        List<String> keys = new ArrayList<>();
        for (SlotProvider provider : PROVIDERS.values()) {
            List<String> ids = provider.slotIds(entity);
            if (ids != null) {
                for (String id : ids) {
                    keys.add(provider.id() + ":" + id);
                }
            }
            keys.add(provider.id() + ANY_SUFFIX);
        }
        return keys;
    }
}
