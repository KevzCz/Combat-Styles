package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.style.StyleEntry;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public record EquipmentTrigger(Optional<Integer> everyNthSwing,
                          Optional<Integer> slotIndex,
                          Optional<Boolean> lastSlot,
                          Optional<String> hitbox,
                          Optional<String> styleId,
                          Optional<String> affixId,
                          Optional<Boolean> requiresAffix,
                          Optional<Integer> idleTicks,
                          PhaseGate phase,
                          HurtGate hurt,
                          Optional<SlotSelection> slots,
                          Optional<SlotBand> slotBand,
                          HealthGate health,
                          ActivationGate activation,
                          List<EquipmentTrigger> anyOf,
                          List<EquipmentTrigger> allOf) {

    public static final EquipmentTrigger ALWAYS = new EquipmentTrigger(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            PhaseGate.ANY, HurtGate.ANY,
            Optional.empty(), Optional.empty(),
            HealthGate.ANY, ActivationGate.ANY, List.of(), List.of());

    public Optional<Boolean> onMiss() {
        return phase.onMiss();
    }

    public Optional<Boolean> onKill() {
        return phase.onKill();
    }

    public Optional<Boolean> onSummonHit() {
        return phase.onSummonHit();
    }

    public boolean reactsToHurt() {
        return !hurt.isEmpty();
    }

    public boolean requiresActivation() {
        if (reactsToHurt() || activation.reactsToShoot() || activation.reactsToBlock()
                || activation.usesItem() || activation.reactsToProjectileHit()
                || activation.reactsToCast() || activation.reactsToSummon()) {
            return true;
        }
        for (EquipmentTrigger required : allOf) {
            if (required.requiresActivation()) {
                return true;
            }
        }
        if (anyOf.isEmpty()) {
            return false;
        }
        for (EquipmentTrigger option : anyOf) {
            if (!option.requiresActivation()) {
                return false;
            }
        }
        return true;
    }

    public boolean inTree(Predicate<EquipmentTrigger> test) {
        if (test.test(this)) {
            return true;
        }
        for (EquipmentTrigger required : allOf) {
            if (required.inTree(test)) {
                return true;
            }
        }
        for (EquipmentTrigger option : anyOf) {
            if (option.inTree(test)) {
                return true;
            }
        }
        return false;
    }

    private boolean ownEvent() {
        return reactsToHurt() || phase.onKill().orElse(false)
                || phase.onMiss().orElse(false) || phase.onSummonHit().orElse(false)
                || activation.reactsToShoot() || activation.reactsToBlock()
                || activation.usesItem() || activation.reactsToProjectileHit()
                || activation.reactsToCast()
                || activation.reactsToSummon();
    }

    public boolean isEvent() {
        return inTree(EquipmentTrigger::ownEvent);
    }

    public boolean arms(EffectCooldown window) {
        return isEvent() && window != null && window.windowed();
    }

    public boolean eventFiring(EquipmentContext context) {
        if (ownEventFiring(context)) {
            return true;
        }
        for (EquipmentTrigger required : allOf) {
            if (required.eventFiring(context)) {
                return true;
            }
        }
        for (EquipmentTrigger option : anyOf) {
            if (option.eventFiring(context)) {
                return true;
            }
        }
        return false;
    }

    private boolean ownEventFiring(EquipmentContext context) {
        if (reactsToHurt()) {
            return context.hurt() != null;
        }
        if (phase.onKill().orElse(false)) {
            return context.killed();
        }
        if (phase.onSummonHit().orElse(false)) {
            return context.summonHit();
        }
        if (phase.onMiss().orElse(false)) {
            return CombatState.lastSwingMissed(context.player());
        }
        if (activation.reactsToShoot()) {
            return context.shot() != null;
        }
        if (activation.reactsToBlock()) {
            return context.blocked() != null;
        }
        if (activation.usesItem()) {
            return context.used() != null;
        }
        if (activation.reactsToProjectileHit()) {
            return context.projectileHit() != null;
        }
        if (activation.reactsToCast()) {
            return context.cast() != null;
        }
        if (activation.reactsToSummon()) {
            return context.summoned() != null;
        }
        return false;
    }

    public EquipmentTrigger {
        hitbox = named(hitbox);
        styleId = named(styleId);
        affixId = named(affixId);
        phase = phase == null ? PhaseGate.ANY : phase;
        hurt = hurt == null ? HurtGate.ANY : hurt;
        activation = activation == null ? ActivationGate.ANY : activation;
        anyOf = anyOf == null ? List.of() : List.copyOf(anyOf);
        allOf = allOf == null ? List.of() : List.copyOf(allOf);
    }

    public static final Codec<EquipmentTrigger> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("every_nth_swing").forGetter(EquipmentTrigger::everyNthSwing),
            Codec.INT.optionalFieldOf("slot_index").forGetter(EquipmentTrigger::slotIndex),
            Codec.BOOL.optionalFieldOf("last_slot").forGetter(EquipmentTrigger::lastSlot),
            Codec.STRING.optionalFieldOf("hitbox").forGetter(EquipmentTrigger::hitbox),
            Codec.STRING.optionalFieldOf("style_id").forGetter(EquipmentTrigger::styleId),
            Codec.STRING.optionalFieldOf("affix_id").forGetter(EquipmentTrigger::affixId),
            Codec.BOOL.optionalFieldOf("requires_affix").forGetter(EquipmentTrigger::requiresAffix),
            Codec.INT.optionalFieldOf("idle_ticks").forGetter(EquipmentTrigger::idleTicks),
            PhaseGate.CODEC.forGetter(EquipmentTrigger::phase),
            HurtGate.CODEC.optionalFieldOf("on_hurt", HurtGate.ANY)
                    .forGetter(EquipmentTrigger::hurt),
            SlotSelection.CODEC.optionalFieldOf("slots").forGetter(EquipmentTrigger::slots),
            SlotBand.CODEC.optionalFieldOf("slot_band").forGetter(EquipmentTrigger::slotBand),
            HealthGate.CODEC.optionalFieldOf("health", HealthGate.ANY)
                    .forGetter(EquipmentTrigger::health),
            ActivationGate.CODEC.forGetter(EquipmentTrigger::activation),
            Codec.lazyInitialized(() -> EquipmentTrigger.CODEC).listOf()
                    .optionalFieldOf("any_of", List.of()).forGetter(EquipmentTrigger::anyOf),
            Codec.lazyInitialized(() -> EquipmentTrigger.CODEC).listOf()
                    .optionalFieldOf("all_of", List.of()).forGetter(EquipmentTrigger::allOf)
    ).apply(instance, EquipmentTrigger::new));

    public boolean isAlways() {
        return withChance(Optional.empty()).equals(ALWAYS);
    }

    public Optional<Float> chance() {
        return phase.chance();
    }

    public boolean rollsChance() {
        return phase.chance().map(value -> value < EffectGroup.ALWAYS).orElse(false);
    }

    public EquipmentTrigger withChance(Optional<Float> value) {
        if (value.equals(phase.chance())) {
            return this;
        }
        return new EquipmentTrigger(everyNthSwing, slotIndex, lastSlot, hitbox, styleId, affixId,
                requiresAffix, idleTicks, phase.withChance(value), hurt, slots, slotBand, health,
                activation, anyOf, allOf);
    }

    private boolean ownInstantEvent() {
        return reactsToHurt() || phase.onKill().orElse(false)
                || phase.onMiss().orElse(false) || phase.onSummonHit().orElse(false)
                || phase.wantsHitPass() || activation.reactsToProjectileHit()
                || activation.reactsToShoot() || activation.reactsToBlock()
                || !activation.dealt().isEmpty()
                || activation.reactsToCast()
                || activation.reactsToSummon()
                || (activation.usesItem() && activation.onUse().instant());
    }

    private boolean ownSwingOnly() {
        return slotIndex.isPresent() || lastSlot.isPresent() || slots.isPresent()
                || slotBand.isPresent() || hitbox.isPresent() || styleId.isPresent()
                || affixId.isPresent() || requiresAffix.isPresent();
    }

    public boolean needsArmingAtRest() {
        if (ownInstantEvent()) {
            return true;
        }
        for (EquipmentTrigger required : allOf) {
            if (required.needsArmingAtRest()) {
                return true;
            }
        }
        if (anyOf.isEmpty()) {
            return false;
        }
        for (EquipmentTrigger option : anyOf) {
            if (!option.needsArmingAtRest()) {
                return false;
            }
        }
        return true;
    }

    public boolean swingOnlyAtRest() {
        return inTree(EquipmentTrigger::ownSwingOnly);
    }

    public boolean swingDriven() {
        return inTree(node -> node.ownSwingOnly() || node.everyNthSwing().isPresent());
    }

    public boolean liveWhileUsing() {
        return inTree(trigger -> trigger.activation().usesItem()
                && !trigger.activation().onUse().instant());
    }

    public boolean readyAtRest(Player player, int swingCount, EquipmentDefinition definition) {
        return readyAtRest(player, swingCount, definition, false);
    }

    public boolean readyAtRest(Player player, int swingCount, EquipmentDefinition definition,
                               boolean swingGates) {
        if (ownInstantEvent()) {
            return false;
        }
        if (swingGates && ownSwingOnly() && !ownSwingGatesPass(CurrentAttack.of(player))) {
            return false;
        }
        if (!activation.onUse().heldNow(player, definition)) {
            return false;
        }
        if (!activation.cast().castingNow(player)) {
            return false;
        }
        if (health.needsTarget() || phase.state().needsTarget()) {
            return false;
        }
        if (health.rejectsSelf(player) != null || phase.state().rejectsAtRest(player) != null) {
            return false;
        }
        for (EquipmentTrigger required : allOf) {
            if (!required.readyAtRest(player, swingCount, definition, swingGates)) {
                return false;
            }
        }
        if (!anyOf.isEmpty()) {
            boolean any = false;
            for (EquipmentTrigger option : anyOf) {
                any |= option.readyAtRest(player, swingCount, definition, swingGates);
            }
            if (!any) {
                return false;
            }
        }
        if (everyNthSwing.isPresent()
                && swingCount % Math.max(1, everyNthSwing.get()) != 0) {
            return false;
        }
        return idleTicks.isEmpty() || CombatState.idleTicks(player) >= idleTicks.get();
    }

    private boolean ownSwingGatesPass(CurrentAttack attack) {
        if (attack == null) {
            return false;
        }
        if (slotIndex.isPresent() && attack.slot() != slotIndex.get()) {
            return false;
        }
        if (lastSlot.orElse(false) && attack.slot() != attack.size() - 1) {
            return false;
        }
        if (slots.isPresent() && !slots.get().matches(attack.slot(), attack.size())) {
            return false;
        }
        if (slotBand.isPresent() && !slotBand.get().matches(attack.slot(), attack.size())) {
            return false;
        }
        if (hitbox.isPresent()
                && (attack.hitbox() == null || !attack.hitbox().equalsIgnoreCase(hitbox.get()))) {
            return false;
        }
        StyleEntry entry = attack.entry();
        if (styleId.isPresent() && (entry == null || !namesStyle(entry, styleId.get()))) {
            return false;
        }
        if (affixId.isPresent() && (entry == null || !entry.affixIds().contains(affixId.get()))) {
            return false;
        }
        return !requiresAffix.orElse(false) || (entry != null && !entry.affixIds().isEmpty());
    }

    public boolean gatesOnHitbox() {
        if (hitbox.isPresent()) {
            return true;
        }
        for (EquipmentTrigger required : allOf) {
            if (required.gatesOnHitbox()) {
                return true;
            }
        }
        for (EquipmentTrigger option : anyOf) {
            if (option.gatesOnHitbox()) {
                return true;
            }
        }
        return false;
    }

    public boolean readyAtHitbox(String shape) {
        if (hitbox.isPresent()
                && (shape == null || !shape.equalsIgnoreCase(hitbox.get()))) {
            return false;
        }
        for (EquipmentTrigger required : allOf) {
            if (!required.readyAtHitbox(shape)) {
                return false;
            }
        }
        if (anyOf.isEmpty()) {
            return true;
        }
        for (EquipmentTrigger option : anyOf) {
            if (option.readyAtHitbox(shape)) {
                return true;
            }
        }
        return false;
    }

    public boolean gatesOnSlot() {
        if (lastSlot.isPresent() || slotIndex.isPresent()
                || slots.isPresent() || slotBand.isPresent()) {
            return true;
        }
        for (EquipmentTrigger required : allOf) {
            if (required.gatesOnSlot()) {
                return true;
            }
        }
        for (EquipmentTrigger option : anyOf) {
            if (option.gatesOnSlot()) {
                return true;
            }
        }
        return false;
    }

    public boolean readyAtSlot(int slotIndexValue, int sequenceSize) {
        if (slotIndexValue < 0 || sequenceSize <= 0) {
            return true;
        }
        if (slotIndex.isPresent() && slotIndexValue != slotIndex.get()) {
            return false;
        }
        if (lastSlot.orElse(false) && slotIndexValue != sequenceSize - 1) {
            return false;
        }
        if (slots.isPresent() && !slots.get().matches(slotIndexValue, sequenceSize)) {
            return false;
        }
        if (slotBand.isPresent()
                && !slotBand.get().matches(slotIndexValue, sequenceSize)) {
            return false;
        }
        for (EquipmentTrigger required : allOf) {
            if (!required.readyAtSlot(slotIndexValue, sequenceSize)) {
                return false;
            }
        }
        if (anyOf.isEmpty()) {
            return true;
        }
        for (EquipmentTrigger option : anyOf) {
            if (option.readyAtSlot(slotIndexValue, sequenceSize)) {
                return true;
            }
        }
        return false;
    }

    public boolean requiresTarget() {
        if (health.needsTarget() || phase.state().needsTarget()
                || phase.wantsHitPass() || activation.reactsToProjectileHit()
                || activation.dealt().needsTarget()) {
            return true;
        }
        for (EquipmentTrigger option : anyOf) {
            if (option.requiresTarget()) {
                return true;
            }
        }
        for (EquipmentTrigger required : allOf) {
            if (required.requiresTarget()) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(EquipmentContext context) {
        return rejectedBy(context) == null;
    }

    public boolean matches(EquipmentContext context, EffectCooldown window) {
        return arms(window) ? rejectedByArmed(context) == null : rejectedBy(context) == null;
    }

    public String explain(EquipmentContext context) {
        return rejectedBy(context);
    }

    private String rejectedByArmed(EquipmentContext context) {
        if (eventFiring(context)) {
            return rejectedBy(context);
        }
        return rejectedByWithoutPhase(context);
    }

    public String describe() {
        if (health.needsTarget()) {
            return "target health";
        }
        if (!anyOf.isEmpty()) {
            return "any_of x" + anyOf.size();
        }
        if (!allOf.isEmpty()) {
            return "all_of x" + allOf.size();
        }
        if (phase.onKill().orElse(false)) {
            return "on_kill";
        }
        if (phase.onMiss().orElse(false)) {
            return "on_miss";
        }
        if (phase.onSummonHit().orElse(false)) {
            return "on_summon_hit";
        }
        if (activation.reactsToProjectileHit()) {
            return "on_projectile_hit";
        }
        if (phase.onPlayerHit().orElse(false)) {
            return "on_player_hit";
        }
        if (phase.onSwing().orElse(false)) {
            return "on_swing";
        }
        if (lastSlot.orElse(false)) {
            return "last_slot";
        }
        if (slotBand.isPresent()) {
            return "slot_band " + slotBand.get().getSerializedName();
        }
        if (slots.isPresent()) {
            return "slots";
        }
        if (slotIndex.isPresent()) {
            return "slot " + slotIndex.get();
        }
        if (idleTicks.isPresent()) {
            return "idle " + idleTicks.get() + "t";
        }
        if (everyNthSwing.isPresent()) {
            return "every " + everyNthSwing.get() + " swings";
        }
        if (requiresAffix.orElse(false)) {
            return "requires_affix";
        }
        if (hitbox.isPresent()) {
            return "hitbox " + hitbox.get();
        }
        return "always";
    }

    private static Optional<String> named(Optional<String> value) {
        return value == null ? Optional.empty() : value.filter(text -> !text.isBlank());
    }

    private static boolean namesStyle(StyleEntry entry, String styleId) {
        return styleId.equals(entry.styleId())
                || styleId.equals(entry.rawStyleId())
                || entry.customStyleId().map(styleId::equals).orElse(false);
    }

    private boolean phaseApplies(EquipmentContext context) {
        if ((context.hurt() != null) != reactsToHurt() && !isEvent()) {
            return false;
        }
        if (phase.onKill().orElse(false) && !context.killed()) {
            return false;
        }
        if (phase.onMiss().orElse(false) && !CombatState.lastSwingMissed(context.player())) {
            return false;
        }
        if (phase.onSummonHit().orElse(false) && !context.summonHit()) {
            return false;
        }
        for (EquipmentTrigger required : allOf) {
            if (!required.phaseApplies(context)) {
                return false;
            }
        }
        for (EquipmentTrigger option : anyOf) {
            if (option.phaseApplies(context)) {
                return true;
            }
        }
        return anyOf.isEmpty();
    }

    public boolean worthReporting(EquipmentContext context) {
        if (!phase.onKill().orElse(false) && !phase.onMiss().orElse(false)
                && !phase.onSummonHit().orElse(false) && idleTicks.isEmpty()
                && requiresAffix.isEmpty() && everyNthSwing.isEmpty()) {
            return false;
        }
        return phaseApplies(context) && rejectedBy(context) != null;
    }

    private boolean anyOfMatches(EquipmentContext context, boolean checkPhase) {
        if (anyOf.isEmpty()) {
            return true;
        }
        for (EquipmentTrigger option : anyOf) {
            if (option.rejectedBy(context, checkPhase) == null) {
                return true;
            }
        }
        return false;
    }

    private String rejectedBy(EquipmentContext context) {
        return rejectedBy(context, true);
    }

    private String rejectedByWithoutPhase(EquipmentContext context) {
        return rejectedBy(context, false);
    }

    private String rejectedBy(EquipmentContext context, boolean checkPhase) {
        if (!anyOfMatches(context, checkPhase)) {
            return "any_of (no option matched)";
        }
        for (EquipmentTrigger required : allOf) {
            String rejection = required.rejectedBy(context, checkPhase);
            if (rejection != null) {
                return "all_of (" + rejection + ")";
            }
        }
        String healthRejection = health.rejects(context);
        if (healthRejection != null) {
            return healthRejection;
        }
        String stateRejection = context.fromSwing()
                ? phase.state().rejects(context.player(), context.target())
                : phase.state().rejectsLive(context.player(), context.target());
        if (stateRejection != null) {
            return stateRejection;
        }
        if (checkPhase && phase.gatesPass()) {
            String passRejection = phase.rejectsPass(context.hitPass());
            if (passRejection != null) {
                return passRejection;
            }
        }
        if (checkPhase) {
            String hurtRejection = hurt.rejects(context);
            if (hurtRejection != null) {
                return hurtRejection;
            }
            String activationRejection = activation.rejects(context);
            if (activationRejection != null) {
                return activationRejection;
            }
        }
        if (everyNthSwing.isPresent()) {
            int every = Math.max(1, everyNthSwing.get());
            if (context.swingCount() % every != 0) {
                return "every_nth_swing (swing " + context.swingCount()
                        + ", needs a multiple of " + every + ")";
            }
        }
        if (slotIndex.isPresent() && context.slotIndex() != slotIndex.get()) {
            return "slot_index " + slotIndex.get() + " (on "
                    + CombatDebug.slot(context.slotIndex()) + ")";
        }
        if (lastSlot.orElse(false) && context.slotIndex() != context.sequenceSize() - 1) {
            return "last_slot (on "
                    + CombatDebug.slot(context.slotIndex(), context.sequenceSize()) + ")";
        }
        if (slots.isPresent()
                && !slots.get().matches(context.slotIndex(), context.sequenceSize())) {
            return "slots (on "
                    + CombatDebug.slot(context.slotIndex(), context.sequenceSize()) + ")";
        }
        if (slotBand.isPresent()
                && !slotBand.get().matches(context.slotIndex(), context.sequenceSize())) {
            return "slot_band " + slotBand.get().getSerializedName()
                    + " (on "
                    + CombatDebug.slot(context.slotIndex(), context.sequenceSize()) + ")";
        }
        if (hitbox.isPresent()) {
            String shape = context.hitbox();
            if (shape == null || !shape.equalsIgnoreCase(hitbox.get())) {
                return "hitbox (swung " + shape + ")";
            }
        }
        StyleEntry entry = context.entry();
        if (styleId.isPresent()) {
            if (entry == null || !namesStyle(entry, styleId.get())) {
                return "style_id";
            }
        }
        if (affixId.isPresent()) {
            if (entry == null || !entry.affixIds().contains(affixId.get())) {
                return "affix_id";
            }
        }
        if (requiresAffix.orElse(false) && (entry == null || entry.affixIds().isEmpty())) {
            return "requires_affix (none on this attack)";
        }
        if (idleTicks.isPresent()
                && CombatState.idleAtSwing(context.player()) < idleTicks.get()) {
            return "idle_ticks (idle "
                    + CombatState.idleAtSwing(context.player()) + " of " + idleTicks.get() + ")";
        }
        if (checkPhase && phase.onMiss().orElse(false)
                && !CombatState.lastSwingMissed(context.player())) {
            return "on_miss (the swing connected)";
        }
        if (checkPhase && phase.onKill().orElse(false) && !context.killed()) {
            return "on_kill (target survived)";
        }
        if (checkPhase && phase.onSummonHit().orElse(false) != context.summonHit()) {
            return "on_summon_hit";
        }
        return null;
    }
}
