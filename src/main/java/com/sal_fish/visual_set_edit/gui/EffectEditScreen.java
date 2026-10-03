package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.api.AbilitySpec;
import com.sal_fish.visual_set_edit.data.TargetFilter;
import com.sal_fish.visual_set_edit.data.effect.*;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class EffectEditScreen extends Screen {
    private int scrollOffset = 0;
    private int contentHeight;

    private final Consumer<EffectEntry> onSave;
    private final Screen returnTo;
    private String effectType = EffectTypeRegistry.defaultId();

    // Potion
    private String potionTarget = "SELF";
    private ResourceLocation selectedMobEffect;
    private int amplifier;
    private int durationSeconds = -1;
    private int cooldownSeconds = 0;
    private boolean showParticles = true;

    // Attribute
    private ResourceLocation selectedAttribute;
    private double amount;
    private AttributeModifier.Operation attrOperation = AttributeModifier.Operation.ADDITION;
    private int attrDurationSeconds = -1;  // -1 = 常驻
    private int attrCooldownSeconds = 0;
    private EditBox attrDurationEdit, attrCooldownEdit;

    private String dynamicSourceAttributeId = "";
    private Button selectSourceAttributeButton;

    // 动态属性 - 药水等级变量
    private String dynamicSourcePotionId = "";
    private EditBox sourcePotionEditBox;

    // Ability
    private String abilityId = "FLIGHT";
    private final FieldSpecPanel abilityPanel = new FieldSpecPanel();
    private String panelAbilityId;

    // Command
    private String commands = "";
    private CommandEffectEntry.Mode commandMode = CommandEffectEntry.Mode.IMPULSE;
    private int commandRepeatInterval = 1;
    private EditBox commandIntervalEdit;
    private CommandEffectEntry.Trigger commandTrigger = CommandEffectEntry.Trigger.ACTIVATE;
    private double commandProbability = 1.0;
    private TargetFilter commandTargetFilter = new TargetFilter();
    private int commandCooldownSeconds = 0;
    private Button targetFilterButton;

    // Iron Spell
    private String spellId = "";
    private int spellLevel = 1;
    private EditBox spellLevelEdit;

    // Slot Count
    private String slotCountSlotId = "";
    private int slotCountAmount = 1;
    private EditBox slotCountAmountEdit;
    private Button selectSlotButton;

    // Spell Level Boost
    private String boostSpellId = "";
    private int boostAmount = 1;
    private EditBox boostAmountEdit;
    private Button selectBoostSpellButton;

    // L2 Hostility Trait
    private String l2traitId = "";
    private int l2traitLevel = 1;
    private EditBox l2traitLevelEdit;
    private Button selectL2TraitButton;

    // L2 Difficulty Mod
    private int l2DifficultyAmount = 1;

    // Dynamic Attribute
    private String dynamicAttributeId = "";
    private AttributeModifier.Operation dynamicOperation = AttributeModifier.Operation.ADDITION;
    private DynamicAttributeEffectEntry.VariableType dynamicVariable = DynamicAttributeEffectEntry.VariableType.GAME_TIME;
    private DynamicAttributeEffectEntry.FormulaType dynamicFormula = DynamicAttributeEffectEntry.FormulaType.LINEAR;
    private double[] dynamicCoeffs = {0, 0};
    private String[] dynamicCoeffExprs = new String[0];
    private double dynamicBase = 2.0;
    private String dynamicBaseExpr = "";
    private double dynamicClipMinX = Double.NaN, dynamicClipMaxX = Double.NaN;
    private String dynamicClipMinExpr = "", dynamicClipMaxExpr = "";
    private Button selectDynamicAttributeButton;
    private final List<EditBox> coeffEdits = new ArrayList<>();
    private final List<PlaceholderSuggestor> suggestors = new ArrayList<>();
    private String dynamicScoreboardObjective = "";
    private EditBox scoreboardObjectiveEdit;

    // Custom
    private String customDisplayText = "";
    private String customColor = "white";
    private EditBox customDisplayTextEdit;
    private EditBox customColorHexEdit;
    private boolean showPointer = true;

    //Tag
    private String tagName = "";

    private EditBox amplifierEdit, durationEdit, cooldownEdit, amountEdit, commandsEdit;
    private Button selectAttributeButton, selectSpellButton;
    private EditBox potionIdEdit;

    private static final List<String> COLORS = List.of(
            "white", "gold", "yellow", "red", "green", "blue", "gray", "dark_gray", "black"
    );

    public EffectEditScreen(Consumer<EffectEntry> onSave, Screen returnTo) {
        this(onSave, returnTo, null);
    }

    public EffectEditScreen(Consumer<EffectEntry> onSave, Screen returnTo, EffectEntry existing) {
        super(Component.translatable("visual_set_edit.gui.effect_edit.title"));
        this.onSave = onSave;
        this.returnTo = returnTo;
        if (existing != null) loadFromExisting(existing);
    }

    private void loadFromExisting(EffectEntry existing) {
        customDisplayText = existing.customDisplayText != null ? existing.customDisplayText : "";
        customColor = existing.customColor != null ? existing.customColor : "white";
        this.showPointer = existing.showPointer;
        effectType = EffectTypeRegistry.contains(existing.type) ? existing.type : EffectTypeRegistry.defaultId();
        EffectEditorRegistry.Editor editor = EffectEditorRegistry.get(effectType);
        if (editor != null) editor.loadFrom(this, existing);
    }

    @Override
    protected void init() {
        clearWidgets();
        suggestors.clear();
        int centerX = width / 2, totalWidth = 160, rowHeight = 18, spacing = 3, y = 30;
        List<String> types = EffectTypeRegistry.availableIds();
        if (!types.contains(effectType)) effectType = EffectTypeRegistry.defaultId();

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.type"), font));
        y += rowHeight;
        CycleButton<String> typeButton = CycleButton.<String>builder(s -> Component.translatable("visual_set_edit.gui.effect.type." + s))
                .withValues(types).displayOnlyValue().withInitialValue(effectType)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.type"), (btn, val) -> { effectType = val; init(); });
        addRenderableWidget(typeButton);
        y += rowHeight + spacing;

        EffectEditorRegistry.Editor editor = EffectEditorRegistry.get(effectType);
        if (editor != null) {
            y = editor.buildFields(this, centerX, y, totalWidth, rowHeight, spacing);
        }

        this.contentHeight = y + 30;
        if (scrollOffset > Math.max(0, contentHeight - this.height)) {
            scrollOffset = Math.max(0, contentHeight - this.height);
        }
    }

    // 通用显示字段：自定义文本、颜色、指针开关；由各类型编辑界面调用
    int buildCustomDisplayFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.custom_display_text"), font)); y += rowHeight;
        customDisplayTextEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.custom_display_text"));
        customDisplayTextEdit.setMaxLength(256); customDisplayTextEdit.setValue(customDisplayText);
        addRenderableWidget(customDisplayTextEdit); y += rowHeight + spacing;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.custom_color"), font)); y += rowHeight;
        String initialColor = COLORS.contains(customColor) ? customColor : COLORS.get(0);
        CycleButton<String> customColorButton = CycleButton.<String>builder(s -> Component.translatable("visual_set_edit.color." + s))
                .withValues(COLORS)
                .displayOnlyValue().withInitialValue(initialColor)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.effect.custom_color"), (btn, val) -> {
                            customColor = val;
                            if (customColorHexEdit != null) {
                                customColorHexEdit.setValue("");
                            }
                        });
        addRenderableWidget(customColorButton); y += rowHeight + spacing;
        // Hex 颜色输入框
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.custom_color_hex"), font));
        y += rowHeight;

        customColorHexEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.custom_color_hex"));
        customColorHexEdit.setMaxLength(5201314);
        if (!COLORS.contains(customColor)) {
            customColorHexEdit.setValue(customColor);
        } else {
            customColorHexEdit.setValue("");
        }
        customColorHexEdit.setResponder(s -> {
            if (!s.isEmpty()) {
                customColor = s;
            }
        });
        addRenderableWidget(customColorHexEdit);
        y += rowHeight + spacing;

        // 指针显示开关
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.show_pointer"), font));
        y += rowHeight;
        CycleButton<Boolean> pointerButton = CycleButton.<Boolean>builder(b ->
                        b ? Component.translatable("visual_set_edit.gui.on") : Component.translatable("visual_set_edit.gui.off"))
                .withValues(true, false)
                .displayOnlyValue()
                .withInitialValue(showPointer)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.effect.show_pointer"),
                        (btn, val) -> showPointer = val);
        addRenderableWidget(pointerButton);
        y += rowHeight + spacing;

        return y;
    }

    private int buildPotionFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.potion.target"), font)); y += rowHeight;
        CycleButton<String> targetButton = CycleButton.<String>builder(s ->
                        Component.translatable("visual_set_edit.gui.effect.potion.target." + s))
                .withValues("SELF", "ATTACK_TARGET", "IMMUNE").displayOnlyValue().withInitialValue(potionTarget)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.effect.potion.target"), (btn, val) -> {
                            potionTarget = val; init(); });
        addRenderableWidget(targetButton); y += rowHeight + spacing;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.potion.id"), font)); y += rowHeight;
        int editWidth = totalWidth - 22;
        potionIdEdit = new EditBox(font, centerX - totalWidth / 2, y, editWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.potion.id"));
        potionIdEdit.setMaxLength(256); potionIdEdit.setValue(selectedMobEffect != null ? selectedMobEffect.toString() : "");
        potionIdEdit.setResponder(s -> {
            String trimmed = s.trim();
            selectedMobEffect = trimmed.isEmpty() ? null : ResourceLocation.tryParse(trimmed);
        });
        addRenderableWidget(potionIdEdit);
        addRenderableWidget(Button.builder(Component.literal("📦"), btn -> {
            assert minecraft != null;
            minecraft.setScreen(new MobEffectListScreen(this, rl -> {
                selectedMobEffect = rl;
                if (potionIdEdit != null) potionIdEdit.setValue(rl.toString());
            }));
        }).pos(centerX - totalWidth / 2 + editWidth + 2, y).size(20, rowHeight).build());
        y += rowHeight + spacing;

        if (!"IMMUNE".equals(potionTarget)) {
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.potion.amplifier"), font)); y += rowHeight;
            amplifierEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.potion.amplifier"));
            amplifierEdit.setMaxLength(5201314); amplifierEdit.setValue(String.valueOf(amplifier));
            addRenderableWidget(amplifierEdit); y += rowHeight + spacing;

            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.potion.duration"), font)); y += rowHeight;
            durationEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.potion.duration"));
            durationEdit.setValue(String.valueOf(durationSeconds)); durationEdit.moveCursorToEnd();
            durationEdit.setResponder(val -> {
                try { durationSeconds = Integer.parseInt(val); } catch (NumberFormatException e) { durationSeconds = -1; }
                if (cooldownEdit != null) cooldownEdit.visible = (durationSeconds != -1);
            });
            addRenderableWidget(durationEdit); y += rowHeight + spacing;

            cooldownEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.potion.cooldown"));
            cooldownEdit.setValue(String.valueOf(cooldownSeconds)); cooldownEdit.setMaxLength(5201314);
            cooldownEdit.visible = (durationSeconds != -1);
            addRenderableWidget(cooldownEdit); y += rowHeight + spacing;
        } else { amplifierEdit = null; durationEdit = null; cooldownEdit = null; }
        if (!"IMMUNE".equals(potionTarget)) {
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.potion.show_particles"), font));
            y += rowHeight;
            CycleButton<Boolean> particleButton = CycleButton.<Boolean>builder(b ->
                            b ? Component.translatable("visual_set_edit.gui.on") : Component.translatable("visual_set_edit.gui.off"))
                    .withValues(true, false)
                    .displayOnlyValue()
                    .withInitialValue(showParticles)
                    .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                            Component.translatable("visual_set_edit.gui.effect.potion.show_particles"),
                            (btn, val) -> showParticles = val);
            addRenderableWidget(particleButton);
            y += rowHeight + spacing;
        }

        y = buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
        y += rowHeight + spacing;   // 保存按钮的高度
        return y;
    }

    private int buildAttributeFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.attribute.id"), font)); y += rowHeight;
        selectAttributeButton = Button.builder(getAttributeButtonText(), btn -> {
            assert minecraft != null;
            minecraft.setScreen(new AttributeListScreen(this, rl -> {
                selectedAttribute = rl;
                selectAttributeButton.setMessage(getAttributeButtonText());
            }));
        }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
        addRenderableWidget(selectAttributeButton); y += rowHeight + spacing;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.attribute.amount"), font)); y += rowHeight;
        amountEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.attribute.amount"));
        amountEdit.setValue(String.valueOf(amount)); addRenderableWidget(amountEdit); y += rowHeight + spacing;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.attribute.operation"), font)); y += rowHeight;
        CycleButton<AttributeModifier.Operation> opButton = CycleButton.<AttributeModifier.Operation>builder(op ->
                        Component.translatable("visual_set_edit.gui.effect.attribute.operation." + op.name().toLowerCase()))
                .withValues(AttributeModifier.Operation.values()).displayOnlyValue().withInitialValue(attrOperation)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.effect.attribute.operation"), (btn, val) -> attrOperation = val);
        addRenderableWidget(opButton); y += rowHeight + spacing;

        // 限时属性：生效时长（秒，-1 = 常驻）
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.attribute.duration"), font)); y += rowHeight;
        attrDurationEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.attribute.duration"));
        attrDurationEdit.setMaxLength(10);
        attrDurationEdit.setValue(String.valueOf(attrDurationSeconds));
        attrDurationEdit.setResponder(s -> {
            try { attrDurationSeconds = Integer.parseInt(s); } catch (Exception ignored) {}
        });
        addRenderableWidget(attrDurationEdit); y += rowHeight + spacing;

        // 限时属性：冷却（秒，0 = 无冷却）
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.attribute.cooldown"), font)); y += rowHeight;
        attrCooldownEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.attribute.cooldown"));
        attrCooldownEdit.setMaxLength(10);
        attrCooldownEdit.setValue(String.valueOf(attrCooldownSeconds));
        attrCooldownEdit.setResponder(s -> {
            try { attrCooldownSeconds = Integer.parseInt(s); } catch (Exception ignored) {}
        });
        addRenderableWidget(attrCooldownEdit); y += rowHeight + spacing;

        y = buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
        y += rowHeight + spacing;
        return y;
    }

    private int buildAbilityFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.ability.id"), font)); y += rowHeight;
        CycleButton<String> abButton = CycleButton.<String>builder(s -> {
                    return switch (s) {
                        case "FLIGHT" -> Component.translatable("visual_set_edit.gui.effect.ability.flight");
                        case "FALL_IMMUNITY" -> Component.translatable("visual_set_edit.gui.effect.ability.fall_immunity");
                        default -> {
                            AbilitySpec ext = AbilityTypeRegistry.get(s);
                            yield ext != null ? ext.displayName() : Component.literal(s);
                        }
                    };
                })
                .withValues(abilityValues()).displayOnlyValue().withInitialValue(abilityId)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.effect.ability.id"), (btn, val) -> {
                            boolean hadExt = AbilityTypeRegistry.get(abilityId) != null;
                            boolean hasExt = AbilityTypeRegistry.get(val) != null;
                            abilityId = val;
                            // 只在内置两项之间切换时不需要重建，保持原有行为
                            if (hadExt || hasExt) init();
                        });
        addRenderableWidget(abButton); y += rowHeight + spacing;

        // 外部注册的能力：字段由 AbilitySpec 描述
        AbilitySpec ext = AbilityTypeRegistry.get(abilityId);
        if (ext == null) {
            if (panelAbilityId != null) {
                abilityPanel.clear();
                panelAbilityId = null;
            }
        } else {
            if (!ext.id().equals(panelAbilityId)) {
                abilityPanel.clear();
                panelAbilityId = ext.id();
            }
            y = abilityPanel.render(FieldSpecPanel.hostOf(this), ext.fields(), centerX, y, totalWidth, rowHeight, spacing);
        }

        y = buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
        y += rowHeight + spacing;
        return y;
    }

    // 内置两项加外部注册的能力
    private static List<String> abilityValues() {
        List<String> ids = new ArrayList<>(List.of("FLIGHT", "FALL_IMMUNITY"));
        ids.addAll(AbilityTypeRegistry.ids());
        return ids;
    }

    void loadAbilityParams(AbilityEffectEntry entry) {
        abilityPanel.loadFrom(entry.params);
        panelAbilityId = entry.abilityId;
    }

    Map<String, Object> saveAbilityParams() {
        AbilitySpec ext = AbilityTypeRegistry.get(abilityId);
        return ext == null ? Map.of() : abilityPanel.collect(ext.fields());
    }

    private int buildCommandFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        // 触发时机选择
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.command.trigger"), font));
        y += rowHeight;
        CycleButton<CommandEffectEntry.Trigger> triggerButton = CycleButton.<CommandEffectEntry.Trigger>builder(
                        t -> Component.translatable("visual_set_edit.gui.effect.command.trigger." + t.name().toLowerCase()))
                .withValues(CommandEffectEntry.Trigger.values())
                .displayOnlyValue()
                .withInitialValue(commandTrigger)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.effect.command.trigger"),
                        (btn, val) -> {
                            commandTrigger = val;
                            init();
                        });
        addRenderableWidget(triggerButton);
        y += rowHeight + spacing;

        // 命令输入
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.command.activate"), font));
        y += rowHeight;
        commandsEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.command.activate"));
        commandsEdit.setMaxLength(5201314);
        commandsEdit.setValue(commands);
        addRenderableWidget(commandsEdit);
        y += rowHeight + spacing;

        // 概率输入
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.command.probability"), font));
        y += rowHeight;

        EditBox probabilityEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.command.probability"));
        probabilityEdit.setMaxLength(10);
        probabilityEdit.setValue(String.valueOf(commandProbability));
        probabilityEdit.setResponder(s -> {
            try {
                commandProbability = Double.parseDouble(s);
            } catch (Exception ignored) {}
        });
        addRenderableWidget(probabilityEdit);
        y += rowHeight + spacing;

        if (commandTrigger == CommandEffectEntry.Trigger.REPEAT) {
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.command.repeat_interval"), font));
            y += rowHeight;
            commandIntervalEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.command.repeat_interval"));
            commandIntervalEdit.setMaxLength(10);
            commandIntervalEdit.setValue(String.valueOf(commandRepeatInterval));
            addRenderableWidget(commandIntervalEdit);
            y += rowHeight + spacing;
        } else {
            commandIntervalEdit = null;
        }

        if (commandTrigger == CommandEffectEntry.Trigger.ON_INTERACT_BLOCK ||
                commandTrigger == CommandEffectEntry.Trigger.ON_INTERACT_ENTITY ||
                commandTrigger == CommandEffectEntry.Trigger.ON_PLACE_BLOCK ||
                commandTrigger == CommandEffectEntry.Trigger.ON_BREAK_BLOCK ||
                commandTrigger == CommandEffectEntry.Trigger.ON_KILL_SPECIFIC) {

            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.command.target_filter"), font));
            y += rowHeight;

            targetFilterButton = Button.builder(
                    getTargetFilterButtonText(),
                    btn -> {
                        assert minecraft != null;
                        minecraft.setScreen(new TargetFilterEditScreen(this, commandTargetFilter, newFilter -> {
                            commandTargetFilter = newFilter;
                            targetFilterButton.setMessage(getTargetFilterButtonText());
                        }));
                    }
            ).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
            addRenderableWidget(targetFilterButton);
            y += rowHeight + spacing;
        }

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.command.cooldown"), font));
        y += rowHeight;
        EditBox commandCooldownEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.command.cooldown"));
        commandCooldownEdit.setMaxLength(10);
        commandCooldownEdit.setValue(String.valueOf(commandCooldownSeconds));
        commandCooldownEdit.setResponder(s -> {
            try {
                commandCooldownSeconds = Integer.parseInt(s);
            } catch (Exception ignored) {}
        });
        addRenderableWidget(commandCooldownEdit);
        y += rowHeight + spacing;

        y = buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
        y += rowHeight + spacing;
        return y;
    }

    private int buildIronSpellFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.iron_spell.spell"), font)); y += rowHeight;
        selectSpellButton = Button.builder(getSpellButtonText(), btn -> {
            assert minecraft != null;
            Screen screen = IntegrationManager.createSpellListScreen(this, rl -> {
                spellId = rl.toString();
                selectSpellButton.setMessage(getSpellButtonText());
            });
            if (screen != null) minecraft.setScreen(screen);
        }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
        addRenderableWidget(selectSpellButton); y += rowHeight + spacing;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.iron_spell.level"), font)); y += rowHeight;
        spellLevelEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.iron_spell.level"));
        spellLevelEdit.setValue(String.valueOf(spellLevel)); spellLevelEdit.setMaxLength(5201314);
        addRenderableWidget(spellLevelEdit); y += rowHeight + spacing;
        y = buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
        y += rowHeight + spacing;
        return y;
    }

    private int buildSlotCountFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.slot_count.slot"), font)); y += rowHeight;
        selectSlotButton = Button.builder(getSlotCountSlotButtonText(), btn -> {
            assert minecraft != null;
            minecraft.setScreen(new SlotSelectionScreen(this, slotCountSlotId, newSlot -> {
                slotCountSlotId = newSlot;
                if (selectSlotButton != null) selectSlotButton.setMessage(getSlotCountSlotButtonText());
            }, true));
        }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
        addRenderableWidget(selectSlotButton); y += rowHeight + spacing;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.slot_count.amount"), font)); y += rowHeight;
        slotCountAmountEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.slot_count.amount"));
        slotCountAmountEdit.setMaxLength(256); slotCountAmountEdit.setValue(String.valueOf(slotCountAmount));
        addRenderableWidget(slotCountAmountEdit); y += rowHeight + spacing;
        y = buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
        y += rowHeight + spacing;
        return y;
    }

    private int buildSpellLevelBoostFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.spell_level_boost.spell"), font)); y += rowHeight;
        selectBoostSpellButton = Button.builder(getBoostSpellButtonText(), btn -> {
            assert minecraft != null;
            Screen screen = IntegrationManager.createSpellListScreen(this, rl -> {
                boostSpellId = rl.toString();
                selectBoostSpellButton.setMessage(getBoostSpellButtonText());
            });
            if (screen != null) minecraft.setScreen(screen);
        }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
        addRenderableWidget(selectBoostSpellButton); y += rowHeight + spacing;

        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.effect.spell_level_boost.all_spells"),
                btn -> { boostSpellId = ""; selectBoostSpellButton.setMessage(getBoostSpellButtonText()); }
        ).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build()); y += rowHeight + spacing;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.spell_level_boost.amount"), font)); y += rowHeight;
        boostAmountEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.spell_level_boost.amount"));
        boostAmountEdit.setMaxLength(9); boostAmountEdit.setValue(String.valueOf(boostAmount));
        addRenderableWidget(boostAmountEdit); y += rowHeight + spacing;
        y = buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
        y += rowHeight + spacing;
        return y;
    }

    private int buildL2HostilityTraitFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.potion.target.ATTACK_TARGET"), font));
        y += rowHeight + spacing;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.l2hostility_trait.trait"), font));
        y += rowHeight;
        selectL2TraitButton = Button.builder(getL2TraitButtonText(), btn -> {
            assert minecraft != null;
            Screen screen = IntegrationManager.createL2TraitListScreen(this, rl -> {
                l2traitId = rl.toString();
                selectL2TraitButton.setMessage(getL2TraitButtonText());
            });
            if (screen != null) minecraft.setScreen(screen);
        }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
        addRenderableWidget(selectL2TraitButton);
        y += rowHeight + spacing;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.l2hostility_trait.level"), font));
        y += rowHeight;
        l2traitLevelEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.l2hostility_trait.level"));
        l2traitLevelEdit.setMaxLength(3);
        l2traitLevelEdit.setValue(String.valueOf(l2traitLevel));
        addRenderableWidget(l2traitLevelEdit);
        y += rowHeight + spacing;

        y = buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
        y += rowHeight + spacing;
        return y;
    }

    private int buildL2DifficultyModFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.l2_difficulty_mod.player"), font));
        y += rowHeight + spacing;

        // 变化量输入
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.l2_difficulty_mod.amount"), font));
        y += rowHeight;
        EditBox amountEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.l2_difficulty_mod.amount"));
        amountEdit.setMaxLength(10);
        amountEdit.setValue(String.valueOf(l2DifficultyAmount));
        amountEdit.setResponder(s -> {
            try { l2DifficultyAmount = Integer.parseInt(s); } catch (Exception ignored) {}
        });
        addRenderableWidget(amountEdit);
        y += rowHeight + spacing;

        y = buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
        y += rowHeight + spacing;
        return y;
    }

    private int buildDynamicAttributeFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        // 属性选择（目标属性）
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.attribute.id"), font)); y += rowHeight;
        selectDynamicAttributeButton = Button.builder(getDynamicAttributeButtonText(), btn -> {
            assert minecraft != null;
            minecraft.setScreen(new AttributeListScreen(this, rl -> {
                dynamicAttributeId = rl.toString();
                selectDynamicAttributeButton.setMessage(getDynamicAttributeButtonText());
            }));
        }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
        addRenderableWidget(selectDynamicAttributeButton); y += rowHeight + spacing;

        // 操作
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.attribute.operation"), font)); y += rowHeight;
        CycleButton<AttributeModifier.Operation> opButton = CycleButton.<AttributeModifier.Operation>builder(op ->
                        Component.translatable("visual_set_edit.gui.effect.attribute.operation." + op.name().toLowerCase()))
                .withValues(AttributeModifier.Operation.values()).displayOnlyValue().withInitialValue(dynamicOperation)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.effect.attribute.operation"), (btn, val) -> dynamicOperation = val);
        addRenderableWidget(opButton); y += rowHeight + spacing;

        // 变量
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.variable"), font)); y += rowHeight;
        CycleButton<DynamicAttributeEffectEntry.VariableType> varButton = CycleButton.<DynamicAttributeEffectEntry.VariableType>builder(
                        v -> Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.var." + v.name()))
                .withValues(DynamicAttributeEffectEntry.VariableType.values()).displayOnlyValue().withInitialValue(dynamicVariable)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.variable"), (btn, val) -> {
                            dynamicVariable = val;
                            init(); // 切换变量类型后刷新界面，以显示/隐藏源属性选择器
                        });
        addRenderableWidget(varButton); y += rowHeight + spacing;

        // 当变量类型为“属性值”时，显示源属性选择与取值模式
        if (dynamicVariable == DynamicAttributeEffectEntry.VariableType.ATTRIBUTE_VALUE) {
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.source_attribute"), font));
            y += rowHeight;

            selectSourceAttributeButton = Button.builder(getDynamicSourceAttributeButtonText(), btn -> {
                assert minecraft != null;
                minecraft.setScreen(new AttributeListScreen(this, rl -> {
                    dynamicSourceAttributeId = rl.toString();
                    if (selectSourceAttributeButton != null) {
                        selectSourceAttributeButton.setMessage(getDynamicSourceAttributeButtonText());
                    }
                }));
            }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
            addRenderableWidget(selectSourceAttributeButton);
            y += rowHeight + spacing;
        }

        //当变量类型为“计分板”时
        if (dynamicVariable == DynamicAttributeEffectEntry.VariableType.SCOREBOARD_VALUE) {
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.scoreboard_objective"), font));
            y += rowHeight;

            int editWidth = totalWidth - 22;
            scoreboardObjectiveEdit = new EditBox(font, centerX - totalWidth / 2, y, editWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.scoreboard_objective"));
            scoreboardObjectiveEdit.setMaxLength(5201314);
            scoreboardObjectiveEdit.setValue(dynamicScoreboardObjective);
            scoreboardObjectiveEdit.setResponder(s -> dynamicScoreboardObjective = s);
            addRenderableWidget(scoreboardObjectiveEdit);

            Button selectScoreboardButton = Button.builder(Component.literal("📦"), btn -> {
                assert minecraft != null;
                minecraft.setScreen(new ScoreboardObjectiveListScreen(this, name -> {
                    dynamicScoreboardObjective = name;
                    if (scoreboardObjectiveEdit != null) {
                        scoreboardObjectiveEdit.setValue(name);
                    }
                }));
            }).pos(centerX - totalWidth / 2 + editWidth + 2, y).size(20, rowHeight).build();
            addRenderableWidget(selectScoreboardButton);
            y += rowHeight + spacing;
        }

        //当变量类型为“药水等级”时
        if (dynamicVariable == DynamicAttributeEffectEntry.VariableType.POTION_LEVEL) {
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.source_potion"), font));
            y += rowHeight;

            int editWidth = totalWidth - 22;
            sourcePotionEditBox = new EditBox(font, centerX - totalWidth / 2, y, editWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.source_potion"));
            sourcePotionEditBox.setMaxLength(5201314);
            sourcePotionEditBox.setValue(dynamicSourcePotionId);
            sourcePotionEditBox.setResponder(s -> dynamicSourcePotionId = s.trim());
            addRenderableWidget(sourcePotionEditBox);

            Button selectSourcePotionButton = Button.builder(Component.literal("📦"), btn -> {
                assert minecraft != null;
                minecraft.setScreen(new MobEffectListScreen(this, rl -> {
                    dynamicSourcePotionId = rl.toString();
                    if (sourcePotionEditBox != null) {
                        sourcePotionEditBox.setValue(rl.toString());
                    }
                }));
            }).pos(centerX - totalWidth / 2 + editWidth + 2, y).size(20, rowHeight).build();
            addRenderableWidget(selectSourcePotionButton);
            y += rowHeight + spacing;
        }

        // 公式类型
        StringWidget formulaLabel = new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.formula"), font);
        addRenderableWidget(formulaLabel); y += rowHeight;
        CycleButton<DynamicAttributeEffectEntry.FormulaType> formulaBtn = CycleButton.<DynamicAttributeEffectEntry.FormulaType>builder(
                        f -> Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.formula." + f.name()))
                .withValues(DynamicAttributeEffectEntry.FormulaType.values()).displayOnlyValue().withInitialValue(dynamicFormula)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.formula"), (btn, val) -> {
                            dynamicFormula = val;
                            init();
                        });
        addRenderableWidget(formulaBtn);
        y += rowHeight + spacing;

        // 系数输入
        coeffEdits.clear();
        suggestors.clear();
        switch (dynamicFormula) {
            case LINEAR -> {
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "a", 0);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "b", 1);
                y += rowHeight + spacing;
            }
            case QUADRATIC -> {
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "a", 0);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "b", 1);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "c", 2);
                y += rowHeight + spacing;
            }
            case EXPONENTIAL, LOGARITHMIC -> {
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "a", 0);
                y += rowHeight + spacing;
                // 底数
                addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.base"), font));
                y += rowHeight;
                EditBox baseEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight, Component.literal("base"));
                baseEdit.setMaxLength(5201314);
                baseEdit.setValue(!dynamicBaseExpr.isEmpty() ? dynamicBaseExpr : String.valueOf(dynamicBase));
                baseEdit.setResponder(s -> {
                    String trimmed = s.trim();
                    dynamicBaseExpr = trimmed;
                    try { dynamicBase = Double.parseDouble(trimmed); } catch (Exception ignored) {}
                });
                addRenderableWidget(baseEdit);
                suggestors.add(new PlaceholderSuggestor(baseEdit));
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "c", 1);
                y += rowHeight + spacing;
            }
            case POWER -> {
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "a", 0);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "k", 1);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "c", 2);
                y += rowHeight + spacing;
            }
            case STEP -> {
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "a", 0);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "b", 1);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "c", 2);
                y += rowHeight + spacing;
            }
            case SIGMOID -> {
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "a", 0);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "b", 1);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "c", 2);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "d", 3);
                y += rowHeight + spacing;
            }
            case SINE, TRIANGLE, SQUARE, SAWTOOTH -> {
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "a", 0);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "T", 1);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "p", 2);
                y += rowHeight + spacing;
                addCoeffEdit(centerX, y, totalWidth, rowHeight, spacing, "c", 3);
                y += rowHeight + spacing;
            }
        }

        // 裁剪范围
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.clip_min"), font)); y += rowHeight;
        EditBox dynamicClipMinEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight, Component.literal(""));
        dynamicClipMinEdit.setMaxLength(5201314);
        dynamicClipMinEdit.setValue(!dynamicClipMinExpr.isEmpty() ? dynamicClipMinExpr
                : (Double.isNaN(dynamicClipMinX) ? "" : String.valueOf(dynamicClipMinX)));
        dynamicClipMinEdit.setResponder(s -> {
            String trimmed = s.trim();
            dynamicClipMinExpr = trimmed;
            if (trimmed.isEmpty()) dynamicClipMinX = Double.NaN;
            else { try { dynamicClipMinX = Double.parseDouble(trimmed); } catch (Exception ignored) {} }
        });
        addRenderableWidget(dynamicClipMinEdit);
        suggestors.add(new PlaceholderSuggestor(dynamicClipMinEdit));
        y += rowHeight + spacing;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.clip_max"), font)); y += rowHeight;
        EditBox dynamicClipMaxEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight, Component.literal(""));
        dynamicClipMaxEdit.setMaxLength(5201314);
        dynamicClipMaxEdit.setValue(!dynamicClipMaxExpr.isEmpty() ? dynamicClipMaxExpr
                : (Double.isNaN(dynamicClipMaxX) ? "" : String.valueOf(dynamicClipMaxX)));
        dynamicClipMaxEdit.setResponder(s -> {
            String trimmed = s.trim();
            dynamicClipMaxExpr = trimmed;
            if (trimmed.isEmpty()) dynamicClipMaxX = Double.NaN;
            else { try { dynamicClipMaxX = Double.parseDouble(trimmed); } catch (Exception ignored) {} }
        });
        addRenderableWidget(dynamicClipMaxEdit);
        suggestors.add(new PlaceholderSuggestor(dynamicClipMaxEdit));
        y += rowHeight + spacing;

        y = buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
        y += rowHeight + spacing;
        return y;
    }

    private int buildTagFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.tag.name"), font));
        y += rowHeight;
        EditBox tagEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.tag.name"));
        tagEdit.setMaxLength(256);
        tagEdit.setValue(tagName);
        tagEdit.setResponder(s -> tagName = s);
        addRenderableWidget(tagEdit);
        y += rowHeight + spacing;

        y = buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
        y += rowHeight + spacing;
        return y;
    }

    private void addCoeffEdit(int centerX, int y, int totalWidth, int rowHeight, int spacing, String label, int index) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, 20, rowHeight,
                Component.literal(label + ":"), font));
        EditBox edit = new EditBox(font, centerX - totalWidth / 2 + 22, y, totalWidth - 22, rowHeight, Component.literal(label));
        edit.setMaxLength(5201314);
        // 反填优先表达式字段（玩家填过 %属性id% 或算式则原样显示），否则用数值字段
        String expr = index < dynamicCoeffExprs.length ? dynamicCoeffExprs[index] : null;
        if (expr != null && !expr.isBlank()) {
            edit.setValue(expr);
        } else if (index < dynamicCoeffs.length) {
            edit.setValue(String.valueOf(dynamicCoeffs[index]));
        } else {
            edit.setValue("0");
        }
        edit.setResponder(s -> {
            String trimmed = s.trim();
            // 原始字符串始终存入表达式字段（支持 %属性id% / 算式 / 纯数字）
            if (index >= dynamicCoeffExprs.length) {
                String[] newArr = new String[index + 1];
                System.arraycopy(dynamicCoeffExprs, 0, newArr, 0, dynamicCoeffExprs.length);
                dynamicCoeffExprs = newArr;
            }
            dynamicCoeffExprs[index] = trimmed;
            // 纯数字时同步数值字段（保留老结构兼容）；其他输入只走表达式字段
            try {
                double val = Double.parseDouble(trimmed);
                if (index >= dynamicCoeffs.length) {
                    double[] newArr = new double[index + 1];
                    System.arraycopy(dynamicCoeffs, 0, newArr, 0, dynamicCoeffs.length);
                    dynamicCoeffs = newArr;
                }
                dynamicCoeffs[index] = val;
            } catch (Exception ignored) {}
        });
        addRenderableWidget(edit);
        coeffEdits.add(edit);
        suggestors.add(new PlaceholderSuggestor(edit));
    }

    private Component getDynamicAttributeButtonText() {
        if (dynamicAttributeId == null || dynamicAttributeId.isEmpty())
            return Component.translatable("visual_set_edit.gui.click_select_item");
        ResourceLocation rl = ResourceLocation.tryParse(dynamicAttributeId);
        if (rl != null) {
            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(rl);
            if (attr != null) return Component.translatable(attr.getDescriptionId());
        }
        return Component.literal(dynamicAttributeId);
    }

    private Component getL2TraitButtonText() {
        String name = IntegrationManager.getL2TraitDisplayName(l2traitId);
        if (name != null) return Component.literal(name);
        if (l2traitId == null || l2traitId.isEmpty()) return Component.translatable("visual_set_edit.gui.click_select_item");
        return Component.literal(l2traitId);
    }

    private Component getBoostSpellButtonText() {
        if (boostSpellId == null || boostSpellId.isEmpty())
            return Component.translatable("visual_set_edit.gui.effect.spell_level_boost.all_spells");
        String name = IntegrationManager.getSpellDisplayName(boostSpellId);
        return name != null ? Component.literal(name) : Component.literal(boostSpellId);
    }

    private Component getSpellButtonText() {
        if (spellId == null || spellId.isEmpty()) return Component.translatable("visual_set_edit.gui.click_select_item");
        String name = IntegrationManager.getSpellDisplayName(spellId);
        return name != null ? Component.literal(name) : Component.literal(spellId);
    }

    // 保存按钮；由各类型编辑界面在字段渲染完后调用
    void saveButton(int centerX, int y, int totalWidth, int rowHeight) {
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.save"), b -> {
            EffectEntry effect = createEffect();
            if (effect != null) { onSave.accept(effect); assert minecraft != null; minecraft.setScreen(returnTo); }
        }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build());
    }

    private Component getAttributeButtonText() {
        if (selectedAttribute != null) {
            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(selectedAttribute);
            if (attr != null) return Component.translatable(attr.getDescriptionId());
            return Component.literal(selectedAttribute.toString());
        }
        return Component.translatable("visual_set_edit.gui.click_select_item");
    }

    private Component getSlotCountSlotButtonText() {
        if (slotCountSlotId.isEmpty()) return Component.translatable("visual_set_edit.gui.click_select_slot");
        return Component.literal(slotCountSlotId);
    }

    private Component getDynamicSourceAttributeButtonText() {
        if (dynamicSourceAttributeId == null || dynamicSourceAttributeId.isEmpty()) {
            return Component.translatable("visual_set_edit.gui.click_select_item");
        }
        ResourceLocation rl = ResourceLocation.tryParse(dynamicSourceAttributeId);
        if (rl != null) {
            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(rl);
            if (attr != null) {
                return Component.translatable(attr.getDescriptionId());
            }
        }
        return Component.literal(dynamicSourceAttributeId);
    }

    private Component getTargetFilterButtonText() {
        if (commandTargetFilter == null || commandTargetFilter.isEmpty()) {
            return Component.translatable("visual_set_edit.gui.effect.command.target_filter.none");
        }
        StringBuilder sb = new StringBuilder();
        if (commandTargetFilter.blockId != null) sb.append("Block: ").append(commandTargetFilter.blockId);
        if (commandTargetFilter.blockTag != null) sb.append("BlockTag: ").append(commandTargetFilter.blockTag);
        if (commandTargetFilter.entityTypeId != null) sb.append("Entity: ").append(commandTargetFilter.entityTypeId);
        if (commandTargetFilter.entityTypeTag != null) sb.append("EntityTag: ").append(commandTargetFilter.entityTypeTag);
        return Component.literal(sb.toString());
    }

    private EffectEntry createEffect() {
        EffectEditorRegistry.Editor editor = EffectEditorRegistry.get(effectType);
        EffectEntry e = editor != null ? editor.create(this) : null;
        if (e != null) {
            e.customDisplayText = customDisplayTextEdit != null ? customDisplayTextEdit.getValue() : customDisplayText;
            e.customColor = customColor;
            e.showPointer = showPointer;
        }
        return e;
    }

    private String[] buildCoeffExpressions() {
        int len = Math.max(dynamicCoeffs.length, dynamicCoeffExprs.length);
        boolean any = false;
        String[] arr = new String[len];
        for (int i = 0; i < len; i++) {
            String expr = i < dynamicCoeffExprs.length ? dynamicCoeffExprs[i] : null;
            arr[i] = (expr != null && !expr.isBlank()) ? expr.trim() : null;
            if (arr[i] != null) any = true;
        }
        return any ? arr : null;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.renderBackground(graphics);

        int offset = scrollOffset;

        for (var child : children()) {
            if (child instanceof AbstractWidget widget) {
                widget.setY(widget.getY() - offset);
            }
        }

        super.render(graphics, mouseX, mouseY, partial);

        for (PlaceholderSuggestor suggestor : suggestors) {
            suggestor.render(graphics, mouseX, mouseY);
        }

        for (var child : children()) {
            if (child instanceof AbstractWidget widget) {
                widget.setY(widget.getY() + offset);
            }
        }

        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.edit_effect"),
                width / 2, 10, 0xFFFFFF);

        // 绘制滚动条（原有逻辑保持不变）
        if (contentHeight > this.height) {
            int scrollBarHeight = (int) ((float) this.height / contentHeight * this.height);
            int scrollBarY = (int) ((float) scrollOffset / (contentHeight - this.height) * (this.height - scrollBarHeight));
            int scrollBarX = this.width - 4;
            graphics.fill(scrollBarX, 0, this.width, this.height, 0x22FFFFFF);
            graphics.fill(scrollBarX, scrollBarY, this.width, scrollBarY + scrollBarHeight, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (PlaceholderSuggestor suggestor : suggestors) {
            if (suggestor.mouseClicked(mouseX, mouseY)) return true;
        }
        return super.mouseClicked(mouseX, mouseY + scrollOffset, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (PlaceholderSuggestor suggestor : suggestors) {
            if (suggestor.keyPressed(keyCode)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        for (PlaceholderSuggestor suggestor : suggestors) {
            if (suggestor.mouseScrolled(mouseX, mouseY, scrollDelta)) return true;
        }
        int maxScroll = Math.max(0, contentHeight - this.height);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollDelta * 20));
        return true;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.setScreen(returnTo); }

    public CommandEffectEntry.Mode getCommandMode() {
        return commandMode;
    }

    // 供通用字段编辑器使用
    Font fieldFont() {
        return font;
    }

    void attachField(AbstractWidget widget) {
        addRenderableWidget(widget);
    }

    // 效果编辑界面注册
    static void registerEditors() {
        EffectEditorRegistry.register("potion", new EffectEditorRegistry.Editor() {
            @Override
            public int buildFields(EffectEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                return screen.buildPotionFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(EffectEditScreen screen, EffectEntry entry) {
                if (!(entry instanceof PotionEffectEntry pot)) return;
                screen.potionTarget = pot.target != null ? pot.target : "SELF";
                screen.selectedMobEffect = ResourceLocation.tryParse(pot.mobEffectId);
                screen.amplifier = pot.amplifier;
                screen.durationSeconds = pot.durationSeconds;
                screen.cooldownSeconds = pot.cooldownSeconds;
                screen.showParticles = pot.showParticles;
            }

            @Override
            public EffectEntry create(EffectEditScreen screen) {
                PotionEffectEntry pot = new PotionEffectEntry();
                pot.target = screen.potionTarget;
                if (screen.potionIdEdit != null && !screen.potionIdEdit.getValue().trim().isEmpty())
                    pot.mobEffectId = screen.potionIdEdit.getValue().trim();
                else if (screen.selectedMobEffect != null) pot.mobEffectId = screen.selectedMobEffect.toString();
                else pot.mobEffectId = "";
                if (!"IMMUNE".equals(screen.potionTarget)) {
                    try { pot.amplifier = Integer.parseInt(screen.amplifierEdit.getValue()); } catch (Exception ignored) {}
                    try { pot.durationSeconds = Integer.parseInt(screen.durationEdit.getValue()); } catch (Exception ignored) {}
                    if (screen.durationSeconds != -1 && screen.cooldownEdit != null)
                        try { pot.cooldownSeconds = Integer.parseInt(screen.cooldownEdit.getValue()); } catch (Exception ignored) {}
                    else pot.cooldownSeconds = 0;
                } else { pot.amplifier = 0; pot.durationSeconds = -1; pot.cooldownSeconds = 0; }
                pot.showParticles = screen.showParticles;
                return pot;
            }
        });

        EffectEditorRegistry.register("attribute", new EffectEditorRegistry.Editor() {
            @Override
            public int buildFields(EffectEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                return screen.buildAttributeFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(EffectEditScreen screen, EffectEntry entry) {
                if (!(entry instanceof AttributeEffectEntry attr)) return;
                screen.selectedAttribute = ResourceLocation.tryParse(attr.attributeId);
                screen.amount = attr.amount;
                screen.attrOperation = attr.operation;
                screen.attrDurationSeconds = attr.durationSeconds;
                screen.attrCooldownSeconds = attr.cooldownSeconds;
            }

            @Override
            public EffectEntry create(EffectEditScreen screen) {
                AttributeEffectEntry attr = new AttributeEffectEntry();
                attr.attributeId = screen.selectedAttribute != null ? screen.selectedAttribute.toString() : "";
                try { attr.amount = Double.parseDouble(screen.amountEdit.getValue()); } catch (Exception ignored) {}
                attr.operation = screen.attrOperation;
                attr.durationSeconds = screen.attrDurationEdit != null ? screen.attrDurationSeconds : -1;
                attr.cooldownSeconds = screen.attrCooldownEdit != null ? Math.max(0, screen.attrCooldownSeconds) : 0;
                return attr;
            }
        });

        EffectEditorRegistry.register("ability", new EffectEditorRegistry.Editor() {
            @Override
            public int buildFields(EffectEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                return screen.buildAbilityFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(EffectEditScreen screen, EffectEntry entry) {
                if (!(entry instanceof AbilityEffectEntry ab)) return;
                screen.abilityId = ab.abilityId;
                screen.loadAbilityParams(ab);
            }

            @Override
            public EffectEntry create(EffectEditScreen screen) {
                AbilityEffectEntry ab = new AbilityEffectEntry();
                ab.abilityId = screen.abilityId;
                ab.params.putAll(screen.saveAbilityParams());
                return ab;
            }
        });

        EffectEditorRegistry.register("command", new EffectEditorRegistry.Editor() {
            @Override
            public int buildFields(EffectEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                return screen.buildCommandFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(EffectEditScreen screen, EffectEntry entry) {
                if (!(entry instanceof CommandEffectEntry cmd)) return;
                screen.commandTrigger = cmd.trigger != null ? cmd.trigger : CommandEffectEntry.Trigger.ACTIVATE;
                screen.commands = cmd.commands != null && !cmd.commands.isEmpty()
                        ? String.join(";", cmd.commands)
                        : (cmd.activateCommands != null && !cmd.activateCommands.isEmpty()
                        ? String.join(";", cmd.activateCommands) : "");
                screen.commandMode = cmd.mode != null ? cmd.mode : CommandEffectEntry.Mode.IMPULSE;
                screen.commandRepeatInterval = cmd.repeatIntervalSeconds > 0 ? cmd.repeatIntervalSeconds : 1;
                screen.commandProbability = cmd.probability;
                screen.commandTargetFilter = Objects.requireNonNullElseGet(cmd.targetFilter, TargetFilter::new);
                screen.commandCooldownSeconds = cmd.cooldownSeconds;
            }

            @Override
            public EffectEntry create(EffectEditScreen screen) {
                CommandEffectEntry cmd = new CommandEffectEntry();
                cmd.trigger = screen.commandTrigger;
                if (screen.commandsEdit != null && !screen.commandsEdit.getValue().trim().isEmpty()) {
                    cmd.commands = List.of(screen.commandsEdit.getValue().split(";"));
                }
                if (screen.commandTrigger == CommandEffectEntry.Trigger.REPEAT && screen.commandIntervalEdit != null) {
                    try { cmd.repeatIntervalSeconds = Integer.parseInt(screen.commandIntervalEdit.getValue()); } catch (Exception ex) { cmd.repeatIntervalSeconds = 1; }
                } else {
                    cmd.repeatIntervalSeconds = 0;
                }
                cmd.activateCommands = cmd.commands;
                cmd.probability = screen.commandProbability;
                cmd.targetFilter = screen.commandTargetFilter;
                cmd.cooldownSeconds = screen.commandCooldownSeconds;
                return cmd;
            }
        });

        EffectEditorRegistry.register("dynamic_attribute", new EffectEditorRegistry.Editor() {
            @Override
            public int buildFields(EffectEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                return screen.buildDynamicAttributeFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(EffectEditScreen screen, EffectEntry entry) {
                if (!(entry instanceof DynamicAttributeEffectEntry dynAttr)) return;
                screen.dynamicAttributeId = dynAttr.attributeId != null ? dynAttr.attributeId : "";
                screen.dynamicOperation = dynAttr.operation;
                screen.dynamicVariable = dynAttr.variableType;
                screen.dynamicFormula = dynAttr.formulaType;
                screen.dynamicCoeffs = dynAttr.coefficients != null ? dynAttr.coefficients.clone() : new double[]{0, 0};
                screen.dynamicCoeffExprs = dynAttr.coeffExpressions != null ? dynAttr.coeffExpressions.clone() : new String[0];
                screen.dynamicBase = dynAttr.base;
                screen.dynamicBaseExpr = dynAttr.baseExpression != null ? dynAttr.baseExpression : "";
                screen.dynamicClipMinX = dynAttr.clipMinX;
                screen.dynamicClipMaxX = dynAttr.clipMaxX;
                screen.dynamicClipMinExpr = dynAttr.clipMinExpression != null ? dynAttr.clipMinExpression : "";
                screen.dynamicClipMaxExpr = dynAttr.clipMaxExpression != null ? dynAttr.clipMaxExpression : "";
                screen.dynamicSourceAttributeId = dynAttr.sourceAttributeId != null ? dynAttr.sourceAttributeId : "";
                screen.dynamicScoreboardObjective = dynAttr.scoreboardObjective != null ? dynAttr.scoreboardObjective : "";
                screen.dynamicSourcePotionId = dynAttr.sourcePotionId != null ? dynAttr.sourcePotionId : "";
            }

            @Override
            public EffectEntry create(EffectEditScreen screen) {
                DynamicAttributeEffectEntry dyn = new DynamicAttributeEffectEntry();
                dyn.attributeId = screen.dynamicAttributeId;
                dyn.operation = screen.dynamicOperation;
                dyn.variableType = screen.dynamicVariable;
                dyn.formulaType = screen.dynamicFormula;
                dyn.coefficients = screen.dynamicCoeffs.clone();
                dyn.coeffExpressions = screen.buildCoeffExpressions();
                dyn.base = screen.dynamicBase;
                dyn.baseExpression = screen.dynamicBaseExpr.isEmpty() ? null : screen.dynamicBaseExpr;
                dyn.clipMinX = screen.dynamicClipMinX;
                dyn.clipMaxX = screen.dynamicClipMaxX;
                dyn.clipMinExpression = screen.dynamicClipMinExpr.isEmpty() ? null : screen.dynamicClipMinExpr;
                dyn.clipMaxExpression = screen.dynamicClipMaxExpr.isEmpty() ? null : screen.dynamicClipMaxExpr;
                dyn.sourceAttributeId = screen.dynamicSourceAttributeId;
                dyn.scoreboardObjective = screen.dynamicScoreboardObjective;
                dyn.sourcePotionId = screen.dynamicSourcePotionId;
                return dyn;
            }
        });

        EffectEditorRegistry.register("tag", new EffectEditorRegistry.Editor() {
            @Override
            public int buildFields(EffectEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                return screen.buildTagFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(EffectEditScreen screen, EffectEntry entry) {
                if (!(entry instanceof TagEffectEntry tagEffect)) return;
                screen.tagName = tagEffect.tagName != null ? tagEffect.tagName : "";
            }

            @Override
            public EffectEntry create(EffectEditScreen screen) {
                TagEffectEntry tag = new TagEffectEntry();
                tag.tagName = screen.tagName;
                return tag;
            }
        });

        EffectEditorRegistry.register("iron_spell", new EffectEditorRegistry.Editor() {
            @Override
            public int buildFields(EffectEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                return screen.buildIronSpellFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(EffectEditScreen screen, EffectEntry entry) {
                if (!(entry instanceof IronSpellEffectEntry iron)) return;
                screen.spellId = iron.spellId != null ? iron.spellId : "";
                screen.spellLevel = iron.spellLevel > 0 ? iron.spellLevel : 1;
            }

            @Override
            public EffectEntry create(EffectEditScreen screen) {
                IronSpellEffectEntry iron = new IronSpellEffectEntry();
                iron.spellId = screen.spellId;
                try { iron.spellLevel = Integer.parseInt(screen.spellLevelEdit.getValue()); } catch (Exception ignored) {}
                return iron;
            }
        });

        EffectEditorRegistry.register("spell_level_boost", new EffectEditorRegistry.Editor() {
            @Override
            public int buildFields(EffectEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                return screen.buildSpellLevelBoostFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(EffectEditScreen screen, EffectEntry entry) {
                if (!(entry instanceof SpellLevelBoostEffectEntry boost)) return;
                screen.boostSpellId = boost.spellId != null ? boost.spellId : "";
                screen.boostAmount = boost.boostAmount > 0 ? boost.boostAmount : 1;
            }

            @Override
            public EffectEntry create(EffectEditScreen screen) {
                SpellLevelBoostEffectEntry boost = new SpellLevelBoostEffectEntry();
                boost.spellId = screen.boostSpellId.isEmpty() ? null : screen.boostSpellId;
                try { boost.boostAmount = Integer.parseInt(screen.boostAmountEdit.getValue()); } catch (Exception ex) { boost.boostAmount = 1; }
                return boost;
            }
        });

        EffectEditorRegistry.register("slot_count", new EffectEditorRegistry.Editor() {
            @Override
            public int buildFields(EffectEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                return screen.buildSlotCountFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(EffectEditScreen screen, EffectEntry entry) {
                if (!(entry instanceof SlotCountEffectEntry slot)) return;
                screen.slotCountSlotId = slot.slotId != null ? slot.slotId : "";
                screen.slotCountAmount = slot.amount;
            }

            @Override
            public EffectEntry create(EffectEditScreen screen) {
                SlotCountEffectEntry slot = new SlotCountEffectEntry();
                slot.slotId = screen.slotCountSlotId;
                try { slot.amount = Integer.parseInt(screen.slotCountAmountEdit.getValue()); } catch (Exception ignored) {}
                return slot;
            }
        });

        EffectEditorRegistry.register("l2hostility_trait", new EffectEditorRegistry.Editor() {
            @Override
            public int buildFields(EffectEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                return screen.buildL2HostilityTraitFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(EffectEditScreen screen, EffectEntry entry) {
                if (!(entry instanceof L2HostilityTraitEffectEntry traitEff)) return;
                screen.l2traitId = traitEff.traitId != null ? traitEff.traitId : "";
                screen.l2traitLevel = traitEff.level;
            }

            @Override
            public EffectEntry create(EffectEditScreen screen) {
                L2HostilityTraitEffectEntry trait = new L2HostilityTraitEffectEntry();
                trait.target = "ATTACK_TARGET";
                trait.traitId = screen.l2traitId.isEmpty() ? null : screen.l2traitId;
                try { trait.level = Integer.parseInt(screen.l2traitLevelEdit.getValue()); } catch (Exception ex) { trait.level = 1; }
                return trait;
            }
        });

        EffectEditorRegistry.register("l2_difficulty_mod", new EffectEditorRegistry.Editor() {
            @Override
            public int buildFields(EffectEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                return screen.buildL2DifficultyModFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(EffectEditScreen screen, EffectEntry entry) {
                if (!(entry instanceof L2DifficultyModEffectEntry mod)) return;
                screen.l2DifficultyAmount = mod.amount;
            }

            @Override
            public EffectEntry create(EffectEditScreen screen) {
                L2DifficultyModEffectEntry mod = new L2DifficultyModEffectEntry();
                mod.amount = screen.l2DifficultyAmount;
                return mod;
            }
        });
    }
}