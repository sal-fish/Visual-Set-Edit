package com.sal_fish.visual_set_edit.integration;

import com.sal_fish.visual_set_edit.config.CuriosItemMappingManager;
import com.sal_fish.visual_set_edit.config.CuriosItemMappingManager.RegisteredEntry;
import com.sal_fish.visual_set_edit.data.effect.EffectEntry;
import com.sal_fish.visual_set_edit.data.effect.SlotCountEffectEntry;
import com.sal_fish.visual_set_edit.event.ActiveSetTracker;
import com.sal_fish.visual_set_edit.event.SetEventHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import top.theillusivec4.curios.common.capability.ItemizedCurioCapability;

import java.util.*;

public class CuriosIntegration implements IModIntegration {

    @Override
    public boolean isLoaded() { return true; }

    @Override
    public void onInitialize() {
        // 1. 装备验证事件
        MinecraftForge.EVENT_BUS.addListener(this::onCurioEquip);

        // 2. 为物品附加能力（用于快捷放入）
        MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, this::onAttachItemCapability);
    }

    private void onAttachItemCapability(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (stack.isEmpty()) return;
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return;
        String itemId = key.toString();

        // 获取与物品 NBT 匹配的所有注册条目
        List<RegisteredEntry> matching = CuriosItemMappingManager.getMatchingEntries(itemId, stack.getTag());
        if (matching.isEmpty()) return;

        // 使用第一个匹配条目的配置（可根据需要调整合并策略）
        RegisteredEntry first = matching.get(0);
        ICurioItem curioItem = new VseCurioItem(first.canQuickEquip, first.canRemove);
        event.addCapability(new ResourceLocation("visual_set_edit", "curio"),
                CuriosApi.createCurioProvider(new ItemizedCurioCapability(curioItem, stack)));
    }

    private void onCurioEquip(CurioEquipEvent event) {
        ItemStack stack = event.getStack();
        if (stack.isEmpty()) return;
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return;
        String itemId = key.toString();

        // 获取所有匹配 NBT 的注册条目
        List<RegisteredEntry> matching = CuriosItemMappingManager.getMatchingEntries(itemId, stack.getTag());
        if (matching.isEmpty()) return;

        String targetSlot = event.getSlotContext().identifier();

        // 检查目标槽位是否在任何一个匹配条目的槽位列表中
        for (RegisteredEntry entry : matching) {
            for (String allowed : entry.slots) {
                String normalized = allowed.startsWith("curios:") ? allowed.substring(7) : allowed;
                if (targetSlot.equals(normalized)) {
                    event.setResult(Event.Result.ALLOW);
                    break;
                }
            }
            if (event.getResult() == Event.Result.ALLOW) break;
        }

        SetEventHandler.forceReevaluate(event.getEntity());
    }

    public static void cleanupSlotModifiersOnClone(LivingEntity oldEntity, LivingEntity newEntity) {
        if (!IntegrationManager.isCuriosLoaded()) return;
        for (var active : ActiveSetTracker.getActivePhases(oldEntity)) {
            for (EffectEntry entry : active.phase().effects) {
                if (entry instanceof SlotCountEffectEntry slotEntry) {
                    slotEntry.ensureUniqueId();
                    String realSlotId = slotEntry.slotId.startsWith("curios:") ?
                            slotEntry.slotId.substring(7) : slotEntry.slotId;
                    CuriosApi.getCuriosInventory(newEntity).ifPresent(handler -> {
                        handler.removeSlotModifier(realSlotId, UUID.fromString(slotEntry.uniqueId));
                    });
                }
            }
        }
    }

    @Override public double getMana(LivingEntity player) { return 0; }
    @Override public double getManaPercent(LivingEntity player) { return 0; }
    @Override public boolean isCasting(LivingEntity player) { return false; }
    @Override public void tryCastActiveSpell(LivingEntity player) {
    }

    @Override public List<String> getExtraSlots() {
        Set<String> slotIds = CuriosApi.getSlotHelper().getSlotTypeIds();
        return new ArrayList<>(slotIds);
    }

    @Override public ItemStack getSlotStack(LivingEntity entity, String slotId) {
        List<ItemStack> stacks = getSlotStacks(entity, slotId);
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0);
    }

    @Override public List<ItemStack> getSlotStacks(LivingEntity entity, String slotId) {
        var handler = CuriosApi.getCuriosHelper().getCuriosHandler(entity)
                .map(h -> h.getStacksHandler(slotId))
                .orElse(Optional.empty());
        if (handler.isPresent()) {
            ICurioStacksHandler stacksHandler = handler.get();
            IDynamicStackHandler stacks = stacksHandler.getStacks();
            int slots = stacks.getSlots();
            if (slots > 0) {
                List<ItemStack> result = new ArrayList<>();
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (stack != null && !stack.isEmpty()) {
                        result.add(stack);
                    }
                }
                return result;
            }
        }
        return Collections.emptyList();
    }

    @Override public boolean canItemGoInSlot(String slotId, ItemStack stack) {
        if (stack.isEmpty()) return false;
        SlotContext ctx = new SlotContext(slotId, null, 0, false, true);
        return CuriosApi.isStackValid(ctx, stack);
    }
}