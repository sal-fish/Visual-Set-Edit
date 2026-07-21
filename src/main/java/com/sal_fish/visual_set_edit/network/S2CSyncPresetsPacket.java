package com.sal_fish.visual_set_edit.network;

import com.sal_fish.visual_set_edit.VisualSetEdit;
import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.Preset;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public record S2CSyncPresetsPacket(List<Preset> presets) {

    public static void encode(S2CSyncPresetsPacket msg, FriendlyByteBuf buf) {
        try {
            String json = PresetManager.GSON.toJson(msg.presets);
            byte[] compressed = compress(json);
            buf.writeByteArray(compressed);
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress S2C sync packet", e);
        }
    }

    public static S2CSyncPresetsPacket decode(FriendlyByteBuf buf) {
        try {
            byte[] compressed = buf.readByteArray();
            String json = decompress(compressed);
            List<Preset> list = PresetManager.GSON.fromJson(json,
                    new TypeToken<List<Preset>>(){}.getType());
            return new S2CSyncPresetsPacket(list);
        } catch (Exception e) {
            VisualSetEdit.LOGGER.error("Failed to decode S2CSyncPresetsPacket", e);
            return new S2CSyncPresetsPacket(List.of());
        }
    }

    public static void handle(S2CSyncPresetsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PresetManager.clientCachePresets(msg.presets));
        ctx.get().setPacketHandled(true);
    }

    //压缩
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