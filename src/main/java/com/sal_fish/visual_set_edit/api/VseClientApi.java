package com.sal_fish.visual_set_edit.api;

import com.sal_fish.visual_set_edit.gui.PickerScreenRegistry;

// 客户端专用扩展入口：需要客户端界面的注册走这里，双端代码不要引用本类
public final class VseClientApi {

    private VseClientApi() {}

    // 注册一个选择屏；pickerId 与 FieldSpec.picker 传入的一致
    public static void registerPicker(String pickerId, PickerScreenFactory factory) {
        PickerScreenRegistry.register(pickerId, factory);
    }
}
