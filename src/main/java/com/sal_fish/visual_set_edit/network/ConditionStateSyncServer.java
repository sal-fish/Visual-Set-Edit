package com.sal_fish.visual_set_edit.network;

import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.ConditionStateCache;
import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.condition.Condition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 服务端求值全部附加条件并把结果同步给客户端，只在结果变化时发包
public final class ConditionStateSyncServer {

    private static final Map<UUID, byte[]> LAST_SENT = new HashMap<>();

    private ConditionStateSyncServer() {}

    public static void tick(ServerPlayer player) {
        List<Preset> presets = PresetManager.getPresets();
        int total = ConditionStateCache.totalOf(presets);
        byte[] bits = new byte[(total + 7) >> 3];

        int idx = 0;
        for (Preset preset : presets) {
            for (SetPhase phase : preset.phases) {
                for (Condition cond : phase.additionalConditions) {
                    if (testSafely(cond, player)) bits[idx >> 3] |= (byte) (1 << (idx & 7));
                    idx++;
                }
            }
        }

        byte[] last = LAST_SENT.get(player.getUUID());
        if (last != null && Arrays.equals(last, bits)) return;

        LAST_SENT.put(player.getUUID(), bits);
        VsePacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new S2CConditionStatePacket(total, bits)
        );
    }

    // 让该玩家的下次 tick 必定重发
    public static void invalidate(UUID uuid) {
        LAST_SENT.remove(uuid);
    }

    // 预设结构变化后调用，例如 /vse reload
    public static void invalidateAll() {
        LAST_SENT.clear();
    }

    // 客户端算不出的条件可能抛异常，这里吞掉并视为不成立
    private static boolean testSafely(Condition cond, ServerPlayer player) {
        try {
            return cond.test(player);
        } catch (Throwable t) {
            return false;
        }
    }
}
