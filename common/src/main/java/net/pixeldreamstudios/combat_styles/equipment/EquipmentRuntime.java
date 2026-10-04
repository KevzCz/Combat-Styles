package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.affix.AffixDamageDealer;
import net.pixeldreamstudios.combat_styles.network.AlignComboPayload;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.affix.AffixMechanics;
import net.pixeldreamstudios.combat_styles.affix.EffectChance;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class EquipmentRuntime {

    private static final String PLAYER_SEPARATOR = "|";
    private static final Map<UUID, Integer> SWING_COUNTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> HELD_CHARGES = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> LATCHED = new ConcurrentHashMap<>();
    private static final Map<UUID, String> SPENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> SUBSTITUTED_SWING = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> COMBO_CYCLES = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> AWAITING_CYCLE = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> ARMED_PASSES = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> ANNOUNCED = new ConcurrentHashMap<>();

    private EquipmentRuntime() {
    }

    public static void forget(UUID playerId) {
        SWING_COUNTS.remove(playerId);
        HELD_CHARGES.remove(playerId);
        SPENDING.remove(playerId);
        CHANCES.keySet().removeIf(key -> key.startsWith(playerId + PLAYER_SEPARATOR));
        CycleChance.forget(playerId);
        SUBSTITUTED_SWING.remove(playerId);
        COMBO_CYCLES.remove(playerId);
        AWAITING_CYCLE.remove(playerId);
        ARMED_PASSES.remove(playerId);
        ROLLS.keySet().removeIf(key -> key.startsWith(playerId + PLAYER_SEPARATOR));
        ATTACK_PROCS.remove(playerId);
        LATCHED.remove(playerId);
        SubstitutionAnchor.forget(playerId);
        ANNOUNCED.remove(playerId);
        Cooldowns.forget(playerId);
        GroupWindows.forget(playerId);
        GroupDraw.forget(playerId);
        SubstitutionChoice.forget(playerId);
        HOLDS.keySet().removeIf(key -> key.startsWith(playerId + PLAYER_SEPARATOR));
    }

    public static boolean liveAtRest(Player player, EquipmentDefinition definition,
                                     String effectKey, TriggeredEffect entry) {
        return liveAtRest(player, definition, 0, effectKey, entry);
    }

    public static boolean liveAtRest(Player player, EquipmentDefinition definition, int copy,
                                     String effectKey, TriggeredEffect entry) {
        return liveAtRest(player, definition, copy, effectKey, entry,
                !EquipmentDefinition.substitutes(entry.effect()));
    }

    public static boolean liveAtRest(Player player, EquipmentDefinition definition,
                                     String effectKey, TriggeredEffect entry,
                                     boolean swingGates) {
        return liveAtRest(player, definition, 0, effectKey, entry, swingGates);
    }

    public static boolean liveAtRest(Player player, EquipmentDefinition definition, int copy,
                                     String effectKey, TriggeredEffect entry,
                                     boolean swingGates) {
        String owner = EffectKeys.ownerOf(definition.id(), copy);
        EffectGroup group = definition.groupOf(effectKey);
        String name = group == null ? null : groupNameOf(definition, group);
        boolean held = group != null && holdingEffect(player, definition, effectKey);
        if (group != null && !held) {
            if (name == null || !groupLive(player, definition, copy, group, name, swingGates)) {
                return false;
            }
            if (!GroupWindows.armed(player, owner, name)
                    && groupCooling(player, definition, copy, effectKey)) {
                return false;
            }
        }
        if (!effectLive(player, definition, copy, effectKey, entry, swingGates)) {
            return false;
        }
        if (!CycleChance.passes(player, definition, copy, effectKey, entry)) {
            return false;
        }
        return group == null || held || GroupDraw.holds(player, definition, copy, effectKey);
    }

    private static boolean holdingEffect(Player player, EquipmentDefinition definition,
                                         String effectKey) {
        if (!effectKey.equals(SubstitutionChoice.of(player, definition.id()))) {
            return false;
        }
        if (player.level().isClientSide()) {
            return ChargeMirror.holdProgress(definition.id()) > 0;
        }
        Hold hold = HOLDS.get(holdKey(player, definition.id()));
        return hold != null && effectKey.equals(hold.effectKey())
                && player.level().getGameTime() <= hold.expiresAt();
    }

    public static void openSwingWindows(Player player, EquipmentContext context) {
        if (player == null || player.level().isClientSide() || EquipmentRegistry.isEmpty()
                || EquipmentProviders.isEmpty()) {
            return;
        }
        for (WornEquipment worn : WornEquipment.of(player)) {
            EquipmentDefinition definition = worn.definition();
            if (substitutedThisAttack(player, definition.id())) {
                continue;
            }
            boolean opened = false;
            for (Map.Entry<String, EffectGroup> named : definition.groups().entrySet()) {
                opened |= GroupWindows.openOnSwing(player, worn.owner(), named.getKey(),
                        named.getValue(), context);
            }
            if (opened && player instanceof ServerPlayer served) {
                NetworkManager.syncCooldowns(served, definition.id());
            }
        }
    }

    private static boolean groupLive(Player player, EquipmentDefinition definition, int copy,
                                     EffectGroup group, String name, boolean swingGates) {
        if (group.trigger().arms(group.cooldown()) || definition.groupArmsOnSwing(group)) {
            return GroupWindows.armed(player, EffectKeys.ownerOf(definition.id(), copy), name);
        }
        boolean gates = swingGates && !definition.groupSubstitutes(group);
        if (StateEdges.tracked(definition, group)) {
            return StateEdges.held(player, definition, copy, name, group, swingCount(player),
                    gates);
        }
        return !group.gated()
                || group.trigger().readyAtRest(player, swingCount(player), definition, gates);
    }

    private static boolean effectLive(Player player, EquipmentDefinition definition, int copy,
                                      String effectKey, TriggeredEffect entry,
                                      boolean swingGates) {
        if (entry.trigger().arms(entry.cooldown()) || entry.armsOnSwing()) {
            return EffectWindows.armed(player, EffectKeys.ownerOf(definition.id(), copy),
                    effectKey);
        }
        if (entry.trigger().reactsToHurt()) {
            return false;
        }
        if (StateEdges.tracked(entry)) {
            return StateEdges.held(player, definition, copy, effectKey, entry,
                    swingCount(player), swingGates);
        }
        return entry.trigger().readyAtRest(player, swingCount(player), definition, swingGates);
    }

    public static int gatedSlotOf(Player player, EquipmentDefinition definition, int size) {
        if (definition == null || size <= 0) {
            return 0;
        }
        for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
            if (!EquipmentDefinition.substitutes(named.getValue().effect())) {
                continue;
            }
            EffectGroup group = definition.groupOf(named.getKey());
            int gated = firstAllowedSlot(named.getValue().trigger(), size);
            if (group != null && !group.armsOnSwing()) {
                gated = Math.max(gated, firstAllowedSlot(group.trigger(), size));
            }
            return gated;
        }
        return 0;
    }

    private static int firstAllowedSlot(EquipmentTrigger trigger, int size) {
        if (!trigger.gatesOnSlot()) {
            return 0;
        }
        for (int slot = 0; slot < size; slot++) {
            if (trigger.readyAtSlot(slot, size)) {
                return slot;
            }
        }
        return 0;
    }

    public static int swingCount(Player player) {
        if (player.level().isClientSide()) {
            return ChargeMirror.swingCount();
        }
        return SWING_COUNTS.getOrDefault(player.getUUID(), 0);
    }

    public static void advanceSwing(Player player) {
        SWING_COUNTS.merge(player.getUUID(), 1, (a, b) -> a + b);
    }

    public static EquipmentDefinition substituting(Player player) {
        return substituting(player, null);
    }

    public static List<EquipmentDefinition> allSubstituting(Player player, ItemStack weapon) {
        if (player == null || EquipmentRegistry.isEmpty() || EquipmentProviders.isEmpty()) {
            return List.of();
        }
        List<EquipmentDefinition> out = new ArrayList<>();
        for (ItemStack stack : WornItems.of(player)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
            if (definition == null || !hasSubstitution(definition)) {
                continue;
            }
            for (TriggeredEffect entry : definition.triggered()) {
                if (!EquipmentDefinition.substitutes(entry.effect())) {
                    continue;
                }
                StyleSubstitution substitution =
                        entry.effect().substitution().orElse(null);
                if (substitution != null && !substitution.isEmpty()
                        && weapon != null && !WeaponMatch.matches(weapon, substitution)) {
                    continue;
                }
                out.add(definition);
                break;
            }
        }
        return out;
    }

    public static void latchCharges(Player player) {
        latchCharges(player, SubstitutionAnchor.NO_COMBO);
    }

    public static void latchCharges(Player player, int comboCount) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        HELD_CHARGES.remove(player.getUUID());
        LATCHED.remove(player.getUUID());
        SPENDING.remove(player.getUUID());
        Set<String> held = new LinkedHashSet<>();
        Set<String> spent = new LinkedHashSet<>();
        for (ItemStack stack : WornItems.of(player)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
            if (definition == null || !hasSubstitution(definition)) {
                continue;
            }
            if (charged(player, definition) && swingCouldUse(player, definition)) {
                held.add(definition.id());
                if (usesCharge(definition)) {
                    armPass(player, definition.id());
                }
                resolveChoice(player, definition.id());
                if (comboCount != SubstitutionAnchor.NO_COMBO
                        && hasSubstitution(definition)) {
                    SubstitutionAnchor.engage(player, comboCount + 1);
                }
            } else {
                spent.add(definition.id());
            }
        }
        if (!held.isEmpty()) {
            HELD_CHARGES.put(player.getUUID(), held);
        }
        if (!spent.isEmpty()) {
            LATCHED.put(player.getUUID(), spent);
        }
        if (!held.isEmpty() || !spent.isEmpty()) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "charges latched — held " + held + ", on cooldown " + spent);
            }
        }
    }

    private static boolean swingCouldUse(Player player, EquipmentDefinition definition) {
        if (holding(player, definition)) {
            return true;
        }
        WeaponAttributes.Attack swung = CombatState.swungAttack(player);
        if (swung == null) {
            return true;
        }
        if (!SubstitutionMatch.provides(definition, swung)) {
            return false;
        }
        return !ClientSwing.declaredElsewhere(player, swung);
    }

    public static void anchorSubstitution(Player player, int comboCount) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        EquipmentDefinition definition = substituting(player);
        if (definition == null || !hasSubstitution(definition)) {
            return;
        }
        SubstitutionAnchor.engage(player, comboCount);
    }

    private static EquipmentDefinition definitionOf(Player player, String id) {
        if (id == null) {
            return null;
        }
        if (player != null) {
            for (ItemStack stack : WornItems.of(player)) {
                EquipmentDefinition worn = EquipmentProviders.definitionFor(stack);
                if (worn != null && id.equals(worn.id())) {
                    return worn;
                }
            }
        }
        return EquipmentRegistry.get(id);
    }

    public static boolean holdDriven(Player player, String id) {
        EquipmentDefinition definition = definitionOf(player, id);
        if (definition == null || holdingSubstitution(player, definition) == null) {
            return false;
        }
        return holding(player, definition);
    }

    public static void dropUnusedCharges(Player player, Set<String> spent) {
        if (player == null) {
            return;
        }
        SUBSTITUTED_SWING.put(player.getUUID(), Set.copyOf(spent));
        Set<String> held = HELD_CHARGES.remove(player.getUUID());
        if (held == null || held.isEmpty()) {
            CombatDebug.note(player, "no charge was held this swing — nothing to spend or refund");
            return;
        }
        long now = player.level().getGameTime();
        for (String id : held) {
            if (spent.contains(id)) {
                if (advanceHold(player, id, now)) {
                    CombatDebug.fired(player, id, "charge held — sequence still running");
                    continue;
                }
                CombatDebug.fired(player, id, "charge spent — its attack swung");
                SPENDING.put(player.getUUID(), id);
                startRecharge(player, id, now);
                continue;
            }
            CombatDebug.skipped(player, id, "charge refunded — its attack did not swing");
            Cooldowns.clear(player, id, Cooldowns.Track.CHARGE);
        }
    }

    private record Hold(int played, long expiresAt, int combo, String effectKey) {
    }

    private static final Map<String, Hold> HOLDS = new ConcurrentHashMap<>();

    private static String holdKey(Player player, String id) {
        return player.getUUID() + PLAYER_SEPARATOR + id;
    }

    private static StyleSubstitution holdingSubstitution(Player player, EquipmentDefinition definition) {
        return holdingSubstitution(player, definition, null);
    }

    private static StyleSubstitution holdingSubstitution(Player player, EquipmentDefinition definition,
                                                         String effectKey) {
        TriggeredEffect chosen = effectKey == null
                ? null
                : definition.effects().get(effectKey);
        EquipmentEffect effect = chosen == null
                ? definition.substitutingEffect(player)
                : chosen.effect();
        StyleSubstitution substitution = effect.substitution().orElse(null);
        return substitution != null && substitution.holdsCharge() ? substitution : null;
    }

    private static int declaredIndexOf(Player player, StyleSubstitution substitution) {
        String animation = ClientSwing.animationOf(player);
        if (animation == null) {
            return -1;
        }
        List<String> styles = substitution.styles();
        for (int index = 0; index < styles.size(); index++) {
            StyleEntry entry = SequenceService.entryFor(styles.get(index));
            StylePage page = entry == null ? null : StyleRegistry.get(entry.styleId());
            if (page != null && animation.equals(page.attack().animation())) {
                return index;
            }
        }
        return -1;
    }

    private static boolean advanceHold(Player player, String id, long now) {
        EquipmentDefinition definition = definitionOf(player, id);
        if (definition == null) {
            return false;
        }
        String key = holdKey(player, id);
        Hold hold = HOLDS.get(key);
        String effectKey = hold != null ? hold.effectKey() : SubstitutionChoice.of(player, id);
        StyleSubstitution substitution = holdingSubstitution(player, definition, effectKey);
        if (substitution == null) {
            return false;
        }
        int combo = swingCount(player);
        if (hold != null && !substitution.decays() && combo < hold.combo()) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, id + " hold dropped — combo rewound "
                        + hold.combo() + " -> " + combo);
            }
            HOLDS.remove(key);
            hold = null;
        }
        boolean running = hold != null
                && (!substitution.decays() || now <= hold.expiresAt());
        int declared = declaredIndexOf(player, substitution);
        int played = declared >= 0
                ? declared + 1
                : (running ? hold.played() + 1 : 1);
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, id + " hold advance — client played "
                    + ClientSwing.animationOf(player) + ", index " + declared
                    + ", progress " + played + "/" + substitution.styles().size());
        }
        int size = substitution.styles().size();
        if (played >= size) {
            HOLDS.remove(key);
            return false;
        }
        HOLDS.put(key, new Hold(played, substitution.decays()
                ? now + substitution.decayTicks()
                : Long.MAX_VALUE, combo, effectKey));
        return true;
    }

    private static boolean holding(Player player, EquipmentDefinition definition) {
        Hold hold = HOLDS.get(holdKey(player, definition.id()));
        return hold != null && player.level().getGameTime() <= hold.expiresAt();
    }

    public static int holdProgress(Player player, EquipmentDefinition definition) {
        if (player == null || definition == null) {
            return 0;
        }
        if (player.level().isClientSide()) {
            return ChargeMirror.holdProgress(definition.id());
        }
        Hold hold = HOLDS.get(holdKey(player, definition.id()));
        if (hold == null) {
            return 0;
        }
        if (player.level().getGameTime() > hold.expiresAt()) {
            HOLDS.remove(holdKey(player, definition.id()));
            return 0;
        }
        return hold.played();
    }

    private static void startRecharge(Player player, String id, long now) {
        HOLDS.remove(holdKey(player, id));
        disarmPass(player, id);
        claimGroupUse(player, id);
        awaitCycle(player, id);
        startGroupCooldown(player, id);
        GroupDraw.endPass(player, id);
        SubstitutionChoice.record(player, id, null);
        SubstitutionAnchor.release(player);
        EquipmentDefinition definition = definitionOf(player, id);
        if (definition == null) {
            return;
        }
        StyleSubstitution finished =
                definition.substitutingEffect(player).substitution().orElse(null);
        if (finished != null && finished.restartCombo()
                && player instanceof ServerPlayer served) {
            ComboHold.align(player, 0);
            NetworkManager.sendAlignCombo(served, new AlignComboPayload(0));
        }
        for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
            TriggeredEffect entry = named.getValue();
            if (!EquipmentDefinition.substitutes(entry.effect()) || entry.cooldownTicks() <= 0) {
                continue;
            }
            Cooldowns.start(player, id, named.getKey(), 0,
                    Cooldowns.Track.CHARGE, entry.cooldownTicks());
        }
    }

    public static void releaseCharges(Player player) {
        if (player != null) {
            HELD_CHARGES.remove(player.getUUID());
            LATCHED.remove(player.getUUID());
        }
    }

    private static void chose(Player player, String itemId, String effectKey) {
        if (player.level().isClientSide()) {
            return;
        }
        SubstitutionChoice.record(player, itemId, effectKey);
    }

    public static EquipmentDefinition substituting(Player player, ItemStack weapon) {
        if (player == null || EquipmentRegistry.isEmpty() || EquipmentProviders.isEmpty()) {
            return null;
        }
        for (ItemStack stack : WornItems.of(player)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
            if (definition == null) {
                continue;
            }
            if (!charged(player, definition)) {
                continue;
            }
            if (usesCharge(definition) && !passArmedFor(player, definition)) {
                if (CombatDebug.enabled(player)) {
                    CombatDebug.note(player, "substituting — " + definition.id()
                            + " not armed (" + (player.level().isClientSide()
                                    ? "client" : "server") + ")");
                }
                continue;
            }
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                TriggeredEffect entry = named.getValue();
                if (!EquipmentDefinition.substitutes(entry.effect())) {
                    continue;
                }
                if (!liveAtRest(player, definition, named.getKey(), entry)) {
                    if (CombatDebug.enabled(player)) {
                        CombatDebug.note(player, "substituting — " + definition.id()
                                + " not live at rest");
                    }
                    continue;
                }
                if (!GroupDraw.holds(player, definition, named.getKey())) {
                    if (CombatDebug.enabled(player)) {
                        CombatDebug.note(player, "substituting — " + definition.id()
                                + " group did not draw " + named.getKey());
                    }
                    continue;
                }
                StyleSubstitution substitution =
                        entry.effect().substitution().orElse(null);
                if (substitution != null && !substitution.isEmpty()) {
                    if (weapon == null || WeaponMatch.matches(weapon, substitution)) {
                        chose(player, definition.id(), named.getKey());
                        return definition;
                    }
                    if (CombatDebug.enabled(player)) {
                        CombatDebug.note(player, "substituting — " + definition.id()
                                + " weapon mismatch on " + weapon.getItem());
                    }
                    continue;
                }
                if (weapon == null || !definition.broadlyMatched()
                        || definition.matches(weapon)) {
                    chose(player, definition.id(), named.getKey());
                    return definition;
                }
            }
        }
        return null;
    }

    public record Match(EquipmentDefinition definition, TriggeredEffect entry, String key, int copy) {

        public Match(EquipmentDefinition definition, TriggeredEffect entry, String key) {
            this(definition, entry, key, 0);
        }

        public String owner() {
            return EffectKeys.ownerOf(definition.id(), copy);
        }

        public EquipmentEffect effect() {
            return entry.effect();
        }

        public EquipmentTrigger trigger() {
            return entry.trigger();
        }

        public boolean hears(Predicate<EquipmentTrigger> event) {
            if (entry.trigger().inTree(event)) {
                return true;
            }
            EffectGroup group = definition.groupOf(key);
            return group != null && group.trigger().inTree(event);
        }

        public String id() {
            return definition.id();
        }

        public String itemId() {
            return definition.id();
        }

        public Optional<StyleRarity> rarity() {
            return definition.rarity();
        }
    }

    public static List<Match> matching(Player player, EquipmentContext context) {
        if (player == null || EquipmentRegistry.isEmpty() || EquipmentProviders.isEmpty()) {
            return List.of();
        }
        List<Match> matched = new ArrayList<>();
        for (WornEquipment worn : WornEquipment.of(player)) {
            EquipmentDefinition definition = worn.definition();
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                TriggeredEffect entry = named.getValue();
                if (entry.trigger().matches(context, entry.cooldown())
                        || summonPayloadMatches(context, entry)) {
                    matched.add(new Match(definition, entry, named.getKey(), worn.copy()));
                }
            }
        }
        return matched;
    }

    private static boolean summonPayloadMatches(EquipmentContext context, TriggeredEffect entry) {
        return context != null && context.summonHit()
                && entry.effect().events().has(EventKind.ON_SUMMON_HIT)
                && !entry.trigger().onSummonHit().orElse(false)
                && entry.trigger().matches(context.withoutSummonHit(), entry.cooldown());
    }

    public static List<Match> activeFor(Player player, EquipmentContext context) {
        return activeFor(player, context, false);
    }

    public static List<Match> activeFor(Player player, EquipmentContext context,
                                        boolean missPhase) {
        return activeFor(player, context, missPhase, true);
    }

    public static List<Match> activeFor(Player player, EquipmentContext context,
                                        boolean missPhase, boolean onHit) {
        return activeFor(player, context, missPhase, onHit, false);
    }

    private static final Map<UUID, Set<String>> ATTACK_PROCS = new ConcurrentHashMap<>();

    public static boolean attackOpen(Player player) {
        return CurrentAttack.of(player) != null;
    }

    public static boolean procsPerAttack(Match match) {
        EquipmentEffect effect = match.effect();
        if (effect.onSwing().isEmpty() || effect.onHit().isEmpty()
                || !swingSide(match.trigger())) {
            return false;
        }
        EffectGroup group = match.definition().groupOf(match.key());
        return group == null || swingSide(group.trigger());
    }

    private static boolean swingSide(EquipmentTrigger trigger) {
        return !trigger.isEvent() && !hitSide(trigger)
                && !trigger.inTree(node -> node.phase().gatesPass());
    }

    public static void beginAttackProcs(Player player) {
        ATTACK_PROCS.put(player.getUUID(), ConcurrentHashMap.newKeySet());
    }

    public static void recordAttackProc(Player player, Match match) {
        ATTACK_PROCS.computeIfAbsent(player.getUUID(), key -> ConcurrentHashMap.newKeySet())
                .add(EffectKeys.of(match.owner(), match.key()));
    }

    public static boolean procdThisAttack(Player player, Match match) {
        Set<String> procs = ATTACK_PROCS.get(player.getUUID());
        return procs != null && procs.contains(EffectKeys.of(match.owner(), match.key()));
    }

    public static List<Match> perAttackFor(Player player, EquipmentContext context) {
        List<Match> carried = new ArrayList<>();
        for (Match match : eligible(player, context, false, true, false)) {
            if (procsPerAttack(match)) {
                carried.add(match);
            }
        }
        return carried;
    }

    public static List<Match> activeFor(Player player, EquipmentContext context,
                                        boolean missPhase, boolean onHit, boolean perAttack) {
        List<Match> eligible = new ArrayList<>(eligible(player, context, missPhase, onHit, true));
        if (perAttack) {
            eligible.removeIf(EquipmentRuntime::procsPerAttack);
        }
        List<Match> ready = new ArrayList<>(eligible.size());
        for (Match match : pickGroups(player, context, eligible)) {
            if (match.entry().cooldown().windowed() || !consumeCooldown(player, match)) {
                ready.add(match);
            }
        }
        return ready;
    }

    public static List<Match> previewFor(Player player, EquipmentContext context,
                                         boolean onHit) {
        List<Match> eligible = eligible(player, context, false, onHit, false);
        List<Match> ready = new ArrayList<>(eligible.size());
        for (Match match : eligible) {
            boolean grouped = match.definition().groupOf(match.key()) != null;
            if (!grouped && !chancePasses(player, context, match)) {
                continue;
            }
            if (groupWouldPass(player, context, match, eligible)
                    && effectWouldFire(player, context, match)) {
                ready.add(match);
            }
        }
        return ready;
    }

    private static List<Match> eligible(Player player, EquipmentContext context,
                                        boolean missPhase, boolean onHit, boolean report) {
        List<Match> matched = matching(player, context);
        if (report && CombatDebug.enabled(player)) {
            reportRejections(player, context, matched);
        }
        if (matched.isEmpty()) {
            return matched;
        }
        List<Match> eligible = new ArrayList<>(matched.size());
        for (Match match : matched) {
            if (missPhase
                    ? !(match.hears(trigger -> trigger.onMiss().orElse(false))
                            || match.effect().events().has(EventKind.ON_MISS))
                    : match.trigger().onMiss().orElse(false)) {
                continue;
            }
            if (!actsOn(match, context, onHit, missPhase)) {
                continue;
            }
            if (!SubstitutionMatch.swung(player, match.effect())) {
                continue;
            }
            if (!conditionCouldPass(player, match, onHit)) {
                continue;
            }
            eligible.add(match);
        }
        return eligible;
    }

    private static boolean groupWouldPass(Player player, EquipmentContext context, Match match,
                                          List<Match> eligible) {
        EffectGroup group = match.definition().groupOf(match.key());
        if (group == null) {
            return true;
        }
        String name = groupNameOf(match);
        if (name == null || !GroupWindows.wouldClaim(player, match.owner(), name, group, context)) {
            return false;
        }
        if (sharesDraw(match.definition(), group)) {
            return !(fromAttack(context) && substitutedThisAttack(player, match.id()))
                    && GroupDraw.holds(player, match.definition(), match.copy(), match.key());
        }
        List<Match> contenders = new ArrayList<>();
        for (Match other : eligible) {
            if (other.definition() == match.definition() && other.copy() == match.copy()
                    && name.equals(groupNameOf(other))) {
                contenders.add(other);
            }
        }
        GroupRoll roll = rollGroup(player, context, group, match.owner(), name, contenders);
        return (roll.passed() || GroupWindows.armed(player, match.owner(), name))
                && roll.picked().contains(match.key());
    }

    private static boolean effectWouldFire(Player player, EquipmentContext context,
                                           Match match) {
        TriggeredEffect entry = match.entry();
        EffectCooldown window = entry.cooldown();
        boolean arms = entry.trigger().arms(window);
        if (window.windowed()) {
            if (arms && entry.trigger().eventFiring(context)) {
                return false;
            }
            if (arms || entry.armsOnSwing()) {
                return EffectWindows.armed(player, match);
            }
            return EffectWindows.armed(player, match)
                    || !Gates.cooling(player, GateScope.EFFECT, match.owner(), match.key(), window);
        }
        return !Cooldowns.active(player, match.id(), match.key(), match.copy(), Cooldowns.Track.PROC);
    }

    public static boolean rewriteAllowed(Player player, Match match) {
        EquipmentDefinition definition = match.definition();
        if (!CycleChance.passes(player, definition, match.copy(), match.key(), match.entry())) {
            return false;
        }
        EffectGroup group = definition.groupOf(match.key());
        if (group != null) {
            String name = groupNameOf(definition, group);
            if (name == null) {
                return false;
            }
            boolean open = GroupWindows.armed(player, match.owner(), name);
            if ((group.trigger().arms(group.cooldown()) || definition.groupArmsOnSwing(group))
                    && !open) {
                return false;
            }
            if (!open && Gates.cooling(player, GateScope.GROUP, match.owner(), name,
                    group.cooldown())) {
                return false;
            }
            if (StateEdges.tracked(definition, group) && !StateEdges.held(player, definition,
                    match.copy(), name, group, swingCount(player), false)) {
                return false;
            }
            if (!GroupDraw.holds(player, definition, match.copy(), match.key())) {
                return false;
            }
        }
        TriggeredEffect entry = match.entry();
        EffectCooldown window = entry.cooldown();
        if (entry.trigger().arms(window) || entry.armsOnSwing()) {
            return EffectWindows.armed(player, match);
        }
        if (window.windowed()) {
            return EffectWindows.armed(player, match)
                    || !Gates.cooling(player, GateScope.EFFECT, match.owner(), match.key(), window);
        }
        if (StateEdges.tracked(entry) && !StateEdges.held(player, definition,
                match.copy(), match.key(), entry, swingCount(player), false)) {
            return false;
        }
        return !Cooldowns.active(player, match.id(), match.key(), match.copy(), Cooldowns.Track.PROC);
    }

    public static List<Match> pickGroups(Player player, EquipmentContext context,
                                         List<Match> eligible) {
        Map<String, List<Match>> grouped = null;
        List<Match> loose = new ArrayList<>(eligible.size());
        for (Match match : eligible) {
            String group = groupNameOf(match);
            if (group == null) {
                if (chancePasses(player, context, match)) {
                    loose.add(match);
                }
                continue;
            }
            if (grouped == null) {
                grouped = new LinkedHashMap<>();
            }
            grouped.computeIfAbsent(EffectKeys.of(match.owner(), group),
                    ignored -> new ArrayList<>()).add(match);
        }
        if (grouped == null) {
            return loose;
        }
        List<Match> kept = new ArrayList<>(loose);
        for (List<Match> contenders : grouped.values()) {
            kept.addAll(drawFrom(player, context, contenders));
        }
        List<Match> ordered = new ArrayList<>(kept.size());
        for (Match match : eligible) {
            if (kept.contains(match)) {
                ordered.add(match);
            }
        }
        return ordered;
    }

    private static String groupNameOf(Match match) {
        for (Map.Entry<String, EffectGroup> named : match.definition().groups().entrySet()) {
            if (named.getValue().holds(match.key())) {
                return named.getKey();
            }
        }
        return null;
    }

    private static List<Match> drawFrom(Player player, EquipmentContext context,
                                        List<Match> contenders) {
        Match first = contenders.getFirst();
        EffectGroup group = first.definition().groupOf(first.key());
        String name = groupNameOf(first);
        if (group == null || name == null) {
            return contenders;
        }
        if (sharesDraw(first.definition(), group)) {
            if (fromAttack(context) && substitutedThisAttack(player, first.id())) {
                CombatDebug.skipped(player, first.id(), "group " + name
                        + " already acted through its substitution this attack");
                return List.of();
            }
            List<Match> held = new ArrayList<>(contenders.size());
            for (Match match : contenders) {
                if (GroupDraw.holds(player, match.definition(), match.copy(), match.key())) {
                    held.add(match);
                }
            }
            if (held.isEmpty()
                    || !GroupWindows.claim(player, first.owner(), name, group, context, true)) {
                return List.of();
            }
            GroupDraw.endPass(player, first.owner(), name);
            if (GroupDraw.perCycle(first.definition(), group)) {
                awaitCycle(player, first.id());
            }
            return held;
        }
        GroupRoll roll = rollGroup(player, context, group, first.owner(), name, contenders);
        boolean arming = group.trigger().arms(group.cooldown()) && context != null
                && group.trigger().eventFiring(context);
        if (roll.picked().isEmpty() && !arming) {
            CombatDebug.skipped(player, first.id(), "group " + name + " picked nothing");
            return List.of();
        }
        if (!GroupWindows.claim(player, first.owner(), name, group, context, roll.passed())
                && !(arming && roll.passed() && firesOnEvent(context, contenders)
                        && GroupWindows.claimUse(player, first.owner(), name, group))) {
            return List.of();
        }
        List<Match> drawn = new ArrayList<>(contenders.size());
        for (Match match : contenders) {
            if (roll.picked().contains(match.key())) {
                drawn.add(match);
            }
        }
        return drawn;
    }

    public static int comboCycle(Player player) {
        return COMBO_CYCLES.getOrDefault(player.getUUID(), 0);
    }

    public static void endCycleOnLastSlot(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        boolean repeating = ComboControl.consumeRepeat(player);
        if (repeating || BurstSequence.active(player)) {
            return;
        }
        CurrentAttack attack = CurrentAttack.of(player);
        if (attack != null && attack.size() > 0 && attack.slot() == attack.size() - 1) {
            endCycle(player, "last slot swung");
            if (wearsSubstitution(player)) {
                restartCombo(player);
            }
            if (player instanceof ServerPlayer served) {
                NetworkManager.syncCooldowns(served, null);
            }
        }
    }

    private static boolean wearsSubstitution(Player player) {
        for (ItemStack stack : WornItems.of(player)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
            if (definition != null && hasSubstitution(definition)) {
                return true;
            }
        }
        return false;
    }

    private static void restartCombo(Player player) {
        SubstitutionAnchor.release(player);
        ComboHold.align(player, 0);
        if (player instanceof ServerPlayer served) {
            NetworkManager.sendAlignCombo(served, new AlignComboPayload(0));
        }
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "cycle — combo restarted at the first slot");
        }
    }

    private static void endCycle(Player player, String why) {
        COMBO_CYCLES.merge(player.getUUID(), 1, Integer::sum);
        Set<String> waiting = AWAITING_CYCLE.remove(player.getUUID());
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "cycle — " + why + ", next cycle may pick"
                    + (waiting == null || waiting.isEmpty() ? "" : " (" + waiting + ")"));
        }
    }

    private static void awaitCycle(Player player, String itemId) {
        AWAITING_CYCLE.computeIfAbsent(player.getUUID(), key -> ConcurrentHashMap.newKeySet())
                .add(itemId);
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "cycle — " + itemId + " waits for the combo to restart");
        }
    }

    public static boolean awaitingCycle(Player player, String itemId) {
        Set<String> waiting = AWAITING_CYCLE.get(player.getUUID());
        if (waiting == null || !waiting.contains(itemId)) {
            return false;
        }
        if (CurrentAttack.of(player) == null) {
            endCycle(player, "combo reset");
            return false;
        }
        return true;
    }

    private static boolean fromAttack(EquipmentContext context) {
        return context == null || context.fromSwing();
    }

    private static boolean substitutedThisAttack(Player player, String itemId) {
        return SUBSTITUTED_SWING.getOrDefault(player.getUUID(), Set.of()).contains(itemId);
    }

    private static boolean firesOnEvent(EquipmentContext context, List<Match> contenders) {
        EventKind activation = EventKind.activationOf(context);
        List<EventKind> kinds = activation != null
                ? List.of(activation)
                : EventKind.meleeOf(context, context != null && context.hitPass(), false);
        for (Match match : contenders) {
            if (!match.effect().events().getAll(kinds).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean sharesDraw(EquipmentDefinition definition, EffectGroup group) {
        return group.armsOnSwing() || definition.groupSubstitutes(group);
    }

    private record ChanceRoll(long tick, int target, boolean hitPass, boolean passed) {
    }

    private static final Map<String, ChanceRoll> CHANCES = new ConcurrentHashMap<>();

    private static boolean chancePasses(Player player, EquipmentContext context, Match match) {
        if (!match.trigger().rollsChance()) {
            return true;
        }
        if (CycleChance.applies(match.definition(), match.key(), match.entry())) {
            return CycleChance.passes(player, match.definition(), match.copy(), match.key(), match.entry());
        }
        String key = player.getUUID() + PLAYER_SEPARATOR + EffectKeys.of(match.owner(), match.key());
        long tick = player.level().getGameTime();
        int target = context == null || context.target() == null
                ? NO_TARGET
                : context.target().getId();
        boolean hitPass = context != null && context.hitPass();
        ChanceRoll memo = CHANCES.get(key);
        if (memo != null && memo.tick() == tick && memo.target() == target
                && memo.hitPass() == hitPass) {
            return memo.passed();
        }
        boolean passed = EffectChance.rolls(player, match.trigger().chance().orElse(1.0F));
        CHANCES.put(key, new ChanceRoll(tick, target, hitPass, passed));
        if (!passed) {
            CombatDebug.skipped(player, match.id(), "chance roll failed for " + match.key());
        }
        return passed;
    }

    private record GroupRoll(long tick, int target, boolean hitPass, boolean passed,
                             Set<String> picked) {
    }

    private static final int NO_TARGET = -1;
    private static final Map<String, GroupRoll> ROLLS = new ConcurrentHashMap<>();

    private static GroupRoll rollGroup(Player player, EquipmentContext context, EffectGroup group,
                                       String itemId, String name, List<Match> contenders) {
        String key = player.getUUID() + PLAYER_SEPARATOR + EffectKeys.of(itemId, name);
        long tick = player.level().getGameTime();
        int target = context == null || context.target() == null
                ? NO_TARGET
                : context.target().getId();
        boolean hitPass = context != null && context.hitPass();
        GroupRoll memo = ROLLS.get(key);
        if (memo != null && memo.tick() == tick && memo.target() == target
                && memo.hitPass() == hitPass) {
            return memo;
        }
        boolean passed = EffectChance.rolls(player, group.chance());
        List<Match> alive = new ArrayList<>(contenders.size());
        for (Match match : contenders) {
            EffectGroup.Entry entry = group.entryFor(match.key());
            if (entry == null || entry.share() > 0.0F) {
                alive.add(match);
            }
        }
        int wanted = EffectChance.rolls(player, group.noneChance())
                ? 0
                : group.picksFor(alive.size(), player.getRandom().nextFloat());
        Set<String> picked = new LinkedHashSet<>();
        if (wanted >= alive.size()) {
            alive.forEach(match -> picked.add(match.key()));
        } else {
            List<Match> pool = new ArrayList<>(alive);
            for (int i = 0; i < wanted && !pool.isEmpty(); i++) {
                picked.add(takeWeighted(player, pool, group).key());
            }
        }
        GroupRoll rolled = new GroupRoll(tick, target, hitPass, passed, picked);
        ROLLS.put(key, rolled);
        return rolled;
    }

    private static Match takeWeighted(Player player, List<Match> pool, EffectGroup group) {
        float total = 0.0F;
        for (Match match : pool) {
            total += shareOf(group, match);
        }
        if (total <= 0.0F) {
            return pool.removeFirst();
        }
        float roll = player.getRandom().nextFloat() * total;
        for (int i = 0; i < pool.size(); i++) {
            roll -= shareOf(group, pool.get(i));
            if (roll < 0.0F) {
                return pool.remove(i);
            }
        }
        return pool.removeLast();
    }

    private static float shareOf(EffectGroup group, Match match) {
        EffectGroup.Entry entry = group.entryFor(match.key());
        return entry == null ? EffectGroup.ALWAYS : entry.share();
    }

    private static void reportRejections(Player player, EquipmentContext context,
                                         List<Match> matched) {
        Set<String> passed = new HashSet<>();
        for (Match match : matched) {
            passed.add(match.id() + "." + match.key());
        }
        for (ItemStack stack : WornItems.of(player)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
            if (definition == null) {
                continue;
            }
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                String id = definition.id() + "." + named.getKey();
                if (passed.contains(id)) {
                    continue;
                }
                CombatDebug.skipped(player, id, named.getValue().trigger().explain(context));
            }
        }
    }

    private static boolean conditionCouldPass(Player player, Match match, boolean onHit) {
        AffixEffects.OnHit effects = onHit
                ? match.effect().onHit().orElse(null)
                : match.effect().onSwing().orElse(null);
        if (effects == null) {
            return true;
        }
        AffixEffects.Condition condition = effects.condition();
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        return AffixMechanics.selfConditionMet(condition, player);
    }

    private static boolean actsOn(Match match, EquipmentContext context, boolean onHit,
                                  boolean missPhase) {
        EquipmentEffect effect = match.effect();
        TriggeredEffect entry = match.entry();
        boolean eventHere = !effect.events()
                .getAll(EventKind.meleeOf(context, onHit, missPhase)).isEmpty();
        boolean selfHere = !entry.selfEffects().isEmpty() && (entry.selfEffectsInPasses()
                ? entry.selfEffectsOnHit() == onHit
                : eventHere);
        boolean armingHere = armsWithoutPayload(match) && armsInHitPass(match) == onHit;
        if (onHit) {
            return effect.onHit().isPresent() || !effect.grantAffixes().isEmpty()
                    || effect.summonProfile().filter(SummonProfile::replacesSummons).isPresent()
                    || effect.rewritesAttack()
                    || effect.summonDive().isPresent() || effect.echo().isPresent()
                    || effect.substitution().filter(StyleSubstitution::inserts).isPresent()
                    || selfHere || armingHere || eventHere;
        }
        return effect.onSwing().isPresent() || effect.resetCombo().orElse(0) > 0
                || selfHere || armingHere || eventHere;
    }

    private static boolean armsWithoutPayload(Match match) {
        EquipmentEffect effect = match.effect();
        if (!effect.hasAlwaysOnParts() && !effect.rewritesAttack()) {
            return false;
        }
        if (match.entry().cooldown().windowed()) {
            return true;
        }
        EffectGroup group = match.definition().groupOf(match.key());
        return group != null && group.windowed();
    }

    private static boolean armsInHitPass(Match match) {
        if (hitSide(match.trigger())) {
            return true;
        }
        EffectGroup group = match.definition().groupOf(match.key());
        return group != null && hitSide(group.trigger());
    }

    private static boolean hitSide(EquipmentTrigger trigger) {
        return trigger.requiresTarget() || trigger.inTree(node -> node.onKill().orElse(false));
    }

    private static boolean usesCharge(EquipmentDefinition definition) {
        for (TriggeredEffect entry : definition.triggered()) {
            if (EquipmentDefinition.substitutes(entry.effect()) && entry.cooldownTicks() > 0) {
                return true;
            }
        }
        for (EffectGroup group : definition.groups().values()) {
            if (!group.cooldown().isEmpty() && definition.groupSubstitutes(group)) {
                return true;
            }
        }
        return false;
    }

    private static boolean passArmedFor(Player player, EquipmentDefinition definition) {
        if (player.level().isClientSide()) {
            return ChargeMirror.passArmed(definition.id());
        }
        return passArmed(player, definition.id()) || holding(player, definition);
    }

    public static boolean charged(Player player, EquipmentDefinition definition) {
        if (!hasSubstitution(definition) || !usesCharge(definition)) {
            return true;
        }
        if (player.level().isClientSide()) {
            return ChargeMirror.charged(definition.id());
        }
        Set<String> held = HELD_CHARGES.get(player.getUUID());
        if (held != null && held.contains(definition.id())) {
            return true;
        }
        if (holding(player, definition)) {
            return true;
        }
        if (definition.id().equals(SPENDING.get(player.getUUID()))) {
            return true;
        }
        Set<String> latched = LATCHED.get(player.getUUID());
        if (latched != null && latched.contains(definition.id())) {
            return false;
        }
        return offCooldown(player, definition);
    }

    private static boolean hasSubstitution(EquipmentDefinition definition) {
        for (TriggeredEffect entry : definition.triggered()) {
            if (EquipmentDefinition.substitutes(entry.effect())) {
                return true;
            }
        }
        return false;
    }

    public static void announceReady(Player player, Set<String> ready) {
        if (player == null) {
            return;
        }
        if (ready.isEmpty()) {
            ANNOUNCED.remove(player.getUUID());
        } else {
            ANNOUNCED.put(player.getUUID(), Set.copyOf(ready));
        }
    }

    public static Set<String> readySubstitutions(Player player) {
        Set<String> ready = new LinkedHashSet<>();
        for (ItemStack stack : WornItems.of(player)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
            if (definition != null && hasSubstitution(definition)
                    && offCooldown(player, definition)) {
                ready.add(definition.id());
            }
        }
        return ready;
    }

    private static boolean offCooldown(Player player, EquipmentDefinition definition) {
        for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
            if (!EquipmentDefinition.substitutes(named.getValue().effect())) {
                continue;
            }
            if (Cooldowns.active(player, definition.id(), named.getKey(), 0,
                    Cooldowns.Track.CHARGE)) {
                continue;
            }
            if (groupCooling(player, definition, named.getKey())) {
                continue;
            }
            return true;
        }
        return false;
    }

    public static float rechargeProgress(Player player) {
        return rechargeProgress(player, null);
    }

    public static float rechargeProgress(Player player, String ownerId) {
        if (player == null || EquipmentRegistry.isEmpty() || EquipmentProviders.isEmpty()) {
            return 1.0F;
        }
        float lowest = 1.0F;
        for (ItemStack stack : WornItems.of(player)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
            if (definition == null || !hasSubstitution(definition)) {
                continue;
            }
            if (ownerId != null && !ownerId.equals(definition.id())) {
                continue;
            }
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                TriggeredEffect entry = named.getValue();
                int span = entry.cooldownTicks();
                if (!EquipmentDefinition.substitutes(entry.effect()) || span <= 0) {
                    continue;
                }
                int left = Cooldowns.remaining(player, definition.id(), named.getKey(), 0,
                        Cooldowns.Track.CHARGE);
                if (left <= 0) {
                    continue;
                }
                lowest = Math.min(lowest, 1.0F - left / (float) span);
            }
            for (Map.Entry<String, EffectGroup> named : definition.groups().entrySet()) {
                int span = named.getValue().cooldown().ticks();
                if (span <= 0) {
                    continue;
                }
                int left = Gates.remaining(player, GateScope.GROUP, definition.id(),
                        named.getKey());
                if (left > 0) {
                    lowest = Math.min(lowest, 1.0F - left / (float) span);
                }
            }
        }
        return Mth.clamp(lowest, 0.0F, 1.0F);
    }

    public static void armPass(Player player, String id) {
        if (player == null || id == null) {
            return;
        }
        ARMED_PASSES.computeIfAbsent(player.getUUID(),
                key -> ConcurrentHashMap.newKeySet()).add(id);
    }

    public static void disarmPass(Player player, String id) {
        Set<String> armed = player == null ? null : ARMED_PASSES.get(player.getUUID());
        if (armed != null) {
            armed.remove(id);
        }
    }

    public static boolean passArmed(Player player, String id) {
        Set<String> armed = player == null ? null : ARMED_PASSES.get(player.getUUID());
        return armed != null && armed.contains(id);
    }

    public static Set<String> armMarksFor(Player player) {
        Set<String> armed = new LinkedHashSet<>();
        Set<String> running = player == null ? null : ARMED_PASSES.get(player.getUUID());
        if (running != null) {
            armed.addAll(running);
        }
        if (player != null && !AffixDamageDealer.swingInProgress(player)) {
            for (String id : readySubstitutions(player)) {
                armPass(player, id);
                armed.add(id);
                resolveChoice(player, id);
            }
        }
        if (armed.isEmpty()) {
            return Set.of();
        }
        Set<String> marks = new LinkedHashSet<>();
        for (String id : armed) {
            marks.add(ChargeMirror.ARM_MARK + id);
        }
        return marks;
    }

    private static void resolveChoice(Player player, String id) {
        EquipmentDefinition definition = definitionOf(player, id);
        if (definition == null || definition.groups().isEmpty()) {
            return;
        }
        String existing = SubstitutionChoice.of(player, id);
        if (existing != null) {
            TriggeredEffect kept = definition.effects().get(existing);
            if (kept != null && liveAtRest(player, definition, existing, kept)) {
                return;
            }
            SubstitutionChoice.record(player, id, null);
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "choice — " + id + " dropped " + existing
                        + " (no longer drawn)");
            }
        }
        Hold running = HOLDS.get(holdKey(player, id));
        if (running != null && running.effectKey() != null) {
            SubstitutionChoice.record(player, id, running.effectKey());
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "choice — " + id + " kept " + running.effectKey()
                        + " (hold at " + running.played() + ")");
            }
            return;
        }
        for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
            if (!EquipmentDefinition.substitutes(named.getValue().effect())
                    || !liveAtRest(player, definition, named.getKey(), named.getValue())) {
                continue;
            }
            if (groupCooling(player, definition, named.getKey())) {
                continue;
            }
            if (GroupDraw.holds(player, definition, named.getKey())) {
                SubstitutionChoice.record(player, id, named.getKey());
                if (CombatDebug.enabled(player)) {
                    CombatDebug.note(player, "choice — " + id + " picked " + named.getKey());
                }
                return;
            }
        }
    }

    private static boolean groupCooling(Player player, EquipmentDefinition definition,
                                        String effectKey) {
        return groupCooling(player, definition, 0, effectKey);
    }

    private static boolean groupCooling(Player player, EquipmentDefinition definition, int copy,
                                        String effectKey) {
        EffectGroup group = definition.groupOf(effectKey);
        if (group == null) {
            return false;
        }
        String name = groupNameOf(definition, group);
        return name != null && Gates.cooling(player, GateScope.GROUP,
                EffectKeys.ownerOf(definition.id(), copy), name, group.cooldown());
    }

    private static void claimGroupUse(Player player, String id) {
        String chosen = SubstitutionChoice.of(player, id);
        EquipmentDefinition definition = definitionOf(player, id);
        if (chosen == null || definition == null) {
            return;
        }
        EffectGroup group = definition.groupOf(chosen);
        String name = group == null ? null : groupNameOf(definition, group);
        if (name != null && group.armsOnSwing()) {
            GroupWindows.claimUse(player, id, name, group);
        }
    }

    private static void startGroupCooldown(Player player, String id) {
        if (player.level().isClientSide()) {
            return;
        }
        String chosen = SubstitutionChoice.of(player, id);
        EquipmentDefinition definition = definitionOf(player, id);
        if (chosen == null || definition == null) {
            return;
        }
        EffectGroup group = definition.groupOf(chosen);
        if (group == null) {
            return;
        }
        String name = groupNameOf(definition, group);
        if (name != null) {
            Gates.beginCooldown(player, GateScope.GROUP, id, name, group.cooldown());
        }
    }

    private static String groupNameOf(EquipmentDefinition definition, EffectGroup group) {
        for (Map.Entry<String, EffectGroup> named : definition.groups().entrySet()) {
            if (named.getValue() == group) {
                return named.getKey();
            }
        }
        return null;
    }

    public static Set<String> choiceMarksFor(Player player) {
        if (player == null) {
            return Set.of();
        }
        Map<String, String> chosen = SubstitutionChoice.snapshot(player);
        if (chosen.isEmpty()) {
            return Set.of();
        }
        Set<String> marks = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : chosen.entrySet()) {
            marks.add(ChargeMirror.CHOICE_MARK + entry.getKey()
                    + EffectKeys.SEPARATOR + entry.getValue());
        }
        return marks;
    }

    public static Set<String> holdMarksFor(Player player) {
        if (player == null || HOLDS.isEmpty()) {
            return Set.of();
        }
        long now = player.level().getGameTime();
        String prefix = player.getUUID() + PLAYER_SEPARATOR;
        Set<String> marks = new LinkedHashSet<>();
        for (Map.Entry<String, Hold> entry : HOLDS.entrySet()) {
            if (!entry.getKey().startsWith(prefix) || now > entry.getValue().expiresAt()) {
                continue;
            }
            marks.add(entry.getKey().substring(prefix.length())
                    + ChargeMirror.HOLD_MARK + entry.getValue().played());
        }
        return marks;
    }

    public static Set<String> armedFor(Player player) {
        if (player == null || EquipmentRegistry.isEmpty() || EquipmentProviders.isEmpty()) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (WornEquipment worn : WornEquipment.of(player)) {
            EquipmentDefinition definition = worn.definition();
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                TriggeredEffect entry = named.getValue();
                if (!liveAtRest(player, definition, worn.copy(), named.getKey(), entry)) {
                    continue;
                }
                if (!Cooldowns.active(player, definition.id(), named.getKey(), 0,
                        Cooldowns.Track.CHARGE)) {
                    out.add(definition.id());
                    break;
                }
            }
        }
        return out;
    }

    public static Map<String, Integer> cooldownsFor(Player player) {
        if (player == null || EquipmentRegistry.isEmpty() || EquipmentProviders.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        for (WornEquipment worn : WornEquipment.of(player)) {
            EquipmentDefinition definition = worn.definition();
            String itemId = definition.id();
            String owner = worn.owner();
            int longest = 0;
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                int charge = Cooldowns.remaining(player, itemId, named.getKey(), 0,
                        Cooldowns.Track.CHARGE);
                int lane = Math.max(charge, Math.max(
                        Cooldowns.remaining(player, itemId, named.getKey(), worn.copy(),
                                Cooldowns.Track.PROC),
                        StateEdges.remaining(player, owner, named.getKey())));
                if (lane > 0) {
                    out.merge(EffectKeys.of(owner, named.getKey()), lane, Math::max);
                }
                longest = Math.max(longest, charge);
            }
            for (Map.Entry<String, EffectGroup> named : definition.groups().entrySet()) {
                int group = GroupWindows.remaining(player, owner, named.getKey());
                int lane = Math.max(group, StateEdges.remaining(player, owner,
                        StateEdges.groupKey(named.getKey())));
                if (lane > 0) {
                    out.merge(EffectKeys.of(owner, named.getKey()), lane, Math::max);
                }
                longest = Math.max(longest, group);
            }
            if (longest > 0) {
                out.merge(itemId, longest, Math::max);
            }
        }
        return out;
    }

    public static Map<String, Integer> cooldownLengthsFor(Player player) {
        if (player == null || EquipmentRegistry.isEmpty() || EquipmentProviders.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        for (WornEquipment worn : WornEquipment.of(player)) {
            EquipmentDefinition definition = worn.definition();
            String itemId = definition.id();
            String owner = worn.owner();
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                int ticks = named.getValue().cooldownTicks();
                if (ticks > 0) {
                    out.merge(EffectKeys.of(owner, named.getKey()), ticks, Math::max);
                    out.merge(itemId, ticks, Math::max);
                }
            }
            for (Map.Entry<String, EffectGroup> named : definition.groups().entrySet()) {
                EffectGroup group = named.getValue();
                int ticks = group.onCooldown() ? group.cooldown().ticks() : 0;
                if (ticks <= 0 && group.windowed()) {
                    ticks = group.duration();
                }
                if (ticks > 0) {
                    out.merge(EffectKeys.of(owner, named.getKey()), ticks, Math::max);
                    out.merge(itemId, ticks, Math::max);
                }
            }
        }
        return out;
    }

    public static boolean claimCooldown(Player player, String itemId) {
        if (player == null || player.level().isClientSide() || itemId == null) {
            return true;
        }
        EquipmentDefinition definition = definitionOf(player, itemId);
        if (definition == null) {
            return true;
        }
        boolean claimed = false;
        for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
            int ticks = named.getValue().cooldownTicks();
            if (ticks <= 0) {
                continue;
            }
            if (Cooldowns.active(player, itemId, named.getKey(), 0,
                    Cooldowns.Track.PROC)) {
                return false;
            }
            Cooldowns.start(player, itemId, named.getKey(), 0,
                    Cooldowns.Track.PROC, ticks);
            claimed = true;
        }
        return claimed || definition.effects().isEmpty();
    }

    public static boolean consumeProcCooldown(Player player, Match match) {
        return consumeCooldown(player, match);
    }

    private static boolean consumeCooldown(Player player, Match match) {
        int ticks = match.entry().cooldownTicks();
        if (ticks <= 0) {
            return false;
        }
        if (Cooldowns.active(player, match.id(), match.key(), match.copy(),
                Cooldowns.Track.PROC)) {
            return true;
        }
        Cooldowns.start(player, match.id(), match.key(), match.copy(),
                Cooldowns.Track.PROC, ticks);
        return false;
    }

    public static void tickCooldowns(long gameTime) {
        Cooldowns.tick(gameTime);
    }

    public static void tickHolds(Player player) {
        if (player == null || HOLDS.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        String prefix = player.getUUID() + PLAYER_SEPARATOR;
        for (Map.Entry<String, Hold> entry : HOLDS.entrySet()) {
            if (!entry.getKey().startsWith(prefix) || now <= entry.getValue().expiresAt()) {
                continue;
            }
            String id = entry.getKey().substring(prefix.length());
            HOLDS.remove(entry.getKey());
            CombatDebug.skipped(player, id, "charge hold lapsed — recharging");
            startRecharge(player, id, now);
        }
    }
}
