package com.sal_fish.visual_set_edit.data.effect;

import com.google.gson.annotations.Expose;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommandEffectEntry extends EffectEntry {
    public enum Mode {
        IMPULSE,
        REPEATING
    }
    public enum Trigger {
        ACTIVATE,
        DEACTIVATE,
        REPEAT,
        ON_ATTACK,
        ON_HURT,
        ON_KILL,
        ON_DEATH
    }

    @Expose public List<String> activateCommands;
    @Expose public List<String> deactivateCommands;
    @Expose public Mode mode = Mode.IMPULSE;
    @Expose public int repeatIntervalSeconds = 1;
    @Expose public String uniqueId;
    @Expose public Trigger trigger = Trigger.ACTIVATE;
    @Expose public List<String> commands = new ArrayList<>();
    @Expose public double probability = 1.0;

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
        if (commands == null || commands.isEmpty()) {
            if (trigger == Trigger.ACTIVATE && activateCommands != null && !activateCommands.isEmpty()) {
                commands = new ArrayList<>(activateCommands);
            } else if (trigger == Trigger.DEACTIVATE && deactivateCommands != null && !deactivateCommands.isEmpty()) {
                commands = new ArrayList<>(deactivateCommands);
            } else if (trigger == Trigger.REPEAT && activateCommands != null && !activateCommands.isEmpty()) {
                commands = new ArrayList<>(activateCommands);
            }
        }
        if (trigger == Trigger.REPEAT && repeatIntervalSeconds <= 0) {
            repeatIntervalSeconds = 1;
        }
        if (probability < 0) probability = 0;
        if (probability > 1) probability = 1;
    }

    @Override
    public void resetUniqueId() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    @Override
    public void apply(LivingEntity entity) {
        ensureUniqueId();
        if (trigger == Trigger.ACTIVATE) {
            executeCommands(entity, commands);
        } else if (trigger == Trigger.REPEAT) {
            String key = "vse_cmd_" + uniqueId;
            if (!entity.getPersistentData().contains(key)) {
                entity.getPersistentData().putLong(key, entity.level().getGameTime());
            }
        }
    }

    @Override
    public void remove(LivingEntity entity) {
        ensureUniqueId();
        if (trigger == Trigger.DEACTIVATE) {
            executeCommands(entity, commands);
        }
    }

    public void executeCommands(LivingEntity entity, List<String> cmds) {
        if (cmds == null || cmds.isEmpty()) return;
        // 概率判定
        if (probability < 1.0) {
            if (entity.level().random.nextDouble() >= probability) {
                return;
            }
        }
        if (!(entity.level() instanceof ServerLevel level)) return;
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
        String triggerName = Component.translatable("visual_set_edit.gui.effect.command.trigger." + trigger.name().toLowerCase()).getString();
        String cmdSummary = commands != null && !commands.isEmpty() ? String.join(", ", commands) : "";
        return Component.translatable("visual_set_edit.gui.effect.command.display", triggerName, cmdSummary).getString();
    }
}