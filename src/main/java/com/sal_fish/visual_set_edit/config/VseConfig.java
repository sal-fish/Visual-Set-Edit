package com.sal_fish.visual_set_edit.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class VseConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ALLOW_COMMAND_GUI;
    public static final ForgeConfigSpec.BooleanValue ALLOW_KEYBIND_GUI;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ALLOW_COMMAND_GUI = builder
                .comment("是否允许通过 /vse gui 指令打开 GUI。",
                        "false 时任何权限等级（含 OP 4）都无法通过指令打开。")
                .define("allowCommandGui", true);
        ALLOW_KEYBIND_GUI = builder
                .comment("是否允许通过快捷键打开 GUI。",
                        "false 时任何权限等级（含 OP 4）都无法通过快捷键打开。")
                .define("allowKeybindGui", true);
        SPEC = builder.build();
    }

    private VseConfig() {
    }
}
