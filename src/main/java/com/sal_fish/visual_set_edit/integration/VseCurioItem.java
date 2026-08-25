package com.sal_fish.visual_set_edit.integration;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;

import javax.annotation.Nonnull;
import java.util.Optional;

public class VseCurioItem implements ICurioItem {

    private final boolean canQuickEquip;
    private final boolean canRemove;
    private final boolean anySlot;

    public VseCurioItem() {
        this(true, true, false);
    }

    public VseCurioItem(boolean canQuickEquip, boolean canRemove) {
        this(canQuickEquip, canRemove, false);
    }

    public VseCurioItem(boolean canQuickEquip, boolean canRemove, boolean anySlot) {
        this.canQuickEquip = canQuickEquip;
        this.canRemove = canRemove;
        this.anySlot = anySlot;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return canQuickEquip;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        if (anySlot) {
            return true;
        }
        return true;
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        if (canRemove) return true;
        if (slotContext.entity() instanceof Player player && player.isCreative()) {
            return true;
        }
        return false;
    }

    @Override
    @Nonnull
    public DropRule getDropRule(SlotContext slotContext, DamageSource source, int lootingLevel,
                                boolean recentlyHit, ItemStack stack) {
        if (slotContext != null && slotContext.entity() != null) {
            Optional<top.theillusivec4.curios.api.type.ISlotType> slotType =
                    CuriosApi.getSlot(slotContext.identifier(), slotContext.entity().level());
            if (slotType.isPresent()) {
                return slotType.get().getDropRule();
            }
        }
        return DropRule.DEFAULT;
    }
}