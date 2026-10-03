package com.sal_fish.visual_set_edit.data;

import java.util.List;

// 条件成立状态缓存，服务端算好写入、客户端读取
public final class ConditionStateCache {

    private static volatile byte[] bits;
    private static volatile int total = -1;

    private ConditionStateCache() {}

    // 条件总数，用于平坦下标
    public static int totalOf(List<Preset> presets) {
        int n = 0;
        for (Preset preset : presets) {
            for (SetPhase phase : preset.phases) n += phase.additionalConditions.size();
        }
        return n;
    }

    // 预设顺序 → 阶段顺序 → 条件顺序的平坦下标，两端必须一致
    public static int indexOf(List<Preset> presets, int presetIdx, int phaseIdx, int condIdx) {
        if (presetIdx < 0 || presetIdx >= presets.size()) return -1;
        int idx = 0;
        for (int i = 0; i < presetIdx; i++) {
            for (SetPhase phase : presets.get(i).phases) idx += phase.additionalConditions.size();
        }
        Preset target = presets.get(presetIdx);
        if (phaseIdx >= target.phases.size()) return -1;
        for (int i = 0; i < phaseIdx; i++) {
            idx += target.phases.get(i).additionalConditions.size();
        }
        int size = target.phases.get(phaseIdx).additionalConditions.size();
        return condIdx >= 0 && condIdx < size ? idx + condIdx : -1;
    }

    public static void update(int count, byte[] data) {
        total = count;
        bits = data;
    }

    public static void clear() {
        bits = null;
        total = -1;
    }

    // 无同步数据或预设列表长度不符时返回 null，调用方自行回落本地判定
    public static Boolean get(int flatIndex, int expectedTotal) {
        byte[] data = bits;
        if (data == null || flatIndex < 0 || total != expectedTotal) return null;
        if (flatIndex >> 3 >= data.length) return null;
        return (data[flatIndex >> 3] & (1 << (flatIndex & 7))) != 0;
    }
}
