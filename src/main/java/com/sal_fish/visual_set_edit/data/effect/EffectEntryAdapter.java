package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.*;
import com.sal_fish.visual_set_edit.util.VseLog;
import java.lang.reflect.Type;

public class EffectEntryAdapter implements JsonDeserializer<EffectEntry>, JsonSerializer<EffectEntry> {

    @Override
    public EffectEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String type = json.getAsJsonObject().get("type").getAsString();
        Class<? extends EffectEntry> clazz = EffectTypeRegistry.classOf(type);
        if (clazz == null) {
            // 缺少对应模组或类型未注册时保留占位，避免整个预设加载失败
            VseLog.warnOnce("vse.effect.unknown." + type, "[VSE] 未知效果类型", type);
            return new MissingEffectEntry(type);
        }
        return context.deserialize(json, clazz);
    }

    @Override
    public JsonElement serialize(EffectEntry src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src);
    }
}
