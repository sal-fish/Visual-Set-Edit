package com.sal_fish.visual_set_edit.event;

import com.sal_fish.visual_set_edit.config.PresetManager;
import com.sal_fish.visual_set_edit.data.NbtMatchRule;
import com.sal_fish.visual_set_edit.data.Preset;
import com.sal_fish.visual_set_edit.data.SetPhase;
import com.sal_fish.visual_set_edit.data.SlotCondition;
import com.sal_fish.visual_set_edit.data.effect.*;
import com.sal_fish.visual_set_edit.integration.IModIntegration;
import com.sal_fish.visual_set_edit.integration.IntegrationManager;
import com.sal_fish.visual_set_edit.network.S2CSyncPresetsPacket;
import com.sal_fish.visual_set_edit.network.VsePacketHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

public class SetEventHandler {

    private static final Set<UUID> UPDATING = new HashSet<>();
    private static final Set<UUID> PENDING_FIX = new HashSet<>();
    private static final Map<UUID, Long> LAST_HURT_TICKS = new HashMap<>();
    private static final Map<UUID, Float> LAST_HURT_AMOUNT = new HashMap<>();
    private static final Map<UUID, Integer> SNAPSHOT_HASH_CACHE = new HashMap<>();

    //公开辅助方法
    public static void forceReevaluate(LivingEntity entity) {
        SNAPSHOT_HASH_CACHE.remove(entity.getUUID());
    }
    public static void clearSnapshotCache(UUID uuid) {
        SNAPSHOT_HASH_CACHE.remove(uuid);
    }
    public static Long getLastHurtTick(LivingEntity entity) {
        return LAST_HURT_TICKS.get(entity.getUUID());
    }
    public static Float getLastHurtAmount(LivingEntity entity) {
        return LAST_HURT_AMOUNT.get(entity.getUUID());
    }

