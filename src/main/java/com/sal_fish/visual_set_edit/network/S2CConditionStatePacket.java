package com.sal_fish.visual_set_edit.network;

import com.sal_fish.visual_set_edit.data.ConditionStateCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 服务端算好的条件成立状态，位图顺序与 ConditionStateCache 的平坦下标一致
public record S2CConditionStatePacket(int total, byte[] bits) {

    public static void encode(S2CConditionStatePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.total);
        buf.writeByteArray(msg.bits);
    }

    public static S2CConditionStatePacket decode(FriendlyByteBuf buf) {
        int total = buf.readVarInt();
        byte[] bits = buf.readByteArray();
        return new S2CConditionStatePacket(total, bits);
    }

    public static void handle(S2CConditionStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ConditionStateCache.update(msg.total, msg.bits));
        ctx.get().setPacketHandled(true);
    }
}
