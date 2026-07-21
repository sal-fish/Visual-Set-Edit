package com.sal_fish.visual_set_edit.network;

import com.sal_fish.visual_set_edit.gui.RegistryDataCache;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record S2CResponseRegistryDataPacket(String registryType, List<ResourceLocation> ids) {
    public static void encode(S2CResponseRegistryDataPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.registryType);
        buf.writeVarInt(msg.ids.size());
        for (ResourceLocation id : msg.ids) {
            buf.writeResourceLocation(id);
        }
    }

    public static S2CResponseRegistryDataPacket decode(FriendlyByteBuf buf) {
        String type = buf.readUtf();
        int size = buf.readVarInt();
        List<ResourceLocation> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readResourceLocation());
        }
        return new S2CResponseRegistryDataPacket(type, ids);
    }

    public static void handle(S2CResponseRegistryDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            RegistryDataCache.put(msg.registryType, msg.ids);
        });
        ctx.get().setPacketHandled(true);
    }
}