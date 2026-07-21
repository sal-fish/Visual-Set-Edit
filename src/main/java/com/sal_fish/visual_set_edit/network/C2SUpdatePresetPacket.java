package com.sal_fish.visual_set_edit.network;

import com.google.common.reflect.TypeToken;
import com.sal_fish.visual_set_edit.VisualSetEdit;
import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.Preset;
import net.minecraft.network.FriendlyByteBuf;
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
            // 1. 序列化为 JSON 字符串
            String json = PresetManager.GSON.toJson(msg.presets);
            // 2. 使用 GZIP 压缩
            byte[] compressed = compress(json);
            // 3. 写入字节数组（无长度限制）
            buf.writeByteArray(compressed);
        } catch (IOException e) {
            // 不应该发生，但以防万一
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
            if (Objects.requireNonNull(ctx.get().getSender()).hasPermissions(2)) {
                PresetManager.savePresets(msg.presets);
                VsePacketHandler.INSTANCE.send(
                        PacketDistributor.ALL.noArg(),
                        new S2CSyncPresetsPacket(msg.presets)
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // ========== 压缩工具方法 ==========

    private static byte[] compress(String str) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
        }
        // GZIPOutputStream 在 close 时才完成写入，这里 try-with-resources 会调用 close
        return baos.toByteArray();
    }

    private static String decompress(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try (GZIPInputStream gzip = new GZIPInputStream(bais)) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}