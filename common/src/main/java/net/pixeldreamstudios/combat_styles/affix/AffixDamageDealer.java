package net.pixeldreamstudios.combat_styles.affix;
import net.pixeldreamstudios.combat_styles.api.CombatScheduler;
import net.pixeldreamstudios.combat_styles.damage.DamageBonus;
import net.pixeldreamstudios.combat_styles.damage.DamageHurt;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.damage.DamageDealer;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.ComboState;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.WeaponRegistry;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.api.event.CombatStylesEvents;
import net.pixeldreamstudios.combat_styles.attribute.AttackAttributes;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.entity.ConfigurableSummon;
import net.pixeldreamstudios.combat_styles.entity.SummonHooks;
import net.pixeldreamstudios.combat_styles.entity.SummonOrders;
import net.pixeldreamstudios.combat_styles.entity.SummonRoster;
import net.pixeldreamstudios.combat_styles.equipment.BurstSequence;
import net.pixeldreamstudios.combat_styles.equipment.CombatState;
import net.pixeldreamstudios.combat_styles.equipment.ComboControl;
import net.pixeldreamstudios.combat_styles.equipment.EffectWindows;
import net.pixeldreamstudios.combat_styles.equipment.SequenceSlots;
import net.pixeldreamstudios.combat_styles.equipment.SubstitutionMatch;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentAttributes;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentContext;
import net.pixeldreamstudios.combat_styles.equipment.SummonDive;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentEffect;
import net.pixeldreamstudios.combat_styles.equipment.StyleSubstitution;
import net.pixeldreamstudios.combat_styles.equipment.TriggeredEffect;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentProviders;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRuntime;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.network.SyncCooldownsPayload;
import net.pixeldreamstudios.combat_styles.network.SwingHastePayload;
import net.pixeldreamstudios.combat_styles.equipment.MissTracker;
import net.pixeldreamstudios.combat_styles.equipment.PendingExtraHit;
import net.pixeldreamstudios.combat_styles.equipment.PendingGrants;
import net.pixeldreamstudios.combat_styles.equipment.WornItems;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;
import net.pixeldreamstudios.combat_styles.equipment.ClientSwing;
import net.pixeldreamstudios.combat_styles.api.SwingTiming;
import net.pixeldreamstudios.combat_styles.equipment.SwingHaste;
import net.pixeldreamstudios.combat_styles.equipment.SubstitutionScope;
import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;
import net.pixeldreamstudios.combat_styles.style.DeclaredClaims;
import net.pixeldreamstudios.combat_styles.style.AttackAttribute;
import net.pixeldreamstudios.combat_styles.style.AttackDrag;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.HitboxClaims;
import net.pixeldreamstudios.combat_styles.style.HitboxSet;
import net.pixeldreamstudios.combat_styles.style.InnateAffixes;
import net.pixeldreamstudios.combat_styles.style.SwingWindow;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.SwingHolder;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class AffixDamageDealer {
    private static final ThreadLocal<Boolean> ECHOING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final ThreadLocal<Boolean> RESOLVING_EXTRA =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final double MAX_SWING_DELAY = 4.0D;
    private static final double TICKS_PER_SECOND = 20.0D;

    private record PendingSwing(Player player, int comboCount, long dueAt) {
    }

    private record PendingEffect(Player player, StyleAffix affix, AffixEffects.OnHit effects,
                                 float damage, ResolvedAffixes resolved, long dueAt,
                                 WeaponAttributes.Attack attack, SwingContext context) {
    }

    private record PendingGroup(Player player, long dueAt, Runnable action) {
    }

    private static final Set<PendingSwing> PENDING = ConcurrentHashMap.newKeySet();
    private static final Set<PendingGroup> PENDING_GROUPS = ConcurrentHashMap.newKeySet();
    private static final Set<PendingEffect> PENDING_EFFECTS = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<WeaponAttributes.Attack> SWING_ATTACK = new ThreadLocal<>();
    private static final ThreadLocal<SwingContext> SWING_CONTEXT = new ThreadLocal<>();

    private record SwingAffixes(long swingId, ResolvedAffixes resolved) {
    }

    private static final ThreadLocal<SwingAffixes> SWING_AFFIXES = new ThreadLocal<>();

    public static void forgetHistory(UUID playerId) {
        HitStreaks.forget(playerId);
        DEFERRED_HITS.remove(playerId);
    }

    private static SwingContext enrich(SwingContext context, Player player, LivingEntity target) {
        SwingContext base = context == null ? SwingContext.UNKNOWN : context;
        base = base.withWeaponCategory(SequenceService.categoryOf(swingingWeapon(player)));
        boolean same = HitStreaks.isSameTarget(player, target);
        int streak = target == null
                ? HitStreaks.streakOf(player)
                : HitStreaks.streakOf(player, target);
        if (streak <= 0) {
            return base.withHistory(0, same);
        }
        return base.withHistory(streak, same);
    }

    private AffixDamageDealer() {
    }

    public static float executeBonusFor(Player player, LivingEntity target, float amount) {
        if (player == null || target == null || amount <= 0.0F || DamageDealer.isDealing()) {
            return 0.0F;
        }
        ResolvedAffixes resolved = affixesForCurrentAttack(player, swingingWeapon(player));
        if (resolved.isEmpty()) {
            return 0.0F;
        }
        float bonus = 0.0F;
        for (StyleAffix affix : resolved.affixes()) {
            AffixEffects.OnHit onHit = affix.onHit();
            if (onHit.execute().isEmpty() || onHit.execute().get().instantKill()) {
                continue;
            }
            if (!AffixMechanics.conditionMet(onHit.condition(), player, target, swingContext())) {
                continue;
            }
            if (onHit.cooldown().isPresent()
                    && AffixMechanics.onCooldown(player, affix, onHit.cooldown().get(),
                        swingContext().slotIndex())) {
                continue;
            }
            if (onHit.cost().isPresent()
                    && !AffixMechanics.canAffordCost(player, onHit.cost().get())) {
                continue;
            }
            bonus += AffixMechanics.executeBonus(onHit.execute().get(), target, amount);
        }
        return bonus;
    }

    public static float amplifyBonusFor(Player player, LivingEntity target, float amount) {
        if (player == null || target == null || amount <= 0.0F || DamageDealer.isDealing()) {
            return 0.0F;
        }
        ResolvedAffixes resolved = affixesForCurrentAttack(player, swingingWeapon(player));

        List<StyleAffix> amplifiers = new ArrayList<>(resolved.affixes());
        amplifiers.addAll(equipmentAmplifiers(player, target, amount));
        if (amplifiers.isEmpty()) {
            return 0.0F;
        }
        float bonus = 0.0F;
        for (StyleAffix affix : amplifiers) {
            AffixEffects.OnHit onHit = affix.onHit();
            List<AffixEffects.Damage> amplifying = onHit.allDamage().stream()
                    .filter(AffixEffects.Damage::amplifies)
                    .toList();
            if (amplifying.isEmpty() && onHit.momentum().isEmpty()) {
                continue;
            }
            if (!AffixMechanics.conditionMet(onHit.condition(), player, target, swingContext())) {
                continue;
            }
            if (onHit.cooldown().isPresent()
                    && AffixMechanics.onCooldown(player, affix, onHit.cooldown().get(),
                        swingContext().slotIndex())) {
                continue;
            }

            if (onHit.cost().isPresent()
                    && !AffixMechanics.canAffordCost(player, onHit.cost().get())) {
                continue;
            }
            float momentum = 1.0F;
            for (AffixEffects.Momentum stacking : onHit.momentum().stream().toList()) {
                momentum *= AffixMechanics.momentumMultiplier(player, stacking);
            }
            float scaled = amount * onHit.condition().multiplier()
                    * momentum
                    * precedingScale(onHit.condition())
                    * (float) affix.damageMultiplier();
            float added = 0.0F;
            if (amplifying.isEmpty() && momentum > 1.0F) {
                added += amount * (momentum - 1.0F);
            }
            for (AffixEffects.Damage damage : amplifying) {
                float ratio = Math.max(0.0F, damage.ratio());
                if (damage.scaling().isPresent()) {
                    AffixEffects.Scaling scaling = damage.scaling().get();
                    ratio = scaling.apply(ratio, SpellPowerAccess.valueOf(
                            player, scaling.sources(), scaling.strongest()));
                }
                added += scaled * ratio + Math.max(0.0F, damage.flat());
            }
            bonus += added;
            if (added > 0.0F) {
                if (CombatDebug.enabled(player)) {
                    CombatDebug.fired(player, affix.serializedId(),
                            "+" + round(added) + " before armour");
                }
            }
        }
        return bonus;
    }

    public static StyleAffix affixForCurrentAttack(Player player, ItemStack stack) {
        List<StyleAffix> affixes = affixesForCurrentAttack(player, stack).affixes();
        return affixes.isEmpty() ? null : affixes.getFirst();
    }

    public static ResolvedAffixes affixesForCombo(Player player, ItemStack stack, int comboCount) {
        if (player == null || stack == null || stack.isEmpty()) {
            return ResolvedAffixes.EMPTY;
        }
        StyleSequence sequence = SequenceService.sequenceOf(stack, player.getUUID());
        List<StyleEntry> entries = sequence.entries();
        if (entries.isEmpty()) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "sequence — " + sequence.name()
                        + " resolved empty for " + stack.getItem()
                        + ", no slot context");
            }
            SWING_ATTACK.set(nativeAttack(stack, SequenceSlots.sequenceComboOf(player, comboCount)));
            SWING_CONTEXT.set(SwingContext.UNKNOWN);
            return ResolvedAffixes.EMPTY;
        }
        int index = SequenceSlots.slotFor(SequenceSlots.sequenceComboOf(player, comboCount), entries.size());
        WeaponAttributes.Attack swung = CombatState.swungAttack(player);
        if (swung != null) {
            StylePage chosen = pageOf(entries.get(index));
            if (chosen == null || !matches(chosen.attack(), swung)) {
                int found = SequenceSlots.indexOfAttack(entries, swung);
                if (found < 0) {
                    if (CombatDebug.enabled(player)) {
                        CombatDebug.note(player, "sequence — swung " + swung.animation()
                                + " is not in this sequence, resolving no affixes");
                    }
                    SWING_ATTACK.set(null);
                    SWING_CONTEXT.remove();
                    return ResolvedAffixes.EMPTY;
                }
                index = found;
            }
        }
        StyleEntry entry = entries.get(index);
        StylePage page = pageOf(entry);
        SWING_ATTACK.set(page == null ? null : page.attack());
        SWING_CONTEXT.set(SwingContext.of(entries, index, SequenceSlots.usableSlotCount(player, entries.size())));
        ResolvedAffixes resolved = withEquipmentTuning(player,
                entry.resolveAffixes(page).restrictedTo(page), entry, index, entries.size());
        SWING_AFFIXES.set(new SwingAffixes(CombatState.currentSwingId(player), resolved));
        return resolved;
    }

    public static boolean swingMoves(Player player, ItemStack stack, int comboCount) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        if (player.isShiftKeyDown() && CombatStylesConfig.get().sneakSuppressesMotion()) {
            return false;
        }
        StyleSequence sequence;
        SwingHolder.push(player);
        try {
            sequence = SequenceService.sequenceOf(stack, player.getUUID());
        } finally {
            SwingHolder.pop();
        }
        List<StyleEntry> entries = sequence.entries();
        if (entries.isEmpty()) {
            return false;
        }
        int index = SequenceSlots.slotFor(SequenceSlots.sequenceComboOf(player, comboCount), entries.size());
        AttackOverride override = attackOverrideFor(player, comboCount);
        if (override != null
                && grantsDrag(override.behaviourOrDefault().motionOrNone())) {
            return true;
        }
        StyleEntry entry = entries.get(index);
        ResolvedAffixes resolved = withEquipmentTuning(player,
                entry.resolveAffixes(pageOf(entry)).restrictedTo(pageOf(entry)), entry, index, entries.size());
        for (StyleAffix affix : resolved.affixes()) {
            if (grantsDrag(affix.onSwing().mechanics().motion())
                    || grantsDrag(affix.onHit().mechanics().motion())) {
                return true;
            }
        }
        return false;
    }

    private static boolean grantsDrag(AffixEffects.Motion motion) {
        return AttackDrag.grantsDrag(motion);
    }

    private static StylePage pageOf(StyleEntry entry) {
        StylePage page = StyleRegistry.get(entry.styleId());
        if (page == null || entry.attackOverride().isEmpty()) {
            return page;
        }
        return AttackOverrides.applyTo(page, entry.attackOverride().get());
    }

    public static ResolvedAffixes affixesForCurrentAttack(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return ResolvedAffixes.EMPTY;
        }
        if (!RESOLVING_EXTRA.get()) {
            RESOLVING_EXTRA.set(Boolean.TRUE);
            try {
                WeaponAttributes.Attack extraSwing = CombatState.swungAttack(player);
                List<String> extraOwn = PendingExtraHit.affixesFor(player, extraSwing);
                if (!extraOwn.isEmpty()) {
                    return ResolvedAffixes.of(extraOwn,
                            PendingExtraHit.tuningFor(player, extraSwing));
                }
            } finally {
                RESOLVING_EXTRA.set(Boolean.FALSE);
            }
        }
        StyleSequence sequence = SequenceService.sequenceOf(stack, player.getUUID());
        if (sequence.isEmpty()) {
            return ResolvedAffixes.EMPTY;
        }

        WeaponAttributes.Attack current = CombatState.swungAttack(player);
        if (current == null) {
            current = currentAttack(player);
        }
        if (current == null) {
            return ResolvedAffixes.EMPTY;
        }
        List<StyleEntry> entries = sequence.entries();
        if (CombatDebug.enabled(player)) {
            StringBuilder ids = new StringBuilder();
            for (StyleEntry listed : entries) {
                ids.append(ids.isEmpty() ? "" : ", ").append(listed.styleId());
            }
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "sequence — " + sequence.name()
                        + " size " + entries.size() + " [" + ids + "] vs swung "
                        + current.animation());
            }
        }

        SwingContext context = SWING_CONTEXT.get();
        if (context != null && context.isKnown() && context.slotIndex() < entries.size()) {
            StyleEntry exact = entries.get(context.slotIndex());
            StylePage page = pageOf(exact);
            if (page != null && matches(page.attack(), current)) {
                return withEquipmentTuning(player, exact.resolveAffixes(page).restrictedTo(page),
                        exact, context.slotIndex(), entries.size());
            }
            SWING_CONTEXT.remove();
        }

        for (int i = 0; i < entries.size(); i++) {
            StyleEntry entry = entries.get(i);
            StylePage page = pageOf(entry);
            if (page != null && matches(page.attack(), current)) {
                SWING_CONTEXT.set(SwingContext.of(entries, i, SequenceSlots.usableSlotCount(player, entries.size())));
                return withEquipmentTuning(player, entry.resolveAffixes(page).restrictedTo(page),
                        entry, i, entries.size());
            }
        }

        SwingAffixes swung = SWING_AFFIXES.get();
        return swung != null && swung.swingId() == CombatState.currentSwingId(player)
                ? swung.resolved()
                : ResolvedAffixes.EMPTY;
    }

    public static void captureSwungAttack(Player player, int rawCombo) {
        if (player == null || player.level().isClientSide()) {
            return;
        }

        EquipmentRuntime.advanceSwing(player);
        EquipmentRuntime.anchorSubstitution(player, rawCombo);
        if (player instanceof ServerPlayer swinger) {
            NetworkManager.syncCooldowns(swinger, SyncCooldownsPayload.NO_PROC);
        }
        KILLS_THIS_SWING.get().clear();
        HitStreaks.beginSwing(player);
        SwingWindow.beginSwing(player);
        ComboControl.recordSwing(player, rawCombo);
        int combo = ComboControl.adjust(player, rawCombo);
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "combo — client sent " + rawCombo + ", using " + combo
                    + (combo == rawCombo ? "" : " ADJUSTED by " + (combo - rawCombo)));
        }
        CombatState.recordSwungEntry(player, entryFor(player, combo));
        BurstSequence.advance(player);
        AttackHand substituted = PlayerAttackHelper.getCurrentAttack(player, combo);
        CombatState.recordSwungHand(player, resolveSwing(player, combo, substituted));
        CombatState.recordSwungSlot(player, swungSlotOf(player, combo));
        DeclaredClaims.applyDeferred(player);
        applyPendingHaste(player, combo);
        EquipmentAttributes.sync(player);
    }

    private static AttackHand resolveSwing(Player player, int combo, AttackHand substituted) {
        String declared = ClientSwing.animationOf(player);
        String server = substituted == null || substituted.attack() == null
                ? "-" : substituted.attack().animation();
        if (!ClientSwing.hasDeclaration(player)) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "resolve — combo " + combo
                        + " no declaration, using server " + server);
            }
            return substituted;
        }
        if (substituted != null && ClientSwing.matchesDeclared(player, substituted.attack())) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "resolve — combo " + combo + " AGREE #"
                        + ClientSwing.serialOf(player) + " " + server);
            }
            return substituted;
        }
        AttackHand base = SubstitutionScope.queuedAttack(player, combo);
        String baseName = base == null || base.attack() == null
                ? "-" : base.attack().animation();
        if (base != null && ClientSwing.matchesDeclared(player, base.attack())) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "resolve — combo " + combo + " DISAGREE #"
                        + ClientSwing.serialOf(player)
                        + " client " + declared + " server " + server
                        + " -> using unsubstituted " + baseName);
            }
            return base;
        }
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "resolve — combo " + combo + " UNMATCHED #"
                    + ClientSwing.serialOf(player)
                    + " client " + declared + " server " + server
                    + " base " + baseName + " -> keeping server");
        }
        return substituted;
    }

    private static void applyPendingHaste(Player player, int combo) {
        WeaponAttributes.Attack attack = CombatState.swungAttack(player);
        String hitbox = attack == null || attack.hitbox() == null
                ? null
                : attack.hitbox().name();
        SwingHaste.endSwing(player, SwingHaste.swingIndex(player));
        SwingHaste.beginSwing(player);
        if (SwingHaste.armed(player)) {
            SwingHaste.consume(player, hitbox);
        }
        applyAttackTiming(player, combo);
    }

    public static void beginBurst(Player player, StyleSubstitution substitution) {
        if (player == null || substitution == null || substitution.borrowSlots().isEmpty()) {
            return;
        }
        ItemStack weapon = swingingWeapon(player);
        List<StyleEntry> base = SubstitutionScope.withoutSubstitution(
                () -> SequenceService.sequenceOf(weapon, player.getUUID()).entries());
        if (base.isEmpty()) {
            return;
        }
        List<StyleEntry> borrowed = new ArrayList<>();
        for (int slot : substitution.borrowSlots()) {
            int index = StyleSubstitution.resolveSlot(slot, base.size());
            if (index >= 0 && index < base.size()) {
                borrowed.add(base.get(index));
            }
        }
        if (borrowed.isEmpty()) {
            return;
        }
        BurstSequence.begin(player, borrowed, substitution.lingerTicks(), 0);
    }

    public static void applyLocalAttackTiming(Player player, int rawCombo) {
        SwingHaste.clearSelf(player);
        int combo = SequenceSlots.sequenceComboOf(player, rawCombo);
        ItemStack weapon = swingingWeapon(player, rawCombo);
        StyleSequence sequence = SequenceService.sequenceOf(weapon, player.getUUID());
        int slots = sequence.entries().size();
        if (slots <= 0) {
            return;
        }
        AttackOverride previous = combo <= 0
                ? null
                : attackOverrideFor(player, rawCombo - 1);
        float inherited = previous == null
                ? SwingHaste.NEUTRAL_MULTIPLIER
                : previous.hastensNext()
                        .map(haste -> (float) haste.amount())
                        .orElse(SwingHaste.NEUTRAL_MULTIPLIER);
        AttackOverride override = attackOverrideFor(player, rawCombo);
        float own = override == null
                ? SwingHaste.NEUTRAL_MULTIPLIER
                : override.cooldownScale()
                        .map(scale -> (float) scale.amount())
                        .orElse(SwingHaste.NEUTRAL_MULTIPLIER);
        float multiplier = own * inherited;
        if (CombatStylesConfig.get().debugLogging()) {
            CombatStyles.LOGGER.info(
                    "[LOCAL-TIMING] rawCombo={} seqCombo={} own={} inherited={} granted={}",
                    rawCombo, combo, own, inherited, multiplier);
        }
    }

    private static final ThreadLocal<Boolean> ATTACK_MOTION =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static boolean attackDeclaresMotion(Player player) {
        return Boolean.TRUE.equals(ATTACK_MOTION.get());
    }

    public static void runExtraMotion(Player player, AttackOverride override) {
        if (player == null || override == null) {
            return;
        }
        AffixEffects.Motion motion = override.behaviourOrDefault().motionOrNone();
        if (motion.isEmpty()) {
            return;
        }
        runGroup(player, null, 0.0F, motion);
    }

    private static void applyAttackMotion(Player player, AttackOverride override) {
        AffixEffects.Motion motion = override == null
                ? AffixEffects.Motion.NONE
                : override.behaviourOrDefault().motionOrNone();
        ATTACK_MOTION.set(!motion.isEmpty());
        if (motion.isEmpty()) {
            return;
        }
        runGroup(player, null, 0.0F, motion);
    }

    private static void applyAttackTiming(Player player, int combo) {
        AttackOverride override = extraShaped(player, combo);
        applyAttackMotion(player, override);
        SwingHaste.setSequenceScale(player, SwingScales.upcomingTimingScaleFor(player, combo));
    }

    public static void latchSwingCharges(Player player, int rawCombo) {
        if (DamageDealer.isDealing() || player == null || player.level().isClientSide()) {
            return;
        }
        EquipmentRuntime.latchCharges(player, ComboControl.adjust(player, rawCombo));
    }

    public static void armSwing(Player player, int rawCombo) {
        if (DamageDealer.isDealing() || player == null || player.level().isClientSide()) {
            return;
        }
        armSwingAttributes(player, ComboControl.adjust(player, rawCombo));
    }

    public static void armSweepAttributes(Player player, int rawCombo) {
        if (DamageDealer.isDealing() || player == null || player.level().isClientSide()) {
            return;
        }
        armSwingAttributes(player, ComboControl.adjust(player, rawCombo));
    }

    public static void onPlayerSwing(Player player, int rawCombo) {
        if (DamageDealer.isDealing() || player == null || player.level().isClientSide()) {
            return;
        }

        SwingHolder.push(player);
        int comboCount = ComboControl.adjust(player, rawCombo);
        if (CombatDebug.enabled(player)) {
            CombatDebug.phase(player, "swing", "idle "
                    + CombatState.idleAtSwing(player) + "t before this swing");
        }
        ComboControl.markUsed(player, rawCombo);

        CombatState.recordSwing(player);
        MissTracker.schedule(player, comboCount);
        int delay = upswingTicks(player, comboCount);
        if (delay > 0) {
            PENDING.add(new PendingSwing(player, comboCount,
                    player.level().getGameTime() + delay));
            return;
        }
        applySwing(player, comboCount);
        ClientSwing.consume(player);
    }

    public static boolean swingInProgress(Player player) {
        if (player == null) {
            return false;
        }
        for (PendingSwing pending : PENDING) {
            if (pending.player() == player) {
                return true;
            }
        }
        return false;
    }

    private static int upswingTicks(Player player, int comboCount) {
        WeaponAttributes attributes = WeaponRegistry.getAttributes(
                swingingWeapon(player, comboCount));
        if (attributes == null || attributes.attacks() == null
                || attributes.attacks().length == 0) {
            return 0;
        }
        WeaponAttributes.Attack attack =
                attributes.attacks()[SequenceSlots.slotFor(SequenceSlots.sequenceComboOf(player, comboCount),
                        attributes.attacks().length)];
        if (attack == null) {
            return 0;
        }
        double speed = player.getAttributeValue(Attributes.ATTACK_SPEED);
        if (speed <= 0.0D) {
            return 0;
        }
        double duration = TICKS_PER_SECOND / speed;
        double upswing = Math.clamp(attack.upswing(), 0.0D, 1.0D) * duration;
        double progress = Math.clamp(CombatStylesConfig.get().swingEffectDelay(), 0.0D, 1.0D);
        return (int) Math.round(Math.min(upswing * progress, MAX_SWING_DELAY));
    }

    private static int delayTicksOf(AffixEffects.OnHit effects) {
        return effects == null ? 0 : Math.max(0, effects.delayTicks());
    }

    public static void tickPending(long gameTime) {
        HitStreaks.expire(gameTime);

        PENDING_GROUPS.removeIf(pending -> {
            if (gameTime < pending.dueAt()) {
                return false;
            }
            Player player = pending.player();
            if (player != null && player.isAlive() && !player.level().isClientSide()) {
                DamageDealer.beginDealing();
                try {
                    pending.action().run();
                } catch (Throwable throwable) {
                    CombatStyles.LOGGER.error("Failed to apply delayed mechanic group", throwable);
                } finally {
                    DamageDealer.endDealing();
                }
            }
            return true;
        });

        PENDING.removeIf(pending -> {
            if (gameTime < pending.dueAt()) {
                return false;
            }
            Player player = pending.player();
            if (player != null && player.isAlive() && !player.level().isClientSide()) {
                SwingHolder.push(player);
                try {
                    applySwing(player, pending.comboCount());
                } finally {
                    SwingHolder.pop();
                    ClientSwing.consume(player);
                }
            }
            return true;
        });

        PENDING_EFFECTS.removeIf(pending -> {
            if (gameTime < pending.dueAt()) {
                return false;
            }
            Player player = pending.player();
            if (player != null && player.isAlive() && !player.level().isClientSide()) {
                DamageDealer.beginDealing();
                SWING_ATTACK.set(pending.attack());
                SWING_CONTEXT.set(pending.context());
                try {
                    apply(player, null, pending.affix(), pending.effects(),
                            pending.damage(), pending.resolved());
                } catch (Throwable throwable) {
                    CombatStyles.LOGGER.error("Failed to apply delayed affix {}",
                            pending.affix().serializedId(), throwable);
                } finally {
                    DamageDealer.endDealing();
                    SWING_ATTACK.remove();
                    SWING_CONTEXT.remove();
                    SWING_AFFIXES.remove();
                    AffixSlashWaves.endSwing();
                }
            }
            return true;
        });

        AffixSlashWaves.tick(gameTime);
    }

    private static Set<String> spentSubstitutions(Player player, int comboCount) {
        ItemStack weapon = swingingWeapon(player, comboCount);
        List<EquipmentDefinition> substituting =
                EquipmentRuntime.allSubstituting(player, weapon);
        if (substituting.isEmpty()) {
            return Set.of();
        }
        WeaponAttributes.Attack swung = CombatState.swungAttack(player);
        if (swung == null) {
            CombatDebug.note(player, "charge attribution — no swung attack recorded, spending none");
            return Set.of();
        }
        if (ClientSwing.hasDeclaration(player)
                && !ClientSwing.matchesDeclared(player, swung)) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "charge attribution — client played "
                        + ClientSwing.animationOf(player) + ", not " + swung.animation()
                        + "; spending none");
            }
            return Set.of();
        }
        Set<String> spent = new LinkedHashSet<>();
        for (EquipmentDefinition definition : substituting) {
            if (EquipmentRuntime.holdDriven(player, definition.id())) {
                if (CombatDebug.enabled(player)) {
                    CombatDebug.note(player, "charge attribution — " + definition.id()
                            + " sequence running, the hold decides");
                }
                spent.add(definition.id());
                continue;
            }
            boolean mine = SubstitutionMatch.provides(definition, swung);
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "charge attribution — " + definition.id()
                        + " vs swung " + swung.animation() + "/" + swung.hitbox()
                        + " => " + (mine ? "spend" : "refund"));
            }
            if (mine) {
                spent.add(definition.id());
            }
        }
        return spent;
    }

    public static void armSwingAttributes(LivingEntity attacker, ItemStack weapon, int comboCount) {
        if (attacker == null || weapon == null || weapon.isEmpty()) {
            return;
        }
        if (attacker instanceof Player player) {
            armSwingAttributes(player, comboCount);
            return;
        }
        SwingHolder.push(attacker);
        try {
            AttackOverride override = overrideForStack(attacker, weapon, comboCount);
            if (override == null || override.attributes().isEmpty()) {
                AttackAttributes.disarm(attacker);
                return;
            }
            AttackAttributes.arm(attacker, override);
        } finally {
            SwingHolder.pop();
        }
    }

    private static AttackOverride overrideForStack(LivingEntity attacker, ItemStack weapon,
                                                   int comboCount) {
        StyleSequence sequence = SequenceService.sequenceOf(weapon, attacker.getUUID());
        List<StyleEntry> entries = sequence.entries();
        if (entries.isEmpty()) {
            return null;
        }
        int slot = SequenceSlots.slotFor(comboCount, entries.size());
        StyleEntry entry = entries.get(slot);
        AttackOverride declared = entry.attackOverride().orElse(null);
        AttackOverride fromItem =
                SequenceService.attackOverridesOf(weapon).get(String.valueOf(slot));
        if (declared == null) {
            return fromItem;
        }
        return fromItem == null ? declared : declared.overriddenBy(fromItem);
    }

    private static void armSwingAttributes(Player player, int comboCount) {
        SwingHolder.push(player);
        try {
            ResolvedAffixes resolved = affixesForCombo(player,
                    swingingWeapon(player, comboCount), comboCount);
            if (SWING_CONTEXT.get() == null) {
                if (CombatDebug.enabled(player)) {
                    CombatDebug.note(player, "swingattr — combo " + comboCount
                            + " DISARM (swung attack is not in this sequence)");
                }
                AttackAttributes.disarm(player, PendingExtraHit.index());
                return;
            }
            AttackOverride shaped = extraShaped(player, comboCount);
            AttackOverride override = mergeSwingAttributes(shaped, resolved);
            double ratio = extraRatio(player, comboCount, shaped);
            if (override == null
                    || (override.attributes().isEmpty() && ratio == NEUTRAL_RATIO)) {
                if (CombatDebug.enabled(player)) {
                    CombatDebug.note(player, "swingattr — combo " + comboCount
                            + " DISARM (override " + (override == null ? "null"
                                    : "empty attrs") + ", ratio " + ratio + ")");
                }
                AttackAttributes.disarm(player, PendingExtraHit.index());
                return;
            }
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "swingattr — combo " + comboCount
                        + " ARM " + override.attributes().keySet()
                        + " ratio " + ratio);
            }
            AttackAttributes.arm(player, override, ratio, PendingExtraHit.index());
        } finally {
            SwingHolder.pop();
        }
    }

    private static final double NEUTRAL_RATIO = 1.0D;

    private static double extraRatio(Player player, int comboCount, AttackOverride shaped) {
        if (PendingExtraHit.index() < 0 || shaped == null) {
            return NEUTRAL_RATIO;
        }
        AttackHand hand = PlayerAttackHelper.getCurrentAttack(player, comboCount);
        return hand == null
                ? NEUTRAL_RATIO
                : PendingExtraHit.ratioOf(shaped, hand.attack());
    }

    public static AttackOverride shapedOverrideFor(Player player, int comboCount) {
        return extraShaped(player, comboCount);
    }

    private static AttackOverride extraShaped(Player player, int comboCount) {
        AttackOverride base = attackOverrideFor(player, comboCount);
        int index = PendingExtraHit.index();
        boolean debug = CombatStylesConfig.get().debugLogging();
        if (index < 0) {
            if (debug) {
                CombatStyles.LOGGER.info("[EXTRA] no index (combo={})", comboCount);
            }
            return base;
        }
        AttackHand hand = PlayerAttackHelper.getCurrentAttack(player, comboCount);
        if (hand == null) {
            if (debug) {
                CombatStyles.LOGGER.info("[EXTRA] index={} but no hand (combo={})",
                        index, comboCount);
            }
            return base;
        }
        AttackOverride shaped = PendingExtraHit.shapeFor(player, hand.attack(), base);
        if (debug) {
            CombatStyles.LOGGER.info("[EXTRA] index={} hits={} shaped={} attrs={}",
                    index, PendingExtraHit.hitsOf(player, hand.attack()).size(),
                    shaped != null, shaped == null ? 0 : shaped.attributes().size());
        }
        return shaped == null ? base : shaped;
    }

    private static void applySwing(Player player, int comboCount) {
        EquipmentRuntime.beginAttackProcs(player);
        try {
            Set<String> spent = spentSubstitutions(player, comboCount);
            ResolvedAffixes resolved = affixesForCombo(player,
                    swingingWeapon(player, comboCount), comboCount);
            EquipmentRuntime.dropUnusedCharges(player, spent);
            AttackOverride swingOverride = mergeSwingAttributes(
                    attackOverrideFor(player, comboCount), resolved);
            boolean hasSwingAttributes = swingOverride != null && !swingOverride.attributes().isEmpty();
            if (resolved.isEmpty() && EquipmentRegistry.isEmpty() && !hasSwingAttributes) {
                SWING_ATTACK.remove();
                SWING_CONTEXT.remove();
                SWING_AFFIXES.remove();
                return;
            }
            SWING_CONTEXT.set(enrich(SWING_CONTEXT.get(), player, null));
            WeaponAttributes.Attack swung = SWING_ATTACK.get();
            float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                    * (swung == null ? 1.0F : (float) swung.damageMultiplier());
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "damage — combo " + comboCount
                        + " attack " + (swung == null ? "-" : swung.animation())
                        + " mult " + (swung == null ? 1.0D : swung.damageMultiplier())
                        + " base " + String.format("%.2f", base)
                        + " declared " + ClientSwing.animationOf(player));
            }

            if (!CombatStylesEvents.SWING.isEmpty()) {
                CombatStylesEvents.SwingEvent event = new CombatStylesEvents.SwingEvent(
                        player, swingingWeapon(player, comboCount), comboCount, swung, resolved,
                        SWING_CONTEXT.get(), base);
                if (!CombatStylesEvents.SWING.allow(listener -> listener.onSwing(event))) {
                    SWING_ATTACK.remove();
                    SWING_CONTEXT.remove();
                    SWING_AFFIXES.remove();
                    return;
                }
            }

            DamageDealer.beginDealing();
            try {
                for (StyleAffix affix : resolved.affixes()) {
                    if (affix.onSwing().isEmpty()
                            || affix.onSwing().condition().onMiss().orElse(false)) {
                        continue;
                    }
                    try {
                        int delay = delayTicksOf(affix.onSwing());
                        if (delay > 0) {
                            PENDING_EFFECTS.add(new PendingEffect(player, affix, affix.onSwing(),
                                    base, resolved, player.level().getGameTime() + delay,
                                    attackOf(player), swingContext()));
                        } else {
                            apply(player, null, affix, affix.onSwing(), base, resolved);
                            scheduleEcho(player, null, affix, affix.onSwing(), base, resolved,
                                    wornEcho(player, null, false, 0.0F));
                        }
                    } catch (Throwable throwable) {
                        CombatStyles.LOGGER.error("Failed to apply swing affix {}",
                                affix.serializedId(), throwable);
                    }
                }
                applyEquipment(player, comboCount, base, resolved, false, null, false);
            } finally {
                DamageDealer.endDealing();
                AffixSlashWaves.endSwing();
                SWING_ATTACK.remove();
            }
        } finally {
            EquipmentRuntime.releaseCharges(player);
            releaseDeferredHits(player);
            EquipmentRuntime.endCycleOnLastSlot(player);
        }
    }

    public static void applyMissEquipment(Player player, int comboCount) {
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AttackAttributes.disarm(player);
        ResolvedAffixes resolved = affixesForCombo(player,
                swingingWeapon(player, comboCount), comboCount);
        if (!CombatStylesEvents.MISS.isEmpty()) {
            CombatStylesEvents.MissEvent event = new CombatStylesEvents.MissEvent(
                    player, swingingWeapon(player, comboCount), comboCount, swingContext());
            if (!CombatStylesEvents.MISS.allow(listener -> listener.onMiss(event))) {
                return;
            }
        }
        DamageDealer.beginDealing();
        try {
            for (StyleAffix affix : resolved.affixes()) {
                if (affix.onSwing().isEmpty()
                        || !affix.onSwing().condition().onMiss().orElse(false)) {
                    continue;
                }
                try {
                    apply(player, null, affix, affix.onSwing(), base, resolved);
                } catch (Throwable throwable) {
                    CombatStyles.LOGGER.error("Failed to apply miss affix {}",
                            affix.serializedId(), throwable);
                }
            }
            applyEquipment(player, comboCount, base, ResolvedAffixes.EMPTY, false, null, false, true);
        } finally {
            DamageDealer.endDealing();
            AffixSlashWaves.endSwing();
        }
    }

    private static final ThreadLocal<Boolean> KILLED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final ThreadLocal<Set<Integer>> KILLS_THIS_SWING =
            ThreadLocal.withInitial(LinkedHashSet::new);

    public static ResolvedAffixes affixesForTrail(Player player, ResolvedAffixes resolved,
                                                  StyleEntry entry, int slotIndex, int size) {
        return withEquipmentTuning(player, resolved, entry, slotIndex, size);
    }

    private static ComboState usableCombo(Player player) {
        if (PlayerAttackHelper.isDualWielding(player)) {
            return null;
        }
        AttackHand hand = CombatState.swungHand(player);
        if (hand == null && player instanceof EntityPlayer_BetterCombat holder) {
            hand = holder.getCurrentAttack();
        }
        return hand == null ? null : hand.combo();
    }

    private static ResolvedAffixes withEquipmentTuning(Player player, ResolvedAffixes resolved,
                                                       StyleEntry entry, int slotIndex, int size) {
        if (EquipmentRegistry.isEmpty()) {
            return resolved;
        }
        Map<String, AffixTuning> tuning = null;
        List<String> granted = null;
        int usableIndex = slotIndex;
        int usableSize = size;
        if (usableSize <= 0) {
            ComboState usable = usableCombo(player);
            if (usable != null && usable.total() > 0) {
                usableIndex = usable.current() - 1;
                usableSize = usable.total();
            }
        }
        EquipmentContext context = entry == null
                ? null
                : new EquipmentContext(player, entry, attackOf(player), usableIndex, usableSize,
                        EquipmentRuntime.swingCount(player),
                        Boolean.TRUE.equals(KILLED.get()), false);

        String innate = EquipmentRuntime.substituting(player) == null
                ? InnateAffixes.affixFor(swingingWeapon(player), slotIndex)
                : null;
        if (innate != null) {
            granted = new ArrayList<>(resolved.affixes().stream()
                    .map(StyleAffix::serializedId).toList());
            if (!granted.contains(innate)) {
                granted.add(innate);
            }
        }

        for (String affix : PendingGrants.peek(player)) {
            if (granted == null) {
                granted = new ArrayList<>(resolved.affixes().stream()
                        .map(StyleAffix::serializedId).toList());
            }
            if (!granted.contains(affix)) {
                granted.add(affix);
            }
        }

        for (ItemStack worn : WornItems.of(player)) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(worn);
            if (definition == null) {
                continue;
            }
            for (Map.Entry<String, TriggeredEffect> named : definition.effects().entrySet()) {
                TriggeredEffect triggered = named.getValue();
                EquipmentEffect granting = triggered.effect();
                boolean passes = (context == null
                        || triggered.trigger().matches(context, triggered.cooldown()))
                        && EquipmentRuntime.rewriteAllowed(player,
                                new EquipmentRuntime.Match(definition, triggered, named.getKey()));
                if (passes && !granting.tuneAffixes().isEmpty()) {
                    if (tuning == null) {
                        tuning = new LinkedHashMap<>();
                    }
                    for (Map.Entry<String, AffixTuning> match : granting.tuneAffixes().entrySet()) {
                        tuning.merge(match.getKey(), match.getValue(),
                                (existing, added) -> added.mergedUnder(existing));
                    }
                }
                if (!passes && CombatDebug.enabled(player)) {
                    if (CombatDebug.enabled(player)) {
                        CombatDebug.note(player, definition.id() + " tuning skipped: "
                                + triggered.trigger().explain(context));
                    }
                }
                if (context == null || granting.grantAffixes().isEmpty()) {
                    continue;
                }
                if (!passes) {
                    if (triggered.trigger().worthReporting(context)) {
                        if (CombatDebug.enabled(player)) {
                            CombatDebug.note(player, definition.id() + " not granted: "
                                    + triggered.trigger().explain(context));
                        }
                    }
                    continue;
                }
                if (granted == null) {
                    granted = new ArrayList<>(resolved.affixes().stream()
                            .map(StyleAffix::serializedId).toList());
                }
                for (String affix : granting.grantAffixes()) {
                    if (!granted.contains(affix)) {
                        granted.add(affix);
                    }
                }
            }
        }

        Map<String, AffixTuning> ownTuning = entry == null ? Map.of() : entry.affixTuning();
        if (!ownTuning.isEmpty()) {
            if (tuning == null) {
                tuning = new LinkedHashMap<>();
            }
            for (Map.Entry<String, AffixTuning> own : ownTuning.entrySet()) {
                tuning.merge(own.getKey(), own.getValue(),
                        (equipmentTuning, playerTuning) -> playerTuning.mergedUnder(equipmentTuning));
            }
        }

        ResolvedAffixes result = granted == null
                ? resolved
                : ResolvedAffixes.of(StyleEntry.sorted(granted));
        if (result.isEmpty() || tuning == null) {
            return result;
        }
        return result.tunedBy(tuning);
    }

    public static void onSummonHit(Player player, LivingEntity target, float dealtDamage) {
        if (player == null || player.level().isClientSide() || !isPresent(target)) {
            return;
        }
        if (DamageDealer.isDealing() || EquipmentRegistry.isEmpty()) {
            return;
        }
        DamageDealer.beginDealing();
        try {
            applySummonEquipment(player, target, dealtDamage);
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.error("Failed to apply summon-hit equipment", throwable);
        } finally {
            DamageDealer.endDealing();
        }
    }

    private static void dive(Player player, LivingEntity target, SummonDive dive, float dealtDamage,
                             String id) {
        try {
            float shared = dealtDamage * dive.damageShare();
            String ability = dive.ability().orElse(null);
            if (ability != null && !ability.isBlank()) {
                orderDive(player, target, dive, ability, shared);
            } else {
                SummonHooks.dive(player, target, shared, dive.entities(), dive.maxDiving());
            }
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.error("Failed to dive summons for {}", id, throwable);
        }
    }

    private static void orderDive(Player player, LivingEntity target, SummonDive dive,
                                  String ability, float damage) {
        List<ConfigurableSummon> roster = SummonRoster.available(player, dive.targeting());
        int limit = dive.maxDiving() <= 0 ? Integer.MAX_VALUE : dive.maxDiving();
        int sent = 0;
        for (ConfigurableSummon summon : roster) {
            if (sent >= limit) {
                return;
            }
            if (!dive.accepts(entityIdOf(summon))) {
                continue;
            }
            if (SummonOrders.commandOne(player, summon, target, ability, damage,
                    dive.tuning().orElse(null))) {
                sent++;
            }
        }
    }

    private static String entityIdOf(ConfigurableSummon summon) {
        if (!(summon instanceof Entity entity)) {
            return "";
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id == null ? "" : id.toString();
    }

    private static void applySummonEquipment(Player player, LivingEntity target, float dealtDamage) {
        EquipmentContext context = new EquipmentContext(player, null, SWING_ATTACK.get(),
                0, 0, EquipmentRuntime.swingCount(player), !target.isAlive(), true)
                .withTarget(target);
        for (EquipmentRuntime.Match match : EquipmentRuntime.activeFor(player, context, false)) {
            try {
                fireEquipment(player, match, context, true, target, dealtDamage,
                        ResolvedAffixes.EMPTY);
            } catch (Throwable throwable) {
                CombatStyles.LOGGER.error("Failed to apply equipment {} on summon hit",
                        match.id(), throwable);
            }
        }
    }

    private static void applyEquipment(Player player, int comboCount, float base,
                                   ResolvedAffixes resolved, boolean onHit,
                                   LivingEntity target, boolean killed) {
        applyEquipment(player, comboCount, base, resolved, onHit, target, killed, false);
    }

    private record DeferredHit(Player player, EquipmentRuntime.Match match,
                               EquipmentContext context, LivingEntity target, float base,
                               ResolvedAffixes resolved, int comboCount) {
    }

    private static final Map<UUID, List<DeferredHit>> DEFERRED_HITS = new ConcurrentHashMap<>();

    private static void applyEquipment(Player player, int comboCount, float base,
                                   ResolvedAffixes resolved, boolean onHit,
                                   LivingEntity target, boolean killed, boolean missPhase) {
        if (EquipmentRegistry.isEmpty()) {
            return;
        }
        if (missPhase) {
            CombatDebug.phase(player, "miss", "checking worn items");
        }
        boolean swingPass = !onHit && !missPhase;
        boolean perAttack = onHit && !missPhase && EquipmentRuntime.attackOpen(player);
        EquipmentContext context = equipmentContext(player, comboCount, killed)
                .withTarget(target)
                .onHitPass(onHit, onHit ? base : 0.0F);
        if (swingPass) {
            EquipmentRuntime.openSwingWindows(player, context);
        }
        for (EquipmentRuntime.Match match
                : EquipmentRuntime.activeFor(player, context, missPhase, onHit, perAttack)) {
            boolean fired = dispatchEquipment(player, match, context, onHit, target, base,
                    resolved, comboCount, missPhase, true);
            if (fired && swingPass && EquipmentRuntime.procsPerAttack(match)) {
                EquipmentRuntime.recordAttackProc(player, match);
            }
        }
        if (!perAttack) {
            return;
        }
        for (EquipmentRuntime.Match match : EquipmentRuntime.perAttackFor(player, context)) {
            if (swingInProgress(player)) {
                DEFERRED_HITS.computeIfAbsent(player.getUUID(), key -> new ArrayList<>())
                        .add(new DeferredHit(player, match, context, target, base, resolved,
                                comboCount));
                continue;
            }
            if (EquipmentRuntime.procdThisAttack(player, match)) {
                dispatchEquipment(player, match, context, true, target, base, resolved,
                        comboCount, false, false);
            } else {
                CombatDebug.skipped(player, match.id(), "its swing did not proc this attack");
            }
        }
    }

    private static void releaseDeferredHits(Player player) {
        List<DeferredHit> waiting = DEFERRED_HITS.remove(player.getUUID());
        if (waiting == null) {
            return;
        }
        DamageDealer.beginDealing();
        try {
            for (DeferredHit hit : waiting) {
                if (!EquipmentRuntime.procdThisAttack(player, hit.match())) {
                    CombatDebug.skipped(player, hit.match().id(),
                            "its swing did not proc this attack");
                    continue;
                }
                LivingEntity target = hit.target();
                dispatchEquipment(player, hit.match(), hit.context(), true,
                        isPresent(target) ? target : null, hit.base(), hit.resolved(),
                        hit.comboCount(), false, false);
            }
        } finally {
            DamageDealer.endDealing();
        }
    }

    private static boolean dispatchEquipment(Player player, EquipmentRuntime.Match match,
                                             EquipmentContext context, boolean onHit,
                                             LivingEntity target, float base,
                                             ResolvedAffixes resolved, int comboCount,
                                             boolean missPhase, boolean gated) {
        if (!CombatStylesEvents.EQUIPMENT.isEmpty()) {
            CombatStylesEvents.EquipmentEvent event = new CombatStylesEvents.EquipmentEvent(
                    player, match.id(), swingingWeapon(player, comboCount),
                    context.slotIndex(), missPhase);
            if (!CombatStylesEvents.EQUIPMENT.allow(
                    listener -> listener.onEquipmentTrigger(event))) {
                return false;
            }
        }
        try {
            if (!fireEquipment(player, match, context, onHit, target, base, resolved, gated)) {
                return false;
            }
            match.effect().substitution()
                    .filter(StyleSubstitution::inserts)
                    .ifPresent(substitution -> {
                        beginBurst(player, substitution);
                        if (CombatDebug.enabled(player)) {
                            CombatDebug.fired(player, match.id(), "burst of "
                                    + substitution.borrowSlots().size() + " queued");
                        }
                    });
            int repeats = match.effect().resetCombo().orElse(0);
            if (repeats > 0 && !onHit) {
                ComboControl.holdSlot(player, repeats);
            }
            if (context.killed() && match.hears(trigger -> trigger.onKill().orElse(false))) {
                for (String affix : match.effect().grantAffixes()) {
                    PendingGrants.queue(player, affix, 1);
                }
            }
            if (onHit && target != null) {
                match.effect().summonDive().ifPresent(order ->
                        dive(player, target, order, base, match.id()));
            }
            return true;
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.error("Failed to apply equipment {}", match.id(), throwable);
            return false;
        }
    }

    private static boolean fireEquipment(Player player, EquipmentRuntime.Match match,
                                         EquipmentContext context, boolean onHit,
                                         LivingEntity target, float base,
                                         ResolvedAffixes resolved) {
        return fireEquipment(player, match, context, onHit, target, base, resolved, true);
    }

    private static boolean fireEquipment(Player player, EquipmentRuntime.Match match,
                                         EquipmentContext context, boolean onHit,
                                         LivingEntity target, float base,
                                         ResolvedAffixes resolved, boolean gated) {
        TriggeredEffect entry = match.entry();
        AffixEffects.OnHit effects = onHit
                ? match.effect().onHit().orElse(null)
                : match.effect().onSwing().orElse(null);
        boolean selfHere = !entry.selfEffects().isEmpty() && entry.selfEffectsOnHit() == onHit;
        if (gated && entry.cooldown().windowed()) {
            if (EffectWindows.arms(match) && match.trigger().eventFiring(context)) {
                if (EffectWindows.arm(player, match)) {
                    if (CombatDebug.enabled(player)) {
                        CombatDebug.fired(player, match.id(), "armed on "
                                + match.trigger().describe());
                    }
                    syncCooldowns(player, match);
                }
                return false;
            }
            boolean passed = entry.armsOnSwing()
                    ? (onHit ? EffectWindows.armed(player, match.id(), match.key())
                            : EffectWindows.claimSwing(player, match))
                    : EffectWindows.claim(player, match);
            if (!passed) {
                CombatDebug.skipped(player, match.id(), "window not armed");
                return false;
            }
            syncCooldowns(player, match);
        }
        if (effects == null && !selfHere) {
            return true;
        }
        StyleAffix source = EquipmentAffix.of(match);
        if (CombatDebug.enabled(player)) {
            CombatDebug.fired(player, match.id(), "procced on " + match.trigger().describe()
                    + " — " + (effects == null
                            ? "self effects"
                            : payloadOf(match.effect(), effects)));
        }
        syncCooldowns(player, match);
        Runnable fire = () -> {
            if (selfHere) {
                AffixImpacts.applySelfEffects(player, entry.selfEffects());
            }
            if (effects != null) {
                apply(player, target, source, effects, base, resolved);
                if (!effects.isEmpty()) {
                    scheduleEcho(player, target, source, effects, base, resolved,
                            effects.echo().orElse(null));
                }
            }
        };
        if (!CombatScheduler.after(player, entry.delayTicks(), match.id(), fire)) {
            fire.run();
        }
        return true;
    }

    private static void syncCooldowns(Player player, EquipmentRuntime.Match match) {
        if (player instanceof ServerPlayer served) {
            NetworkManager.syncCooldowns(served, match.itemId());
        }
    }

    private static float precedingScale(AffixEffects.Condition condition) {
        AffixEffects.Requires requires = condition.slot().requires();
        if (requires.perPreceding() <= 0.0F) {
            return 1.0F;
        }
        SwingContext context = SWING_CONTEXT.get();
        int preceding = context == null ? 0 : context.precedingStyleIds().size();
        return requires.scaleFor(preceding);
    }

    private static AttackOverride mergeSwingAttributes(AttackOverride override,
                                                       ResolvedAffixes resolved) {
        if (resolved.isEmpty()) {
            return override;
        }
        Map<String, AttackAttribute> merged = new LinkedHashMap<>();
        for (StyleAffix affix : resolved.affixes()) {
            if (affix.onSwing().condition().onMiss().orElse(false)) {
                continue;
            }
            for (AffixEffects.AttackAttributeBonus bonus : affix.onSwing().attackAttributes()) {
                if (bonus.amount() == 0.0D) {
                    continue;
                }
                AttributeModifier.Operation operation =
                        AffixMechanics.operationOf(bonus.operation());
                String key = bonus.attribute();
                AttackAttribute existing = merged.get(key);
                if (existing == null) {
                    merged.put(key, new AttackAttribute(bonus.amount(), operation));
                    continue;
                }
                if (!bonus.stacks() || existing.operation() != operation) {
                    continue;
                }
                merged.put(key, new AttackAttribute(existing.amount() + bonus.amount(), operation));
            }
        }
        if (merged.isEmpty()) {
            return override;
        }
        AttackOverride base = override == null ? AttackOverride.EMPTY : override;
        Map<String, AttackAttribute> combined = new LinkedHashMap<>(merged);
        combined.putAll(base.attributes());
        return base.withAttributes(combined);
    }

    public static AttackOverride attackOverrideFor(Player player, int comboCount) {
        ItemStack weapon = swingingWeapon(player, comboCount);
        StyleSequence sequence;
        SwingHolder.push(player);
        try {
            sequence = SequenceService.sequenceOf(weapon, player.getUUID());
        } finally {
            SwingHolder.pop();
        }
        List<StyleEntry> entries = sequence.entries();
        boolean substituted = SequenceService.SUBSTITUTED.equals(sequence.name());
        if (entries.isEmpty()) {
            if (substituted) {
                return null;
            }
            int nativeSlot = SequenceSlots.slotFor(SequenceSlots.sequenceComboOf(player, comboCount), nativeAttackCount(weapon));
            return SequenceService.attackOverridesOf(weapon).get(String.valueOf(nativeSlot));
        }
        int slot = SequenceSlots.slotFor(SequenceSlots.sequenceComboOf(player, comboCount), entries.size());
        WeaponAttributes.Attack swungAttack = CombatState.swungAttack(player);
        if (swungAttack != null) {
            StylePage at = pageOf(entries.get(slot));
            if (at == null || !matches(at.attack(), swungAttack)) {
                int found = SequenceSlots.indexOfAttack(entries, swungAttack);
                if (found >= 0) {
                    slot = found;
                }
            }
        }
        StyleEntry entry = entries.get(slot);
        AttackOverride direct = entry.attackOverride().orElse(null);
        int itemSlot;
        if (substituted) {
            SwingHolder.push(player);
            try {
                itemSlot = SequenceService.originalSlotFor(weapon, player.getUUID(), slot);
            } finally {
                SwingHolder.pop();
            }
        } else {
            itemSlot = slot;
        }
        AttackOverride fromItem = itemSlot < 0
                ? null
                : SequenceService.attackOverridesOf(weapon).get(String.valueOf(itemSlot));
        AttackOverride merged = direct == null ? fromItem
                : (fromItem == null ? direct : direct.overriddenBy(fromItem));
        StylePage page = StyleRegistry.get(entry.styleId());
        AttackOverride fromStyle = page == null ? null : AttackOverrides.get(page.styleId());
        if (fromStyle == null) {
            return merged;
        }
        return merged == null ? fromStyle : fromStyle.overriddenBy(merged);
    }

    private static EquipmentContext equipmentContext(Player player, int comboCount, boolean killed) {
        ItemStack weapon = swingingWeapon(player, comboCount);
        StyleSequence sequence = SequenceService.sequenceOf(weapon, player.getUUID());
        List<StyleEntry> entries = sequence.entries();
        int size = entries.isEmpty() ? nativeAttackCount(weapon) : entries.size();
        int index = SequenceSlots.slotFor(SequenceSlots.sequenceComboOf(player, comboCount), size);
        AttackHand hand = PlayerAttackHelper.isDualWielding(player)
                ? null
                : PlayerAttackHelper.getCurrentAttack(player, comboCount);
        ComboState combo = hand == null ? null : hand.combo();
        if (combo != null && combo.total() > 0) {
            index = combo.current() - 1;
            size = combo.total();
        }
        StyleEntry entry = entries.isEmpty() || index < 0 || index >= entries.size()
                ? null
                : entries.get(index);
        StyleEntry swung = CombatState.swungEntry(player);
        return new EquipmentContext(player, swung == null ? entry : swung,
                attackOf(player), index, size,
                EquipmentRuntime.swingCount(player), killed);
    }

    private static int swungSlotOf(Player player, int comboCount) {
        List<StyleEntry> entries = SequenceService.sequenceOf(
                swingingWeapon(player, comboCount), player.getUUID()).entries();
        int bySlot = SequenceSlots.slotFor(SequenceSlots.sequenceComboOf(player, comboCount),
                entries.size());
        return SequenceSlots.slotOf(entries, CombatState.swungAttack(player), bySlot);
    }

    private static StyleEntry entryFor(Player player, int comboCount) {
        ItemStack weapon = swingingWeapon(player, comboCount);
        List<StyleEntry> entries = SequenceService.sequenceOf(weapon, player.getUUID()).entries();
        if (entries.isEmpty()) {
            return null;
        }
        int declared = ClientSwing.declaredSlot(player);
        if (declared >= 0 && declared < entries.size()) {
            return entries.get(declared);
        }
        return entries.get(SequenceSlots.slotFor(SequenceSlots.sequenceComboOf(player, comboCount), entries.size()));
    }

    private static WeaponAttributes.Attack nativeAttack(ItemStack stack, int comboCount) {
        WeaponAttributes attributes = WeaponRegistryAccess.rawAttributesFor(stack);
        if (attributes == null || attributes.attacks() == null
                || attributes.attacks().length == 0) {
            return null;
        }
        WeaponAttributes.Attack[] attacks = attributes.attacks();
        return attacks[SequenceSlots.slotFor(comboCount, attacks.length)];
    }

    private static int nativeAttackCount(ItemStack stack) {
        WeaponAttributes attributes = WeaponRegistryAccess.rawAttributesFor(stack);
        if (attributes == null || attributes.attacks() == null) {
            return 0;
        }
        return attributes.attacks().length;
    }

    public static void recordLanded(Player player, float dealtDamage) {
        if (player == null || player.level().isClientSide() || dealtDamage <= 0.0F) {
            return;
        }
        CombatState.recordHit(player);
    }

    private static final ThreadLocal<Integer> ALIVE_TARGET = new ThreadLocal<>();

    public static void markTargetAlive(Entity target) {
        if (target instanceof LivingEntity living
                && living.isAlive() && !living.isRemoved() && living.getHealth() > 0.0F) {
            ALIVE_TARGET.set(living.getId());
        } else {
            ALIVE_TARGET.remove();
        }
    }

    private static boolean wasAliveBeforeHit(LivingEntity living) {
        Integer id = ALIVE_TARGET.get();
        return id != null && living != null && id == living.getId();
    }

    public static void onPlayerHit(Player player, Entity target, float dealtDamage) {
        if (DamageDealer.isDealing() || !(target instanceof LivingEntity living)) {
            return;
        }
        if (player == null || player.level().isClientSide() || dealtDamage <= 0.0F) {
            return;
        }
        SwingHolder.push(player);

        AttackAttributes.disarm(player);
        ResolvedAffixes resolved = affixesForCurrentAttack(player, swingingWeapon(player));
        SWING_CONTEXT.set(enrich(SWING_CONTEXT.get(), player, living));
        HitStreaks.record(player, living);
        CombatState.recordHit(player);
        HitboxSet.Shape claimedShape = HitboxClaims.shapeFor(player, living);
        List<String> boxAffixes = claimedShape == null ? List.of() : claimedShape.affixes();
        if (resolved.isEmpty() && EquipmentRegistry.isEmpty() && boxAffixes.isEmpty()) {
            return;
        }
        List<String> ownAffixes =
                PendingExtraHit.affixesFor(player, CombatState.swungAttack(player));
        if (!boxAffixes.isEmpty()) {
            resolved = ResolvedAffixes.of(boxAffixes, claimedShape.tuning());
            CombatDebug.phase(player, "hit",
                    "hitbox uses its own " + boxAffixes.size() + " affix(es)");
        } else if (!ownAffixes.isEmpty()) {
            resolved = ResolvedAffixes.of(ownAffixes,
                    PendingExtraHit.tuningFor(player, CombatState.swungAttack(player)));
            CombatDebug.phase(player, "hit",
                    "extra hit uses its own " + ownAffixes.size() + " affix(es)");
        } else if (!PendingExtraHit.inheritsAffixes(player, CombatState.swungAttack(player))) {
            CombatDebug.phase(player, "hit", "extra hit does not inherit affixes");
            return;
        }

        boolean aliveBeforeHit = wasAliveBeforeHit(living);

        DamageDealer.beginDealing();
        try {
            if (CombatDebug.enabled(player)) {
                SwingContext at = SWING_CONTEXT.get();
                WeaponAttributes.Attack landed = CombatState.swungAttack(player);
                CombatDebug.phase(player, "hit", "slot "
                        + (at == null ? "?" : CombatDebug.slot(at.slotIndex(), at.slotCount()))
                        + ", " + (landed == null ? "unknown attack" : landed.animation())
                        + ", " + dealtDamage + " damage, "
                        + names(resolved));
            }
            StyleAffix converter = resolved.conversionSource().orElse(null);
            for (StyleAffix affix : resolved.affixes()) {
                if (affix.onHit().isEmpty()) {
                    CombatDebug.skipped(player, affix.serializedId(), "no on_hit block");
                    continue;
                }
                AffixEffects.OnHit effects = affix.onHit();
                if (converter != null && affix != converter) {
                    effects = withoutDamage(effects);
                    if (effects.isEmpty()) {
                        continue;
                    }
                }
                if (!CombatStylesEvents.HIT.isEmpty()) {
                    StyleAffix fired = affix;
                    CombatStylesEvents.HitEvent event = new CombatStylesEvents.HitEvent(
                            player, living, swingingWeapon(player), fired, resolved,
                            SWING_CONTEXT.get(), dealtDamage);
                    if (!CombatStylesEvents.HIT.allow(listener -> listener.onHit(event))) {
                        continue;
                    }
                }
                try {
                    apply(player, living, affix, effects, dealtDamage, resolved);
                    scheduleEcho(player, living, affix, effects, dealtDamage, resolved,
                            wornEcho(player, living, true, dealtDamage));
                } catch (Throwable throwable) {
                    CombatStyles.LOGGER.error("Failed to apply affix {}",
                            affix.serializedId(), throwable);
                }
            }
            SwingContext context = SWING_CONTEXT.get();
            int slotIndex = context == null ? 0 : context.slotIndex();
            boolean killed = aliveBeforeHit
                    && (!living.isAlive() || living.isRemoved()
                        || living.getHealth() <= 0.0F);

            if (killed && !KILLS_THIS_SWING.get().add(living.getId())) {
                killed = false;
            }
            if (killed) {
                CombatDebug.phase(player, "kill", living.getType().toShortString());
            }
            KILLED.set(killed);
            if (killed && !CombatStylesEvents.KILL.isEmpty()) {
                CombatStylesEvents.KillEvent event = new CombatStylesEvents.KillEvent(
                        player, living, swingingWeapon(player), resolved, slotIndex);
                CombatStylesEvents.KILL.forEach(listener -> listener.onKill(event));
            }
            PendingGrants.consume(player);
            applyEquipment(player, slotIndex, dealtDamage, resolved, true, living, killed);
        } finally {
            DamageDealer.endDealing();
            AffixSlashWaves.endSwing();
            KILLED.remove();
        }
    }

    private static String round(double value) {
        return String.valueOf(Math.round(value * 100.0D) / 100.0D);
    }

    private static AffixEffects.OnHit withoutDamage(AffixEffects.OnHit onHit) {
        return new AffixEffects.OnHit(Optional.empty(), List.of(), onHit.statusEffects(), onHit.ignite(),
                onHit.lifesteal(), onHit.knockback(), onHit.splash(), onHit.selfEffects(),
                onHit.sound(), onHit.particles(), onHit.projectiles(), onHit.castSpells(),
                onHit.slashWave(), onHit.mechanics(), onHit.freezeTicks(), onHit.delayTicks(), onHit.echo());
    }

    private static void runGroup(Player player, LivingEntity target, float total,
                                 AffixEffects.Motion motion) {
        if (deferred(player, motion, group -> runGroup(player, target, total, group))) {
            return;
        }
        if (player != null && player.isShiftKeyDown()
                && CombatStylesConfig.get().sneakSuppressesMotion()) {
            CombatDebug.skipped(player, "motion", "sneaking");
            return;
        }
        motion.dash().ifPresent(dash -> {
            boolean connected = AffixMechanics.applyDash(player, dash, target);
            if (!connected || !isPresent(target)) {
                return;
            }
            if (dash.onConnectRatio() > 0.0F) {
                float bonus = total * dash.onConnectRatio();
                if (bonus >= AffixImpacts.MINIMUM_DAMAGE) {
                    DamageHurt.bypassingIFrames(target,
                            player.damageSources().playerAttack(player), bonus);
                }
            }
            if (dash.onConnectKnockback() > 0.0F) {
                Vec3 away = target.position().subtract(player.position()).normalize();
                target.push(away.x * dash.onConnectKnockback(), 0.35D,
                        away.z * dash.onConnectKnockback());
                target.hurtMarked = true;
            }
        });
        motion.blink().ifPresent(blink -> AffixMechanics.applyBlink(
                player, isPresent(target) ? target : null, blink));
    }

    private static boolean isPresent(LivingEntity target) {
        return target != null && target.isAlive();
    }

    private static void runGroup(Player player, LivingEntity target, float total,
                                 AffixEffects.Sustain sustain) {
        if (deferred(player, sustain, group -> runGroup(player, target, total, group))) {
            return;
        }
        sustain.heal().ifPresent(heal -> AffixMechanics.applyHeal(player, heal, total));
        sustain.shield().ifPresent(shield -> AffixMechanics.applyShield(player, shield));
    }

    private static void runGroup(Player player, LivingEntity target, float total,
                                 AffixEffects.Control control, StyleAffix affix) {
        if (deferred(player, control, group -> runGroup(player, target, total, group, affix))) {
            return;
        }
        if (isPresent(target)) {
            control.disarm().ifPresent(disarm ->
                    AffixMechanics.applyDisarm(player, target, disarm));
        }
        control.vault().ifPresent(vault -> AffixMechanics.applyVault(
                player, isPresent(target) ? target : player, vault));
        control.hasten().ifPresent(hasten -> {
            if (!EffectChance.rolls(player, hasten.chance())) {
                return;
            }
            WeaponAttributes.Attack swung = CombatState.swungAttack(player);
            String hitbox = swung == null || swung.hitbox() == null
                    ? null
                    : swung.hitbox().name();
            if (hasten.thisSwing() != 1.0F) {
                SwingTiming.INSTANCE.scaleThisSwing(player, hasten.thisSwing());
            }
            SwingHaste.grant(player, hasten.ratio(), hasten.swings(), hitbox);
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkManager.sendSwingHaste(serverPlayer,
                        SwingHastePayload.of(hasten.ratio(), hasten.swings(), hitbox));
            }
            if (CombatDebug.enabled(player)) {
                CombatDebug.fired(player, affix == null ? "hasten" : affix.serializedId(),
                        "next " + (hitbox == null ? "attack" : hitbox)
                                + " runs at " + Math.round(hasten.ratio() * 100) + "% cooldown");
            }
        });
    }

    private static void runGroup(Player player, LivingEntity target, float total,
                                 AffixEffects.Offense offense, DamageSource source) {
        if (offense.cleave().isEmpty()) {
            return;
        }
        if (deferred(player, offense, group -> runGroup(player, target, total, group, source))) {
            return;
        }
        if (isPresent(target)) {
            offense.cleave().ifPresent(cleave ->
                    AffixMechanics.applyCleave(player, target, total, cleave, source));
        }
    }

    private static void runGroup(Player player, AffixEffects.Spawn spawn, LivingEntity target) {
        runGroup(player, spawn, target, 0.0F);
    }

    private static void runGroup(Player player, AffixEffects.Spawn spawn, LivingEntity target,
                                 float dealtDamage) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (deferred(player, spawn, group -> runGroup(player, group, target, dealtDamage))) {
            return;
        }
        AffixMechanics.applySummons(level, player, target, spawn.summons(), dealtDamage);
        AffixMechanics.runCommands(level, player, spawn.commands());
    }

    private static void runGroup(Player player, AffixEffects.Order order, LivingEntity target,
                                 float dealtDamage) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (deferred(player, order, group -> runGroup(player, group, target, dealtDamage))) {
            return;
        }
        AffixMechanics.applyOrder(level, player, target, order, dealtDamage);
    }

    private static String names(ResolvedAffixes resolved) {
        if (resolved.affixes().isEmpty()) {
            return "no affixes";
        }
        List<String> found = new ArrayList<>();
        for (StyleAffix affix : resolved.affixes()) {
            found.add(affix.serializedId().replace("combat_styles:", ""));
        }
        return String.join(", ", found);
    }

    private static String payloadOf(EquipmentEffect effect, AffixEffects.OnHit hook) {
        List<String> parts = new ArrayList<>();
        hook.allDamage().forEach(damage -> parts.add(damage.amplifies()
                ? "amplify " + Math.round(damage.ratio() * 100.0F) + "%"
                : "damage " + Math.round(damage.ratio() * 100.0F) + "%"));
        if (!hook.statusEffects().isEmpty()) {
            parts.add(hook.statusEffects().size() + " status effect(s)");
        }
        if (!hook.attributes().isEmpty()) {
            parts.add(hook.attributes().size() + " attribute(s)");
        }
        hook.mechanics().sustain().heal().ifPresent(heal -> parts.add("heal"));
        hook.mechanics().sustain().shield().ifPresent(shield -> parts.add("shield"));
        hook.ignite().ifPresent(ignite -> parts.add("ignite"));
        hook.lifesteal().ifPresent(steal -> parts.add("lifesteal"));
        hook.splash().ifPresent(splash -> parts.add("splash"));
        hook.chain().ifPresent(chain -> parts.add("chain"));
        hook.cleave().ifPresent(cleave -> parts.add("cleave"));
        if (!hook.mechanics().order().ability().isEmpty()) {
            parts.add("orders summons");
        }
        if (!effect.grantAffixes().isEmpty()) {
            parts.add("grants " + effect.grantAffixes().size() + " affix(es)");
        }
        return parts.isEmpty() ? "no payload" : String.join(", ", parts);
    }

    private static AffixEffects.Echo wornEcho(Player player, LivingEntity target, boolean onHit,
                                              float dealt) {
        if (EquipmentRegistry.isEmpty() || ECHOING.get()) {
            return null;
        }
        for (EquipmentRuntime.Match match : EquipmentRuntime.previewFor(player,
                previewContext(player, target, onHit, dealt), onHit)) {
            AffixEffects.Echo echo = match.effect().onHit()
                    .flatMap(AffixEffects.OnHit::echo)
                    .or(match.effect()::echo)
                    .orElse(null);
            if (echo != null && echo.times() > 0) {
                return echo;
            }
        }
        return null;
    }

    private static List<StyleAffix> equipmentAmplifiers(Player player, LivingEntity target,
                                                        float amount) {
        if (EquipmentRegistry.isEmpty() || ECHOING.get()) {
            return List.of();
        }
        List<StyleAffix> found = new ArrayList<>();
        for (EquipmentRuntime.Match match : EquipmentRuntime.previewFor(player,
                previewContext(player, target, true, amount), true)) {
            AffixEffects.OnHit onHit = match.effect().onHit().orElse(null);
            if (onHit == null) {
                continue;
            }
            boolean amplifies = onHit.allDamage().stream()
                    .anyMatch(AffixEffects.Damage::amplifies);
            if (amplifies || !onHit.momentum().isEmpty()) {
                found.add(EquipmentAffix.of(match));
            }
        }
        return found;
    }

    private static EquipmentContext previewContext(Player player, LivingEntity target,
                                                   boolean onHit, float dealt) {
        SwingContext context = SWING_CONTEXT.get();
        return equipmentContext(player, context == null ? 0 : context.slotIndex(), false)
                .withTarget(target)
                .onHitPass(onHit, onHit ? dealt : 0.0F);
    }
    private static void scheduleEcho(Player player, LivingEntity target, StyleAffix affix,
                                     AffixEffects.OnHit effects, float base,
                                     ResolvedAffixes resolved,
                                     AffixEffects.Echo echo) {
        if (echo == null || echo.times() <= 0) {
            return;
        }
        int gap = Math.max(1, echo.delayTicks());
        WeaponAttributes.Attack origin = currentAttack(player);
        if (origin == null) {
            origin = SWING_ATTACK.get();
        }
        SwingContext context = SWING_CONTEXT.get();
        WeaponAttributes.Attack captured = origin;
        for (int repeat = 1; repeat <= echo.times(); repeat++) {
            float scaled = echo.damageSpec().isPresent()
                    ? echo.damageSpec().get().amountFor(player, base)
                    : base * echo.ratio();
            schedule(player, gap * repeat, () -> {
                WeaponAttributes.Attack previous = SWING_ATTACK.get();
                SwingContext previousContext = SWING_CONTEXT.get();
                if (captured != null) {
                    SWING_ATTACK.set(captured);
                }
                if (context != null) {
                    SWING_CONTEXT.set(context);
                }
                ECHOING.set(Boolean.TRUE);
                try {
                    apply(player, isPresent(target) ? target : null, affix,
                            effects, scaled, resolved);
                } finally {
                    ECHOING.set(Boolean.FALSE);
                    if (previous == null) {
                        SWING_ATTACK.remove();
                    } else {
                        SWING_ATTACK.set(previous);
                    }
                    if (previousContext == null) {
                        SWING_CONTEXT.remove();
                    } else {
                        SWING_CONTEXT.set(previousContext);
                    }
                }
            });
        }
    }

    private static <T extends AffixEffects.Delayed<T>> boolean deferred(
            Player player, T group, Consumer<T> action) {
        if (group.isEmpty()) {
            return true;
        }
        return schedule(player, group.delayTicks(), () -> action.accept(group.withoutDelay()));
    }

    private static boolean schedule(Player player, int delayTicks, Runnable action) {
        if (delayTicks <= 0) {
            return false;
        }
        PENDING_GROUPS.add(new PendingGroup(player,
                player.level().getGameTime() + delayTicks, action));
        return true;
    }

    public static void applyHurtReaction(Player player, LivingEntity attacker,
                                        StyleAffix affix, AffixEffects.OnHit onHit,
                                        float amount) {
        apply(player, attacker, affix, onHit, amount, ResolvedAffixes.EMPTY);
    }

    private static final ThreadLocal<ResolvedAffixes> ATTACK_AFFIXES = new ThreadLocal<>();

    public static void withAttackAffixes(ResolvedAffixes resolved, Runnable action) {
        ResolvedAffixes previous = ATTACK_AFFIXES.get();
        ATTACK_AFFIXES.set(resolved == null ? ResolvedAffixes.EMPTY : resolved);
        try {
            action.run();
        } finally {
            if (previous == null) {
                ATTACK_AFFIXES.remove();
            } else {
                ATTACK_AFFIXES.set(previous);
            }
        }
    }

    public static List<DamageBonus> attackBonuses(Player player, DamageSource source) {
        ResolvedAffixes resolved = ATTACK_AFFIXES.get();
        if (resolved == null && source.is(DamageTypes.PLAYER_ATTACK)
                && source.getEntity() == player) {
            resolved = affixesForCurrentAttack(player, swingingWeapon(player));
        }
        if (resolved == null || resolved.isEmpty()) {
            return List.of();
        }
        List<DamageBonus> bonuses = new ArrayList<>();
        for (StyleAffix affix : resolved.affixes()) {
            bonuses.addAll(affix.damageBonus());
        }
        return bonuses;
    }

    private static void apply(Player player, LivingEntity target, StyleAffix affix,
                              AffixEffects.OnHit onHit, float dealtDamage, ResolvedAffixes resolved) {
        withAttackAffixes(resolved,
                () -> applyPayload(player, target, affix, onHit, dealtDamage, resolved));
    }

    private static void applyPayload(Player player, LivingEntity target, StyleAffix affix,
                                     AffixEffects.OnHit onHit, float dealtDamage,
                                     ResolvedAffixes resolved) {
        if (target == null) {
            applyUntargeted(player, affix, onHit, dealtDamage, resolved);
            return;
        }

        if (!AffixMechanics.conditionMet(onHit.condition(), player, target, swingContext())) {
            CombatDebug.skipped(player, affix.serializedId(), "condition not met");
            return;
        }

        boolean targetWasAlive = wasAliveBeforeHit(target);

        boolean echoing = Boolean.TRUE.equals(ECHOING.get());
        if (!echoing && onHit.cooldown().isPresent()
                && AffixMechanics.onCooldown(player, affix, onHit.cooldown().get(),
                        swingContext().slotIndex())) {
            CombatDebug.skipped(player, affix.serializedId(), "on cooldown");
            return;
        }

        if (!echoing
                && AffixMechanics.targetLimitReached(player, affix, onHit.mechanics())) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.skipped(player, affix.serializedId(),
                        "already acted on " + onHit.mechanics().maxTargetsPerSwing()
                                + " target(s) this swing");
            }
            return;
        }
        if (onHit.cost().isPresent() && !AffixMechanics.payCost(player, onHit.cost().get())) {
            return;
        }
        if (!echoing) {
            onHit.cooldown().ifPresent(cooldown ->
                    AffixMechanics.startCooldown(player, affix, cooldown,
                            swingContext().slotIndex()));
        }
        if (onHit.condition().refundSlot()) {
            ComboControl.holdSlot(player);
        }
        AffixMechanics.applyAttributes(player, target, affix, onHit.attributes());

        float scaled = dealtDamage * onHit.condition().multiplier()
                * precedingScale(onHit.condition());
        for (AffixEffects.Momentum momentum : onHit.momentum().stream().toList()) {
            scaled *= AffixMechanics.currentMomentum(player, momentum);
        }

        scaled *= (float) affix.damageMultiplier();

        float elemental = 0.0F;
        for (AffixEffects.Damage entry : onHit.allDamage()) {
            if (!entry.amplifies() || echoing) {
                elemental += AffixImpacts.dealDamage(player, target, entry, scaled);
            }
        }

        float total = scaled + elemental;
        if (elemental > 0.0F) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.fired(player, affix.serializedId(),
                        "extra " + round(elemental) + " damage (hit was " + round(dealtDamage) + ")");
            }
        } else if (onHit.allDamage().stream().noneMatch(AffixEffects.Damage::amplifies)) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, affix.serializedId().replace("combat_styles:", "")
                        + " ran, no damage of its own");
            }
        }

        onHit.execute().ifPresent(execute -> {
            if (execute.instantKill() && target.isAlive()
                    && AffixMechanics.executeBonus(execute, target, total) > 0.0F) {
                target.kill();
            }
            if (execute.repeatOnKill() && targetWasAlive && !target.isAlive()) {
                ComboControl.holdSlot(player);
            }
        });

        onHit.chain().ifPresent(chain -> {
            float amount = DamageSpec.resolve(chain.damageSpec(), player, total, chain.ratio());

            DamageSource source = AffixImpacts.spreadSourceFor(player, onHit, chain);
            List<LivingEntity> links = AffixMechanics.chainTargets(player, target, chain);
            int struck = 0;
            for (LivingEntity link : links) {
                if (amount < AffixImpacts.MINIMUM_DAMAGE) {
                    break;
                }
                DamageDealer.dealSpec(player, link, chain.damageSpec().orElse(null), amount, source);
                amount *= chain.falloff();
                struck++;
            }
            if (struck > 0) {
                if (CombatDebug.enabled(player)) {
                    CombatDebug.fired(player, affix.serializedId(),
                            "arced to " + struck + " target(s)");
                }
            } else {
                if (CombatDebug.enabled(player)) {
                    CombatDebug.skipped(player, affix.serializedId(),
                            "chain found nobody within " + chain.range() + " blocks");
                }
            }
        });

        AffixEffects.Mechanics mechanics = onHit.mechanics();
        if (mechanics.motion().dash().isPresent()) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "dash from ON-HIT path (" + affix.serializedId() + ")");
            }
        }
        runGroup(player, target, total, mechanics.motion());
        runGroup(player, target, total, mechanics.sustain());
        runGroup(player, target, total, mechanics.control(), affix);
        runGroup(player, target, total, mechanics.offense(),
                mechanics.offense().cleave()
                        .map(cleave -> AffixImpacts.spreadSourceFor(player, onHit, cleave))
                        .orElse(null));

        onHit.ignite().ifPresent(ignite -> {
            if (EffectChance.rolls(player, ignite.chance())) {
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(),
                        ignite.seconds() * AffixImpacts.SECOND_IN_TICKS));
            }
        });

        if (onHit.freezeTicks() > 0.0F) {
            target.setTicksFrozen((int) Math.max(target.getTicksFrozen(), onHit.freezeTicks()));
        }

        for (AffixEffects.StatusEffect effect : onHit.statusEffects()) {
            if (!EffectChance.rolls(player, effect.chance())) {
                continue;
            }
            AffixImpacts.mobEffect(effect.effect()).ifPresent(holder -> AffixMechanics.applyStatus(
                    target, holder, effect.duration(), effect.amplifier(),
                    effect.stacks(), effect.maxAmplifier()));
        }

        for (AffixEffects.SelfEffect effect : onHit.selfEffects()) {
            AffixImpacts.mobEffect(effect.effect()).ifPresent(holder -> AffixMechanics.applyStatus(
                    player, holder, effect.duration(), effect.amplifier(),
                    effect.stacks(), effect.maxAmplifier()));
        }

        onHit.lifesteal().ifPresent(lifesteal -> {
            if (EffectChance.rolls(player, lifesteal.chance())) {
                player.heal(DamageSpec.resolve(lifesteal.damageSpec(), player, total, lifesteal.ratio()));
            }
        });

        onHit.knockback().ifPresent(knockback -> {
            Vec3 direction = target.position().subtract(player.position()).normalize();
            if (knockback.pull()) {
                direction = direction.reverse();
            }
            target.push(direction.x * knockback.strength(), 0.1D, direction.z * knockback.strength());
            target.hurtMarked = true;
        });

        onHit.splash().ifPresent(splash ->
                AffixImpacts.applySplash(player, target, affix, splash, total,
                        AffixImpacts.spreadSourceFor(player, onHit, splash)));

        onHit.sound().ifPresent(sound -> {
            ResourceLocation soundId = ResourceLocation.tryParse(sound.id());
            if (soundId == null) {
                return;
            }
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(soundId);
            if (event != null) {
                player.level().playSound(null, target.blockPosition(), event, SoundSource.PLAYERS,
                        sound.volume(), sound.pitch());
            }
        });

        if (player.level() instanceof ServerLevel serverLevel) {
            for (AffixEffects.Particles particles : onHit.particles()) {
                AffixImpacts.spawnParticles(serverLevel, target, particles);
            }
            for (AffixEffects.Projectile projectile : onHit.projectiles()) {
                AffixProjectiles.spawn(serverLevel, player, target, projectile, total);
            }
            for (CastSpell cast : onHit.castSpells()) {
                SpellCasts.cast(player, target, cast, total);
            }
            onHit.slashWave().ifPresent(wave ->
                    AffixSlashWaves.queue(serverLevel, player, affix, wave, total, resolved));
            runGroup(player, onHit.mechanics().spawn(), target, total);
            runGroup(player, onHit.mechanics().order(), target, total);
        }
    }

    private static void applyUntargeted(Player player, StyleAffix affix, AffixEffects.OnHit onHit,
                                        float base, ResolvedAffixes resolved) {
        if (onHit.condition().needsTarget()) {
            CombatDebug.skipped(player, affix.serializedId(),
                    "condition needs a target but this swing hit nothing");
            return;
        }
        if (!AffixMechanics.conditionMet(onHit.condition(), player, null, swingContext())) {
            CombatDebug.skipped(player, affix.serializedId(), "condition not met");
            return;
        }
        boolean echoing = Boolean.TRUE.equals(ECHOING.get());
        if (!echoing && onHit.cooldown().isPresent()
                && AffixMechanics.onCooldown(player, affix, onHit.cooldown().get(),
                        swingContext().slotIndex())) {
            return;
        }
        if (onHit.cost().isPresent() && !AffixMechanics.payCost(player, onHit.cost().get())) {
            return;
        }
        if (!echoing) {
            onHit.cooldown().ifPresent(cooldown ->
                    AffixMechanics.startCooldown(player, affix, cooldown,
                            swingContext().slotIndex()));
        }
        AffixMechanics.applyAttributes(player, null, affix, onHit.attributes());
        if (onHit.mechanics().motion().dash().isPresent()) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.note(player, "dash from ON-SWING path (" + affix.serializedId() + ")");
            }
        }
        if (!attackDeclaresMotion(player)) {
            runGroup(player, null, base, onHit.mechanics().motion());
        }
        runGroup(player, null, base, onHit.mechanics().sustain());
        runGroup(player, null, base, onHit.mechanics().control(), affix);

        for (AffixEffects.SelfEffect effect : onHit.selfEffects()) {
            AffixImpacts.mobEffect(effect.effect()).ifPresent(holder -> player.addEffect(
                    new MobEffectInstance(holder, effect.duration(), effect.amplifier())));
        }

        onHit.sound().ifPresent(sound -> {
            ResourceLocation soundId = ResourceLocation.tryParse(sound.id());
            if (soundId == null) {
                return;
            }
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(soundId);
            if (event != null) {
                player.level().playSound(null, player.blockPosition(), event, SoundSource.PLAYERS,
                        sound.volume(), sound.pitch());
            }
        });

        AffixImpacts.applyAreaEffects(player, affix, onHit, base);

        if (player.level() instanceof ServerLevel serverLevel) {
            for (AffixEffects.Particles particles : onHit.particles()) {
                AffixImpacts.spawnParticles(serverLevel, player, particles);
            }
            for (AffixEffects.Projectile projectile : onHit.projectiles()) {
                AffixProjectiles.spawn(serverLevel, player, null, projectile, base);
            }
            for (CastSpell cast : onHit.castSpells()) {
                SpellCasts.cast(player, null, cast, base);
            }
            onHit.slashWave().ifPresent(wave ->
                    AffixSlashWaves.queue(serverLevel, player, affix, wave, base, resolved));
            runGroup(player, onHit.mechanics().spawn(), null);
            runGroup(player, onHit.mechanics().order(), null, 0.0F);
        }
    }

    static WeaponAttributes.Attack attackOf(Player player) {
        WeaponAttributes.Attack resolved = SWING_ATTACK.get();
        if (resolved != null) {
            return resolved;
        }
        WeaponAttributes.Attack swung = CombatState.swungAttack(player);
        return swung == null ? currentAttack(player) : swung;
    }

    static WeaponAttributes.Attack swingAttackOf(Player player) {
        if (Boolean.TRUE.equals(ECHOING.get())) {
            WeaponAttributes.Attack captured = SWING_ATTACK.get();
            if (captured != null) {
                return captured;
            }
        }
        WeaponAttributes.Attack attack = currentAttack(player);
        return attack == null ? SWING_ATTACK.get() : attack;
    }

    private static SwingContext swingContext() {
        SwingContext context = SWING_CONTEXT.get();
        return context == null ? SwingContext.UNKNOWN : context;
    }

    private static final ThreadLocal<Boolean> RESOLVING_ATTACK =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static WeaponAttributes.Attack currentAttack(Player player) {
        if (!(player instanceof EntityPlayer_BetterCombat holder)
                || RESOLVING_ATTACK.get()) {
            return null;
        }
        RESOLVING_ATTACK.set(Boolean.TRUE);
        try {
            AttackHand hand = holder.getCurrentAttack();
            return hand == null ? null : hand.attack();
        } finally {
            RESOLVING_ATTACK.set(Boolean.FALSE);
        }
    }

    public static ItemStack swingingWeapon(Player player, int comboCount) {
        if (!player.level().isClientSide()) {
            AttackHand swung = CombatState.swungHand(player);
            if (swung != null) {
                ItemStack held = swung.itemStack();
                if (held != null && !held.isEmpty() && stillCarried(player, held)) {
                    return held;
                }
            }
        }
        return SequenceSlots.swungWithOffHand(player, comboCount)
                ? player.getOffhandItem()
                : player.getMainHandItem();
    }

    private static boolean stillCarried(Player player, ItemStack held) {
        return held.is(player.getMainHandItem().getItem())
                || held.is(player.getOffhandItem().getItem());
    }

    public static ItemStack swingingWeapon(Player player) {
        if (player instanceof EntityPlayer_BetterCombat holder) {
            AttackHand hand = holder.getCurrentAttack();
            if (hand != null) {
                ItemStack held = hand.itemStack();
                if (held != null && !held.isEmpty()) {
                    return held;
                }
                return hand.isOffHand() ? player.getOffhandItem() : player.getMainHandItem();
            }
        }
        return player.getMainHandItem();
    }

    private static boolean matches(WeaponAttributes.Attack a, WeaponAttributes.Attack b) {
        return SubstitutionScope.sameAttack(a, b);
    }
}
