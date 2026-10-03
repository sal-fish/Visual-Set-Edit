package com.sal_fish.visual_set_edit.api;

import net.minecraft.world.entity.player.Player;

import java.util.Map;

// 能力处理器：外部用它给出子能力的生效与失效逻辑
public interface AbilityHandler {

    void apply(Player player, Map<String, Object> params);

    void remove(Player player, Map<String, Object> params);
}
