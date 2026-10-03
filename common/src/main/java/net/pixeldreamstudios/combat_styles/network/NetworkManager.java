package net.pixeldreamstudios.combat_styles.network;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.config.ConfigSnapshot;
import net.pixeldreamstudios.combat_styles.item.StyleExtractorItem;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.pixeldreamstudios.combat_styles.equipment.ClientSwing;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentPoses;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentOverrides;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRegistry;
import net.pixeldreamstudios.combat_styles.equipment.Gates;
import net.pixeldreamstudios.combat_styles.equipment.Cooldowns;
import net.pixeldreamstudios.combat_styles.equipment.CycleChance;
import net.pixeldreamstudios.combat_styles.equipment.GroupDraw;
import net.pixeldreamstudios.combat_styles.equipment.CombatState;
import net.pixeldreamstudios.combat_styles.equipment.PlayerTags;
import net.pixeldreamstudios.combat_styles.equipment.StackPose;
import net.pixeldreamstudios.combat_styles.equipment.ChargeMirror;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentRuntime;
import net.pixeldreamstudios.combat_styles.progression.StyleProgression;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.BudgetSources;
import net.pixeldreamstudios.combat_styles.style.DeclaredClaims;
import net.pixeldreamstudios.combat_styles.style.CustomAttackConfig;
import net.pixeldreamstudios.combat_styles.style.CustomAttacks;
import net.pixeldreamstudios.combat_styles.style.CustomStyleConfig;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.DefaultStyles;
import net.pixeldreamstudios.combat_styles.style.SequenceLimits;
import net.pixeldreamstudios.combat_styles.style.WeaponCategories;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleCost;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;
import net.pixeldreamstudios.combat_styles.style.StyleText;
import net.pixeldreamstudios.combat_styles.style.SyntheticAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.pixeldreamstudios.combat_styles.component.ModComponents;

public final class NetworkManager {
    private NetworkManager() {
    }

    @ExpectPlatform
    public static void register() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendApplyStackEquipment(ApplyStackEquipmentPayload payload) {
        throw new AssertionError();
    }

    public static void handleApplyStackEquipment(ServerPlayer player,
                                                 ApplyStackEquipmentPayload payload) {
        if (player == null || payload == null || !player.getAbilities().instabuild) {
            return;
        }
        ItemStack stack = player.getItemBySlot(EquipmentSlot.MAINHAND);
        if (stack.isEmpty()) {
            return;
        }
        stack.set(ModComponents.STACK_EQUIPMENT.get(), payload.definition());
    }

