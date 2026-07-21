package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.Map;

public class EffectEntryAdapter implements JsonDeserializer<EffectEntry>, JsonSerializer<EffectEntry> {
    private static final Map<String, Type> REGISTRY = Map.ofEntries(
            Map.entry("potion", PotionEffectEntry.class),
            Map.entry("attribute", AttributeEffectEntry.class),
            Map.entry("iron_spell", IronSpellEffectEntry.class),
            Map.entry("ability", AbilityEffectEntry.class),
            Map.entry("command", CommandEffectEntry.class),
            Map.entry("slot_count", SlotCountEffectEntry.class),
            Map.entry("spell_level_boost", SpellLevelBoostEffectEntry.class),
            Map.entry("l2hostility_trait", L2HostilityTraitEffectEntry.class),
            Map.entry("dynamic_attribute", DynamicAttributeEffectEntry.class),
            Map.entry("l2_difficulty_mod", L2DifficultyModEffectEntry.class),
            Map.entry("tag", TagEffectEntry.class)
    );


    @Override
    public EffectEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String type = json.getAsJsonObject().get("type").getAsString();
        Type clazz = REGISTRY.get(type);
        if (clazz == null) throw new JsonParseException("Unknown effect type: " + type);
        return context.deserialize(json, clazz);
    }

    @Override
    public JsonElement serialize(EffectEntry src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src);
    }
}