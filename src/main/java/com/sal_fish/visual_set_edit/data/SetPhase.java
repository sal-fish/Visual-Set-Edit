package com.sal_fish.visual_set_edit.data;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.data.condition.Condition;
import com.sal_fish.visual_set_edit.data.effect.EffectEntry;
import java.util.ArrayList;
import java.util.List;

public class SetPhase {
    @Expose public String translationKey;
    @Expose public String fallbackName;
    @Expose public int requiredCount = 0;
    @Expose public List<SlotCondition> slotConditions = new ArrayList<>();
    @Expose public List<EffectEntry> effects = new ArrayList<>();
    @Expose public List<Condition> additionalConditions = new ArrayList<>();

    public void initEffects() {
        for (EffectEntry effect : effects) {
            effect.initAfterLoad();
        }
    }
}