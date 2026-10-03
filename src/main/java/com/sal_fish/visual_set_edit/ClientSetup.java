package com.sal_fish.visual_set_edit;

import com.sal_fish.visual_set_edit.api.VseClientApi;
import com.sal_fish.visual_set_edit.gui.AttributeListScreen;
import com.sal_fish.visual_set_edit.gui.BiomeListScreen;
import com.sal_fish.visual_set_edit.gui.BlockListScreen;
import com.sal_fish.visual_set_edit.gui.BlockTagListScreen;
import com.sal_fish.visual_set_edit.gui.DimensionListScreen;
import com.sal_fish.visual_set_edit.gui.EntityTypeListScreen;
import com.sal_fish.visual_set_edit.gui.EntityTypeTagListScreen;
import com.sal_fish.visual_set_edit.gui.ItemListScreen;
import com.sal_fish.visual_set_edit.gui.L2TraitListScreen;
import com.sal_fish.visual_set_edit.gui.MobEffectListScreen;
import com.sal_fish.visual_set_edit.gui.ScoreboardObjectiveListScreen;
import com.sal_fish.visual_set_edit.gui.SlotSelectionScreen;
import com.sal_fish.visual_set_edit.gui.SpellListScreen;
import com.sal_fish.visual_set_edit.gui.StructureListScreen;
import com.sal_fish.visual_set_edit.gui.TagListScreen;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import com.sal_fish.visual_set_edit.tooltip.TooltipRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = VisualSetEdit.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new TooltipRenderer());
        registerBuiltinPickers();
    }

    // 内置选择屏，供 FieldSpec.picker 直接引用
    private static void registerBuiltinPickers() {
        VseClientApi.registerPicker("vse:attribute",
                (parent, picked) -> new AttributeListScreen(parent, rl -> picked.accept(rl.toString())));
        VseClientApi.registerPicker("vse:item",
                (parent, picked) -> new ItemListScreen(parent, "", rl -> picked.accept(rl.toString())));
        VseClientApi.registerPicker("vse:block",
                (parent, picked) -> new BlockListScreen(parent, rl -> picked.accept(rl.toString())));
        VseClientApi.registerPicker("vse:mob_effect",
                (parent, picked) -> new MobEffectListScreen(parent, rl -> picked.accept(rl.toString())));
        VseClientApi.registerPicker("vse:entity_type",
                (parent, picked) -> new EntityTypeListScreen(parent, rl -> picked.accept(rl.toString())));
        VseClientApi.registerPicker("vse:dimension",
                (parent, picked) -> new DimensionListScreen(parent, rl -> picked.accept(rl.toString())));
        VseClientApi.registerPicker("vse:biome",
                (parent, picked) -> new BiomeListScreen(parent, rl -> picked.accept(rl.toString())));
        VseClientApi.registerPicker("vse:structure",
                (parent, picked) -> new StructureListScreen(parent, rl -> picked.accept(rl.toString())));
        VseClientApi.registerPicker("vse:tag",
                TagListScreen::new);
        VseClientApi.registerPicker("vse:block_tag",
                BlockTagListScreen::new);
        VseClientApi.registerPicker("vse:entity_type_tag",
                EntityTypeTagListScreen::new);
        VseClientApi.registerPicker("vse:scoreboard_objective",
                ScoreboardObjectiveListScreen::new);
        VseClientApi.registerPicker("vse:slot",
                (parent, picked) -> new SlotSelectionScreen(parent, "", picked));

        // 软依赖选择屏只在对应模组存在时注册
        if (IntegrationManager.isIronSpellsLoaded()) {
            VseClientApi.registerPicker("vse:iron_spell",
                    (parent, picked) -> new SpellListScreen(parent, rl -> picked.accept(rl.toString())));
        }
        if (IntegrationManager.isL2HostilityLoaded()) {
            VseClientApi.registerPicker("vse:l2_trait",
                    (parent, picked) -> new L2TraitListScreen(parent, rl -> picked.accept(rl.toString())));
        }
    }
}
