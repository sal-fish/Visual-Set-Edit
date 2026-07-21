package com.sal_fish.visual_set_edit.util;

import net.minecraft.nbt.CompoundTag;

public class NbtMatcher {
    public static boolean equals(CompoundTag a, CompoundTag b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b); // simplistic, full deep compare could be used
    }
}