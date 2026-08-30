package com.sal_fish.visual_set_edit.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class VsePacketHandler {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("visual_set_edit", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        int id = 0;

        // 服务端 → 客户端
        INSTANCE.registerMessage(id++, S2CSyncPresetsPacket.class,
                S2CSyncPresetsPacket::encode,
                S2CSyncPresetsPacket::decode,
                S2CSyncPresetsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // 客户端 → 服务端
        INSTANCE.registerMessage(id++, C2SUpdatePresetPacket.class,
                C2SUpdatePresetPacket::encode,
                C2SUpdatePresetPacket::decode,
                C2SUpdatePresetPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // 服务端 → 客户端
        INSTANCE.registerMessage(id++, S2COpenGuiPacket.class,
                S2COpenGuiPacket::encode,
                S2COpenGuiPacket::decode,
                S2COpenGuiPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // 客户端 → 服务端
        INSTANCE.registerMessage(id++, C2SRequestRegistryDataPacket.class,
                C2SRequestRegistryDataPacket::encode,
                C2SRequestRegistryDataPacket::decode,
                C2SRequestRegistryDataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // 服务端 → 客户端
        INSTANCE.registerMessage(id++, S2CResponseRegistryDataPacket.class,
                S2CResponseRegistryDataPacket::encode,
                S2CResponseRegistryDataPacket::decode,
                S2CResponseRegistryDataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}