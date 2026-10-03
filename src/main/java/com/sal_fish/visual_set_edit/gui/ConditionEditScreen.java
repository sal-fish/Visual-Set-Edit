package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.api.ConditionFieldSpec;
import com.sal_fish.visual_set_edit.api.PickerScreenFactory;
import com.sal_fish.visual_set_edit.data.condition.*;
import com.sal_fish.visual_set_edit.data.SlotCondition;
import com.sal_fish.visual_set_edit.data.NbtMatchRule;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import com.sal_fish.visual_set_edit.util.ExpressionEvaluator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConditionEditScreen extends Screen {
    private final Consumer<Condition> onSave;
    private final Screen returnTo;

    private String condType = ConditionTypeRegistry.defaultId();
    private String field = "";
    private String comparator = "EQ";
    private String value = "";

    private String invSlot = "HEAD";
    private String invItemId = null;
    private String invTagId = null;
    private NbtMatchRule invNbtRule = NbtMatchRule.IGNORE;
    private int invDurMin = 0;
    private int invDurMax = 100;

    private String compOp = "AND";

    private String isField = "MANA";
    private String isComparator = "EQ";
    private double isValue = 0;
    private String isValueRaw = null;   // 数值框原始文本；null = 未编辑

    private String attrId = "";
    private String attrComparator = "GTE";
    private double attrValue = 0;
    private String attrValueRaw = null;
    private EditBox attrValueEdit;
    private Button selectAttrButton;

    private String sbObjective = "";
    private String sbComparator = "GTE";
    private double sbValue = 0;
    private String sbValueRaw = null;
    private EditBox sbValueEdit;
    private Button selectSbButton;

    private EditBox valueEdit, invDurMinEdit, invDurMaxEdit, isValueEdit;
    private Button invSlotButton, invItemButton;
    private EditBox invTagEdit;

    private String conditionCustomDisplayText = "";
    private EditBox conditionCustomDisplayTextEdit;
    private int playerStateEffectAmplifier = -1;  // HAS_EFFECT 条件要求的效果等级（-1 = 任意）
    private EditBox effectLevelEdit;

    private final List<Condition> tempChildren = new ArrayList<>();
    private final List<PlaceholderSuggestor> suggestors = new ArrayList<>();

    public ConditionEditScreen(Consumer<Condition> onSave, Screen returnTo) {
        this(onSave, returnTo, null);
    }

    public ConditionEditScreen(Consumer<Condition> onSave, Screen returnTo, Condition existing) {
        super(Component.translatable("visual_set_edit.gui.edit_condition"));
        this.onSave = onSave;
        this.returnTo = returnTo;
        if (existing != null) {
            loadFromExisting(existing);
        }
    }

    private void loadFromExisting(Condition c) {
        condType = c.type != null ? c.type : ConditionTypeRegistry.defaultId();
        conditionCustomDisplayText = c.customDisplayText != null ? c.customDisplayText : "";
        ConditionEditorRegistry.getOrDefault(condType).loadFrom(this, c);
    }

    // 优先表达式，其次固定值
    private static String rawOf(String expression, double value) {
        return (expression != null && !expression.isBlank()) ? expression : String.valueOf(value);
    }

    @Override
    protected void init() {
        clearWidgets();
        int centerX = width / 2;
        int totalWidth = 160;
        int rowHeight = 18;
        int spacing = 3;
        int y = 30;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.type"), font));
        y += rowHeight;
        List<String> types = ConditionTypeRegistry.availableIds();
        if (!types.contains(condType)) condType = ConditionTypeRegistry.defaultId();
        CycleButton<String> typeButton = CycleButton.<String>builder(s ->
                        Component.translatable("visual_set_edit.gui.condition.type." + s))
                .withValues(types)
                .displayOnlyValue()
                .withInitialValue(condType)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.type"), (btn, val) -> {
                            condType = val;
                            init();
                        });
        addRenderableWidget(typeButton);
        y += rowHeight + spacing;

        ConditionEditorRegistry.Editor editor = ConditionEditorRegistry.get(condType);
        if (editor != null) {
            editor.buildFields(this, centerX, y, totalWidth, rowHeight, spacing);
        }

        suggestors.clear();
        addSuggestor(valueEdit);
        addSuggestor(isValueEdit);
        addSuggestor(attrValueEdit);
        addSuggestor(sbValueEdit);
    }

    private void addSuggestor(EditBox box) {
        if (box != null && children().contains(box)) suggestors.add(new PlaceholderSuggestor(box));
    }

    void buildCommonFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        // 值输入区每次重建都会换控件，旧引用必须清掉
        valueEdit = null;

        List<String> fieldOptions = getFieldOptions();
        if (!fieldOptions.isEmpty()) {
            if (field.isEmpty() || !fieldOptions.contains(field)) {
                field = fieldOptions.get(0);
            }
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.field"), font));
            y += rowHeight;
            CycleButton<String> fieldButton = CycleButton.<String>builder(s -> {
                        ConditionFieldSpec ext = ConditionFieldRegistry.get(condType, s);
                        if (ext != null && ext.displayText() != null) return ext.displayText().get();
                        return Component.translatable("visual_set_edit.gui.condition.field." + condType + "." + s);
                    })
                    .withValues(fieldOptions)
                    .displayOnlyValue()
                    .withInitialValue(field)
                    .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                            Component.translatable("visual_set_edit.gui.condition.field"), (btn, val) -> {
                                field = val;
                                init();
                            });
            addRenderableWidget(fieldButton);
            y += rowHeight + spacing;
        }

        // 外部注册的字段走独立分支，内置字段逻辑不受影响
        ConditionFieldSpec registeredField = ConditionFieldRegistry.get(condType, field);
        if (registeredField != null) {
            y = buildRegisteredFieldValue(registeredField, centerX, y, totalWidth, rowHeight, spacing);
            y += 6;
            y = buildCustomDisplayField(centerX, y, totalWidth, rowHeight, spacing);
            saveButton(centerX, y, totalWidth, rowHeight);
            return;
        }

        // 比较符部分：IS_HURT 和 TAG 不需要通用比较符，HAS_EFFECT 用两项比较符
        if (!"IS_HURT".equals(field) && !"TAG".equals(field) && !isHasEffect()) {
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.comparator"), font));
            y += rowHeight;
            CycleButton<String> comparatorButton = CycleButton.<String>builder(s -> Component.translatable("visual_set_edit.gui.condition.comparator." + s))
                    .withValues("EQ", "NEQ", "GT", "LT", "GTE", "LTE")
                    .displayOnlyValue()
                    .withInitialValue(comparator)
                    .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                            Component.translatable("visual_set_edit.gui.condition.comparator"), (btn, val) -> comparator = val);
            addRenderableWidget(comparatorButton);
            y += rowHeight + spacing;
        } else if ("IS_HURT".equals(field)) {
            comparator = ""; // IS_HURT 不比较
        }

        // 值输入区域
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.value"), font));
        y += rowHeight;

        if ("WEATHER".equals(field)) {
            CycleButton<String> weatherButton = CycleButton.<String>builder(s ->
                            Component.translatable("visual_set_edit.gui.condition.weather." + s))
                    .withValues("RAIN", "THUNDER", "CLEAR")
                    .displayOnlyValue()
                    .withInitialValue(value.isEmpty() ? "RAIN" : value)
                    .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                            Component.translatable("visual_set_edit.gui.condition.value"), (btn, val) -> value = val);
            addRenderableWidget(weatherButton);
            y += rowHeight + spacing;
        } else if ("IS_HURT".equals(field)) {
            // 时间窗口输入
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.value.hurt_window"), font));
            y += rowHeight;
            String[] parts = value.split(",");
            String windowText = parts.length > 0 ? parts[0].trim() : "";
            EditBox windowEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.value.hurt_window"));
            windowEdit.setMaxLength(10);
            windowEdit.setValue(windowText);
            windowEdit.setResponder(s -> {
                String[] p = value.split(",");
                String threshold = p.length > 1 ? p[1].trim() : "";
                value = s.trim() + "," + threshold;
            });
            addRenderableWidget(windowEdit);
            y += rowHeight + spacing;

            // 伤害阈值输入
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.value.hurt_threshold"), font));
            y += rowHeight;
            String thresholdText = parts.length > 1 ? parts[1].trim() : "";
            EditBox thresholdEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.value.hurt_threshold"));
            thresholdEdit.setMaxLength(10);
            thresholdEdit.setValue(thresholdText);
            thresholdEdit.setResponder(s -> {
                String[] p = value.split(",");
                String window = p.length > 0 ? p[0].trim() : "";
                value = window + "," + s.trim();
            });
            addRenderableWidget(thresholdEdit);
            y += rowHeight + spacing;
        } else if ("TAG".equals(field)) {
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.comparator"), font));
            y += rowHeight;
            CycleButton<String> tagComparatorButton = CycleButton.<String>builder(s -> Component.translatable("visual_set_edit.gui.condition.comparator." + s))
                    .withValues("EQ", "NEQ")
                    .displayOnlyValue()
                    .withInitialValue(comparator.isEmpty() ? "EQ" : comparator)
                    .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                            Component.translatable("visual_set_edit.gui.condition.comparator"), (btn, val) -> comparator = val);
            addRenderableWidget(tagComparatorButton);
            y += rowHeight + spacing;

            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.tag_name"), font));
            y += rowHeight;
            valueEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.tag_name"));
            valueEdit.setMaxLength(256);
            valueEdit.setValue(value);
            valueEdit.setResponder(s -> value = s);
            addRenderableWidget(valueEdit);
            y += rowHeight + spacing;
        } else {
            boolean needListButton = "DIMENSION".equals(field) || "BIOME".equals(field) || "STRUCTURE".equals(field)
                    || isHasEffect();
            int editWidth = needListButton ? totalWidth - 22 : totalWidth;

            valueEdit = new EditBox(font, centerX - totalWidth / 2, y, editWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.value"));
            valueEdit.setMaxLength(5201314);
            valueEdit.setValue(value);

            // 百分比提示
            if (condType.equals("player_state") && (field.equals("HEALTH") || field.equals("FOOD"))) {
                String hint = Component.translatable("visual_set_edit.gui.condition.value.percent_hint").getString();
                valueEdit.setResponder(s -> {
                    if (s.isEmpty()) {
                        valueEdit.setSuggestion(hint);
                    } else {
                        valueEdit.setSuggestion("");
                    }
                    value = s;
                });
                if (value.isEmpty()) {
                    valueEdit.setSuggestion(hint);
                }
            } else {
                valueEdit.setResponder(s -> value = s);
            }

            addRenderableWidget(valueEdit);

            if (needListButton) {
                Button listButton = Button.builder(Component.literal("📦"),
                        btn -> {
                            assert minecraft != null;
                            Screen listScreen = switch (field) {
                                case "DIMENSION" -> new DimensionListScreen(this, rl -> {
                                    value = rl.toString();
                                    if (valueEdit != null) valueEdit.setValue(value);
                                });
                                case "BIOME" -> new BiomeListScreen(this, rl -> {
                                    value = rl.toString();
                                    if (valueEdit != null) valueEdit.setValue(value);
                                });
                                case "STRUCTURE" -> new StructureListScreen(this, rl -> {
                                    value = rl.toString();
                                    if (valueEdit != null) valueEdit.setValue(value);
                                });
                                case "HAS_EFFECT" -> new MobEffectListScreen(this, rl -> {
                                    value = rl.toString();
                                    if (valueEdit != null) valueEdit.setValue(value);
                                });
                                default -> null;
                            };
                            if (listScreen != null) {
                                minecraft.setScreen(listScreen);
                            }
                        }).pos(centerX - totalWidth / 2 + editWidth + 2, y).size(20, rowHeight).build();
                addRenderableWidget(listButton);
            }
            y += rowHeight + spacing;

            // HAS_EFFECT：比较符（等于/不等于）+ 效果等级（-1 = 任意，0 = 1级，1 = 2级…）
            if (isHasEffect()) {
                addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.condition.comparator"), font));
                y += rowHeight;
                // 该字段只有等于/不等于两种语义，其余取值一律归到等于
                comparator = "NEQ".equals(comparator) ? "NEQ" : "EQ";
                CycleButton<String> effectComparatorButton = CycleButton.<String>builder(s ->
                                Component.translatable("visual_set_edit.gui.condition.comparator." + s))
                        .withValues("EQ", "NEQ")
                        .displayOnlyValue()
                        .withInitialValue(comparator)
                        .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                                Component.translatable("visual_set_edit.gui.condition.comparator"),
                                (btn, val) -> comparator = val);
                addRenderableWidget(effectComparatorButton);
                y += rowHeight + spacing;

                addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.condition.value.effect_level"), font));
                y += rowHeight;
                effectLevelEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.condition.value.effect_level"));
                effectLevelEdit.setMaxLength(4);
                effectLevelEdit.setValue(String.valueOf(playerStateEffectAmplifier));
                effectLevelEdit.setResponder(s -> {
                    try { playerStateEffectAmplifier = Integer.parseInt(s); } catch (Exception ignored) {}
                });
                addRenderableWidget(effectLevelEdit);
                y += rowHeight + spacing;
            }
        }
        y += 6;

        y = buildCustomDisplayField(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
    }

    // 外部注册字段的取值控件
    private int buildRegisteredFieldValue(ConditionFieldSpec spec, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        int x = centerX - totalWidth / 2;
        boolean optionsForm = spec.valueForm() == ConditionFieldSpec.ValueForm.OPTIONS;

        if (optionsForm) {
            comparator = "";
        } else {
            addRenderableWidget(new StringWidget(x, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.comparator"), font));
            y += rowHeight;
            CycleButton<String> comparatorButton = CycleButton.<String>builder(s ->
                            Component.translatable("visual_set_edit.gui.condition.comparator." + s))
                    .withValues("EQ", "NEQ", "GT", "LT", "GTE", "LTE")
                    .displayOnlyValue()
                    .withInitialValue(comparator.isEmpty() ? "EQ" : comparator)
                    .create(x, y, totalWidth, rowHeight,
                            Component.translatable("visual_set_edit.gui.condition.comparator"),
                            (btn, val) -> comparator = val);
            addRenderableWidget(comparatorButton);
            y += rowHeight + spacing;
        }

        addRenderableWidget(new StringWidget(x, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.value"), font));
        y += rowHeight;

        if (optionsForm) {
            List<String> options = spec.options();
            // 空选项会让 CycleButton 抛 IllegalStateException，这里只画标签
            if (!options.isEmpty()) {
                String init = options.contains(value) ? value : options.get(0);
                value = init;
                CycleButton<String> optionButton = CycleButton.<String>builder(s -> {
                            Function<String, Component> renderer = spec.optionRenderer();
                            return renderer != null ? renderer.apply(s) : Component.literal(s);
                        })
                        .withValues(options)
                        .displayOnlyValue()
                        .withInitialValue(init)
                        .create(x, y, totalWidth, rowHeight,
                                Component.translatable("visual_set_edit.gui.condition.value"),
                                (btn, val) -> value = val);
                addRenderableWidget(optionButton);
            }
            return y + rowHeight + spacing;
        }

        boolean withPicker = spec.valueForm() == ConditionFieldSpec.ValueForm.PICKER;
        int editWidth = withPicker ? totalWidth - 22 : totalWidth;
        valueEdit = new EditBox(font, x, y, editWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.value"));
        valueEdit.setMaxLength(5201314);
        valueEdit.setValue(value);
        valueEdit.setResponder(s -> value = s);
        addRenderableWidget(valueEdit);

        if (withPicker) {
            PickerScreenFactory factory = PickerScreenRegistry.get(spec.pickerId());
            if (factory != null) {
                Button listButton = Button.builder(Component.literal("📦"), btn ->
                        Minecraft.getInstance().setScreen(factory.create(this, picked -> {
                            value = picked;
                            if (valueEdit != null) valueEdit.setValue(picked);
                        }))
                ).pos(x + editWidth + 2, y).size(20, rowHeight).build();
                addRenderableWidget(listButton);
            }
        }
        return y + rowHeight + spacing;
    }

    //库存条件
    void buildInventoryFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        // 槽位选择
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.inventory.slot"), font));
        y += rowHeight;
        invSlotButton = Button.builder(getSlotButtonText(),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(new SlotSelectionScreen(this, invSlot, newSlot -> {
                        invSlot = newSlot;
                        invSlotButton.setMessage(getSlotButtonText());
                    }));
                }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
        addRenderableWidget(invSlotButton);
        y += rowHeight + spacing;

        // 物品选择按钮
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.inventory.item"), font));
        y += rowHeight;
        invItemButton = Button.builder(getItemButtonText(),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(new ItemListScreen(this, invSlot, rl -> {
                        invItemId = rl.toString();
                        invTagId = null; // 互斥
                        invTagEdit.setValue("");
                        invItemButton.setMessage(getItemButtonText());
                    }));
                }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
        invItemButton.active = (invTagId == null || invTagId.isEmpty());
        addRenderableWidget(invItemButton);
        y += rowHeight + spacing;

        // Tag 输入
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.inventory.tag"), font));
        y += rowHeight;
        int tagEditWidth = totalWidth - 22;
        invTagEdit = new EditBox(font, centerX - totalWidth / 2, y, tagEditWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.inventory.tag"));
        invTagEdit.setMaxLength(5201314);
        invTagEdit.setValue(invTagId != null ? invTagId : "");
        invTagEdit.setResponder(s -> {
            invTagId = s.trim().isEmpty() ? null : s.trim();
            invItemButton.active = (invTagId == null);
            if (invTagId != null) invItemId = null;
            invItemButton.setMessage(getItemButtonText());
        });
        addRenderableWidget(invTagEdit);

        Button tagSelectButton = Button.builder(Component.literal("📦"),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(new TagListScreen(this, tagId -> {
                        invTagId = tagId;
                        invTagEdit.setValue(tagId);
                        invItemButton.active = false;
                        invItemId = null;
                        invItemButton.setMessage(getItemButtonText());
                    }));
                }).pos(centerX - totalWidth / 2 + tagEditWidth + 2, y).size(20, rowHeight).build();
        addRenderableWidget(tagSelectButton);
        y += rowHeight + spacing;

        // 耐久范围
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.inventory.min_durability"), font));
        y += rowHeight;
        invDurMinEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.inventory.min_durability"));
        invDurMinEdit.setMaxLength(5201314);
        invDurMinEdit.setValue(String.valueOf(invDurMin));
        addRenderableWidget(invDurMinEdit);
        y += rowHeight + spacing;

        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.inventory.max_durability"), font));
        y += rowHeight;
        invDurMaxEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.inventory.max_durability"));
        invDurMaxEdit.setValue(String.valueOf(invDurMax));
        addRenderableWidget(invDurMaxEdit);
        y += rowHeight + spacing + 6;

        y = buildCustomDisplayField(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
    }

    //铁魔法条件
    void buildIronSpellFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        List<String> isFieldOpts = List.of("MANA", "MANA_PERCENT", "CASTING");
        if (isField == null || isField.isEmpty() || !isFieldOpts.contains(isField)) {
            isField = isFieldOpts.get(0);
        }
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.field"), font));
        y += rowHeight;
        CycleButton<String> isFieldButton = CycleButton.<String>builder(s -> Component.translatable("visual_set_edit.gui.condition.field.iron_spell." + s))
                .withValues(isFieldOpts)
                .displayOnlyValue()
                .withInitialValue(isField)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.condition.field"), (btn, val) -> {
                            isField = val;
                            init();
                        });
        addRenderableWidget(isFieldButton);
        y += rowHeight + spacing;

        if (!"CASTING".equals(isField)) {
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.comparator"), font));
            y += rowHeight;
            CycleButton<String> isComparatorButton = CycleButton.<String>builder(s -> Component.translatable("visual_set_edit.gui.condition.comparator." + s))
                    .withValues("EQ", "NEQ", "GT", "LT", "GTE", "LTE")
                    .displayOnlyValue()
                    .withInitialValue(isComparator)
                    .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                            Component.translatable("visual_set_edit.gui.condition.comparator"), (btn, val) -> isComparator = val);
            addRenderableWidget(isComparatorButton);
            y += rowHeight + spacing;

            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.value"), font));
            y += rowHeight;
            isValueEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.value"));
            isValueEdit.setMaxLength(5201314);
            isValueEdit.setValue(isValueRaw != null ? isValueRaw : String.valueOf(isValue));
            isValueEdit.setResponder(s -> isValueRaw = s);
            addRenderableWidget(isValueEdit);
            y += rowHeight + spacing;
        }
        y += 6;
        y = buildCustomDisplayField(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
    }
    //属性条件
    void buildAttributeConditionFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        // 属性选择按钮
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.attribute"), font));
        y += rowHeight;
        selectAttrButton = Button.builder(getAttrButtonText(), btn -> {
            assert minecraft != null;
            minecraft.setScreen(new AttributeListScreen(this, rl -> {
                attrId = rl.toString();
                selectAttrButton.setMessage(getAttrButtonText());
            }));
        }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
        addRenderableWidget(selectAttrButton);
        y += rowHeight + spacing;

        // 比较符
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.comparator"), font));
        y += rowHeight;
        CycleButton<String> comparatorBtn = CycleButton.<String>builder(s -> Component.translatable("visual_set_edit.gui.condition.comparator." + s))
                .withValues("EQ", "NEQ", "GT", "LT", "GTE", "LTE")
                .displayOnlyValue()
                .withInitialValue(attrComparator)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.condition.comparator"), (btn, val) -> attrComparator = val);
        addRenderableWidget(comparatorBtn);
        y += rowHeight + spacing;

        // 数值输入
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.value"), font));
        y += rowHeight;
        attrValueEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.value"));
        attrValueEdit.setMaxLength(5201314);
        attrValueEdit.setValue(attrValueRaw != null ? attrValueRaw : String.valueOf(attrValue));
        attrValueEdit.setResponder(s -> attrValueRaw = s);
        addRenderableWidget(attrValueEdit);
        y += rowHeight + spacing;

        y = buildCustomDisplayField(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
    }

    //计分板条件
    void buildScoreboardConditionFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        // 计分板选择按钮
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.scoreboard.objective"), font));
        y += rowHeight;
        selectSbButton = Button.builder(getSbButtonText(), btn -> {
            assert minecraft != null;
            minecraft.setScreen(new ScoreboardObjectiveListScreen(this, name -> {
                sbObjective = name;
                selectSbButton.setMessage(getSbButtonText());
            }));
        }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build();
        addRenderableWidget(selectSbButton);
        y += rowHeight + spacing;

        // 比较符
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.comparator"), font));
        y += rowHeight;
        CycleButton<String> comparatorBtn = CycleButton.<String>builder(s -> Component.translatable("visual_set_edit.gui.condition.comparator." + s))
                .withValues("EQ", "NEQ", "GT", "LT", "GTE", "LTE")
                .displayOnlyValue()
                .withInitialValue(sbComparator)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.condition.comparator"), (btn, val) -> sbComparator = val);
        addRenderableWidget(comparatorBtn);
        y += rowHeight + spacing;

        // 数值输入
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.value"), font));
        y += rowHeight;
        sbValueEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.value"));
        sbValueEdit.setMaxLength(5201314);
        sbValueEdit.setValue(sbValueRaw != null ? sbValueRaw : String.valueOf(sbValue));
        sbValueEdit.setResponder(s -> sbValueRaw = s);
        addRenderableWidget(sbValueEdit);
        y += rowHeight + spacing;

        y = buildCustomDisplayField(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
    }

    //复合条件
    void buildCompositeFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.composite.op"), font));
        y += rowHeight;
        CycleButton<String> compOpButton = CycleButton.<String>builder(s -> Component.translatable("visual_set_edit.gui.condition.composite.op." + s))
                .withValues("AND", "OR", "NOT")
                .displayOnlyValue()
                .withInitialValue(compOp)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.condition.composite.op"), (btn, val) -> compOp = val);
        addRenderableWidget(compOpButton);
        y += rowHeight + spacing;

        // 子条件列表
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.composite.children"), font));
        y += rowHeight;

        for (int i = 0; i < tempChildren.size(); i++) {
            Condition child = tempChildren.get(i);
            int index = i;
            String text = child.getFinalDisplayText();
            addRenderableWidget(Button.builder(Component.literal(text), btn -> {
                assert minecraft != null;
                minecraft.setScreen(new ConditionEditScreen(edited -> {
                    tempChildren.set(index, edited);
                    minecraft.setScreen(this);
                }, this, child));
            }).pos(centerX - totalWidth / 2, y).size(totalWidth - 22, rowHeight).build());

            addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.delete"),
                    btn -> {
                        tempChildren.remove(index);
                        init();
                    }).pos(centerX - totalWidth / 2 + totalWidth - 20, y).size(20, rowHeight).build());
            y += rowHeight + 2;
        }

        addRenderableWidget(Button.builder(Component.translatable("visual_set_edit.gui.add_child_condition"),
                btn -> {
                    assert minecraft != null;
                    minecraft.setScreen(new ConditionEditScreen(child -> {
                        tempChildren.add(child);
                        minecraft.setScreen(this);
                    }, this));
                }).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build());
        y += rowHeight + 6;

        y = buildCustomDisplayField(centerX, y, totalWidth, rowHeight, spacing);
        saveButton(centerX, y, totalWidth, rowHeight);
    }

    //自定义显示文本（与效果系统一致，优先于自动生成的描述）
    private int buildCustomDisplayField(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.custom_display_text"), font));
        y += rowHeight;
        conditionCustomDisplayTextEdit = new EditBox(font, centerX - totalWidth / 2, y, totalWidth, rowHeight,
                Component.translatable("visual_set_edit.gui.condition.custom_display_text"));
        conditionCustomDisplayTextEdit.setMaxLength(256);
        conditionCustomDisplayTextEdit.setValue(conditionCustomDisplayText);
        conditionCustomDisplayTextEdit.setResponder(s -> conditionCustomDisplayText = s);
        addRenderableWidget(conditionCustomDisplayTextEdit);
        y += rowHeight + spacing;
        return y;
    }

    private void saveButton(int centerX, int y, int totalWidth, int rowHeight) {
        addRenderableWidget(Button.builder(
                Component.translatable("visual_set_edit.gui.save"),
                b -> {
                    Condition c = createCondition();
                    if (c != null) {
                        onSave.accept(c);
                        assert minecraft != null;
                        minecraft.setScreen(returnTo);
                    }
                }
        ).pos(centerX - totalWidth / 2, y).size(totalWidth, rowHeight).build());
    }

    private Condition createCondition() {
        ConditionEditorRegistry.Editor editor = ConditionEditorRegistry.getOrDefault(condType);
        return applyCustomDisplay(editor.create(this));
    }

    Font fieldFont() {
        return font;
    }

    void attachField(AbstractWidget widget) {
        addRenderableWidget(widget);
    }

    // 条件编辑界面注册
    static void registerEditors() {
        ConditionEditorRegistry.register("environment", new ConditionEditorRegistry.Editor() {
            @Override
            public void buildFields(ConditionEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                screen.buildCommonFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(ConditionEditScreen screen, Condition entry) {
                if (!(entry instanceof EnvironmentCondition env)) return;
                screen.field = env.field;
                screen.comparator = env.comparator;
                screen.value = env.value;
            }

            @Override
            public Condition create(ConditionEditScreen screen) {
                EnvironmentCondition e = new EnvironmentCondition();
                e.field = screen.field;
                e.comparator = screen.comparator;
                e.value = screen.value; // 天气下拉直接更新了 value 字段
                if (screen.valueEdit != null) e.value = screen.valueEdit.getValue(); // 覆盖以保证最新值
                return e;
            }
        });

        ConditionEditorRegistry.register("player_state", new ConditionEditorRegistry.Editor() {
            @Override
            public void buildFields(ConditionEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                screen.buildCommonFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(ConditionEditScreen screen, Condition entry) {
                if (!(entry instanceof PlayerStateCondition ps)) return;
                screen.field = ps.field;
                screen.comparator = ps.comparator;
                screen.value = ps.value;
                screen.playerStateEffectAmplifier = ps.effectAmplifier;
            }

            @Override
            public Condition create(ConditionEditScreen screen) {
                PlayerStateCondition p = new PlayerStateCondition();
                p.field = screen.field;
                p.comparator = screen.comparator;
                p.value = screen.valueEdit != null ? screen.valueEdit.getValue() : screen.value;
                p.effectAmplifier = screen.effectLevelEdit != null ? screen.playerStateEffectAmplifier : -1;
                return p;
            }
        });

        ConditionEditorRegistry.register("inventory", new ConditionEditorRegistry.Editor() {
            @Override
            public void buildFields(ConditionEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                screen.buildInventoryFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(ConditionEditScreen screen, Condition entry) {
                if (!(entry instanceof InventoryCondition inv)) return;
                screen.invSlot = inv.slot != null ? inv.slot : "HEAD";
                if (inv.itemCondition != null) {
                    screen.invItemId = inv.itemCondition.itemId;
                    screen.invTagId = inv.itemCondition.tagId;
                    screen.invNbtRule = inv.itemCondition.nbtRule;
                    screen.invDurMin = inv.itemCondition.durabilityMinPercent;
                    screen.invDurMax = inv.itemCondition.durabilityMaxPercent;
                }
            }

            @Override
            public Condition create(ConditionEditScreen screen) {
                InventoryCondition ic = new InventoryCondition();
                ic.slot = screen.invSlot;
                SlotCondition sc = new SlotCondition();
                sc.itemId = screen.invItemId;
                sc.tagId = screen.invTagId;
                sc.nbtRule = screen.invNbtRule;
                try { sc.durabilityMinPercent = Integer.parseInt(screen.invDurMinEdit.getValue()); } catch (Exception ignored) {}
                try { sc.durabilityMaxPercent = Integer.parseInt(screen.invDurMaxEdit.getValue()); } catch (Exception ignored) {}
                ic.itemCondition = sc;
                return ic;
            }
        });

        ConditionEditorRegistry.register("iron_spell", new ConditionEditorRegistry.Editor() {
            @Override
            public void buildFields(ConditionEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                screen.buildIronSpellFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(ConditionEditScreen screen, Condition entry) {
                if (!(entry instanceof IronSpellCondition is)) return;
                screen.isField = is.field;
                screen.isComparator = is.comparator;
                screen.isValue = is.value;
                screen.isValueRaw = rawOf(is.valueExpression, is.value);
            }

            @Override
            public Condition create(ConditionEditScreen screen) {
                IronSpellCondition is = new IronSpellCondition();
                is.field = screen.isField;
                is.comparator = screen.isComparator;
                String raw = (screen.isValueRaw != null ? screen.isValueRaw : String.valueOf(screen.isValue)).trim();
                if (ExpressionEvaluator.looksLikeExpression(raw)) {
                    is.valueExpression = raw;
                    is.value = screen.isValue;
                } else {
                    try { is.value = Double.parseDouble(raw); } catch (Exception ignored) {}
                }
                return is;
            }
        });

        ConditionEditorRegistry.register("attribute", new ConditionEditorRegistry.Editor() {
            @Override
            public void buildFields(ConditionEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                screen.buildAttributeConditionFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(ConditionEditScreen screen, Condition entry) {
                if (!(entry instanceof AttributeCondition attrCond)) return;
                screen.attrId = attrCond.attributeId;
                screen.attrComparator = attrCond.comparator;
                screen.attrValue = attrCond.value;
                screen.attrValueRaw = rawOf(attrCond.valueExpression, attrCond.value);
            }

            @Override
            public Condition create(ConditionEditScreen screen) {
                AttributeCondition ac = new AttributeCondition();
                ac.attributeId = screen.attrId;
                ac.comparator = screen.attrComparator;
                String raw = (screen.attrValueRaw != null ? screen.attrValueRaw : String.valueOf(screen.attrValue)).trim();
                if (ExpressionEvaluator.looksLikeExpression(raw)) {
                    ac.valueExpression = raw;
                    ac.value = screen.attrValue;
                } else {
                    try { ac.value = Double.parseDouble(raw); } catch (Exception ignored) {}
                }
                return ac;
            }
        });

        ConditionEditorRegistry.register("scoreboard", new ConditionEditorRegistry.Editor() {
            @Override
            public void buildFields(ConditionEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                screen.buildScoreboardConditionFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(ConditionEditScreen screen, Condition entry) {
                if (!(entry instanceof ScoreboardCondition sbCond)) return;
                screen.sbObjective = sbCond.objective != null ? sbCond.objective : "";
                screen.sbComparator = sbCond.comparator != null ? sbCond.comparator : "GTE";
                screen.sbValue = sbCond.value;
                screen.sbValueRaw = rawOf(sbCond.valueExpression, sbCond.value);
            }

            @Override
            public Condition create(ConditionEditScreen screen) {
                ScoreboardCondition sbc = new ScoreboardCondition();
                sbc.objective = screen.sbObjective;
                sbc.comparator = screen.sbComparator;
                String raw = (screen.sbValueRaw != null ? screen.sbValueRaw : String.valueOf(screen.sbValue)).trim();
                if (ExpressionEvaluator.looksLikeExpression(raw)) {
                    sbc.valueExpression = raw;
                    sbc.value = screen.sbValue;
                } else {
                    try { sbc.value = Double.parseDouble(raw); } catch (Exception ignored) {}
                }
                return sbc;
            }
        });

        ConditionEditorRegistry.register("composite", new ConditionEditorRegistry.Editor() {
            @Override
            public void buildFields(ConditionEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
                screen.buildCompositeFields(centerX, y, totalWidth, rowHeight, spacing);
            }

            @Override
            public void loadFrom(ConditionEditScreen screen, Condition entry) {
                if (!(entry instanceof CompositeCondition comp)) return;
                screen.compOp = comp.op;
                // 编辑缓冲在这里初始化，不能靠"空"反推是否已初始化
                screen.tempChildren.clear();
                if (comp.children != null) screen.tempChildren.addAll(comp.children);
            }

            @Override
            public Condition create(ConditionEditScreen screen) {
                CompositeCondition cc = new CompositeCondition();
                cc.op = screen.compOp;
                cc.children = new ArrayList<>(screen.tempChildren);
                return cc;
            }
        });
    }

    private Condition applyCustomDisplay(Condition c) {
        if (c != null) {
            c.customDisplayText = conditionCustomDisplayTextEdit != null
                    ? conditionCustomDisplayTextEdit.getValue()
                    : conditionCustomDisplayText;
        }
        return c;
    }

    // HAS_EFFECT 使用两项比较符与效果等级，不走通用比较符
    private boolean isHasEffect() {
        return condType.equals("player_state") && "HAS_EFFECT".equals(field);
    }

    private List<String> getFieldOptions() {
        List<String> base = switch (condType) {
            case "environment" -> {
                List<String> options = new ArrayList<>(List.of("LIGHT_SKY", "LIGHT_BLOCK", "DIMENSION", "BIOME", "Y", "WEATHER",
                        "MOON_PHASE", "TIME", "TEMPERATURE"));
                if (IntegrationManager.isL2HostilityLoaded()) {
                    options.add("L2H_CHUNK_DIFFICULTY");
                    options.add("L2H_PLAYER_DIFFICULTY");
                }
                yield options;
            }
            case "player_state" -> List.of("HEALTH", "FOOD", "ARMOR", "XP_LEVEL", "HAS_EFFECT",
                    "FALL_DISTANCE", "SUBMERGED", "SNEAKING", "SPRINTING", "SWIMMING",
                    "ON_GROUND", "ON_WALL", "FLYING", "SLEEPING", "RIDING","IS_HURT", "TAG");
            default -> List.of();
        };
        // 外部注册的字段追加在内置字段之后
        List<String> registered = ConditionFieldRegistry.fieldIdsOf(condType);
        if (registered.isEmpty()) return base;
        List<String> all = new ArrayList<>(base);
        all.addAll(registered);
        return all;
    }

    private Component getSlotButtonText() {
        return Component.translatable("visual_set_edit.slot." + invSlot.toLowerCase());
    }

    private Component getItemButtonText() {
        if (invItemId != null && !invItemId.isEmpty()) {
            return Component.literal(invItemId);
        }
        return Component.translatable("visual_set_edit.gui.click_select_item");
    }

    private Component getAttrButtonText() {
        if (attrId == null || attrId.isEmpty()) {
            return Component.translatable("visual_set_edit.gui.click_select_item");
        }
        ResourceLocation rl = ResourceLocation.tryParse(attrId);
        if (rl != null) {
            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(rl);
            if (attr != null) {
                return Component.translatable(attr.getDescriptionId());
            }
        }
        return Component.literal(attrId);
    }

    private Component getSbButtonText() {
        if (sbObjective == null || sbObjective.isEmpty()) {
            return Component.translatable("visual_set_edit.gui.click_select_item");
        }
        return Component.literal(sbObjective);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        for (PlaceholderSuggestor suggestor : suggestors) {
            suggestor.render(graphics, mouseX, mouseY);
        }
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.edit_condition"),
                width / 2, 10, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (PlaceholderSuggestor suggestor : suggestors) {
            if (suggestor.keyPressed(keyCode)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (PlaceholderSuggestor suggestor : suggestors) {
            if (suggestor.mouseClicked(mouseX, mouseY)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        for (PlaceholderSuggestor suggestor : suggestors) {
            if (suggestor.mouseScrolled(mouseX, mouseY, scrollDelta)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(returnTo);
    }
}