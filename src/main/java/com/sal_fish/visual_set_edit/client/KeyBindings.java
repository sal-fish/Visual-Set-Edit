package com.sal_fish.visual_set_edit.client;

import com.sal_fish.visual_set_edit.VisualSetEdit;
import com.sal_fish.visual_set_edit.network.C2SCastSpellPacket;
import com.sal_fish.visual_set_edit.network.VsePacketHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = VisualSetEdit.MODID, value = Dist.CLIENT)
public class KeyBindings {
    public static final KeyMapping CAST_SPELL = new KeyMapping(
            "key.visual_set_edit.cast_spell",
            GLFW.GLFW_KEY_R,
            "key.categories.visual_set_edit"
    );

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;
        if (CAST_SPELL.consumeClick()) {
            VsePacketHandler.INSTANCE.sendToServer(new C2SCastSpellPacket());
        }
    }
}