package com.sal_fish.visual_set_edit.gui;

import com.sal_fish.visual_set_edit.api.DataDrivenEffectEntry;
import com.sal_fish.visual_set_edit.api.EffectTypeSpec;
import com.sal_fish.visual_set_edit.data.effect.EffectEntry;

// 按字段描述渲染编辑界面
public class GenericEffectEditor implements EffectEditorRegistry.Editor {

    private final EffectTypeSpec spec;
    private final FieldSpecPanel panel = new FieldSpecPanel();

    public GenericEffectEditor(EffectTypeSpec spec) {
        this.spec = spec;
    }

    @Override
    public int buildFields(EffectEditScreen screen, int centerX, int y, int totalWidth, int rowHeight, int spacing) {
        y = panel.render(FieldSpecPanel.hostOf(screen), spec.fields(), centerX, y, totalWidth, rowHeight, spacing);
        y = screen.buildCustomDisplayFields(centerX, y, totalWidth, rowHeight, spacing);
        screen.saveButton(centerX, y, totalWidth, rowHeight);
        return y + rowHeight + spacing;
    }

    @Override
    public void loadFrom(EffectEditScreen screen, EffectEntry entry) {
        panel.loadFrom(entry instanceof DataDrivenEffectEntry data ? data.params : null);
    }

    @Override
    public EffectEntry create(EffectEditScreen screen) {
        DataDrivenEffectEntry data = new DataDrivenEffectEntry();
        data.type = spec.id();
        data.params.putAll(panel.collect(spec.fields()));
        return data;
    }
}
