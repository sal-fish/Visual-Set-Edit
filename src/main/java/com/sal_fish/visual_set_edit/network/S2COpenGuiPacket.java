package com.sal_fish.visual_set_edit.network;

import com.sal_fish.visual_set_edit.VisualSetEdit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record S2COpenGuiPacket() {
    public static void encode(S2COpenGuiPacket msg, FriendlyByteBuf buf) {}
    public static S2COpenGuiPacket decode(FriendlyByteBuf buf) {
        return new S2COpenGuiPacket();
    }

    public static void handle(S2COpenGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(VisualSetEdit.clientProxy::openPresetListScreen);
        ctx.get().setPacketHandled(true);
    }
}