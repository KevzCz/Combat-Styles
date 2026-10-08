package net.pixeldreamstudios.combat_styles.api;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.equipment.SummonProfiles;
import net.pixeldreamstudios.combat_styles.DataIntegrity;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.affix.CombatEffectDealer;
import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.damage.DamageHurt;
import net.pixeldreamstudios.combat_styles.affix.HitStreaks;
import net.pixeldreamstudios.combat_styles.affix.AffixMechanics;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.ResolvedAffixes;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.api.event.CombatStylesEvents;
import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.config.AbilityConfig;
import net.pixeldreamstudios.combat_styles.effect.EffectLookup;
import net.pixeldreamstudios.combat_styles.entity.ConfigurableSummon;
import net.pixeldreamstudios.combat_styles.entity.SummonRoster;
import net.pixeldreamstudios.combat_styles.entity.SummonOrders;
import net.pixeldreamstudios.combat_styles.entity.SummonTargeting;
import net.pixeldreamstudios.combat_styles.equipment.AccessoryCompat;
import net.pixeldreamstudios.combat_styles.equipment.CombatState;
import net.pixeldreamstudios.combat_styles.equipment.ClientSwing;
import net.pixeldreamstudios.combat_styles.equipment.ComboControl;
import net.pixeldreamstudios.combat_styles.equipment.DodgeState;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentProviders;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRuntime;
import net.pixeldreamstudios.combat_styles.equipment.SubstitutionScope;
import net.pixeldreamstudios.combat_styles.item.StylePageItem;
import net.pixeldreamstudios.combat_styles.loot.WeaponRolls;
import net.pixeldreamstudios.combat_styles.progression.StyleProgression;
import net.pixeldreamstudios.combat_styles.style.BudgetModifiers;
import net.pixeldreamstudios.combat_styles.style.BudgetSources;
import net.pixeldreamstudios.combat_styles.style.CategoryAffinity;
import net.pixeldreamstudios.combat_styles.style.DefaultStyles;
import net.pixeldreamstudios.combat_styles.style.InnateAffixes;
import net.pixeldreamstudios.combat_styles.style.SequenceComposer;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleCost;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class CombatStylesAPI {
    private CombatStylesAPI() {
    }

    public static void onSwing(CombatStylesEvents.SwingListener listener) {
        CombatStylesEvents.SWING.register(listener);
    }

    public static void onHit(CombatStylesEvents.HitListener listener) {
        CombatStylesEvents.HIT.register(listener);
    }

    public static void onWave(CombatStylesEvents.WaveListener listener) {
        CombatStylesEvents.WAVE.register(listener);
    }

    public static void onSequenceChanged(CombatStylesEvents.SequenceListener listener) {
        CombatStylesEvents.SEQUENCE_CHANGED.register(listener);
    }

    public static void onMiss(CombatStylesEvents.MissListener listener) {
        CombatStylesEvents.MISS.register(listener);
    }

    public static void onKill(CombatStylesEvents.KillListener listener) {
        CombatStylesEvents.KILL.register(listener);
    }

    public static void onEquipmentTrigger(CombatStylesEvents.EquipmentListener listener) {
        CombatStylesEvents.EQUIPMENT.register(listener);
    }

    public static StyleSequence baselineOf(ItemStack stack) {
        return SequenceService.baselineOf(stack);
    }

    public static StyleSequence rolledSequenceOf(ItemStack stack) {
        return SequenceService.rolledSequenceOf(stack);
    }

    public static void setRolledSequence(ItemStack stack, StyleSequence sequence) {
        SequenceService.storeRolled(stack, sequence);
    }

    public static boolean rollWeapon(ItemStack stack, RandomSource random) {
        return WeaponRolls.applyForced(stack, random);
    }

    public static boolean hasWeaponRolls() {
        return !WeaponRolls.isEmpty();
    }

    public static StylePage scaledForWeapon(StylePage page, String weaponCategory) {
        return CategoryAffinity.scaled(page, weaponCategory);
    }

    public static CategoryAffinity.Tier affinityOf(String styleCategory, String weaponCategory) {
        return CategoryAffinity.tierOf(styleCategory, weaponCategory);
    }

    public static double costOnWeapon(StylePage page, String weaponCategory) {
        return StyleCost.onWeapon(page, weaponCategory);
    }

    public static List<StylePage> allStyles() {
        return StyleRegistry.all();
    }

    public static List<StylePage> stylesForCategory(String category) {
        return StyleRegistry.forCategory(category);
    }

    public static List<String> categories() {
        return StyleRegistry.categories();
    }

    public static StylePage style(String styleId) {
        return StyleRegistry.get(styleId);
    }

    public static boolean isKnownStyle(String styleId) {
        return StyleRegistry.contains(styleId);
    }

    public static StyleSequence sequenceOf(ItemStack stack) {
        return SequenceService.sequenceOf(stack);
    }

    public static boolean hasSequence(ItemStack stack) {
        return SequenceService.hasSequence(stack);
    }

    public static void applySequence(ItemStack stack, StyleSequence sequence) {
        SequenceService.store(stack, sequence);
    }

    public static StyleSequence sequenceOf(String name, List<String> styleIds) {
        return StyleSequence.ofIds(name, styleIds);
    }

    public static List<WeaponAttributes.Sound> availableSwingSounds() {
        return StyleRegistry.swingSounds();
    }

    public static List<StylePage> nativeNodesOf(ItemStack stack) {
        return StyleRegistry.nodesOf(SequenceService.baseAttributes(stack));
    }

    public static void registerDefaultSequence(ResourceLocation itemId, StyleSequence sequence) {
        DefaultStyles.register(itemId, sequence);
    }

    public static StyleSequence defaultSequenceOf(ItemStack stack) {
        return DefaultStyles.forStack(stack);
    }

    public static void registerSlotLimit(ResourceLocation itemId, int slots) {
        DefaultStyles.registerSlotLimit(itemId, slots);
    }

    public static int slotLimitOf(ItemStack stack) {
        return DefaultStyles.slotLimitFor(stack);
    }

    public static void setSlotLimit(ItemStack stack, int slots) {
        if (stack != null && !stack.isEmpty() && slots > 0) {
            stack.set(ModComponents.SLOT_LIMIT.get(), slots);
        }
    }

    public static void setLocked(ItemStack stack, boolean locked) {
        SequenceService.setLocked(stack, locked);
    }

    public static boolean isLocked(ItemStack stack) {
        return SequenceService.isLocked(stack);
    }

    public static boolean usesDefaultSequence(ItemStack stack) {
        return SequenceService.usesDefault(stack);
    }

    public static int learnedCopies(Player player, String styleId) {
        return StyleProgression.copies(player, styleId);
    }

    public static void registerBudgetModifier(BudgetModifier modifier) {
        BudgetModifiers.register(modifier);
    }

    public static double budgetFor(LivingEntity holder, ItemStack stack) {
        return BudgetSources.totalFor(holder, stack);
    }

    public static void registerEquipment(EquipmentInfo provider) {
        EquipmentProviders.register(provider);
    }

    public static boolean isEquipmentSlotAvailable() {
        return AccessoryCompat.isLoaded();
    }

    public static List<ItemStack> equippedAccessories(Player player) {
        return AccessoryCompat.equipped(player);
    }

    public static List<? extends AffixInfo> allAffixes() {
        return AffixRegistry.all();
    }

    public static AffixInfo affix(String affixId) {
        return AffixRegistry.get(affixId);
    }

    public static List<? extends AffixInfo> affixesFor(StylePage page) {
        return AffixRegistry.applicableTo(page);
    }

    public static AffixInfo affixOnCurrentAttack(Player player, ItemStack stack) {
        return CombatEffectDealer.affixForCurrentAttack(player, stack);
    }

    public static ItemStack affixedPage(String styleId, String affixId) {
        return StylePageItem.of(styleId, affixId);
    }

    public static String entryKey(String styleId, String affixId) {
        return StyleEntry.key(StyleRegistry.representativeOf(styleId), affixId);
    }

    public static StylePage styleByAnimation(String animation, String category) {
        return StyleRegistry.byAnimation(animation, category);
    }

    public static List<? extends AffixInfo> resolveAffixes(List<String> affixIds) {
        return ResolvedAffixes.of(affixIds).affixes();
    }

    public static List<? extends AffixInfo> affixesOnCurrentAttack(Player player, ItemStack stack) {
        return CombatEffectDealer.affixesForCurrentAttack(player, stack).affixes();
    }

    public static boolean hurtBypassingIFrames(LivingEntity target, DamageSource source, float amount) {
        return DamageHurt.bypassingIFrames(target, source, amount);
    }

    public static StyleSequence withSwingSound(StyleSequence sequence, int index, String soundId) {
        return sequence.withSound(index, soundId);
    }

    public static void clearSequence(ItemStack stack) {
        SequenceService.clear(stack);
    }

    public static boolean supportsSequences(ItemStack stack) {
        return SequenceService.isSupported(stack);
    }

    public static String weaponCategory(ItemStack stack) {
        return SequenceService.categoryOf(stack);
    }

    public static WeaponAttributes baseAttributes(ItemStack stack) {
        return SequenceService.baseAttributes(stack);
    }

    public static WeaponAttributes composedAttributes(ItemStack stack) {
        WeaponAttributes base = SequenceService.baseAttributes(stack);
        return base == null ? null : SequenceComposer.compose(base,
                SequenceService.sequenceOf(stack),
                SequenceService.attackOverridesOf(stack),
                DefaultStyles.nativeStyleIdsOf(stack));
    }

    public static double costOf(StylePage page) {
        return StyleCost.of(page);
    }

    public static double costOf(StyleSequence sequence) {
        return StyleCost.totalOf(sequence);
    }

    public static double budgetOf(ItemStack stack) {
        return StyleCost.budgetOf(SequenceService.baseAttributes(stack));
    }

    public static boolean fitsBudget(ItemStack stack, StyleSequence sequence) {
        return StyleCost.fitsBudget(sequence, SequenceService.baseAttributes(stack));
    }

    public static boolean hasLearned(Player player, String styleId) {
        return StyleProgression.hasLearned(player, styleId);
    }

    public static boolean learn(Player player, String styleId) {
        boolean learned = StyleProgression.learn(player, styleId);
        if (learned && player instanceof ServerPlayer served) {
            NetworkManager.syncLearned(served);
        }
        return learned;
    }

    public static Map<String, Integer> learnedStyles(Player player) {
        return StyleProgression.all(player);
    }

    public static int learnedCount(Player player) {
        return StyleProgression.count(player);
    }

    public static SequenceComposer.SequenceStats statsOf(StyleSequence sequence) {
        return SequenceComposer.stats(sequence);
    }
    public static List<EquipmentDefinition> allEquipment() {
        return EquipmentRegistry.all();
    }

    public static EquipmentDefinition equipment(String id) {
        return EquipmentRegistry.get(id);
    }

    public static boolean isKnownEquipment(String id) {
        return EquipmentRegistry.contains(id);
    }

    public static boolean isKnownAffix(String affixId) {
        return AffixRegistry.contains(affixId);
    }

    public static boolean lastSwingMissed(Player player) {
        return CombatState.lastSwingMissed(player);
    }

    public static boolean schedule(Player player, int delayTicks, String label,
                                   Runnable action) {
        return CombatScheduler.after(player, delayTicks, label, action);
    }

    public static void runDelayed(Player player, int delayTicks, String label,
                                  Runnable action) {
        if (!CombatScheduler.after(player, delayTicks, label, action) && action != null) {
            action.run();
        }
    }

    public static long idleTicks(Player player) {
        return CombatState.idleTicks(player);
    }

    public static void refundComboSlot(Player player) {
        ComboControl.holdSlot(player);
    }

    public static WeaponAttributes.Attack swungAttack(Player player) {
        return CombatState.swungAttack(player);
    }

    public static AttackHand swungHand(Player player) {
        return CombatState.swungHand(player);
    }

    public static boolean sprintingAtSwing(Player player) {
        return CombatState.sprintingAtSwing(player);
    }

    public static boolean sameAttack(WeaponAttributes.Attack a, WeaponAttributes.Attack b) {
        return SubstitutionScope.sameAttack(a, b);
    }

    public static String declaredAnimation(Player player) {
        return ClientSwing.animationOf(player);
    }

    public static EquipmentDefinition substitutingFor(Player player, ItemStack weapon) {
        return SubstitutionScope.activeFor(player, weapon);
    }

    public static boolean substitutionSuppressed() {
        return SubstitutionScope.suppressed();
    }

    public static <T> T withoutSubstitution(Supplier<T> action) {
        return SubstitutionScope.withoutSubstitution(action);
    }

    public static AttackHand queuedAttack(Player player, int comboCount) {
        return SubstitutionScope.queuedAttack(player, comboCount);
    }

    public static boolean targetLimitReached(Player player, String affixId,
                                             AffixEffects.Mechanics mechanics) {
        StyleAffix affix = AffixRegistry.get(affixId);
        return affix != null && AffixMechanics.targetLimitReached(player, affix, mechanics);
    }

    public static boolean claimEquipmentCooldown(Player player, String itemId) {
        return EquipmentRuntime.claimCooldown(player, itemId);
    }

    public static long idleAtSwing(Player player) {
        return CombatState.idleAtSwing(player);
    }

    public static long currentSwingId(Player player) {
        return CombatState.currentSwingId(player);
    }

    public static SwingTimingInfo swingTiming() {
        return SwingTiming.INSTANCE;
    }

    public static float cooldownMultiplier(Player player) {
        return SwingTiming.INSTANCE.cooldownMultiplier(player);
    }

    public static void scaleThisSwing(Player player, float multiplier) {
        SwingTiming.INSTANCE.scaleThisSwing(player, multiplier);
    }

    public static void hastenNextSwings(Player player, float multiplier, int swings,
                                        String hitbox) {
        SwingTiming.INSTANCE.hastenNextSwings(player, multiplier, swings, hitbox);
    }

    public static boolean dualWielding(Player player) {
        return SwingTiming.INSTANCE.dualWielding(player);
    }

    public static boolean swungWithOffHand(Player player, int comboCount) {
        return SwingTiming.INSTANCE.swungWithOffHand(player, comboCount);
    }

    public static int sequenceSlot(Player player, int comboCount, int sequenceSize) {
        return SwingTiming.INSTANCE.sequenceSlot(player, comboCount, sequenceSize);
    }

    public static boolean swingMissed(Player player, long swingId) {
        return CombatState.swingMissed(player, swingId);
    }

    public static boolean damagedWithin(Player player, int ticks) {
        return CombatState.damagedWithin(player, ticks);
    }

    public static int hitStreak(Player player) {
        return HitStreaks.streakOf(player);
    }

    public static int hitStreak(Player player, LivingEntity target) {
        return HitStreaks.streakOf(player, target);
    }

    public static boolean isSameTarget(Player player, LivingEntity target) {
        return HitStreaks.isSameTarget(player, target);
    }

    public static boolean hasComboRefund(Player player) {
        return ComboControl.hasRefund(player);
    }

    public static void refundComboSlots(Player player, int slots) {
        ComboControl.holdSlot(player, slots);
    }

    public static Map<String, Integer> equipmentCooldowns(Player player) {
        return EquipmentRuntime.cooldownsFor(player);
    }

    public static Map<String, Integer> equipmentCooldownLengths(Player player) {
        return EquipmentRuntime.cooldownLengthsFor(player);
    }

    public static float rechargeProgress(Player player) {
        return EquipmentRuntime.rechargeProgress(player);
    }

    public static Set<String> readySubstitutions(Player player) {
        return EquipmentRuntime.readySubstitutions(player);
    }

    public static void grantDodge(Player player, float chance, int durationTicks) {
        DodgeState.grant(player, chance, durationTicks);
    }

    public static void verifyData() {
        DataIntegrity.verify();
    }

    public static int summonCountOf(Player player, ResourceLocation type) {
        if (player == null || type == null) {
            return 0;
        }
        int found = 0;
        for (ConfigurableSummon summon : SummonRoster.of(player)) {
            if (summon instanceof Entity entity
                    && type.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) {
                found++;
            }
        }
        return found;
    }

    public static float summonDamageScale(Player player, ConfigurableSummon summon) {
        return player == null || summon == null ? 1.0F : summon.damageScale(player);
    }

    public static boolean hasEffect(LivingEntity entity, String effectId) {
        return effectAmplifier(entity, effectId) >= 0;
    }

    public static int effectStacks(LivingEntity entity, String effectId) {
        int amplifier = effectAmplifier(entity, effectId);
        return amplifier < 0 ? 0 : amplifier + 1;
    }

    public static int effectAmplifier(LivingEntity entity, String effectId) {
        if (entity == null || effectId == null || effectId.isBlank()) {
            return -1;
        }
        Holder<MobEffect> holder = EffectLookup.of(effectId);
        if (holder == null) {
            return -1;
        }
        MobEffectInstance instance = entity.getEffect(holder);
        return instance == null ? -1 : instance.getAmplifier();
    }

    public static int eyeAbilityDuration(String ability) {
        return AbilityConfig.get(ability).duration();
    }

    public static UUID summonOwnerOf(Entity entity) {
        return entity instanceof ConfigurableSummon summon ? summon.summonOwner() : null;
    }

    public static String innateAffixAt(ItemStack stack, int slot) {
        return InnateAffixes.affixFor(stack, slot);
    }

    public static int innateAffixCount(ItemStack stack) {
        return InnateAffixes.slotCount(stack);
    }

    public static double defaultStyleBudgetOf(ItemStack stack) {
        return BudgetSources.defaultBudgetOf(stack);
    }

    public static void commandSummon(Player player, ConfigurableSummon summon,
                                     LivingEntity target, String ability) {
        SummonOrders.commandOne(player, summon, target, ability, 0.0F, null);
    }

    public static int commandSummons(Player player, LivingEntity target, String ability,
                                     float damage, String targeting) {
        return SummonOrders.command(player, target, ability, damage,
                SummonTargeting.byName(targeting));
    }

    public static int commandAllSummons(Player player, LivingEntity target, String ability,
                                        float damage) {
        return SummonOrders.commandAll(player, target, ability, damage);
    }

    public static String summonAbilityFor(Player player, int slot) {
        return SummonOrders.abilityFor(player, SummonProfiles.profileFor(player), null, slot);
    }
}
