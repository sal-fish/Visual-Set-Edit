package com.sal_fish.visual_set_edit.client;

import com.sal_fish.visual_set_edit.proxy.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientProxyImpl implements ClientProxy {
    @Override
    public void openPresetListScreen() {
        Minecraft.getInstance().setScreen(
                new com.sal_fish.visual_set_edit.gui.PresetListScreen()
        );
    }
}