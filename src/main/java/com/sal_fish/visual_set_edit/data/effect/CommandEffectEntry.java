package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;

import java.util.List;
import java.util.UUID;

public class CommandEffectEntry extends EffectEntry {
    public enum Mode {
        IMPULSE,
        REPEATING
    }

    @Expose public List<String> activateCommands;
    @Expose public List<String> deactivateCommands;
    @Expose public Mode mode = Mode.IMPULSE;
    @Expose public int repeatIntervalSeconds = 1;
    @Expose public String uniqueId;

    public CommandEffectEntry() {
        this.type = "command";
        this.uniqueId = UUID.randomUUID().toString();
    }

    public void ensureUniqueId() {
        if (uniqueId == null || uniqueId.isEmpty()) {
            uniqueId = UUID.randomUUID().toString();
        }
    }

    @Override
    public void initAfterLoad() {
        ensureUniqueId();
    }

    @Override
    public void resetUniqueId() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    @Override
    public void apply(LivingEntity entity) {
        executeCommands(entity, activateCommands);
    }

    @Override
    public void remove(LivingEntity entity) {
        executeCommands(entity, deactivateCommands);
    }

    public void executeCommands(LivingEntity entity, List<String> cmds) {
        if (cmds == null || !(entity.level() instanceof ServerLevel level)) return;
        for (String cmd : cmds) {
            CommandSourceStack source = new CommandSourceStack(
                    entity,
                    entity.position(),
                    new Vec2(entity.getXRot(), entity.getYRot()),
                    level,
                    2,
                    entity.getName().getString(),
                    entity.getDisplayName(),
                    level.getServer(),
                    entity
            );
            source = source.withSuppressedOutput();
            level.getServer().getCommands().performPrefixedCommand(source, cmd);
        }
    }

    @Override
    public String getDisplayText() {
        return Component.translatable("visual_set_edit.gui.effect.command.display",
                activateCommands != null ? String.join(", ", activateCommands) : "").getString();
    }
}