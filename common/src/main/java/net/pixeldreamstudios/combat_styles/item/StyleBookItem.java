package net.pixeldreamstudios.combat_styles.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.client.ClientStyleData;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.progression.StyleProgression;
import net.pixeldreamstudios.combat_styles.style.CarriedStyles;
import net.pixeldreamstudios.combat_styles.style.StyleCarrier;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.List;

public class StyleBookItem extends Item implements StyleCarrier {

    private static final String KEY_EMPTY = "item.combat_styles.style_book.empty";
    private static final String KEY_CONTAINS = "item.combat_styles.style_book.contains";
    private static final String KEY_SUMMARY = "tooltip.combat_styles.book_summary";
    private static final String KEY_HINT = "item.combat_styles.style_book.hint";
    private static final String KEY_LEARNED = "message.combat_styles.book_learned";
    private static final String KEY_NOTHING_NEW = "message.combat_styles.book_nothing_new";

    private final StyleRarity rarity;

    public StyleBookItem(Properties properties, StyleRarity rarity) {
        super(properties);
        this.rarity = rarity;
    }

    public StyleRarity rarity() {
        return rarity;
    }

    public static StyleSequence contentsOf(ItemStack stack) {
        return CarriedStyles.of(stack);
    }

    public static void setContents(ItemStack stack, List<StyleEntry> entries) {
        CarriedStyles.store(stack, entries == null || entries.isEmpty()
                ? StyleSequence.EMPTY
                : new StyleSequence("", entries));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack book = player.getItemInHand(hand);
        StyleSequence contents = contentsOf(book);
        if (contents.isEmpty()) {
            return InteractionResultHolder.pass(book);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.success(book);
        }

        int learned = 0;
        for (StyleEntry entry : contents.entries()) {
            if (StyleProgression.learn(player, entry)) {
                learned++;
            }
        }

        if (learned == 0) {
            player.displayClientMessage(Component.translatable(KEY_NOTHING_NEW)
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(book);
        }

        player.displayClientMessage(Component.translatable(KEY_LEARNED, learned)
                .withStyle(ChatFormatting.GREEN), true);
        book.shrink(1);
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkManager.syncLearned(serverPlayer);
        }
        return InteractionResultHolder.consume(book);
    }

    private void appendEntry(List<Component> tooltip, StyleEntry entry, boolean isNew) {
        StylePage page = StyleRegistry.get(entry.styleId());
        MutableComponent line = Component.literal(isNew ? "+ " : "· ")
                .withStyle(isNew ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
                .append(StyleText.name(entry, page)
                        .copy().withStyle(isNew ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
        for (StyleAffix affix : entry.resolveAffixes().affixes()) {
            line.append(Component.literal("  ◆").withStyle(affix.rarity().color()));
        }
        tooltip.add(line);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        StyleSequence contents = contentsOf(stack);
        if (contents.isEmpty()) {
            tooltip.add(Component.translatable(KEY_EMPTY).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        List<StyleEntry> fresh = new ArrayList<>();
        List<StyleEntry> known = new ArrayList<>();
        for (StyleEntry entry : contents.entries()) {
            if (ClientStyleData.hasLearned(entry.styleId())) {
                known.add(entry);
            } else {
                fresh.add(entry);
            }
        }

        tooltip.add(Component.translatable(KEY_CONTAINS, contents.size())
                .withStyle(rarity.color()));
        tooltip.add(Component.translatable(KEY_SUMMARY, fresh.size(), known.size())
                .withStyle(fresh.isEmpty() ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN));
        tooltip.add(Component.empty());

        for (StyleEntry entry : fresh) {
            appendEntry(tooltip, entry, true);
        }
        for (StyleEntry entry : known) {
            appendEntry(tooltip, entry, false);
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(KEY_HINT).withStyle(ChatFormatting.DARK_GRAY));
    }
}
