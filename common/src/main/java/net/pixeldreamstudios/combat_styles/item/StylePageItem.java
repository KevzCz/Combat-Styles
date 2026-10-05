package net.pixeldreamstudios.combat_styles.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.pixeldreamstudios.combat_styles.affix.ResolvedAffixes;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.client.ClientStyleData;
import net.pixeldreamstudios.combat_styles.client.tooltip.ArcTooltipData;
import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.progression.StyleProgression;
import net.pixeldreamstudios.combat_styles.style.AttackOverrides;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleCost;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleText;
import net.pixeldreamstudios.combat_styles.style.StyleTooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StylePageItem extends Item {

    private static final String KEY_UNKNOWN = "item.combat_styles.style_page.unknown";
    private static final String KEY_HINT = "tooltip.combat_styles.page_hint";
    private static final String KEY_LEARNED_STATE = "tooltip.combat_styles.page_learned";
    private static final String KEY_UNLEARNED = "tooltip.combat_styles.page_unlearned";
    private static final String KEY_AFFIXED_NAME = "item.combat_styles.style_page.affixed";
    private static final String KEY_LEARNED = "message.combat_styles.learned";
    private static final String KEY_ALREADY_KNOWN = "message.combat_styles.already_known";

    public StylePageItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(String styleId) {
        return of(styleId, List.of());
    }

    public static ItemStack of(String styleId, String affixId) {
        return of(styleId, affixId == null || affixId.isBlank() ? List.of() : List.of(affixId));
    }

    public static ItemStack of(String styleId, List<String> affixIds) {
        ItemStack stack = new ItemStack(ModItems.STYLE_PAGE.get());
        stack.set(ModComponents.STYLE_ID.get(), styleId);
        StylePage page = StyleRegistry.get(styleId);

        List<StyleAffix> affixes = ResolvedAffixes.of(affixIds).affixes();
        if (!affixes.isEmpty()) {
            List<String> ids = new ArrayList<>();
            for (StyleAffix affix : affixes) {
                ids.add(affix.serializedId());
            }
            ids = StyleEntry.sorted(ids);
            stack.set(ModComponents.AFFIX_IDS.get(), ids);
            stack.set(DataComponents.RARITY, highestRarity(affixes).itemRarity());
        } else if (page != null) {
            stack.set(DataComponents.RARITY, page.rarity().itemRarity());
        }
        return stack;
    }

    private static StyleRarity highestRarity(List<StyleAffix> affixes) {
        StyleRarity best = affixes.getFirst().rarity();
        for (StyleAffix affix : affixes) {
            if (affix.rarity().ordinal() > best.ordinal()) {
                best = affix.rarity();
            }
        }
        return best;
    }

    public static String styleIdOf(ItemStack stack) {
        return stack.get(ModComponents.STYLE_ID.get());
    }

    public static List<String> affixIdsOf(ItemStack stack) {
        List<String> ids = stack.get(ModComponents.AFFIX_IDS.get());
        if (ids != null && !ids.isEmpty()) {
            return StyleEntry.sorted(ids);
        }
        String legacy = stack.get(ModComponents.AFFIX_ID.get());
        return legacy == null || legacy.isBlank() ? List.of() : List.of(legacy);
    }

    public static List<StyleAffix> affixesOf(ItemStack stack) {
        return ResolvedAffixes.of(affixIdsOf(stack)).affixes();
    }

    public static StylePage pageOf(ItemStack stack) {
        return StyleRegistry.get(styleIdOf(stack));
    }

    private static String keyOf(StylePage page, ItemStack stack) {
        List<String> known = new ArrayList<>();
        for (StyleAffix affix : affixesOf(stack)) {
            known.add(affix.serializedId());
        }
        return StyleEntry.key(page.styleId(), StyleEntry.sorted(known));
    }

    @Override
    public Component getName(ItemStack stack) {
        StylePage page = pageOf(stack);
        if (page == null) {
            return Component.translatable(KEY_UNKNOWN);
        }
        List<StyleAffix> affixes = affixesOf(stack);
        if (affixes.isEmpty()) {
            return Component.translatable(getDescriptionId(), StyleText.name(page));
        }

        MutableComponent affixName = Component.empty();
        for (int i = 0; i < affixes.size(); i++) {
            if (i > 0) {
                affixName.append(" ");
            }
            StyleAffix affix = affixes.get(i);
            affixName.append(Component.translatableWithFallback(affix.nameKey(), affix.fallbackName()));
        }
        return Component.translatable(KEY_AFFIXED_NAME, affixName, StyleText.name(page))
                .withStyle(highestRarity(affixes).color());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        StylePage page = pageOf(stack);
        if (page == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (!learn(player, page, stack)) {
            player.displayClientMessage(Component.translatable(KEY_ALREADY_KNOWN, getName(stack))
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.translatable(KEY_LEARNED, getName(stack))
                .withStyle(ChatFormatting.GREEN), true);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.6F, 1.4F);
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkManager.syncLearned(serverPlayer);
        }
        return InteractionResultHolder.success(stack);
    }

    private static boolean learn(Player player, StylePage page, ItemStack stack) {
        if (CustomStyles.get(page.styleId()) == null) {
            return StyleProgression.learn(player, keyOf(page, stack));
        }
        StyleEntry entry = SequenceService.entryFor(page.styleId());
        if (entry == null) {
            return StyleProgression.learn(player, keyOf(page, stack));
        }
        List<String> affixIds = affixIdsOf(stack);
        return StyleProgression.learn(player,
                affixIds.isEmpty() ? entry : entry.withAffixes(affixIds));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return ArcTooltipData.of(pageOf(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        StylePage page = pageOf(stack);
        if (page == null) {
            tooltip.add(Component.translatable(KEY_UNKNOWN).withStyle(ChatFormatting.DARK_RED));
            return;
        }
        List<StyleAffix> affixes = affixesOf(stack);
        double cost = StyleCost.of(page);
        for (StyleAffix affix : affixes) {
            cost *= affix.costModifier();
        }
        tooltip.add(StyleTooltip.subtitle(page, cost));

        if (!affixes.isEmpty()) {
            tooltip.add(Component.empty());
            StyleTooltip.appendAffixes(affixes, tooltip);
        }

        tooltip.add(Component.empty());
        StyleTooltip.appendStatGrid(page, tooltip);
        StyleTooltip.appendTiming(AttackOverrides.get(page.styleId()), tooltip);
        StyleTooltip.appendAttributes(AttackOverrides.get(page.styleId()), tooltip);

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(
                        ClientStyleData.hasLearned(page.styleId()) ? KEY_LEARNED_STATE : KEY_UNLEARNED)
                .withStyle(ClientStyleData.hasLearned(page.styleId())
                        ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN));

        ItemTooltip.expandable(tooltip,
                lines -> StyleTooltip.appendDetails(page, lines),
                lines -> lines.add(StyleTooltip.expandHint()));

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(KEY_HINT).withStyle(ChatFormatting.DARK_GRAY));
    }

}
