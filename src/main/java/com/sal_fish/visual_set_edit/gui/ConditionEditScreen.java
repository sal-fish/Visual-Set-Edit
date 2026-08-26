package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.data.condition.*;
import com.sal_fish.visual_set_edit.data.SlotCondition;
import com.sal_fish.visual_set_edit.data.NbtMatchRule;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import net.minecraft.client.gui.GuiGraphics;
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

public class ConditionEditScreen extends Screen {
    private final Consumer<Condition> onSave;
    private final Screen returnTo;
    private final Condition existingCondition;

    private String condType = "environment";
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
    private final List<Condition> children = new ArrayList<>();

    private String isField = "MANA";
    private String isComparator = "EQ";
    private double isValue = 0;

    private String attrId = "";
    private String attrComparator = "GTE";
    private double attrValue = 0;
    private EditBox attrValueEdit;
    private Button selectAttrButton;

    private EditBox valueEdit, invDurMinEdit, invDurMaxEdit, isValueEdit;
    private Button invSlotButton, invItemButton;
    private EditBox invTagEdit;

    private final List<Condition> tempChildren = new ArrayList<>();

    public ConditionEditScreen(Consumer<Condition> onSave, Screen returnTo) {
        this(onSave, returnTo, null);
    }

    public ConditionEditScreen(Consumer<Condition> onSave, Screen returnTo, Condition existing) {
        super(Component.translatable("visual_set_edit.gui.edit_condition"));
        this.onSave = onSave;
        this.returnTo = returnTo;
        this.existingCondition = existing;
        if (existing != null) {
            loadFromExisting(existing);
        }
    }

    private void loadFromExisting(Condition c) {
        condType = c.type;
        if (c instanceof EnvironmentCondition env) {
            field = env.field;
            comparator = env.comparator;
            value = env.value;
        } else if (c instanceof PlayerStateCondition ps) {
            field = ps.field;
            comparator = ps.comparator;
            value = ps.value;
        } else if (c instanceof InventoryCondition inv) {
            invSlot = inv.slot != null ? inv.slot : "HEAD";
            if (inv.itemCondition != null) {
                invItemId = inv.itemCondition.itemId;
                invTagId = inv.itemCondition.tagId;
                invNbtRule = inv.itemCondition.nbtRule;
                invDurMin = inv.itemCondition.durabilityMinPercent;
                invDurMax = inv.itemCondition.durabilityMaxPercent;
            }
        } else if (c instanceof IronSpellCondition is) {
            isField = is.field;
            isComparator = is.comparator;
            isValue = is.value;
        } else if (c instanceof AttributeCondition attrCond) {
            attrId = attrCond.attributeId;
            attrComparator = attrCond.comparator;
            attrValue = attrCond.value;
        } else if (c instanceof CompositeCondition comp) {
            compOp = comp.op;
            children.clear();
            if (comp.children != null) children.addAll(comp.children);
        }
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
        CycleButton<String> typeButton = CycleButton.<String>builder(s ->
                        Component.translatable("visual_set_edit.gui.condition.type." + s))
                .withValues("environment", "player_state", "inventory", "iron_spell", "attribute", "composite")
                .displayOnlyValue()
                .withInitialValue(condType)
                .create(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                        Component.translatable("visual_set_edit.gui.type"), (btn, val) -> {
                            condType = val;
                            init();
                        });
        addRenderableWidget(typeButton);
        y += rowHeight + spacing;

        switch (condType) {
            case "environment", "player_state" -> buildCommonFields(centerX, y, totalWidth, rowHeight, spacing);
            case "inventory" -> buildInventoryFields(centerX, y, totalWidth, rowHeight, spacing);
            case "iron_spell" -> buildIronSpellFields(centerX, y, totalWidth, rowHeight, spacing);
            case "composite" -> buildCompositeFields(centerX, y, totalWidth, rowHeight, spacing);
            case "attribute" -> buildAttributeConditionFields(centerX, y, totalWidth, rowHeight, spacing);
        }
    }

