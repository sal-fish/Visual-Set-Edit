package com.sal_fish.visual_set_edit.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record C2SRequestRegistryDataPacket(String registryType) {
    public static void encode(C2SRequestRegistryDataPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.registryType);
    }

    public static C2SRequestRegistryDataPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestRegistryDataPacket(buf.readUtf());
    }

    public static void handle(C2SRequestRegistryDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                var list = RegistryDataServer.getRegistryIds(player.server, msg.registryType);
                VsePacketHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new S2CResponseRegistryDataPacket(msg.registryType, list)
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}