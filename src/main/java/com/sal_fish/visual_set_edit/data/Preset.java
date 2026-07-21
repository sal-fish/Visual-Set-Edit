package com.sal_fish.visual_set_edit.data;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.data.effect.EffectEntry;

import java.util.ArrayList;
import java.util.List;

public class Preset {
    @Expose public String id;
    @Expose public String translationKey;
    @Expose public String fallbackName;
    @Expose public int tooltipColor = 0xFFAA00;
    @Expose public List<SetPhase> phases = new ArrayList<>();
    @Expose public boolean showTooltip = true;
    @Expose public List<String> customTooltipLines = new ArrayList<>();
    @Expose public List<String> backgroundStoryLines = new ArrayList<>();

    private transient List<SlotCondition> allSlots;

    public void initAfterLoad() {
        allSlots = new ArrayList<>();
        for (SetPhase phase : phases) {
            allSlots.addAll(phase.slotConditions);
            phase.initEffects();
        }
    }

    public void resetAllUniqueIds() {
        for (SetPhase phase : phases) {
            for (EffectEntry effect : phase.effects) {
                effect.resetUniqueId();
            }
        }
    }

    public List<SlotCondition> getAllSlotConditions() {
        if (allSlots == null) initAfterLoad();
        return allSlots;
    }
}