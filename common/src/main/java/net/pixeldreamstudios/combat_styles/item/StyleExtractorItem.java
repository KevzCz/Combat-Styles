package net.pixeldreamstudios.combat_styles.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.progression.StyleProgression;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.CarriedStyles;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleCarrier;
import net.pixeldreamstudios.combat_styles.style.StyleCost;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;
import net.pixeldreamstudios.combat_styles.style.StyleText;
import net.pixeldreamstudios.combat_styles.style.StyleTooltip;

import java.util.List;

public class StyleExtractorItem extends Item implements StyleCarrier {

    private static final String KEY_EMPTY = "item.combat_styles.style_extractor.empty";
    private static final String KEY_STORED = "item.combat_styles.style_extractor.stored";
    private static final String KEY_HINT = "item.combat_styles.style_extractor.hint";
    private static final String KEY_APPLY_HINT = "item.combat_styles.style_extractor.apply_hint";
    private static final String KEY_LEARNED = "message.combat_styles.style_learned";
    private static final String KEY_TUNED = "item.combat_styles.style_extractor.tuned";
    private static final String KEY_CUSTOM_SOUND = "item.combat_styles.style_extractor.custom_sound";

    public StyleExtractorItem(Properties properties) {
        super(properties);
    }

    public static StyleSequence storedOn(ItemStack stack) {
        return CarriedStyles.of(stack);
    }

    public static boolean hasStored(ItemStack stack) {
        return CarriedStyles.has(stack);
    }

    public static void store(ItemStack stack, StyleSequence sequence) {
        CarriedStyles.store(stack, sequence);
    }

    public static boolean extractEntry(ItemStack extractor, StyleEntry entry) {
        if (hasStored(extractor) || entry == null) {
            return false;
        }
        store(extractor, new StyleSequence("", List.of(entry)));
        return true;
    }

    public static StyleEntry storedEntry(ItemStack extractor) {
        StyleSequence stored = storedOn(extractor);
        return stored.isEmpty() ? null : stored.entries().getFirst();
    }

    public static boolean canEtch(ItemStack weapon) {
        if (!SequenceService.isSupported(weapon) || SequenceService.isLocked(weapon)) {
            return false;
        }
        return !CombatStylesConfig.get().etchOncePerWeapon()
                || !SequenceService.isEtched(weapon);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack extractor = player.getItemInHand(hand);
        StyleEntry entry = storedEntry(extractor);
        if (entry == null) {
            return InteractionResultHolder.pass(extractor);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.success(extractor);
        }

        StyleProgression.learn(player, entry);
        player.displayClientMessage(Component.translatable(KEY_LEARNED,
                StyleText.name(entry, StyleRegistry.get(entry.styleId())))
                .withStyle(ChatFormatting.GREEN), true);

        extractor.shrink(1);
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkManager.syncLearned(serverPlayer);
        }
        return InteractionResultHolder.consume(extractor);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        StyleSequence stored = storedOn(stack);
        if (stored.isEmpty()) {
            tooltip.add(Component.translatable(KEY_EMPTY).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable(KEY_HINT).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        StyleEntry entry = stored.entries().getFirst();
        StylePage page = StyleRegistry.get(entry.styleId());
        if (page == null) {
            tooltip.add(Component.translatable(KEY_STORED)
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" ")
                            .append(StyleText.name(entry, null))
                            .withStyle(ChatFormatting.WHITE)));
            return;
        }

        List<StyleAffix> affixes = entry.resolveAffixes().affixes();
        double cost = StyleCost.of(page);
        for (StyleAffix affix : affixes) {
            cost *= affix.costModifier();
        }

        tooltip.add(Component.translatable(KEY_STORED)
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" ")
                        .append(StyleText.name(entry, page))
                        .withStyle(ChatFormatting.WHITE)));
        tooltip.add(StyleTooltip.subtitle(page, cost));

        if (!affixes.isEmpty()) {
            tooltip.add(Component.empty());
            StyleTooltip.appendAffixes(affixes, tooltip);
        }

        AttackOverride override = effectiveOverride(entry, page);
        tooltip.add(Component.empty());
        StyleTooltip.appendStatGrid(AttackOverrides.applyTo(page, override), tooltip);
        StyleTooltip.appendTiming(override, tooltip);
        StyleTooltip.appendAttributes(override, tooltip);

        if (entry.attackOverride().isPresent()) {
            tooltip.add(Component.translatable(KEY_TUNED).withStyle(ChatFormatting.AQUA));
        }
        if (entry.hasCustomSound()) {
            tooltip.add(Component.translatable(KEY_CUSTOM_SOUND)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        ItemTooltip.expandable(tooltip,
                lines -> StyleTooltip.appendDetails(page, entry.soundId().orElse(null), lines),
                lines -> lines.add(StyleTooltip.expandHint()));

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(KEY_APPLY_HINT).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static AttackOverride effectiveOverride(StyleEntry entry, StylePage page) {
        AttackOverride fromStyle = AttackOverrides.get(page.styleId());
        AttackOverride captured = entry.attackOverride().orElse(null);
        if (fromStyle == null) {
            return captured;
        }
        return captured == null ? fromStyle : fromStyle.overriddenBy(captured);
    }
}