    @ExpectPlatform
    public static void sendApplySequence(ApplySequencePayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendApplyPose(ApplyPosePayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendApplyWeaponCategory(ApplyWeaponCategoryPayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendLearnedToClient(ServerPlayer player, SyncLearnedPayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendDefaultStylesToClient(ServerPlayer player, SyncDefaultStylesPayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendCooldownsToClient(ServerPlayer player, SyncCooldownsPayload payload) {
        throw new AssertionError();
    }

    public static void syncCooldowns(ServerPlayer player, String proc) {
        Map<String, Integer> remaining = EquipmentRuntime.cooldownsFor(player);
        Set<String> armed = new LinkedHashSet<>(EquipmentRuntime.armedFor(player));
        armed.addAll(EquipmentRuntime.holdMarksFor(player));
        armed.addAll(EquipmentRuntime.armMarksFor(player));
        armed.addAll(EquipmentRuntime.choiceMarksFor(player));
        armed.addAll(Gates.marksFor(player));
        armed.addAll(Cooldowns.marksFor(player));
        armed.addAll(GroupDraw.marksFor(player));
        armed.addAll(CycleChance.marksFor(player));
        if (CombatState.tracks(player)) {
            armed.add(CombatState.markFor(player));
        }
        if (!player.getTags().isEmpty()) {
            armed.add(PlayerTags.markFor(player));
        }
        armed.add(ChargeMirror.SWING_MARK + EquipmentRuntime.swingCount(player));
        EquipmentRuntime.announceReady(player, EquipmentRuntime.readySubstitutions(player));
        if (remaining.isEmpty() && armed.isEmpty() && (proc == null || proc.isEmpty())
                && !COOLDOWNS_SENT.remove(player.getUUID())) {
            return;
        }
        if (!remaining.isEmpty() || !armed.isEmpty()) {
            COOLDOWNS_SENT.add(player.getUUID());
        }
        Map<String, Integer> lengths = EquipmentRuntime.cooldownLengthsFor(player);
        long now = player.level().getGameTime();
        Set<String> signature = signatureOf(now, remaining, lengths, armed);
        Sent last = LAST_SENT.get(player.getUUID());
        boolean proced = proc != null && !proc.isEmpty();
        if (!proced && last != null && last.signature().equals(signature)
                && now - last.at() < COOLDOWN_KEEP_ALIVE) {
            return;
        }
        LAST_SENT.put(player.getUUID(), new Sent(signature, now));
        sendCooldownsToClient(player, new SyncCooldownsPayload(remaining, lengths, armed,
                proced ? proc : SyncCooldownsPayload.NO_PROC));
    }

    private record Sent(Set<String> signature, long at) {
    }

    private static final long COOLDOWN_KEEP_ALIVE = SharedConstants.TICKS_PER_SECOND;
    private static final String REMAINING_MARK = "r|";
    private static final String LENGTH_MARK = "l|";
    private static final String VALUE_JOIN = "=";

    private static final Set<UUID> COOLDOWNS_SENT = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Sent> LAST_SENT = new ConcurrentHashMap<>();

    public static void forgetCooldownSync(UUID playerId) {
        COOLDOWNS_SENT.remove(playerId);
        LAST_SENT.remove(playerId);
    }

    private static Set<String> signatureOf(long now, Map<String, Integer> remaining,
                                           Map<String, Integer> lengths, Set<String> armed) {
        Set<String> signature = new HashSet<>();
        remaining.forEach((key, ticks) -> signature.add(REMAINING_MARK + key + VALUE_JOIN
                + (now + ticks)));
        lengths.forEach((key, ticks) -> signature.add(LENGTH_MARK + key + VALUE_JOIN + ticks));
        for (String mark : armed) {
            if (mark.startsWith(ChargeMirror.WINDOW_MARK)) {
                signature.add(Gates.absoluteMark(mark, now));
            } else if (mark.startsWith(ChargeMirror.COMBAT_MARK)) {
                signature.add(CombatState.absoluteMark(mark, now));
            } else if (mark.startsWith(ChargeMirror.COOLDOWN_MARK)) {
                signature.add(Cooldowns.absoluteMark(mark, now));
            } else {
                signature.add(mark);
            }
        }
        return signature;
    }

    public static void syncLearned(ServerPlayer player) {
        StyleProgression.migrate(player);
        sendLearnedToClient(player, new SyncLearnedPayload(StyleProgression.all(player),
                StyleProgression.variants(player)));
    }

    @ExpectPlatform
    public static void sendSpellLocksToClient(ServerPlayer player, SyncSpellLocksPayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendConfigToClient(ServerPlayer player, SyncConfigPayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendEditConfig(EditConfigPayload payload) {
        throw new AssertionError();
    }

    public static void handleEditConfig(ServerPlayer player, EditConfigPayload payload) {
        ConfigEditHandler.handle(player, payload);
    }

    public static void syncConfig(ServerPlayer player) {
        sendConfigToClient(player, new SyncConfigPayload(ConfigSnapshot.capture()));
    }

    public static void broadcastConfig(MinecraftServer server) {
        if (server == null) {
            return;
        }
        SyncConfigPayload payload = new SyncConfigPayload(ConfigSnapshot.capture());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendConfigToClient(player, payload);
        }
    }

    public static void broadcastDefaultStyles(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncDefaultStyles(player);
        }
    }

    public static void syncDefaultStyles(ServerPlayer player) {
        sendDefaultStylesToClient(player, new SyncDefaultStylesPayload(
                DefaultStyles.snapshotSequences(),
                DefaultStyles.snapshotSlotLimits(),
                DefaultStyles.snapshotAppending(),
                DefaultStyles.snapshotTuning(),
                DefaultStyles.snapshotSticky(),
                DefaultStyles.snapshotLocked(),
                DefaultStyles.snapshotSlotTunings(),
                DefaultStyles.snapshotConfigItems(),
                CustomStyles.authoritative(),
                CustomAttacks.authoritative(),
                CustomStyleConfig.loaded().keySet(),
                CustomAttackConfig.loaded().keySet(),
                CustomStyles.datapackIds(),
                CustomAttacks.datapackIds()));
        syncEquipment(player);
        syncAffixes(player);
        syncLoot(player);
        syncAttackOverrides(player);
        syncConfig(player);
    }

    @ExpectPlatform
    public static void sendEquipmentToClient(ServerPlayer player, SyncEquipmentPayload payload) {
        throw new AssertionError();
    }

    public static void syncEquipment(ServerPlayer player) {
        sendEquipmentToClient(player, new SyncEquipmentPayload(EquipmentRegistry.snapshot(),
                EquipmentRegistry.dataIds(), EquipmentOverrides.all().keySet()));
    }

    @ExpectPlatform
    public static void sendAffixesToClient(ServerPlayer player, SyncAffixesPayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendLootToClient(ServerPlayer player, SyncLootPayload payload) {
        throw new AssertionError();
    }

    public static void syncLoot(ServerPlayer player) {
        sendLootToClient(player, SyncLootPayload.capture());
    }

    public static void syncAffixes(ServerPlayer player) {
        sendAffixesToClient(player, new SyncAffixesPayload(AffixRegistry.authoritative(),
                AffixRegistry.customIds(), AffixRegistry.datapackIds()));
    }

    @ExpectPlatform
    public static void sendAttackOverridesToClient(ServerPlayer player,
                                                   SyncAttackOverridesPayload payload) {
        throw new AssertionError();
    }

    public static void syncAttackOverrides(ServerPlayer player) {
        sendAttackOverridesToClient(player,
                new SyncAttackOverridesPayload(AttackOverrides.snapshot()));
    }

    @ExpectPlatform
    public static void sendExtractStyle(ExtractStylePayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendAlignCombo(ServerPlayer player, AlignComboPayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendHoldCombo(ServerPlayer player, HoldComboPayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendSwingHaste(ServerPlayer player, SwingHastePayload payload) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendSwungAttack(SwungAttackPayload payload) {
        throw new AssertionError();
    }

    public static void handleSwungAttack(ServerPlayer player, SwungAttackPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        ClientSwing.declare(player, payload);
    }

    @ExpectPlatform
    public static void sendHitboxClaims(HitboxClaimPayload payload) {
        throw new AssertionError();
    }

    public static void handleHitboxClaims(ServerPlayer player, HitboxClaimPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        DeclaredClaims.accept(player, payload);
    }

    public static void handleExtractStyle(ServerPlayer player, ExtractStylePayload payload) {
        if (player == null || payload == null) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!SequenceService.isSupported(weapon)) {
            return;
        }
        if (CombatStylesConfig.get().etchRequiresCreative()
                && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable(
                    "message.combat_styles.etch_creative_only").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!StyleExtractorItem.canEtch(weapon)) {
            player.displayClientMessage(Component.translatable(
                    "message.combat_styles.already_etched").withStyle(ChatFormatting.RED), true);
            return;
        }

        StyleEntry entry = SequenceService.sequenceOf(weapon, player.getUUID())
                .entry(payload.slotIndex());
        if (entry == null) {
            return;
        }
        entry = withCapturedOverride(weapon, entry, payload.slotIndex());

        ItemStack extractor = findEmptyExtractor(player);
        if (extractor == null) {
            player.displayClientMessage(Component.translatable(
                    "message.combat_styles.no_extractor").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (StyleExtractorItem.extractEntry(extractor, entry)) {
            SequenceService.setEtched(weapon, true);
            player.displayClientMessage(Component.translatable(
                    "message.combat_styles.style_copied").withStyle(ChatFormatting.GREEN), true);
        }
    }

    private static StyleEntry withCapturedOverride(ItemStack weapon, StyleEntry entry,
                                                   int slotIndex) {
        AttackOverride direct = entry.attackOverride().orElse(null);
        AttackOverride fromItem = SequenceService.attackOverridesOf(weapon)
                .get(String.valueOf(slotIndex));
        AttackOverride merged = direct == null
                ? fromItem
                : (fromItem == null ? direct : direct.overriddenBy(fromItem));
        return merged == null ? entry : entry.withAttackOverride(merged);
    }

    private static ItemStack findEmptyExtractor(ServerPlayer player) {
        for (ItemStack candidate : player.getInventory().items) {
            if (candidate.getItem() instanceof StyleExtractorItem
                    && !StyleExtractorItem.hasStored(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static void handleApplyPose(ServerPlayer player, ApplyPosePayload payload) {
        if (player == null || payload == null) {
            return;
        }
        EquipmentSlot slot = payload.slot();
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            return;
        }
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return;
        }
        StackPose pose = payload.remove() ? null : payload.pose();
        if (payload.equipmentLayer()) {
            EquipmentPoses.storeEquipment(stack, pose);
        } else {
            EquipmentPoses.store(stack, pose);
        }
    }

    public static void handleApplyWeaponCategory(ServerPlayer player, ApplyWeaponCategoryPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        EquipmentSlot slot = payload.slot();
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            return;
        }
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return;
        }
        WeaponCategories.store(stack, payload.category());
    }

    public static void handleApplySequence(ServerPlayer player, ApplySequencePayload payload) {
        if (player == null || payload == null) {
            return;
        }
        EquipmentSlot slot = payload.slot();
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
            return;
        }
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return;
        }
        if (player.getAbilities().instabuild) {
            BudgetSources.setOverride(stack, payload.budgetOverride().orElse(null));
        }
        if (payload.improvised() && player.getAbilities().instabuild) {
            SyntheticAttributes.setImprovised(stack, true);
            if (SequenceService.rawBaselineOf(stack).isEmpty()
                    && SequenceService.customSequenceOf(stack).isEmpty()
                    && !payload.sequence().isEmpty()) {
                SequenceService.storeRolled(stack, payload.sequence());
            }
        }
        if (!SequenceService.isSupported(stack)) {
            return;
        }
        if (SequenceService.isLocked(stack)) {
            player.displayClientMessage(Component.translatable("message.combat_styles.locked_item")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        StyleSequence requested = payload.sequence();
        if (requested.isEmpty()) {
            SequenceService.storeMoveset(stack, player.getUUID(), StyleSequence.EMPTY);
            storeAttackOverrides(player, stack, payload.attackOverrides(),
                    player.getAbilities().instabuild);
            return;
        }
        if (requested.size() > DefaultStyles.slotLimitFor(stack)) {
            return;
        }
        int minimum = SequenceLimits.minimumFor(stack);
        if (requested.size() < minimum) {
            player.displayClientMessage(Component.translatable(
                    "message.combat_styles.below_minimum", minimum)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        boolean creative = player.getAbilities().instabuild;
        Set<String> nativeIds = new HashSet<>();
        for (StylePage node : StyleRegistry.nodesOf(SequenceService.baseAttributes(stack))) {
            nativeIds.add(node.styleId());
        }
        for (StyleEntry defaultEntry : SequenceService.baselineOf(stack).entries()) {
            nativeIds.add(defaultEntry.styleId());
            StylePage resolved = StyleRegistry.get(defaultEntry.styleId());
            String resolvedId = resolved == null ? defaultEntry.styleId() : resolved.styleId();
            nativeIds.add(resolvedId);
        }

        Map<String, Integer> used = new HashMap<>();
        Map<String, Integer> styleUsed = new HashMap<>();
        List<String> dropped = new ArrayList<>();
        List<StyleEntry> accepted = new ArrayList<>(requested.size());
        for (StyleEntry entry : requested.entries()) {
            String styleId = entry.styleId();
            if (!StyleRegistry.contains(styleId)) {
                continue;
            }

            List<String> affixIds = new ArrayList<>();
            for (String requestedAffix : entry.affixIds()) {
                StyleAffix affix = AffixRegistry.get(requestedAffix);
                if (affix != null && !affixIds.contains(affix.serializedId())) {
                    affixIds.add(affix.serializedId());
                }
            }
            if (!creative && CombatStylesConfig.get().namedEntriesLocked() && entry.isNamed()) {
                List<String> stored = storedAffixesOf(player, stack, entry);
                if (stored != null) {
                    affixIds = new ArrayList<>(stored);
                }
            }
            if (affixIds.size() > CombatStylesConfig.get().maxAffixesPerSlot()) {
                affixIds = affixIds.subList(0, CombatStylesConfig.get().maxAffixesPerSlot());
            }
            affixIds = StyleEntry.sorted(affixIds);

            StylePage resolvedPage = StyleRegistry.get(styleId);
            boolean enforced = CombatStylesConfig.get().requireLearnedStyles() && !creative;
            String ownedKey = entry.learnedKey();
            String baseKey = entry.customStyleId().isPresent()
                    ? ownedKey
                    : (resolvedPage == null ? styleId : resolvedPage.styleId());
            if (enforced) {
                int owned = StyleProgression.copies(player, ownedKey);
                int granted = SequenceLimits.grantedBy(stack, entry);
                boolean fromOwned = used.getOrDefault(ownedKey, 0) < owned;
                boolean fromGrant = granted > 0
                        && styleUsed.getOrDefault(baseKey, 0) < granted;
                if (!fromOwned && !fromGrant) {
                    dropped.add(StyleText.affixedName(entry, resolvedPage).getString());
                    continue;
                }
                if (!fromOwned) {
                    styleUsed.merge(baseKey, 1, Integer::sum);
                }
            }
            used.merge(ownedKey, 1, Integer::sum);

            String soundId = entry.soundId().orElse(null);
            if (soundId != null && StyleRegistry.swingSound(soundId) == null
                    && !isRegisteredSound(soundId)) {
                soundId = null;
            }

            Optional<AttackOverride> override = creative || entry.customStyleId().isPresent()
                    ? entry.attackOverride()
                    : existingOverrideOf(player, stack, entry);
            accepted.add(new StyleEntry(styleId, Optional.ofNullable(soundId), affixIds, override,
                    entry.name(), entry.nameKey(), entry.affixTuning(), entry.customStyleId()));
        }
        if (!dropped.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                            "message.combat_styles.entries_dropped",
                            dropped.size(), String.join(", ", dropped))
                    .withStyle(ChatFormatting.RED), false);
        }
        if (accepted.isEmpty()) {
            return;
        }

        if (accepted.size() < minimum) {
            player.displayClientMessage(Component.translatable(
                    "message.combat_styles.below_minimum", minimum)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        StyleSequence candidate = new StyleSequence(requested.name(), accepted);
        double allowed = BudgetSources.totalFor(player, stack);
        if (!creative && CombatStylesConfig.get().enforceStyleBudget() && StyleCost.totalOf(candidate,
                SequenceService.categoryOf(stack), nativeIds) > allowed + StyleCost.TOLERANCE) {
            player.displayClientMessage(Component.translatable("message.combat_styles.over_budget")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        SequenceService.storeMoveset(stack, player.getUUID(), candidate);
        storeAttackOverrides(player, stack, payload.attackOverrides(), creative);
    }

    private static List<String> storedAffixesOf(ServerPlayer player, ItemStack stack,
                                                StyleEntry entry) {
        for (StyleEntry stored : SequenceService.sequenceOf(stack, player.getUUID()).entries()) {
            if (stored.isSameNamed(entry)) {
                return stored.affixIds();
            }
        }
        for (StyleEntry stored : SequenceService.baselineOf(stack).entries()) {
            if (stored.isSameNamed(entry)) {
                return stored.affixIds();
            }
        }
        return null;
    }

    private static Optional<AttackOverride> existingOverrideOf(ServerPlayer player, ItemStack stack,
                                                              StyleEntry entry) {
        if (entry.attackOverride().isEmpty()) {
            return Optional.empty();
        }
        for (StyleEntry stored : SequenceService.sequenceOf(stack, player.getUUID()).entries()) {
            if (stored.learnedKey().equals(entry.learnedKey())
                    && stored.attackOverride().equals(entry.attackOverride())) {
                return entry.attackOverride();
            }
        }
        for (StyleEntry stored : SequenceService.baselineOf(stack).entries()) {
            if (stored.learnedKey().equals(entry.learnedKey())
                    && stored.attackOverride().equals(entry.attackOverride())) {
                return entry.attackOverride();
            }
        }
        return Optional.empty();
    }

    private static boolean isRegisteredSound(String soundId) {
        ResourceLocation location = ResourceLocation.tryParse(soundId);
        return location != null && BuiltInRegistries.SOUND_EVENT.containsKey(location);
    }

    private static void storeAttackOverrides(ServerPlayer player, ItemStack stack,
                                             Map<String, AttackOverride> overrides, boolean creative) {
        if (overrides == null || overrides.isEmpty()) {
            SequenceService.clearAttackOverrides(stack);
            return;
        }
        if (!creative) {
            player.displayClientMessage(Component.translatable(
                    "message.combat_styles.attack_override_denied").withStyle(ChatFormatting.RED), true);
            return;
        }
        SequenceService.clearAttackOverrides(stack);
        int slots = SequenceService.sequenceOf(stack, player.getUUID()).size();
        if (slots <= 0) {
            slots = StyleRegistry.nodesOf(SequenceService.baseAttributes(stack)).size();
        }
        for (Map.Entry<String, AttackOverride> entry : overrides.entrySet()) {
            int index;
            try {
                index = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException exception) {
                continue;
            }
            if (index >= 0 && index < slots) {
                SequenceService.setAttackOverride(stack, entry.getKey(), entry.getValue());
            }
        }
    }
}
