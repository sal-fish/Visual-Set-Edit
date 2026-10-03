package com.sal_fish.visual_set_edit.integration;

import com.sal_fish.visual_set_edit.api.SlotProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CuriosSlotProvider implements SlotProvider {

    public static final String ID = "curios";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<String> slotIds(LivingEntity entity) {
        var helper = CuriosApi.getSlotHelper();
        if (helper == null) return Collections.emptyList();
        Set<String> slotIds = helper.getSlotTypeIds();
        return slotIds == null ? Collections.emptyList() : new ArrayList<>(slotIds);
    }

    @Override
    public List<ItemStack> stacks(LivingEntity entity, String slotId) {
        if (entity == null) return Collections.emptyList();
        var handler = CuriosApi.getCuriosHelper().getCuriosHandler(entity)
                .map(h -> h.getStacksHandler(slotId))
                .orElse(Optional.empty());
        if (handler.isEmpty()) return Collections.emptyList();

        var stacks = handler.get().getStacks();
        int slots = stacks.getSlots();
        if (slots <= 0) return Collections.emptyList();

        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (stack != null && !stack.isEmpty()) result.add(stack);
        }
        return result;
    }
}
