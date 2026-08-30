package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.data.TargetFilter;
import com.sal_fish.visual_set_edit.data.effect.*;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class EffectEditScreen extends Screen {
    private int scrollOffset = 0;
    private int contentHeight;

    private final Consumer<EffectEntry> onSave;
    private final Screen returnTo;
    private final EffectEntry existingEffect;
    private String effectType = "potion";

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

    // Ability
    private String abilityId = "FLIGHT";

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
    private double dynamicBase = 2.0;
    private double dynamicClipMinX = Double.NaN, dynamicClipMaxX = Double.NaN;
    private EditBox dynamicClipMinEdit, dynamicClipMaxEdit;
    private Button selectDynamicAttributeButton;
    private final List<EditBox> coeffEdits = new ArrayList<>();
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
        this.existingEffect = existing;
        if (existing != null) loadFromExisting(existing);
    }

    private void loadFromExisting(EffectEntry existing) {
        customDisplayText = existing.customDisplayText != null ? existing.customDisplayText : "";
        customColor = existing.customColor != null ? existing.customColor : "white";
        this.showPointer = existing.showPointer;
        if (existing instanceof PotionEffectEntry pot) {
            effectType = "potion"; potionTarget = pot.target != null ? pot.target : "SELF";
            selectedMobEffect = ResourceLocation.tryParse(pot.mobEffectId); amplifier = pot.amplifier;
            durationSeconds = pot.durationSeconds; cooldownSeconds = pot.cooldownSeconds;
            showParticles = pot.showParticles;
        } else if (existing instanceof AttributeEffectEntry attr) {
            effectType = "attribute"; selectedAttribute = ResourceLocation.tryParse(attr.attributeId);
            amount = attr.amount; attrOperation = attr.operation;
            attrDurationSeconds = attr.durationSeconds; attrCooldownSeconds = attr.cooldownSeconds;
        } else if (existing instanceof AbilityEffectEntry ab) {
            effectType = "ability"; abilityId = ab.abilityId;
        } else if (existing instanceof CommandEffectEntry cmd) {
            effectType = "command";
            commandTrigger = cmd.trigger != null ? cmd.trigger : CommandEffectEntry.Trigger.ACTIVATE;
            commands = cmd.commands != null && !cmd.commands.isEmpty()
                    ? String.join(";", cmd.commands)
                    : (cmd.activateCommands != null && !cmd.activateCommands.isEmpty()
                    ? String.join(";", cmd.activateCommands) : "");
            commandMode = cmd.mode != null ? cmd.mode : CommandEffectEntry.Mode.IMPULSE;
            commandRepeatInterval = cmd.repeatIntervalSeconds > 0 ? cmd.repeatIntervalSeconds : 1;
            commandProbability = cmd.probability;
            this.commandTargetFilter = Objects.requireNonNullElseGet(cmd.targetFilter, TargetFilter::new);
            this.commandCooldownSeconds = cmd.cooldownSeconds;
        } else if (existing instanceof IronSpellEffectEntry iron) {
            effectType = "iron_spell"; spellId = iron.spellId != null ? iron.spellId : "";
            spellLevel = iron.spellLevel > 0 ? iron.spellLevel : 1;
        } else if (existing instanceof SlotCountEffectEntry slot) {
            effectType = "slot_count"; slotCountSlotId = slot.slotId != null ? slot.slotId : "";
            slotCountAmount = slot.amount;
        } else if (existing instanceof SpellLevelBoostEffectEntry boost) {
            effectType = "spell_level_boost"; boostSpellId = boost.spellId != null ? boost.spellId : "";
            boostAmount = boost.boostAmount > 0 ? boost.boostAmount : 1;
        } else if (existing instanceof L2HostilityTraitEffectEntry traitEff) {
            effectType = "l2hostility_trait";
            l2traitId = traitEff.traitId != null ? traitEff.traitId : "";
            l2traitLevel = traitEff.level;
        } else if (existing instanceof DynamicAttributeEffectEntry dynAttr) {
            effectType = "dynamic_attribute";
            dynamicAttributeId = dynAttr.attributeId != null ? dynAttr.attributeId : "";
            dynamicOperation = dynAttr.operation;
            dynamicVariable = dynAttr.variableType;
            dynamicFormula = dynAttr.formulaType;
            dynamicCoeffs = dynAttr.coefficients != null ? dynAttr.coefficients.clone() : new double[]{0, 0};
            dynamicBase = dynAttr.base;
            dynamicClipMinX = dynAttr.clipMinX;
            dynamicClipMaxX = dynAttr.clipMaxX;
            dynamicSourceAttributeId = dynAttr.sourceAttributeId != null ? dynAttr.sourceAttributeId : "";
            dynamicScoreboardObjective = dynAttr.scoreboardObjective != null ? dynAttr.scoreboardObjective : "";
        } else if (existing instanceof L2DifficultyModEffectEntry mod) {
            effectType = "l2_difficulty_mod";
            l2DifficultyAmount = mod.amount;
        } else if (existing instanceof TagEffectEntry tagEffect) {
            effectType = "tag";
            tagName = tagEffect.tagName != null ? tagEffect.tagName : "";
        }
    }

    @Override
    protected void init() {
        clearWidgets();
        int centerX = width / 2, totalWidth = 160, rowHeight = 18, spacing = 3, y = 30;
        List<String> types = new ArrayList<>(List.of("potion", "attribute", "ability", "command", "dynamic_attribute", "tag"));
        if (IntegrationManager.isIronSpellsLoaded()) {
            types.add("iron_spell");
            types.add("spell_level_boost");
        }
        if (IntegrationManager.isCuriosLoaded()) types.add("slot_count");
        if (IntegrationManager.isL2HostilityLoaded()) {
            types.add("l2hostility_trait");
            types.add("l2_difficulty_mod");
        }
        if (!types.contains(effectType)) effectType = "potion";

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.type"), font));
        y += rowHeight;
        CycleButton<String> typeButton = CycleButton.<String>builder(s -> Component.translatable("visual_set_edit.gui.effect.type." + s))
                .withValues(types).displayOnlyValue().withInitialValue(effectType)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.type"), (btn, val) -> { effectType = val; init(); });
        addRenderableWidget(typeButton);
        y += rowHeight + spacing;

        switch (effectType) {
            case "potion" -> y = buildPotionFields(centerX, y, totalWidth, rowHeight, spacing);
            case "attribute" -> y = buildAttributeFields(centerX, y, totalWidth, rowHeight, spacing);
            case "ability" -> y = buildAbilityFields(centerX, y, totalWidth, rowHeight, spacing);
            case "command" -> y = buildCommandFields(centerX, y, totalWidth, rowHeight, spacing);
            case "iron_spell" -> y = buildIronSpellFields(centerX, y, totalWidth, rowHeight, spacing);
            case "slot_count" -> y = buildSlotCountFields(centerX, y, totalWidth, rowHeight, spacing);
            case "spell_level_boost" -> y = buildSpellLevelBoostFields(centerX, y, totalWidth, rowHeight, spacing);
            case "l2hostility_trait" -> y = buildL2HostilityTraitFields(centerX, y, totalWidth, rowHeight, spacing);
            case "dynamic_attribute" -> y = buildDynamicAttributeFields(centerX, y, totalWidth, rowHeight, spacing);
            case "l2_difficulty_mod" -> y = buildL2DifficultyModFields(centerX, y, totalWidth, rowHeight, spacing);
            case "tag" -> y = buildTagFields(centerX, y, totalWidth, rowHeight, spacing);
        }

        this.contentHeight = y + 30;
        if (scrollOffset > Math.max(0, contentHeight - this.height)) {
            scrollOffset = Math.max(0, contentHeight - this.height);
        }
    }

    private int buildCustomDisplayFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
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
                        default -> Component.literal(s);
                    };
                })
                .withValues("FLIGHT", "FALL_IMMUNITY").displayOnlyValue().withInitialValue(abilityId)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.effect.ability.id"), (btn, val) -> abilityId = val);
        addRenderableWidget(abButton); y += rowHeight + spacing;
        y = buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
        y += rowHeight + spacing;
        return y;
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

        // 公式类型
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.formula"), font)); y += rowHeight;
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
                baseEdit.setValue(String.valueOf(dynamicBase));
                baseEdit.setResponder(s -> { try { dynamicBase = Double.parseDouble(s); } catch (Exception ignored) {} });
                addRenderableWidget(baseEdit);
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
        }

        // 裁剪范围
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.clip_min"), font)); y += rowHeight;
        dynamicClipMinEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight, Component.literal(""));
        dynamicClipMinEdit.setValue(Double.isNaN(dynamicClipMinX) ? "" : String.valueOf(dynamicClipMinX));
        addRenderableWidget(dynamicClipMinEdit); y += rowHeight + spacing;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.effect.dynamic_attribute.clip_max"), font)); y += rowHeight;
        dynamicClipMaxEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight, Component.literal(""));
        dynamicClipMaxEdit.setValue(Double.isNaN(dynamicClipMaxX) ? "" : String.valueOf(dynamicClipMaxX));
        addRenderableWidget(dynamicClipMaxEdit); y += rowHeight + spacing;

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
        if (index < dynamicCoeffs.length) {
            edit.setValue(String.valueOf(dynamicCoeffs[index]));
        } else {
            edit.setValue("0");
        }
        edit.setResponder(s -> {
            try {
                double val = Double.parseDouble(s);
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

    private void saveButton(int centerX, int y, int totalWidth, int rowHeight) {
        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.save"), b -> {
            EffectEntry effect = createEffect();
            if (effect != null) { onSave.accept(effect); assert minecraft != null; minecraft.setScreen(returnTo); }
        }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build());
    }

    private Component getPotionButtonText() {
        if (selectedMobEffect != null) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(selectedMobEffect);
            if (effect != null) return Component.translatable(effect.getDescriptionId());
            return Component.literal(selectedMobEffect.toString());
        }
        return Component.translatable("visual_set_edit.gui.click_select_item");
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
        EffectEntry e = null;
        switch (effectType) {
            case "potion" -> {
                PotionEffectEntry pot = new PotionEffectEntry(); pot.target = potionTarget;
                if (potionIdEdit != null && !potionIdEdit.getValue().trim().isEmpty())
                    pot.mobEffectId = potionIdEdit.getValue().trim();
                else if (selectedMobEffect != null) pot.mobEffectId = selectedMobEffect.toString();
                else pot.mobEffectId = "";
                if (!"IMMUNE".equals(potionTarget)) {
                    try { pot.amplifier = Integer.parseInt(amplifierEdit.getValue()); } catch (Exception ignored) {}
                    try { pot.durationSeconds = Integer.parseInt(durationEdit.getValue()); } catch (Exception ignored) {}
                    if (durationSeconds != -1 && cooldownEdit != null)
                        try { pot.cooldownSeconds = Integer.parseInt(cooldownEdit.getValue()); } catch (Exception ignored) {}
                    else pot.cooldownSeconds = 0;
                } else { pot.amplifier = 0; pot.durationSeconds = -1; pot.cooldownSeconds = 0; }
                pot.showParticles = showParticles;
                e = pot;
            }
            case "attribute" -> {
                AttributeEffectEntry attr = new AttributeEffectEntry();
                attr.attributeId = selectedAttribute != null ? selectedAttribute.toString() : "";
                try { attr.amount = Double.parseDouble(amountEdit.getValue()); } catch (Exception ignored) {}
                attr.operation = attrOperation;
                attr.durationSeconds = attrDurationEdit != null ? attrDurationSeconds : -1;
                attr.cooldownSeconds = attrCooldownEdit != null ? Math.max(0, attrCooldownSeconds) : 0;
                e = attr;
            }
            case "ability" -> { AbilityEffectEntry ab = new AbilityEffectEntry(); ab.abilityId = abilityId; e = ab; }
            case "command" -> {
                CommandEffectEntry cmd = new CommandEffectEntry();
                cmd.trigger = commandTrigger;
                if (commandsEdit != null && !commandsEdit.getValue().trim().isEmpty()) {
                    cmd.commands = List.of(commandsEdit.getValue().split(";"));
                }
                if (commandTrigger == CommandEffectEntry.Trigger.REPEAT && commandIntervalEdit != null) {
                    try { cmd.repeatIntervalSeconds = Integer.parseInt(commandIntervalEdit.getValue()); } catch (Exception ex) { cmd.repeatIntervalSeconds = 1; }
                } else {
                    cmd.repeatIntervalSeconds = 0;
                }
                cmd.activateCommands = cmd.commands;
                cmd.probability = commandProbability;
                cmd.targetFilter = this.commandTargetFilter;
                cmd.cooldownSeconds = this.commandCooldownSeconds;
                e = cmd;
            }
            case "iron_spell" -> {
                IronSpellEffectEntry iron = new IronSpellEffectEntry(); iron.spellId = spellId;
                try { iron.spellLevel = Integer.parseInt(spellLevelEdit.getValue()); } catch (Exception ignored) {} e = iron;
            }
            case "slot_count" -> {
                SlotCountEffectEntry slot = new SlotCountEffectEntry(); slot.slotId = slotCountSlotId;
                try { slot.amount = Integer.parseInt(slotCountAmountEdit.getValue()); } catch (Exception ignored) {} e = slot;
            }
            case "spell_level_boost" -> {
                SpellLevelBoostEffectEntry boost = new SpellLevelBoostEffectEntry();
                boost.spellId = boostSpellId.isEmpty() ? null : boostSpellId;
                try { boost.boostAmount = Integer.parseInt(boostAmountEdit.getValue()); } catch (Exception ex) { boost.boostAmount = 1; }
                e = boost;
            }
            case "l2hostility_trait" -> {
                L2HostilityTraitEffectEntry trait = new L2HostilityTraitEffectEntry();
                trait.target = "ATTACK_TARGET";
                trait.traitId = l2traitId.isEmpty() ? null : l2traitId;
                try { trait.level = Integer.parseInt(l2traitLevelEdit.getValue()); } catch (Exception ex) { trait.level = 1; }
                e = trait;
            }
            case "l2_difficulty_mod" -> {
                L2DifficultyModEffectEntry mod = new L2DifficultyModEffectEntry();
                mod.amount = l2DifficultyAmount;
                e = mod;
            }
            case "dynamic_attribute" -> {
                DynamicAttributeEffectEntry dyn = new DynamicAttributeEffectEntry();
                dyn.attributeId = dynamicAttributeId;
                dyn.operation = dynamicOperation;
                dyn.variableType = dynamicVariable;
                dyn.formulaType = dynamicFormula;
                dyn.coefficients = dynamicCoeffs.clone();
                dyn.base = dynamicBase;
                dyn.clipMinX = parseDouble(dynamicClipMinEdit);
                dyn.clipMaxX = parseDouble(dynamicClipMaxEdit);
                dyn.sourceAttributeId = dynamicSourceAttributeId;
                dyn.scoreboardObjective = dynamicScoreboardObjective;
                e = dyn;
            }
            case "tag" -> {
                TagEffectEntry tag = new TagEffectEntry();
                tag.tagName = this.tagName;
                e = tag;
            }
        }
        if (e != null) {
            e.customDisplayText = customDisplayTextEdit != null ? customDisplayTextEdit.getValue() : customDisplayText;
            e.customColor = customColor;
            e.showPointer = showPointer;
        }
        return e;
    }

    private double parseDouble(EditBox edit) {
        if (edit == null) return Double.NaN;
        try { return Double.parseDouble(edit.getValue()); } catch (Exception ignored) { return Double.NaN; }
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
        return super.mouseClicked(mouseX, mouseY + scrollOffset, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int maxScroll = Math.max(0, contentHeight - this.height);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollDelta * 20));
        return true;
    }

    @Override
    public void onClose() { if (minecraft != null) minecraft.setScreen(returnTo); }

    public EffectEntry getExistingEffect() {
        return existingEffect;
    }

    public CommandEffectEntry.Mode getCommandMode() {
        return commandMode;
    }
}