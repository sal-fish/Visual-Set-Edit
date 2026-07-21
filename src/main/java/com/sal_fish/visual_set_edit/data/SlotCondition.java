package com.sal_fish.visual_set_edit.data;

import com.google.gson.annotations.Expose;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Objects;

public class SlotCondition {
    @Expose public String slot;
    @Expose public String itemId;      // "modid:item" or null for any
    @Expose public String tagId;       // "modid:tag"   or null for any (新增)
    @Expose public NbtMatchRule nbtRule = NbtMatchRule.IGNORE;
    @Expose public Map<String, Object> nbtKeys;
    @Expose public int durabilityMinPercent = 0;
    @Expose public int durabilityMaxPercent = 100;
    @Expose public int minCount = 1;
    @Expose public String exactNbt;   // EXACT 模式下用于比较的完整 NBT 字符串

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        if (stack.getCount() < minCount) return false;

        // 1. Tag 匹配优先
        if (tagId != null && !tagId.isEmpty()) {
            ResourceLocation tagRl = ResourceLocation.tryParse(tagId);
            if (tagRl != null) {
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagRl);
                if (!stack.is(tagKey)) return false;
            } else {
                return false; // 无效 tagId
            }
        }
        // 2. 否则使用 itemId 匹配
        else if (itemId != null && !itemId.isEmpty()) {
            ResourceLocation req = ResourceLocation.tryParse(itemId);
            ResourceLocation has = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (req != null && !req.equals(has)) return false;
        }
        // 3. 两者都为空 → 匹配任意物品

        // 耐久检查
        if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage();
            if (max > 0) {
                int cur = max - stack.getDamageValue();
                int percent = cur * 100 / max;
                if (percent < durabilityMinPercent || percent > durabilityMaxPercent) return false;
            }
        }

        // NBT 检查
        if (nbtRule == NbtMatchRule.EXACT) {
            // 优先使用捕获的 exactNbt 进行字符串精确匹配
            if (exactNbt != null && !exactNbt.isEmpty()) {
                CompoundTag stackTag = stack.getTag();
                String stackNbtStr = stackTag != null ? stackTag.toString() : "";
                return stackNbtStr.equals(exactNbt);
            }
            // 否则回退到与物品默认 NBT 比较
            CompoundTag stackTag = stack.getTag();
            CompoundTag comp = stack.getItem().getDefaultInstance().getTag();
            if (stackTag == null && comp == null) return true;
            if (stackTag == null || comp == null) return false;
            return stackTag.equals(comp);
        } else if (nbtRule == NbtMatchRule.CUSTOM_KEYS && nbtKeys != null) {
            CompoundTag tag = stack.getTag();
            if (tag == null) return false;
            for (Map.Entry<String, Object> e : nbtKeys.entrySet()) {
                if (!tag.contains(e.getKey())) return false;
                if (!Objects.requireNonNull(tag.get(e.getKey())).getAsString().equals(String.valueOf(e.getValue())))
                    return false;
            }
        }
        return true;
    }
}