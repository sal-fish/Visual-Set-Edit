package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.*;

public class SlotCountEffectEntry extends EffectEntry {

    @Expose
    public String slotId = "";

    @Expose
    public int amount = 1;

    @Expose
    public String uniqueId;

    public SlotCountEffectEntry() {
        this.type = "slot_count";
        this.uniqueId = UUID.randomUUID().toString();
    }

    public void ensureUniqueId() {
        if (uniqueId == null || uniqueId.isEmpty()) {
            uniqueId = UUID.randomUUID().toString();
        }
    }

    @Override
    public void initAfterLoad() {
        ensureUniqueId();
    }

    @Override
    public void resetUniqueId() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    @Override
    public void apply(LivingEntity entity) {
        if (!IntegrationManager.isCuriosLoaded()) return;
        ensureUniqueId();
        final String realSlotId = slotId.startsWith("curios:") ? slotId.substring(7) : slotId;

        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
            // 如果该槽位处理器尚不存在，则手动创建
            if (!handler.getStacksHandler(realSlotId).isPresent()) {
                ISlotType slotType = CuriosApi.getSlot(realSlotId, entity.level()).orElse(null);
                if (slotType != null) {
                    CurioStacksHandler newHandler = new CurioStacksHandler(
                            handler,
                            realSlotId,
                            slotType.getSize(),
                            slotType.useNativeGui(),
                            slotType.hasCosmetic(),
                            slotType.canToggleRendering(),
                            slotType.getDropRule()
                    );
                    // 将新处理器加入映射
                    Map<String, ICurioStacksHandler> newCurios = new LinkedHashMap<>(handler.getCurios());
                    newCurios.put(realSlotId, newHandler);
                    handler.setCurios(newCurios);
                }
            }

            UUID uuid = UUID.fromString(uniqueId);
            // 添加瞬态槽位修饰符
            handler.addTransientSlotModifier(realSlotId, uuid, "VSE Slot Modifier", amount,
                    AttributeModifier.Operation.ADDITION);

            handler.getStacksHandler(realSlotId).ifPresent(ICurioStacksHandler::getSlots);
        });
    }

    @Override
    public void remove(LivingEntity entity) {
        if (!IntegrationManager.isCuriosLoaded()) return;
        ensureUniqueId();
        final String realSlotId = slotId.startsWith("curios:") ? slotId.substring(7) : slotId;
        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
            UUID uuid = UUID.fromString(uniqueId);
            handler.removeSlotModifier(realSlotId, uuid);
        });
    }

    @Override
    public String getDisplayText() {
        String realSlotId = slotId.startsWith("curios:") ? slotId.substring(7) : slotId;
        String slotName = Component.translatable("curios.identifier." + realSlotId).getString();
        return Component.translatable("visual_set_edit.gui.effect.slot_count.display", slotName, amount).getString();
    }
}