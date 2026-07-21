package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.network.C2SRequestRegistryDataPacket;
import com.sal_fish.visual_set_edit.network.VsePacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.*;

public class RegistryListHelper {
    public static List<ResourceLocation> getRegistryIds(String type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer()) {
            MinecraftServer server = mc.getSingleplayerServer();
            if (server != null) {
                return com.sal_fish.visual_set_edit.network.RegistryDataServer.getRegistryIds(server, type);
            }
        }
        List<ResourceLocation> cached = RegistryDataCache.get(type);
        return cached != null ? cached : Collections.emptyList();
    }

    public static void requestIfNeeded(String type) {
        if (Minecraft.getInstance().hasSingleplayerServer()) return;
        if (RegistryDataCache.get(type) != null) return;
        VsePacketHandler.INSTANCE.sendToServer(new C2SRequestRegistryDataPacket(type));
    }
}