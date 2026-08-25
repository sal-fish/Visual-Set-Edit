package com.sal_fish.visual_set_edit.data;

import com.google.gson.annotations.Expose;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class TargetFilter {
    @Expose public String blockId;       // "modid:block" 或 null
    @Expose public String blockTag;      // "modid:tag" 或 null
    @Expose public String entityTypeId;  // "modid:entity" 或 null
    @Expose public String entityTypeTag; // "modid:tag" 或 null

    public TargetFilter() {}

    // 判断方块是否符合条件
    public boolean matches(BlockState state) {
        if (blockId != null && !blockId.isEmpty()) {
            ResourceLocation rl = ResourceLocation.tryParse(blockId);
            if (rl == null) return false;
            Block block = ForgeRegistries.BLOCKS.getValue(rl);
            if (block == null || state.getBlock() != block) return false;
        } else if (blockTag != null && !blockTag.isEmpty()) {
            ResourceLocation tagRl = ResourceLocation.tryParse(blockTag);
            if (tagRl == null) return false;
            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagRl);
            if (!state.is(tagKey)) return false;
        } else {
            return true;
        }
        return true;
    }

    // 判断实体是否符合条件
    public boolean matches(Entity entity) {
        if (entityTypeId != null && !entityTypeId.isEmpty()) {
            ResourceLocation rl = ResourceLocation.tryParse(entityTypeId);
            if (rl == null) return false;
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(rl);
            if (type == null || entity.getType() != type) return false;
        } else if (entityTypeTag != null && !entityTypeTag.isEmpty()) {
            ResourceLocation tagRl = ResourceLocation.tryParse(entityTypeTag);
            if (tagRl == null) return false;
            TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, tagRl);
            if (!entity.getType().is(tagKey)) return false;
        } else {
            return true;
        }
        return true;
    }

    //判断该过滤器是否未设置任何条件
    public boolean isEmpty() {
        return (blockId == null || blockId.isEmpty())
                && (blockTag == null || blockTag.isEmpty())
                && (entityTypeId == null || entityTypeId.isEmpty())
                && (entityTypeTag == null || entityTypeTag.isEmpty());
    }
}