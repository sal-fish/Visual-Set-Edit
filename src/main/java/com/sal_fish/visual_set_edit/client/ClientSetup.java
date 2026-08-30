package com.sal_fish.visual_set_edit.client;

import com.sal_fish.visual_set_edit.VisualSetEdit;
import com.sal_fish.visual_set_edit.lang.CustomLanguageLoader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = VisualSetEdit.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        CustomLanguageLoader.load();
    }
}