    private void buildCommonFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        List<String> fieldOptions = getFieldOptions();
        if (!fieldOptions.isEmpty()) {
            if (field.isEmpty() || !fieldOptions.contains(field)) {
                field = fieldOptions.get(0);
            }
            addRenderableWidget(new StringWidget(centerX - totalWidth / 2, y, totalWidth, rowHeight,
                    Component.translatable("visual_set_edit.gui.condition.field"), font));
            y += rowHeight;
            CycleButton<String> fieldButton = CycleButton.<String>builder(s -> Component.translatable("visual_set_edit.gui.condition.field." + condType + "." + s))
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

        // 比较符部分：IS_HURT 和 TAG 不需要通用比较符
        if (!"IS_HURT".equals(field) && !"TAG".equals(field)) {
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
                    || ("player_state".equals(condType) && "HAS_EFFECT".equals(field));
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
        }
        y += 6;

        saveButton(centerX, y, totalWidth, rowHeight);
    }

    //库存条件
    private void buildInventoryFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
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

        saveButton(centerX, y, totalWidth, rowHeight);
    }

    //铁魔法条件
    private void buildIronSpellFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
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
            isValueEdit.setValue(String.valueOf(isValue));
            addRenderableWidget(isValueEdit);
            y += rowHeight + spacing;
        }
        y += 6;
        saveButton(centerX, y, totalWidth, rowHeight);
    }
    //属性条件
    private void buildAttributeConditionFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
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
        attrValueEdit.setValue(String.valueOf(attrValue));
        attrValueEdit.setResponder(s -> {
            try { attrValue = Double.parseDouble(s); } catch (Exception ignored) {}
        });
        addRenderableWidget(attrValueEdit);
        y += rowHeight + spacing;

        saveButton(centerX, y, totalWidth, rowHeight);
    }

    //复合条件
    private void buildCompositeFields(int centerX, int y, int totalWidth, int rowHeight, int spacing) {
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

        if (tempChildren.isEmpty()) {
            tempChildren.addAll(children);
        }

        for (int i = 0; i < tempChildren.size(); i++) {
            Condition child = tempChildren.get(i);
            int index = i;
            String text = child.getDisplayText();
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

        saveButton(centerX, y, totalWidth, rowHeight);
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
        return switch (condType) {
            case "environment" -> {
                EnvironmentCondition e = new EnvironmentCondition();
                e.field = field;
                e.comparator = comparator;
                e.value = value; // 天气下拉直接更新了 value 字段
                if (valueEdit != null) e.value = valueEdit.getValue(); // 覆盖以保证最新值
                yield e;
            }
            case "player_state" -> {
                PlayerStateCondition p = new PlayerStateCondition();
                p.field = field;
                p.comparator = comparator;
                p.value = valueEdit != null ? valueEdit.getValue() : value;
                yield p;
            }
            case "inventory" -> {
                InventoryCondition ic = new InventoryCondition();
                ic.slot = invSlot;
                SlotCondition sc = new SlotCondition();
                sc.itemId = invItemId;
                sc.tagId = invTagId;
                sc.nbtRule = invNbtRule;
                try { sc.durabilityMinPercent = Integer.parseInt(invDurMinEdit.getValue()); } catch(Exception ignored) {}
                try { sc.durabilityMaxPercent = Integer.parseInt(invDurMaxEdit.getValue()); } catch(Exception ignored) {}
                ic.itemCondition = sc;
                yield ic;
            }
            case "iron_spell" -> {
                IronSpellCondition is = new IronSpellCondition();
                is.field = isField;
                is.comparator = isComparator;
                try { is.value = Double.parseDouble(isValueEdit != null ? isValueEdit.getValue() : "0"); } catch(Exception ignored) {}
                yield is;
            }
            case "attribute" -> {
                AttributeCondition ac = new AttributeCondition();
                ac.attributeId = attrId;
                ac.comparator = attrComparator;
                try { ac.value = Double.parseDouble(attrValueEdit.getValue()); } catch (Exception ignored) {}
                yield ac;
            }
            case "composite" -> {
                CompositeCondition cc = new CompositeCondition();
                cc.op = compOp;
                cc.children = new ArrayList<>(tempChildren);
                yield cc;
            }
            default -> null;
        };
    }

    private List<String> getFieldOptions() {
        return switch (condType) {
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

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, Component.translatable("visual_set_edit.gui.edit_condition"),
                width / 2, 10, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(returnTo);
    }

    public Condition getExistingCondition() {
        return existingCondition;
    }
}