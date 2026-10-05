package net.pixeldreamstudios.combat_styles.command;

import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.DataIntegrity;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.api.CombatStylesAPI;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.item.StylePageItem;
import net.pixeldreamstudios.combat_styles.loot.BookRolls;
import net.pixeldreamstudios.combat_styles.loot.WeaponRolls;
import net.pixeldreamstudios.combat_styles.network.ConfigEditHandler;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.progression.LearnedStyles;
import net.pixeldreamstudios.combat_styles.progression.ProgressionBudget;
import net.pixeldreamstudios.combat_styles.progression.StyleProgression;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.BudgetSources;
import net.pixeldreamstudios.combat_styles.style.CustomStyle;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.DefaultStyleConfig;
import net.pixeldreamstudios.combat_styles.style.DefaultStyles;
import net.pixeldreamstudios.combat_styles.style.SequenceLimits;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleCost;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleConfigs;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;
import net.pixeldreamstudios.combat_styles.style.StyleText;
import net.pixeldreamstudios.combat_styles.style.SyntheticAttributes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class CombatStylesCommand {

    private static final String ROOT = "combatstyles";
    private static final String KEY_DEBUG_ON = "command.combat_styles.debug_on";
    private static final String KEY_DEBUG_OFF = "command.combat_styles.debug_off";
    private static final String KEY_MINIMUM_IGNORED = "command.combat_styles.minimum_ignored";
    private static final String KEY_MINIMUM_ENFORCED = "command.combat_styles.minimum_enforced";
    private static final String ARG_STYLE = "style";
    private static final String ARG_TARGETS = "targets";
    private static final String ARG_COUNT = "copies";
    private static final String ARG_AFFIX = "affix";
    private static final String ARG_SLOT = "slot";
    private static final String ARG_FIELD = "field";
    private static final String ARG_VALUE = "value";
    private static final String ARG_RARITY = "rarity";
    private static final String ARG_EQUIPMENT = "equipment";
    private static final String ARG_NAMESPACE = "namespace";
    private static final String ALL_NAMESPACES = "all";

    private static final String FIELD_DAMAGE = "damage_multiplier";
    private static final String FIELD_ANGLE = "angle";
    private static final String FIELD_UPSWING = "upswing";
    private static final String FIELD_RANGE = "range_multiplier";
    private static final String FIELD_MOVEMENT = "movement_speed_multiplier";
    private static final int MAX_COPIES = 64;
    private static final int PERMISSION_LEVEL = ConfigEditHandler.OPERATOR_LEVEL;

    private static final SuggestionProvider<CommandSourceStack> STYLE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(StyleRegistry.all().stream().map(StylePage::styleId), builder);

    private static final SuggestionProvider<CommandSourceStack> ITEM_ATTACK_FIELDS = (context, builder) ->
            SharedSuggestionProvider.suggest(new String[]{FIELD_DAMAGE, FIELD_ANGLE, FIELD_UPSWING,
                    FIELD_RANGE, FIELD_MOVEMENT}, builder);

    private static final SuggestionProvider<CommandSourceStack> AFFIX_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    AffixRegistry.all().stream().map(StyleAffix::serializedId), builder);

    private static final SuggestionProvider<CommandSourceStack> RARITY_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(StyleRarity.values()).map(StyleRarity::key), builder);

    private static final SuggestionProvider<CommandSourceStack> NAMESPACE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(Stream.concat(Stream.of(ALL_NAMESPACES),
                    AttackOverrides.namespaces().stream()), builder);

    private static final SuggestionProvider<CommandSourceStack> EQUIPMENT_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(EquipmentReport.ids(), builder);

    private CombatStylesCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT)
                .requires(source -> source.hasPermission(PERMISSION_LEVEL));

        registerInspection(root);
        registerSequenceEditing(root);
        registerAffixes(root);
        registerLoot(root);
        registerProgression(root);
        registerMaintenance(root);

        dispatcher.register(root);
    }

    private static void registerInspection(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("debug")
                .then(Commands.literal("enable")
                        .executes(context -> debug(context, true)))
                .then(Commands.literal("disable")
                        .executes(context -> debug(context, false))));
        root.then(Commands.literal("list").executes(CombatStylesCommand::list));
        root.then(Commands.literal("inspect").executes(CombatStylesCommand::inspect));
        root.then(Commands.literal("rolls").executes(CombatStylesCommand::listRolls));
        root.then(Commands.literal("custom_styles").executes(CombatStylesCommand::listCustomStyles));
        root.then(Commands.literal("affixes").executes(CombatStylesCommand::listAffixes));
        root.then(Commands.literal("equipment")
                .executes(context -> EquipmentReport.overview(context.getSource(),
                        context.getSource().getPlayerOrException()))
                .then(Commands.argument(ARG_EQUIPMENT, StringArgumentType.greedyString())
                        .suggests(EQUIPMENT_SUGGESTIONS)
                        .executes(context -> EquipmentReport.detail(context.getSource(),
                                StringArgumentType.getString(context, ARG_EQUIPMENT)))));
        root.then(Commands.literal("learned").executes(CombatStylesCommand::listLearned));
        root.then(Commands.literal("budget")
                .executes(CombatStylesCommand::showBudget)
                .then(Commands.literal("set")
                        .then(Commands.argument(ARG_VALUE, DoubleArgumentType.doubleArg(0))
                                .executes(CombatStylesCommand::setBudgetOverride)))
                .then(Commands.literal("clear")
                        .executes(CombatStylesCommand::clearBudgetOverride)));
    }

    private static void registerSequenceEditing(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("clear").executes(CombatStylesCommand::clear));
        root.then(Commands.literal("unetch").executes(CombatStylesCommand::unetch));
        root.then(Commands.literal("improvise").executes(CombatStylesCommand::improvise));
        root.then(Commands.literal("lock").executes(context -> setLocked(context, true)));
        root.then(Commands.literal("unlock").executes(context -> setLocked(context, false)));
        root.then(Commands.literal("item_attack")
                .then(Commands.argument(ARG_SLOT, IntegerArgumentType.integer(1))
                        .then(Commands.argument(ARG_FIELD, StringArgumentType.word())
                                .suggests(ITEM_ATTACK_FIELDS)
                                .then(Commands.argument(ARG_VALUE, DoubleArgumentType.doubleArg())
                                        .executes(CombatStylesCommand::setItemAttack)))));
        root.then(Commands.literal("item_attack_clear")
                .executes(CombatStylesCommand::clearItemAttacks));
        root.then(Commands.literal("ignore_minimum")
                .executes(context -> setIgnoreMinimum(context, true)));
        root.then(Commands.literal("enforce_minimum")
                .executes(context -> setIgnoreMinimum(context, false)));
        root.then(Commands.literal("slot_limit")
                .then(Commands.argument(ARG_VALUE, DoubleArgumentType.doubleArg(1))
                        .executes(CombatStylesCommand::setSlotLimit)));
    }

    private static void registerAffixes(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("apply_affix")
                .then(Commands.argument(ARG_AFFIX, StringArgumentType.greedyString())
                        .suggests(AFFIX_SUGGESTIONS)
                        .executes(CombatStylesCommand::applyAffix)));
        root.then(Commands.literal("strip_affix").executes(CombatStylesCommand::stripAffix));
    }

    private static void registerLoot(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("roll").executes(CombatStylesCommand::rollHeld));
        root.then(Commands.literal("roll_clear").executes(CombatStylesCommand::clearRoll));
        root.then(Commands.literal("book")
                .then(Commands.argument(ARG_RARITY, StringArgumentType.word())
                        .suggests(RARITY_SUGGESTIONS)
                        .executes(CombatStylesCommand::giveBook)));
        root.then(Commands.literal("give")
                .then(Commands.argument(ARG_TARGETS, EntityArgument.players())
                        .then(Commands.argument(ARG_STYLE, ResourceLocationArgument.id())
                                .suggests(STYLE_SUGGESTIONS)
                                .executes(context -> give(context, 1))
                                .then(Commands.argument(ARG_COUNT,
                                                IntegerArgumentType.integer(1, MAX_COPIES))
                                        .executes(context -> give(context,
                                                IntegerArgumentType.getInteger(context, ARG_COUNT)))))));
        root.then(Commands.literal("giveall")
                .then(Commands.argument(ARG_TARGETS, EntityArgument.players())
                        .executes(CombatStylesCommand::giveAll)));
    }

    private static void registerProgression(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("learnall")
                .then(Commands.argument(ARG_TARGETS, EntityArgument.players())
                        .executes(CombatStylesCommand::learnAll)
                        .then(Commands.argument(ARG_COUNT,
                                        IntegerArgumentType.integer(1, MAX_COPIES))
                                .executes(CombatStylesCommand::learnAllCopies))));
        root.then(Commands.literal("forgetall")
                .then(Commands.argument(ARG_TARGETS, EntityArgument.players())
                        .executes(CombatStylesCommand::forgetAll)));
    }

    private static void registerMaintenance(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("reload").executes(CombatStylesCommand::reload));
        root.then(Commands.literal("defaults_reload").executes(CombatStylesCommand::reloadDefaults));
        root.then(Commands.literal("attacks_reload").executes(CombatStylesCommand::reloadAttacks));
        root.then(Commands.literal("attacks_refresh")
                .then(Commands.argument(ARG_NAMESPACE, StringArgumentType.word())
                        .suggests(NAMESPACE_SUGGESTIONS)
                        .executes(context -> refreshAttacks(context,
                                StringArgumentType.getString(context, ARG_NAMESPACE)))));
        root.then(Commands.literal("export_default").executes(CombatStylesCommand::exportDefault));
    }

    private static int showBudget(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        final double base = StyleCost.budgetOf(SequenceService.baseAttributes(stack),
                DefaultStyles.slotLimitFor(stack));
        final double rolled = BudgetSources.rolledBudgetOf(stack);
        final int styles = ProgressionBudget.distinctStylesOf(player);
        final double progression = ProgressionBudget.forHolder(player);
        final double total = BudgetSources.totalFor(player, stack);
        final double spent = StyleCost.totalOf(SequenceService.sequenceOf(stack, player.getUUID()),
                SequenceService.categoryOf(stack));

        source.sendSuccess(() -> Component.literal(String.format(
                "weapon %.2f + rolled %.2f + progression %.2f (%d styles) = %.2f  |  spent %.2f",
                base, rolled, progression, styles, total, spent)), false);
        return (int) total;
    }

    private static int listLearned(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Map<String, Integer> copies = CombatStylesAPI.learnedStyles(player);
        Map<String, StyleEntry> variants = StyleProgression.variants(player);

        source.sendSuccess(() -> Component.literal(
                "learned keys: " + copies.size() + ", variant records: " + variants.size()), false);
        for (Map.Entry<String, Integer> entry : copies.entrySet()) {
            String key = entry.getKey();
            StyleEntry variant = variants.get(key);
            String detail = variant == null
                    ? " [no variant record]"
                    : " -> style=" + variant.styleId()
                            + " custom=" + variant.customStyleId().orElse("-")
                            + " affixes=" + variant.affixIds()
                            + " recomputed=" + variant.learnedKey();
            source.sendSuccess(() -> Component.literal(
                    "  " + key + " x" + entry.getValue() + detail), false);
        }
        for (Map.Entry<String, StyleEntry> entry : variants.entrySet()) {
            if (!copies.containsKey(entry.getKey())) {
                source.sendSuccess(() -> Component.literal(
                        "  ORPHAN VARIANT (no copies): " + entry.getKey()), false);
            }
        }
        return copies.size();
    }

    private static int debug(CommandContext<CommandSourceStack> context, boolean on)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        CombatDebug.toggle(player, on);
        context.getSource().sendSuccess(
                () -> Component.translatable(on ? KEY_DEBUG_ON : KEY_DEBUG_OFF)
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (StyleRegistry.isEmpty()) {
            source.sendFailure(Component.translatable("command.combat_styles.empty"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.combat_styles.list_header",
                StyleRegistry.size(), StyleRegistry.categories().size()), false);
        for (String category : StyleRegistry.categories()) {
            StringBuilder builder = new StringBuilder();
            for (StylePage page : StyleRegistry.forCategory(category)) {
                if (!builder.isEmpty()) {
                    builder.append(", ");
                }
                builder.append(page.styleId());
            }
            source.sendSuccess(() -> Component.literal(category + ": " + builder), false);
        }
        return StyleRegistry.size();
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        StyleConfigs.reloadAll();
        DataIntegrity.verify();
        NetworkManager.broadcastDefaultStyles(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable("command.combat_styles.reloaded",
                StyleRegistry.size()), true);
        return StyleRegistry.size();
    }

    private static int applyAffix(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        String styleId = StylePageItem.styleIdOf(stack);
        if (styleId == null) {
            context.getSource().sendFailure(
                    Component.translatable("command.combat_styles.not_a_page"));
            return 0;
        }

        List<String> affixIds = new ArrayList<>();
        for (String token : StringArgumentType.getString(context, ARG_AFFIX).split("[\\s,+]+")) {
            if (token.isBlank()) {
                continue;
            }
            if (!CombatStylesAPI.isKnownAffix(token)) {
                context.getSource().sendFailure(
                        Component.translatable("command.combat_styles.unknown_affix", token));
                return 0;
            }
            if (!affixIds.contains(token)) {
                affixIds.add(token);
            }
        }
        if (affixIds.isEmpty()) {
            context.getSource().sendFailure(
                    Component.translatable("command.combat_styles.unknown_affix", ""));
            return 0;
        }

        List<String> sorted = StyleEntry.sorted(affixIds);
        ItemStack applied = StylePageItem.of(styleId, sorted);
        applied.setCount(stack.getCount());
        player.setItemInHand(InteractionHand.MAIN_HAND, applied);

        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.affix_applied",
                String.join(" ", sorted), styleId), true);
        return sorted.size();
    }

    private static int stripAffix(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        String styleId = StylePageItem.styleIdOf(stack);
        if (styleId == null) {
            context.getSource().sendFailure(
                    Component.translatable("command.combat_styles.not_a_page"));
            return 0;
        }
        ItemStack stripped = StylePageItem.of(styleId);
        stripped.setCount(stack.getCount());
        player.setItemInHand(InteractionHand.MAIN_HAND, stripped);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.affix_stripped", styleId), true);
        return 1;
    }

    private static int setItemAttack(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!CombatStylesAPI.supportsSequences(stack)) {
            context.getSource().sendFailure(Component.translatable("command.combat_styles.unsupported"));
            return 0;
        }

        int slot = IntegerArgumentType.getInteger(context, ARG_SLOT) - 1;
        int slots = SequenceService.sequenceOf(stack, player.getUUID()).size();
        if (slot < 0 || slot >= slots) {
            context.getSource().sendFailure(
                    Component.translatable("command.combat_styles.bad_slot", slots));
            return 0;
        }

        String field = StringArgumentType.getString(context, ARG_FIELD);
        double value = DoubleArgumentType.getDouble(context, ARG_VALUE);
        String key = String.valueOf(slot);

        AttackOverride updated = withField(
                SequenceService.attackOverridesOf(stack).get(key), field, value);
        if (updated == null) {
            context.getSource().sendFailure(
                    Component.translatable("command.combat_styles.unknown_field", field));
            return 0;
        }

        SequenceService.setAttackOverride(stack, key, updated);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.item_attack_set", field, value, slot + 1), true);
        return 1;
    }

    private static AttackOverride withField(AttackOverride base, String field, double value) {
        AttackOverride source = base == null ? AttackOverride.EMPTY : base;
        return switch (field) {
            case FIELD_DAMAGE -> source.withDamageMultiplier(value);
            case FIELD_ANGLE -> source.withAngle(value);
            case FIELD_UPSWING -> source.withUpswing(value);
            case FIELD_RANGE -> source.withRangeMultiplier((float) value);
            case FIELD_MOVEMENT -> source.withMovementSpeedMultiplier((float) value);
            default -> null;
        };
    }

    private static int clearItemAttacks(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("command.combat_styles.unsupported"));
            return 0;
        }
        SequenceService.clearAttackOverrides(stack);
        context.getSource().sendSuccess(
                () -> Component.translatable("command.combat_styles.item_attack_cleared"), true);
        return 1;
    }

    private static int exportDefault(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!SequenceService.isSupported(stack)) {
            context.getSource().sendFailure(Component.translatable("command.combat_styles.unsupported"));
            return 0;
        }

        ResourceLocation itemId = DefaultStyleConfig.itemIdOf(stack);
        if (itemId == null || SequenceService.sequenceOf(stack, player.getUUID()).isEmpty()) {
            context.getSource().sendFailure(
                    Component.translatable("command.combat_styles.nothing_to_export"));
            return 0;
        }

        Path file = DefaultStyleConfig.exportFrom(stack, player.getUUID());
        if (file == null) {
            context.getSource().sendFailure(
                    Component.translatable("command.combat_styles.export_failed"));
            return 0;
        }

        DefaultStyleConfig.reload();
        NetworkManager.broadcastDefaultStyles(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.exported", itemId.toString(), file.toString()), true);
        return 1;
    }

    private static int rollHeld(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !SequenceService.isSupported(stack)) {
            context.getSource().sendFailure(Component.translatable(
                    "command.combat_styles.not_a_weapon"));
            return 0;
        }
        if (WeaponRolls.isEmpty()) {
            context.getSource().sendFailure(Component.translatable(
                    "command.combat_styles.no_rolls"));
            return 0;
        }

        SequenceService.clear(stack);
        SequenceService.storeRolled(stack, StyleSequence.EMPTY);
        final String category = SequenceService.categoryOf(stack);
        final int eligible = WeaponRolls.countMatching(stack);
        boolean rolled = WeaponRolls.applyForced(stack, player.getRandom());
        context.getSource().sendSuccess(() -> Component.translatable(rolled
                ? "command.combat_styles.rolled"
                : "command.combat_styles.roll_missed"), true);
        if (!rolled) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "category: " + (category == null ? "<none>" : category)
                            + " | rolls targeting it: " + eligible
                            + " | " + WeaponRolls.diagnose(stack)),
                    false);
        }
        return rolled ? 1 : 0;
    }

    private static int clearRoll(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ItemStack stack = context.getSource().getPlayerOrException().getMainHandItem();
        SequenceService.storeRolled(stack, StyleSequence.EMPTY);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.roll_cleared"), true);
        return 1;
    }

    private static int giveBook(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String key = StringArgumentType.getString(context, ARG_RARITY);
        StyleRarity rarity = StyleRarity.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(key)).result().orElse(null);
        if (rarity == null) {
            context.getSource().sendFailure(Component.translatable(
                    "command.combat_styles.unknown_rarity", key));
            return 0;
        }
        ItemStack book = BookRolls.create(rarity, player.getRandom());
        player.getInventory().placeItemBackInInventory(book);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.book_given", rarity.key()), true);
        return 1;
    }

    private static int unetch(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ItemStack stack = context.getSource().getPlayerOrException().getMainHandItem();
        if (stack.isEmpty()) {
            return 0;
        }
        SequenceService.setEtched(stack, false);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.unetched"), true);
        return 1;
    }

    private static int improvise(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!player.getAbilities().instabuild) {
            context.getSource().sendFailure(Component.translatable(
                    "command.combat_styles.creative_only"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.translatable(
                    "command.combat_styles.nothing_held"));
            return 0;
        }
        if (SequenceService.nativeAttributes(stack) != null) {
            context.getSource().sendFailure(Component.translatable(
                    "command.combat_styles.already_native"));
            return 0;
        }
        boolean improvised = !SyntheticAttributes.isImprovised(stack);
        SyntheticAttributes.setImprovised(stack, improvised);
        context.getSource().sendSuccess(() -> Component.translatable(improvised
                ? "command.combat_styles.improvised_on"
                : "command.combat_styles.improvised_off"), true);
        return 1;
    }

    private static int listCustomStyles(CommandContext<CommandSourceStack> context) {
        Map<String, List<CustomStyle>> grouped = CustomStyles.byGroup();
        if (grouped.isEmpty()) {
            context.getSource().sendFailure(Component.translatable(
                    "command.combat_styles.no_custom_styles"));
            return 0;
        }
        for (Map.Entry<String, List<CustomStyle>> group : grouped.entrySet()) {
            context.getSource().sendSuccess(() -> Component.literal(group.getKey())
                    .withStyle(ChatFormatting.GOLD), false);
            for (CustomStyle style : group.getValue()) {
                context.getSource().sendSuccess(() -> Component.literal("  " + style.id())
                        .withStyle(ChatFormatting.GRAY), false);
            }
        }
        return CustomStyles.size();
    }

    private static int listRolls(CommandContext<CommandSourceStack> context) {
        int count = WeaponRolls.count();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.roll_count", count), false);
        return count;
    }

    private static int setIgnoreMinimum(CommandContext<CommandSourceStack> context, boolean ignore)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("command.combat_styles.empty"));
            return 0;
        }
        SequenceLimits.setIgnoresMinimum(stack, ignore);
        context.getSource().sendSuccess(() -> Component.translatable(
                ignore ? KEY_MINIMUM_IGNORED : KEY_MINIMUM_ENFORCED), true);
        return 1;
    }

    private static int setSlotLimit(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ItemStack stack = context.getSource().getPlayerOrException().getMainHandItem();
        if (stack.isEmpty()) {
            return 0;
        }
        int slots = (int) DoubleArgumentType.getDouble(context, ARG_VALUE);
        stack.set(ModComponents.SLOT_LIMIT.get(), slots);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.slot_limit_set", slots), true);
        return slots;
    }

    private static int setBudgetOverride(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ItemStack stack = context.getSource().getPlayerOrException().getMainHandItem();
        if (stack.isEmpty()) {
            return 0;
        }
        double value = DoubleArgumentType.getDouble(context, ARG_VALUE);
        BudgetSources.setOverride(stack, value);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.budget_set", StyleText.formatDecimal(value)), true);
        return (int) Math.round(value);
    }

    private static int clearBudgetOverride(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        ItemStack stack = context.getSource().getPlayerOrException().getMainHandItem();
        if (stack.isEmpty()) {
            return 0;
        }
        BudgetSources.setOverride(stack, null);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.budget_cleared"), true);
        return 1;
    }

    private static int reloadDefaults(CommandContext<CommandSourceStack> context) {
        int loaded = DefaultStyleConfig.reload();
        NetworkManager.broadcastDefaultStyles(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.defaults_reloaded", loaded), true);
        return loaded;
    }

    private static int reloadAttacks(CommandContext<CommandSourceStack> context) {
        int loaded = StyleConfigs.reloadAll();
        NetworkManager.broadcastDefaultStyles(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.attacks_reloaded", loaded, StyleRegistry.size()), true);
        return loaded;
    }

    private static int refreshAttacks(CommandContext<CommandSourceStack> context, String namespace) {
        AttackOverrides.Refresh refresh = AttackOverrides.refresh(namespace);
        NetworkManager.broadcastDefaultStyles(context.getSource().getServer());
        if (refresh.moved() == 0) {
            context.getSource().sendFailure(Component.translatable(
                    "command.combat_styles.attacks_refresh_none", namespace));
            return 0;
        }
        for (String line : refresh.lines()) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.combat_styles.attacks_refreshed", refresh.moved(), namespace,
                refresh.backup().toString()), true);
        return refresh.moved();
    }

    private static int inspect(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!SequenceService.isSupported(stack)) {
            context.getSource().sendFailure(Component.translatable("command.combat_styles.unsupported"));
            return 0;
        }
        String category = SequenceService.categoryOf(stack);
        context.getSource().sendSuccess(() -> Component.translatable("command.combat_styles.inspect",
                StyleText.prettify(category),
                SequenceService.sequenceOf(stack, player.getUUID()).size()), false);
        for (String styleId : SequenceService.sequenceOf(stack, player.getUUID()).styleIds()) {
            context.getSource().sendSuccess(() -> Component.literal(" - " + styleId), false);
        }
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        SequenceService.clear(stack);
        context.getSource().sendSuccess(() -> Component.translatable("command.combat_styles.cleared"), true);
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> context, int copies)
            throws CommandSyntaxException {
        String styleId = StyleRegistry.canonicalOf(
                ResourceLocationArgument.getId(context, ARG_STYLE).toString());
        if (!StyleRegistry.contains(styleId)) {
            context.getSource().sendFailure(Component.translatable("command.combat_styles.unknown_style", styleId));
            return 0;
        }
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, ARG_TARGETS);
        for (ServerPlayer target : targets) {
            for (int i = 0; i < copies; i++) {
                target.getInventory().placeItemBackInInventory(StylePageItem.of(styleId));
            }
        }
        context.getSource().sendSuccess(() -> Component.translatable("command.combat_styles.given",
                styleId, targets.size()), true);
        return targets.size();
    }

    private static int setLocked(CommandContext<CommandSourceStack> context, boolean locked)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("command.combat_styles.no_item"));
            return 0;
        }
        SequenceService.setLocked(stack, locked);
        context.getSource().sendSuccess(() -> Component.translatable(locked
                ? "command.combat_styles.locked"
                : "command.combat_styles.unlocked"), true);
        return 1;
    }

    private static int listAffixes(CommandContext<CommandSourceStack> context) {
        if (AffixRegistry.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("command.combat_styles.no_affixes"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("command.combat_styles.affix_header",
                AffixRegistry.size()), false);
        for (StyleAffix affix : AffixRegistry.all()) {
            context.getSource().sendSuccess(() -> Component.literal(" - " + affix.serializedId()
                    + " (" + affix.rarity().key() + ", x" + affix.costModifier() + ")"), false);
        }
        return AffixRegistry.size();
    }

    private static int learnAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return learnAll(context, 1);
    }

    private static int learnAllCopies(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return learnAll(context, IntegerArgumentType.getInteger(context, ARG_COUNT));
    }

    private static int learnAll(CommandContext<CommandSourceStack> context, int copies)
            throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, ARG_TARGETS);
        for (ServerPlayer target : targets) {
            for (StylePage page : StyleRegistry.all()) {
                for (int i = 0; i < copies; i++) {
                    StyleProgression.learn(target, page.styleId());
                }
            }
            NetworkManager.syncLearned(target);
        }
        context.getSource().sendSuccess(() -> Component.translatable("command.combat_styles.learned_all",
                StyleRegistry.size(), targets.size()), true);
        return targets.size();
    }

    private static int forgetAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, ARG_TARGETS);
        for (ServerPlayer target : targets) {
            LearnedStyles learned = StyleProgression.of(target);
            if (learned != null) {
                learned.clear();
            }
            NetworkManager.syncLearned(target);
        }
        context.getSource().sendSuccess(() -> Component.translatable("command.combat_styles.forgot_all",
                targets.size()), true);
        return targets.size();
    }

    private static int giveAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, ARG_TARGETS);
        for (ServerPlayer target : targets) {
            for (StylePage page : StyleRegistry.all()) {
                target.getInventory().placeItemBackInInventory(StylePageItem.of(page.styleId()));
            }
        }
        context.getSource().sendSuccess(() -> Component.translatable("command.combat_styles.given_all",
                StyleRegistry.size(), targets.size()), true);
        return targets.size();
    }
}
