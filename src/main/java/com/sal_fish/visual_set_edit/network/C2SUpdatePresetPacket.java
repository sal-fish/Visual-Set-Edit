package com.sal_fish.visual_set_edit.network;

import com.google.common.reflect.TypeToken;
import com.sal_fish.visual_set_edit.VisualSetEdit;
import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.event.SetEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public record C2SUpdatePresetPacket(List<Preset> presets) {

    public static void encode(C2SUpdatePresetPacket msg, FriendlyByteBuf buf) {
        try {
            String json = PresetManager.GSON.toJson(msg.presets);
            byte[] compressed = compress(json);
            buf.writeByteArray(compressed);
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress packet data", e);
        }
    }

    public static C2SUpdatePresetPacket decode(FriendlyByteBuf buf) {
        try {
            byte[] compressed = buf.readByteArray();
            String json = decompress(compressed);
            List<Preset> list = PresetManager.GSON.fromJson(json,
                    new TypeToken<List<Preset>>() {}.getType());
            return new C2SUpdatePresetPacket(list);
        } catch (Exception e) {
            VisualSetEdit.LOGGER.error("Invalid C2SUpdatePresetPacket received", e);
            return new C2SUpdatePresetPacket(List.of());
        }
    }

    public static void handle(C2SUpdatePresetPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null && sender.hasPermissions(2)) {
                PresetManager.savePresets(msg.presets);
                VsePacketHandler.INSTANCE.send(
                        PacketDistributor.ALL.noArg(),
                        new S2CSyncPresetsPacket(msg.presets)
                );

                for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
                    SetEventHandler.forceReevaluate(player);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static byte[] compress(String str) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    private static String decompress(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try (GZIPInputStream gzip = new GZIPInputStream(bais)) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}