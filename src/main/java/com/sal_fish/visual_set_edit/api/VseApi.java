package com.sal_fish.visual_set_edit.api;

import com.sal_fish.visual_set_edit.data.SlotProviderRegistry;
import com.sal_fish.visual_set_edit.data.condition.ConditionFieldRegistry;
import com.sal_fish.visual_set_edit.data.condition.ConditionTypeRegistry;
import com.sal_fish.visual_set_edit.data.effect.AbilityTypeRegistry;
import com.sal_fish.visual_set_edit.data.effect.EffectTypeRegistry;
import com.sal_fish.visual_set_edit.gui.ConditionEditorRegistry;
import com.sal_fish.visual_set_edit.gui.EffectEditorRegistry;
import com.sal_fish.visual_set_edit.gui.GenericConditionEditor;
import com.sal_fish.visual_set_edit.gui.GenericEffectEditor;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

// 对外扩展入口：效果类型、条件类型、额外槽位来源都只从这里注册；需要客户端界面的注册见 VseClientApi
public final class VseApi {

    // 接口版本号，供外部 mod 做兼容判断
    public static final int API_VERSION = 1;

    private VseApi() {}

    // 注册一个效果类型；spec 为 null、id 为空、handler 为 null 或 id 已被占用时直接返回
    public static void registerEffectType(EffectTypeSpec spec) {
        if (spec == null || spec.id() == null || spec.id().isEmpty()) return;
        if (spec.handler() == null) return;
        if (EffectTypeRegistry.contains(spec.id())) return;

        EffectTypeSpecRegistry.put(spec);
        EffectTypeRegistry.register(spec.id(), DataDrivenEffectEntry.class, spec.available());
        EffectEditorRegistry.register(spec.id(), () -> new GenericEffectEditor(spec));
    }

    // 当前可用的效果类型 id 列表，按注册顺序；available 返回 false 的不在内
    public static List<String> effectTypes() {
        return EffectTypeRegistry.availableIds();
    }

    // 该效果类型 id 是否已被占用，含内置类型
    public static boolean isEffectTypeRegistered(String id) {
        return EffectTypeRegistry.contains(id);
    }

    // 注册一个条件类型；校验规则同 registerEffectType
    public static void registerConditionType(ConditionTypeSpec spec) {
        if (spec == null || spec.id() == null || spec.id().isEmpty()) return;
        if (spec.handler() == null) return;
        if (ConditionTypeRegistry.contains(spec.id())) return;

        ConditionTypeSpecRegistry.put(spec);
        ConditionTypeRegistry.register(spec.id(), DataDrivenCondition.class, spec.available());
        ConditionEditorRegistry.register(spec.id(), () -> new GenericConditionEditor(spec));
    }

    // 当前可用的条件类型 id 列表，按注册顺序
    public static List<String> conditionTypes() {
        return ConditionTypeRegistry.availableIds();
    }

    // 该条件类型 id 是否已被占用，含内置类型
    public static boolean isConditionTypeRegistered(String id) {
        return ConditionTypeRegistry.contains(id);
    }

    // 注册一个额外槽位来源；注册后槽位键为 <providerId>:<slotId>，并附带一个 <providerId>:any
    public static void registerSlotProvider(SlotProvider provider) {
        SlotProviderRegistry.register(provider);
    }

    // 当前全部额外槽位键，含各来源的 any 键；原版 6 个装备槽不在此列
    public static List<String> slotKeys(LivingEntity entity) {
        return SlotProviderRegistry.allSlotKeys(entity);
    }

    // 往"能力"板块追加一个子能力；spec 为 null、id 为空或 handler 为 null 时直接返回
    public static void registerAbility(AbilitySpec spec) {
        AbilityTypeRegistry.register(spec);
    }

    // 往已有条件类型（environment / player_state）追加一个字段；校验规则同上
    public static void registerConditionField(ConditionFieldSpec spec) {
        ConditionFieldRegistry.register(spec);
    }
}
