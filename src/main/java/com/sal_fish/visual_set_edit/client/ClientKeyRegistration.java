package com.sal_fish.visual_set_edit.client;

import com.sal_fish.visual_set_edit.VisualSetEdit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 客户端专用：注册按键映射（MOD 事件总线）
@Mod.EventBusSubscriber(modid = VisualSetEdit.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientKeyRegistration {
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_GUI);
    }
}
