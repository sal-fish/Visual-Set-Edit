package com.sal_fish.visual_set_edit.util;

import net.minecraftforge.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Locale;

public final class SearchUtil {
    private static final String JEC_MATCH_CLASS = "me.towdium.jecharacters.utils.Match";

    private static MethodHandle jecContains;
    private static boolean jecResolved;

    private SearchUtil() {}

    public static boolean contains(String text, String query) {
        if (query == null || query.isEmpty()) return true;
        if (text == null) return false;
        String t = text.toLowerCase(Locale.ROOT);
        String q = query.toLowerCase(Locale.ROOT);
        if (t.contains(q)) return true;
        MethodHandle handle = jecHandle();
        if (handle == null || !hasHan(text) || !hasLatinLetter(q)) return false;
        try {
            return (boolean) handle.invokeExact(text, (CharSequence) q);
        } catch (Throwable e) {
            VseLog.warnOnce("vse.search.pinyin", "[VSE] 拼音搜索调用失败，已回退为普通搜索", e);
            jecContains = null;
            return false;
        }
    }

    private static MethodHandle jecHandle() {
        if (!jecResolved) {
            jecResolved = true;
            if (ModList.get().isLoaded("jecharacters")) {
                try {
                    Class<?> match = Class.forName(JEC_MATCH_CLASS);
                    jecContains = MethodHandles.publicLookup().findStatic(match, "contains",
                            MethodType.methodType(boolean.class, String.class, CharSequence.class));
                } catch (Throwable e) {
                    VseLog.warnOnce("vse.search.pinyin.init", "[VSE] 拼音搜索初始化失败，已回退为普通搜索", e);
                }
            }
        }
        return jecContains;
    }

    private static boolean hasHan(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF) || (c >= 0xF900 && c <= 0xFAFF)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLatinLetter(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z') return true;
        }
        return false;
    }
}
