package com.sal_fish.visual_set_edit.data.condition;

import com.google.gson.*;
import com.sal_fish.visual_set_edit.util.VseLog;
import java.lang.reflect.Type;

public class ConditionAdapter implements JsonDeserializer<Condition>, JsonSerializer<Condition> {

    @Override
    public Condition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String type = json.getAsJsonObject().get("type").getAsString();
        Class<? extends Condition> clazz = ConditionTypeRegistry.classOf(type);
        if (clazz == null) {
            // 缺少对应模组或类型未注册时保留占位，避免整个预设加载失败
            VseLog.warnOnce("vse.condition.unknown." + type, "[VSE] 未知条件类型: " + type);
            return new MissingCondition(type);
        }
        return context.deserialize(json, clazz);
    }

    @Override
    public JsonElement serialize(Condition src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src);
    }
}
