package com.sal_fish.visual_set_edit.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sal_fish.visual_set_edit.VisualSetEdit;
import com.sal_fish.visual_set_edit.network.C2SRequestGuiPacket;
import com.sal_fish.visual_set_edit.network.VsePacketHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

// 客户端专用：反斜杠键打开编辑界面，是否放行由服务端判定
@Mod.EventBusSubscriber(modid = VisualSetEdit.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KeyBindings {
    public static final KeyMapping OPEN_GUI = new KeyMapping(
            "key.visual_set_edit.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSLASH,
            "key.categories.visual_set_edit");

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_GUI.consumeClick()) {
            if (mc.player == null) continue;
            VsePacketHandler.INSTANCE.sendToServer(new C2SRequestGuiPacket());
        }
    }
}
