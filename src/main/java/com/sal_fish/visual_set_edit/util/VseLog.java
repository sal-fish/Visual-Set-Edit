package com.sal_fish.visual_set_edit.util;

import com.sal_fish.visual_set_edit.VisualSetEdit;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 同一 key 只输出一次的日志
public final class VseLog {
    private static final Set<String> ONCE_KEYS = ConcurrentHashMap.newKeySet();

    private VseLog() {}

    public static void warnOnce(String key, String message, Throwable t) {
        if (ONCE_KEYS.add(key)) {
            VisualSetEdit.LOGGER.warn(message, t);
        }
    }

    public static void warnOnce(String key, String message, Object... args) {
        if (ONCE_KEYS.add(key)) {
            VisualSetEdit.LOGGER.warn(message, args);
        }
    }
}
