package com.sal_fish.visual_set_edit.data.condition;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.Map;

public class ConditionAdapter implements JsonDeserializer<Condition>, JsonSerializer<Condition> {
    private static final Map<String, Type> REGISTRY = Map.of(
            "composite", CompositeCondition.class,
            "environment", EnvironmentCondition.class,
            "player_state", PlayerStateCondition.class,
            "inventory", InventoryCondition.class,
            "iron_spell", IronSpellCondition.class,
            "attribute", AttributeCondition.class
    );

    @Override
    public Condition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String type = json.getAsJsonObject().get("type").getAsString();
        Type clazz = REGISTRY.get(type);
        if (clazz == null) throw new JsonParseException("Unknown condition type: " + type);
        return context.deserialize(json, clazz);
    }

    @Override
    public JsonElement serialize(Condition src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src);
    }
}
