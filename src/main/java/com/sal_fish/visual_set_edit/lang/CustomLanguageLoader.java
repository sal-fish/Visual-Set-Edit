package com.sal_fish.visual_set_edit.lang;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.*;
import java.util.Map;

public class CustomLanguageLoader {
    public static void load() {
        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve("visual_set_edit/data/visual_set_edit/lang");
            if (!Files.exists(dir)) return;
            String code = Minecraft.getInstance().getLanguageManager().getSelected();
            Path file = dir.resolve(code + ".json");
            if (Files.exists(file)) {
                Map<String, String> map = new Gson().fromJson(Files.readString(file), new TypeToken<Map<String,String>>(){}.getType());
                Language.getInstance().getLanguageData().putAll(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}