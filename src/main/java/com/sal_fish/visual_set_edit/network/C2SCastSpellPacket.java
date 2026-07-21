package com.sal_fish.visual_set_edit.network;

import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record C2SCastSpellPacket() {
    public static void encode(C2SCastSpellPacket msg, FriendlyByteBuf buf) {}
    public static C2SCastSpellPacket decode(FriendlyByteBuf buf) { return new C2SCastSpellPacket(); }
    public static void handle(C2SCastSpellPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null && IntegrationManager.isIronSpellsLoaded()) {
                IntegrationManager.getIronSpells().tryCastActiveSpell(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}