    //事件处理
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VsePacketHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new S2CSyncPresetsPacket(PresetManager.getPresets())
            );
            recreateEffects(player);
            SNAPSHOT_HASH_CACHE.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearSnapshotCache(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onEquipmentChange(LivingEquipmentChangeEvent event) {
        reevaluateIfChanged(event.getEntity(), true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer newPlayer) {
            ServerPlayer oldPlayer = (ServerPlayer) event.getOriginal();
            DynamicAttributeEffectEntry.transferDynamicData(oldPlayer, newPlayer);
            IntegrationManager.cleanupCuriosSlotsOnClone(oldPlayer, newPlayer);
            recreateEffects(newPlayer);
            SNAPSHOT_HASH_CACHE.remove(newPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        LivingEntity killer = null;

        if (event.getSource().getEntity() instanceof LivingEntity source) {
            killer = source;
        }

        for (var active : ActiveSetTracker.getActivePhases(dead)) {
            for (EffectEntry entry : active.phase().effects) {
                if (entry instanceof CommandEffectEntry cmd && cmd.trigger == CommandEffectEntry.Trigger.ON_DEATH) {
                    cmd.executeCommands(dead, cmd.commands, killer);
                }
            }
        }

        if (killer != null) {
            for (var active : ActiveSetTracker.getActivePhases(killer)) {
                for (EffectEntry entry : active.phase().effects) {
                    if (entry instanceof CommandEffectEntry cmd && cmd.trigger == CommandEffectEntry.Trigger.ON_KILL) {
                        cmd.executeCommands(killer, cmd.commands, dead);
                    }
                }
            }

            for (var active : ActiveSetTracker.getActivePhases(killer)) {
                for (EffectEntry effect : active.phase().effects) {
                    if (effect instanceof DynamicAttributeEffectEntry dynEff
                            && dynEff.variableType == DynamicAttributeEffectEntry.VariableType.KILL_COUNT_SINCE_EQUIP) {
                        DynamicAttributeEffectEntry.incrementKillCount(killer, dynEff);
                    }
                }
            }
        }

        // 清理内存
        SNAPSHOT_HASH_CACHE.remove(dead.getUUID());
        ActiveSetTracker.removeEntity(dead);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        LAST_HURT_TICKS.put(target.getUUID(), target.level().getGameTime());
        LAST_HURT_AMOUNT.put(target.getUUID(), event.getAmount());

        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            for (var active : ActiveSetTracker.getActivePhases(attacker)) {
                for (EffectEntry entry : active.phase().effects) {
                    if (entry instanceof CommandEffectEntry cmd && cmd.trigger == CommandEffectEntry.Trigger.ON_ATTACK) {
                        cmd.executeCommands(attacker, cmd.commands, target);
                    }
                }
            }

            for (var active : ActiveSetTracker.getActivePhases(target)) {
                for (EffectEntry entry : active.phase().effects) {
                    if (entry instanceof CommandEffectEntry cmd && cmd.trigger == CommandEffectEntry.Trigger.ON_HURT) {
                        cmd.executeCommands(target, cmd.commands , attacker);
                    }
                }
            }

            for (var active : ActiveSetTracker.getActivePhases(attacker)) {
                for (EffectEntry entry : active.phase().effects) {
                    if (entry instanceof PotionEffectEntry pot && "ATTACK_TARGET".equals(pot.target)) {
                        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(pot.mobEffectId));
                        if (effect != null) {
                            int dur = pot.durationSeconds == -1 ? MobEffectInstance.INFINITE_DURATION :
                                    (pot.durationSeconds <= 0 ? 60 : pot.durationSeconds * 20);
                            target.addEffect(new MobEffectInstance(effect, dur, pot.amplifier, false, pot.showParticles), attacker);
                        }
                    }
                }
            }
            for (var active : ActiveSetTracker.getActivePhases(attacker)) {
                for (EffectEntry entry : active.phase().effects) {
                    if (entry instanceof L2HostilityTraitEffectEntry traitEff
                            && "ATTACK_TARGET".equals(traitEff.target)) {
                        IntegrationManager.applyL2TraitToTarget(attacker, target, traitEff);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity.getPersistentData().getBoolean("vse_fallimmune")) {
            entity.fallDistance = 0;
        }

        if (PENDING_FIX.remove(entity.getUUID())) {
            ensureAllPermanentEffectsApplied(entity);
        }

        if (entity.tickCount % 20 == 0) {
            reevaluateIfChanged(entity, false);
            ensureAllPermanentEffectsApplied(entity);
            ensureAllPermanentAttributes(entity);
            processTimedPotionEffects(entity);
            processRepeatingCommands(entity);
            processDynamicAttributes(entity);
            IntegrationManager.tickL2Traits(entity);
        }
    }

    //药水效果变化
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (isImmuneToEffect(entity, event.getEffectInstance().getEffect())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onMobEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        MobEffect effect = event.getEffectInstance().getEffect();
        if (isImmuneToEffect(entity, effect)) {
            UPDATING.add(entity.getUUID());
            entity.removeEffect(effect);
            UPDATING.remove(entity.getUUID());
        }
        SNAPSHOT_HASH_CACHE.remove(entity.getUUID());
    }

    @SubscribeEvent
    public void onMobEffectRemoved(MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();
        if (UPDATING.contains(entity.getUUID())) return;

        MobEffect removedEffect = event.getEffect();
        ResourceLocation removedKey = ForgeRegistries.MOB_EFFECTS.getKey(removedEffect);
        if (removedKey != null) {
            String removedKeyStr = removedKey.toString();
            for (var active : ActiveSetTracker.getActivePhases(entity)) {
                for (EffectEntry entry : active.phase().effects) {
                    if (entry instanceof PotionEffectEntry pot && "SELF".equals(pot.target)) {
                        if (removedKeyStr.equals(pot.mobEffectId)) {
                            PENDING_FIX.add(entity.getUUID());
                            break;
                        }
                    }
                }
            }
        }
        SNAPSHOT_HASH_CACHE.remove(entity.getUUID());
    }

    @SubscribeEvent
    public void onMobEffectExpired(MobEffectEvent.Expired event) {
        SNAPSHOT_HASH_CACHE.remove(event.getEntity().getUUID());
    }

    // ========== 新触发器事件 ==========

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() != LogicalSide.SERVER) return;
        Player player = event.getEntity();
        BlockState targetState = event.getLevel().getBlockState(event.getPos());
        handleInstantTrigger(player, CommandEffectEntry.Trigger.ON_INTERACT_BLOCK, targetState, null);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getSide() != LogicalSide.SERVER) return;
        Player player = event.getEntity();
        Entity target = event.getTarget();
        handleInstantTrigger(player, CommandEffectEntry.Trigger.ON_INTERACT_ENTITY, null, target);
    }

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        BlockState placedState = event.getPlacedBlock();
        handleInstantTrigger(living, CommandEffectEntry.Trigger.ON_PLACE_BLOCK, placedState, null);
    }

    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getPlayer();
        BlockState brokenState = event.getState();
        handleInstantTrigger(player, CommandEffectEntry.Trigger.ON_BREAK_BLOCK, brokenState, null);
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        handleInstantTrigger(event.getEntity(), CommandEffectEntry.Trigger.ON_WAKE_UP, null, null);
    }

    //核心评估逻辑
    private boolean isImmuneToEffect(LivingEntity entity, MobEffect effect) {
        ResourceLocation effectKey = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (effectKey == null) return false;
        String effectId = effectKey.toString();

        for (var active : ActiveSetTracker.getActivePhases(entity)) {
            for (EffectEntry entry : active.phase().effects) {
                if (entry instanceof PotionEffectEntry pot
                        && "IMMUNE".equals(pot.target)
                        && effectId.equals(pot.mobEffectId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void reevaluateIfChanged(LivingEntity entity, boolean force) {
        if (!force) {
            int currentHash = computeDynamicHash(entity);
            Integer lastHash = SNAPSHOT_HASH_CACHE.get(entity.getUUID());
            if (lastHash != null && lastHash == currentHash) {
                evaluateTimeSensitivePhases(entity);
                return;
            }
            SNAPSHOT_HASH_CACHE.put(entity.getUUID(), currentHash);
        } else {
            SNAPSHOT_HASH_CACHE.remove(entity.getUUID());
        }

        fullReevaluate(entity);
    }

    private void fullReevaluate(LivingEntity entity) {
        Set<String> equippedIds = collectEquippedItemIds(entity);
        Set<String> candidatePresetIds = new HashSet<>();
        for (String id : equippedIds) candidatePresetIds.addAll(PresetManager.getPresetIdsForItem(id));

        // 加入所有零件套预设 ID
        candidatePresetIds.addAll(PresetManager.ZERO_COUNT_PRESET_IDS);

        List<ActiveSetTracker.ActivePhase> newPhases;
        if (candidatePresetIds.isEmpty()) {
            newPhases = evaluateAllPresets(entity);
        } else {
            newPhases = new ArrayList<>();
            for (String pid : candidatePresetIds) {
                Preset preset = PresetManager.getPresetById(pid);
                if (preset == null) continue;
                for (int i = 0; i < preset.phases.size(); i++) {
                    SetPhase phase = preset.phases.get(i);
                    if (isPhaseActive(entity, phase)) {
                        newPhases.add(new ActiveSetTracker.ActivePhase(preset.id, i, phase));
                    }
                }
            }
        }

        List<ActiveSetTracker.ActivePhase> oldPhases = ActiveSetTracker.getActivePhases(entity);
        if (phasesEqual(oldPhases, newPhases)) return;

        recreateEffectsFromList(entity, newPhases);
    }

    private void evaluateTimeSensitivePhases(LivingEntity entity) {
        Set<String> timeSensitiveIds = PresetManager.TIME_SENSITIVE_PHASE_IDS;
        if (timeSensitiveIds.isEmpty()) return;

        List<ActiveSetTracker.ActivePhase> oldPhases = ActiveSetTracker.getActivePhases(entity);
        List<ActiveSetTracker.ActivePhase> newPhases = new ArrayList<>();

        // 保留所有非时间敏感阶段的旧激活状态
        for (ActiveSetTracker.ActivePhase old : oldPhases) {
            String key = old.presetId() + ":" + old.phaseIndex();
            if (!timeSensitiveIds.contains(key)) {
                newPhases.add(old);
            }
        }

        // 重新评估所有时间敏感阶段
        for (String key : timeSensitiveIds) {
            String[] parts = key.split(":", 2);
            if (parts.length != 2) continue;
            String presetId = parts[0];
            int phaseIndex;
            try {
                phaseIndex = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                continue;
            }
            Preset preset = PresetManager.getPresetById(presetId);
            if (preset == null || phaseIndex < 0 || phaseIndex >= preset.phases.size()) continue;
            SetPhase phase = preset.phases.get(phaseIndex);
            if (isPhaseActive(entity, phase)) {
                newPhases.add(new ActiveSetTracker.ActivePhase(presetId, phaseIndex, phase));
            }
        }

        if (!phasesEqual(oldPhases, newPhases)) {
            recreateEffectsFromList(entity, newPhases);
        }
    }

    private int computeDynamicHash(LivingEntity entity) {
        int health = (int) entity.getHealth();
        int food = entity instanceof ServerPlayer player ? player.getFoodData().getFoodLevel() : 0;
        int armor = entity.getArmorValue();
        int xpLevel = entity instanceof ServerPlayer player ? player.experienceLevel : 0;
        int fallDistance = (int) entity.fallDistance;

        boolean submerged = entity.isInWaterOrBubble();
        boolean sneaking = entity.isCrouching();
        boolean sprinting = entity.isSprinting();
        boolean swimming = entity.isSwimming();
        boolean onGround = entity.onGround();
        boolean onWall = entity.onClimbable();
        boolean flying = entity instanceof ServerPlayer player && player.getAbilities().flying;
        boolean sleeping = entity.isSleeping();
        boolean riding = entity.isPassenger();

        var level = entity.level();
        String dimension = level.dimension().location().toString();
        String biome = level.getBiome(entity.blockPosition()).unwrapKey()
                .map(k -> k.location().toString()).orElse("");
        int blockY = entity.blockPosition().getY();
        boolean isRaining = level.isRaining();
        boolean isThundering = level.isThundering();
        int moonPhase = level.getMoonPhase();
        int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, entity.blockPosition());
        int blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, entity.blockPosition());
        int temperature = (int) (level.getBiome(entity.blockPosition()).get().getBaseTemperature() * 100);

        List<String> activeEffects = entity.getActiveEffects().stream()
                .map(effect -> Objects.requireNonNull(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect())).toString())
                .sorted()
                .collect(Collectors.toList());

        double mana = 0, manaPercent = 0;
        if (IntegrationManager.isIronSpellsLoaded()) {
            IModIntegration is = IntegrationManager.getIronSpells();
            mana = is.getMana(entity);
            manaPercent = is.getManaPercent(entity);
        }

        return Objects.hash(health, food, armor, xpLevel, fallDistance,
                submerged, sneaking, sprinting, swimming, onGround, onWall, flying, sleeping, riding,
                dimension, biome, blockY, isRaining, isThundering, moonPhase,
                skyLight, blockLight, temperature,
                activeEffects, mana, manaPercent);
    }

    //效果维护
    private void processRepeatingCommands(LivingEntity entity) {
        long gameTime = entity.level().getGameTime();
        for (var active : ActiveSetTracker.getActivePhases(entity)) {
            for (EffectEntry entry : active.phase().effects) {
                if (entry instanceof CommandEffectEntry cmd && cmd.trigger == CommandEffectEntry.Trigger.REPEAT) {
                    cmd.ensureUniqueId();
                    String key = "vse_cmd_" + cmd.uniqueId;
                    long lastExec = entity.getPersistentData().getLong(key);
                    long intervalTicks = cmd.repeatIntervalSeconds * 20L;
                    if (intervalTicks <= 0) continue;

                    if (lastExec == 0) {
                        entity.getPersistentData().putLong(key, gameTime);
                        continue;
                    }

                    if (gameTime - lastExec >= intervalTicks) {
                        cmd.executeCommands(entity, cmd.commands);
                        entity.getPersistentData().putLong(key, gameTime);
                    }
                }
            }
        }
    }

    private void processTimedPotionEffects(LivingEntity entity) {
        for (ActiveSetTracker.ActivePhase active : ActiveSetTracker.getActivePhases(entity)) {
            for (EffectEntry entry : active.phase().effects) {
                if (entry instanceof PotionEffectEntry pot
                        && "SELF".equals(pot.target)
                        && pot.durationSeconds != -1) {
                    pot.apply(entity);
                }
            }
        }
    }

    private void processDynamicAttributes(LivingEntity entity) {
        for (var active : ActiveSetTracker.getActivePhases(entity)) {
            for (EffectEntry entry : active.phase().effects) {
                if (entry instanceof DynamicAttributeEffectEntry dyn) {
                    dyn.updateModifier(entity);
                }
            }
        }
    }

    private void ensureAllPermanentEffectsApplied(LivingEntity entity) {
        for (var active : ActiveSetTracker.getActivePhases(entity)) {
            for (EffectEntry entry : active.phase().effects) {
                if (entry instanceof PotionEffectEntry pot && "SELF".equals(pot.target)) {
                    if (pot.durationSeconds != -1) continue;

                    MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(pot.mobEffectId));
                    if (effect != null) {
                        MobEffectInstance current = entity.getEffect(effect);
                        if (current == null || current.getDuration() < 10) {
                            int dur = MobEffectInstance.INFINITE_DURATION;
                            entity.addEffect(new MobEffectInstance(effect, dur, pot.amplifier, false, pot.showParticles), null);
                        }
                    }
                }
            }
        }
    }

    private void ensureAllPermanentAttributes(LivingEntity entity) {
        for (var active : ActiveSetTracker.getActivePhases(entity)) {
            for (EffectEntry entry : active.phase().effects) {
                if (entry instanceof com.sal_fish.visual_set_edit.data.effect.AttributeEffectEntry attr) {
                    attr.ensureUniqueId();
                    net.minecraft.world.entity.ai.attributes.Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(
                            new ResourceLocation(attr.attributeId));
                    if (attribute != null) {
                        var instance = entity.getAttribute(attribute);
                        if (instance != null) {
                            UUID id = UUID.fromString(attr.uniqueId);
                            if (instance.getModifier(id) == null) {
                                attr.apply(entity);
                            }
                        }
                    }
                }
            }
        }
    }

    private SetPhase deepCopyPhase(SetPhase original) {
        String json = PresetManager.GSON.toJson(original);
        SetPhase copy = PresetManager.GSON.fromJson(json, SetPhase.class);
        copy.initEffects();
        return copy;
    }

    //装备收集与阶段比较
    private Set<String> collectEquippedItemIds(LivingEntity entity) {
        Set<String> ids = new HashSet<>();
        addItemId(ids, entity.getItemBySlot(EquipmentSlot.MAINHAND));
        addItemId(ids, entity.getItemBySlot(EquipmentSlot.OFFHAND));
        addItemId(ids, entity.getItemBySlot(EquipmentSlot.HEAD));
        addItemId(ids, entity.getItemBySlot(EquipmentSlot.CHEST));
        addItemId(ids, entity.getItemBySlot(EquipmentSlot.LEGS));
        addItemId(ids, entity.getItemBySlot(EquipmentSlot.FEET));

        if (IntegrationManager.isCuriosLoaded()) {
            IModIntegration curios = IntegrationManager.getCurios();
            for (String slotId : curios.getExtraSlots()) {
                List<ItemStack> stacks = curios.getSlotStacks(entity, slotId);
                for (ItemStack stack : stacks) {
                    addItemId(ids, stack);
                }
            }
        }
        ids.remove("minecraft:air");
        return ids;
    }

    private void addItemId(Set<String> ids, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null) ids.add(id.toString());
    }

    private List<ActiveSetTracker.ActivePhase> evaluateAllPresets(LivingEntity entity) {
        List<ActiveSetTracker.ActivePhase> newPhases = new ArrayList<>();
        for (Preset preset : PresetManager.getPresets()) {
            for (int i = 0; i < preset.phases.size(); i++) {
                SetPhase phase = preset.phases.get(i);
                if (isPhaseActive(entity, phase)) {
                    newPhases.add(new ActiveSetTracker.ActivePhase(preset.id, i, phase));
                }
            }
        }
        return newPhases;
    }

    private boolean phasesEqual(List<ActiveSetTracker.ActivePhase> a, List<ActiveSetTracker.ActivePhase> b) {
        if (a.size() != b.size()) return false;
        Set<String> setA = new HashSet<>();
        for (var phase : a) setA.add(phase.presetId() + ":" + phase.phaseIndex());
        for (var phase : b) if (!setA.contains(phase.presetId() + ":" + phase.phaseIndex())) return false;
        return true;
    }

    //效果重建
    private void recreateEffects(LivingEntity entity) {
        List<ActiveSetTracker.ActivePhase> newPhases = evaluateAllPresets(entity);
        recreateEffectsFromList(entity, newPhases);
    }

    private void recreateEffectsFromList(LivingEntity entity, List<ActiveSetTracker.ActivePhase> newPhases) {
        if (!entity.isAlive() || entity.isDeadOrDying()) return;
        UPDATING.add(entity.getUUID());
        float healthRatio = entity.getMaxHealth() > 0 ? entity.getHealth() / entity.getMaxHealth() : 1.0f;
        List<ActiveSetTracker.ActivePhase> oldPhases = ActiveSetTracker.getActivePhases(entity);
        Map<String, ActiveSetTracker.ActivePhase> oldPhaseMap = new HashMap<>();
        for (var old : oldPhases) {
            oldPhaseMap.put(old.presetId() + ":" + old.phaseIndex(), old);
        }
        List<ActiveSetTracker.ActivePhase> finalPhases = new ArrayList<>();
        for (var old : oldPhases) {
            String key = old.presetId() + ":" + old.phaseIndex();
            boolean stillActive = false;
            for (var n : newPhases) {
                if ((n.presetId() + ":" + n.phaseIndex()).equals(key)) {
                    stillActive = true;
                    break;
                }
            }
            if (!stillActive) {
                for (EffectEntry entry : old.phase().effects) {
                    entry.remove(entity);
                }
            }
        }
        for (var n : newPhases) {
            String key = n.presetId() + ":" + n.phaseIndex();
            ActiveSetTracker.ActivePhase oldPhase = oldPhaseMap.get(key);

            if (oldPhase != null) {
                finalPhases.add(oldPhase);
            } else {
                SetPhase independentPhase = deepCopyPhase(n.phase());
                ActiveSetTracker.ActivePhase newActive =
                        new ActiveSetTracker.ActivePhase(n.presetId(), n.phaseIndex(), independentPhase);
                finalPhases.add(newActive);
                for (EffectEntry entry : independentPhase.effects) {
                    entry.apply(entity);
                }
            }
        }
        ActiveSetTracker.setActivePhases(entity, finalPhases);
        if (entity.getMaxHealth() > 0) {
            float newHealth = entity.getMaxHealth() * Math.min(healthRatio, 1.0f);
            entity.setHealth(Math.max(newHealth, 1.0f));
        }
        UPDATING.remove(entity.getUUID());
    }

    private boolean isPhaseActive(LivingEntity entity, SetPhase phase) {
        List<SlotCondition> sortedConditions = new ArrayList<>(phase.slotConditions);
        sortedConditions.sort(Comparator.comparingInt(cond ->
                (cond.nbtRule == NbtMatchRule.IGNORE) ? 1 : 0));

        int matched = 0;
        Map<String, ItemStack> equipment = new HashMap<>();
        equipment.put("HEAD", entity.getItemBySlot(EquipmentSlot.HEAD));
        equipment.put("CHEST", entity.getItemBySlot(EquipmentSlot.CHEST));
        equipment.put("LEGS", entity.getItemBySlot(EquipmentSlot.LEGS));
        equipment.put("FEET", entity.getItemBySlot(EquipmentSlot.FEET));
        equipment.put("MAINHAND", entity.getItemBySlot(EquipmentSlot.MAINHAND));
        equipment.put("OFFHAND", entity.getItemBySlot(EquipmentSlot.OFFHAND));

        Map<String, List<ItemStack>> curiosStacks = null;
        if (IntegrationManager.isCuriosLoaded()) {
            boolean hasCuriosCond = sortedConditions.stream().anyMatch(cond ->
                    cond.slot.startsWith("curios:") || cond.slot.equals(IModIntegration.ANY_CURIOS_SLOT));
            if (hasCuriosCond) {
                curiosStacks = IntegrationManager.getCurios().getAllEquippedStacks(entity);
            }
        }

        Map<String, Set<Integer>> usedIndices = new HashMap<>();

        for (SlotCondition cond : sortedConditions) {
            if (cond.slot.startsWith("curios:") && IntegrationManager.isCuriosLoaded()) {
                String realSlotId = cond.slot.substring(7);
                List<ItemStack> stacks;

                if (cond.slot.equals(IModIntegration.ANY_CURIOS_SLOT)) {
                    boolean found = false;
                    if (curiosStacks != null) {
                        for (Map.Entry<String, List<ItemStack>> entry : curiosStacks.entrySet()) {
                            String slotId = entry.getKey();
                            List<ItemStack> slotStacks = entry.getValue();
                            for (int i = 0; i < slotStacks.size(); i++) {
                                ItemStack stack = slotStacks.get(i);
                                if (cond.matches(stack)) {
                                    boolean used = usedIndices.containsKey(slotId) && usedIndices.get(slotId).contains(i);
                                    if (!used) {
                                        usedIndices.computeIfAbsent(slotId, k -> new HashSet<>()).add(i);
                                        found = true;
                                        break;
                                    }
                                }
                            }
                            if (found) break;
                        }
                    }
                    if (found) matched++;
                } else {
                    stacks = curiosStacks != null ? curiosStacks.getOrDefault(realSlotId, Collections.emptyList()) : Collections.emptyList();
                    boolean found = false;
                    for (int i = 0; i < stacks.size(); i++) {
                        ItemStack stack = stacks.get(i);
                        boolean matches = cond.matches(stack);
                        boolean used = usedIndices.containsKey(realSlotId) && usedIndices.get(realSlotId).contains(i);

                        if (used) continue;
                        if (matches) {
                            found = true;
                            usedIndices.computeIfAbsent(realSlotId, k -> new HashSet<>()).add(i);
                            break;
                        }
                    }
                    if (found) matched++;
                }
            } else {
                ItemStack stack = equipment.get(cond.slot);
                if (cond.matches(stack)) matched++;
            }
        }

        if (matched < phase.requiredCount) return false;
        for (var cond : phase.additionalConditions) {
            if (!cond.test(entity)) return false;
        }
        return true;
    }

    private void handleInstantTrigger(LivingEntity entity, CommandEffectEntry.Trigger trigger,
                                      BlockState blockState, Entity targetEntity) {
        if (entity.level().isClientSide) return;
        List<ActiveSetTracker.ActivePhase> activePhases = ActiveSetTracker.getActivePhases(entity);
        if (activePhases.isEmpty()) return;

        for (ActiveSetTracker.ActivePhase active : activePhases) {
            for (EffectEntry entry : active.phase().effects) {
                if (!(entry instanceof CommandEffectEntry cmd)) continue;
                if (cmd.trigger != trigger) continue;
                if (blockState != null) {
                    if (!cmd.targetFilter.matches(blockState)) continue;
                }
                if (targetEntity != null) {
                    if (!cmd.targetFilter.matches(targetEntity)) continue;
                }

                cmd.executeCommands(entity, cmd.commands, targetEntity);
            }
        }
    }
}