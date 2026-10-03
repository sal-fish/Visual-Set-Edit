package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.api.ConditionTypeSpec;
import com.sal_fish.visual_set_edit.api.DataDrivenCondition;
import com.sal_fish.visual_set_edit.data.condition.Condition;

// 按字段描述渲染编辑界面
public class GenericConditionEditor implements ConditionEditorRegistry.Editor {

    private final ConditionTypeSpec spec;
    private final FieldSpecPanel panel = new FieldSpecPanel();

    public GenericConditionEditor(ConditionTypeSpec spec) {
        this.spec = spec;
    }

    @Override
    public void buildFields(ConditionEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        panel.render(FieldSpecPanel.hostOf(screen), spec.fields(), centerX, y, totalWidth, rowHeight, spacing);
    }

    @Override
    public void loadFrom(ConditionEditScreen screen, Condition condition) {
        panel.loadFrom(condition instanceof DataDrivenCondition data ? data.params : null);
    }

    @Override
    public Condition create(ConditionEditScreen screen) {
        DataDrivenCondition data = new DataDrivenCondition();
        data.type = spec.id();
        data.needsPlayer = spec.requiresPlayer();
        data.params.putAll(panel.collect(spec.fields()));
        return data;
    }
}
