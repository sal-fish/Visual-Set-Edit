package com.sal_fish.visual_set_edit;

import com.mojang.brigadier.CommandDispatcher;
import com.sal_fish.visual_set_edit.config.CuriosItemMappingManager;
import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.event.ActiveSetTracker;
import com.sal_fish.visual_set_edit.event.SetEventHandler;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import com.sal_fish.visual_set_edit.network.S2COpenGuiPacket;
import com.sal_fish.visual_set_edit.network.VsePacketHandler;
import com.sal_fish.visual_set_edit.proxy.ClientProxy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("visual_set_edit")
public class VisualSetEdit {
    public static final String MODID = "visual_set_edit";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final ClientProxy clientProxy = DistExecutor.safeRunForDist(
            () -> com.sal_fish.visual_set_edit.client.ClientProxyImpl::new,
            () -> com.sal_fish.visual_set_edit.proxy.ServerClientProxy::new
    );

    public VisualSetEdit() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new SetEventHandler());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        VsePacketHandler.register();

        // 1. 初始化联动运行时（如铁魔法事件监听），类型注册已在 EffectEntryAdapter 静态初始化中完成
        IntegrationManager.initCompat();

        // 2. 加载预设（此时所有效果类型均已注册，可以正常反序列化）
        PresetManager.loadPresets();

        // 3. Curios 联动初始化
        if (IntegrationManager.isCuriosLoaded()) {
            CuriosItemMappingManager.load();
            IntegrationManager.getCurios().onInitialize();
        }
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dis = event.getDispatcher();
        dis.register(Commands.literal("vse")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("reload").executes(ctx -> {
                    PresetManager.loadPresets();
                    if (IntegrationManager.isCuriosLoaded()) {
                        CuriosItemMappingManager.load();
                    }
                    ActiveSetTracker.clearAll();
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("visual_set_edit.command.reload.success"), true);
                    return 1;
                }))
                .then(Commands.literal("gui").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    VsePacketHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new S2COpenGuiPacket()
                    );
                    return 1;
                }))
        );
    }
}