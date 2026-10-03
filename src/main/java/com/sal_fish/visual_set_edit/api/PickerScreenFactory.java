package com.sal_fish.visual_set_edit.api;

import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;

// 选择屏工厂：外部用它提供一个"打开选择界面并把选中结果回调回来"的界面
public interface PickerScreenFactory {

    // parent 是调用方界面，选中后需把结果交给 onPicked 并自行返回 parent
    Screen create(Screen parent, Consumer<String> onPicked);
}
