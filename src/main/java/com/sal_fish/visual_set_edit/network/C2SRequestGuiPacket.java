package com.sal_fish.visual_set_edit.network;

import com.sal_fish.visual_set_edit.config.VseConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

// 客户端按快捷键请求打开 GUI，是否放行由服务端判定
public record C2SRequestGuiPacket() {
    public static void encode(C2SRequestGuiPacket msg, FriendlyByteBuf buf) {}

    public static C2SRequestGuiPacket decode(FriendlyByteBuf buf) {
        return new C2SRequestGuiPacket();
    }

    public static void handle(C2SRequestGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            if (!VseConfig.ALLOW_KEYBIND_GUI.get()) return;
            if (!sender.hasPermissions(2)) return;
            VsePacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sender),
                    new S2COpenGuiPacket()
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
