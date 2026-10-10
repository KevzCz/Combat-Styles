package net.pixeldreamstudios.combat_styles.loot;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.item.StylePageItem;
import net.pixeldreamstudios.combat_styles.style.CustomStyle;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StylePool;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.ArrayList;
import java.util.List;

public final class StyleLoot {

    private StyleLoot() {
    }

    public static List<String> rollAffixes(StylePage page, RandomSource random) {
        CustomStyle custom = CustomStyles.get(page.styleId());
        if (custom != null) {
            return custom.entry().affixIds();
        }
        CombatStylesConfig config = CombatStylesConfig.get();
        if (AffixRegistry.isEmpty() || random.nextDouble() >= config.affixChance()) {
            return List.of();
        }

        StyleAffix first = AffixRegistry.weightedPick(page, random);
        if (first == null) {
            return List.of();
        }

        List<String> picked = new ArrayList<>();
        picked.add(first.serializedId());

        int cap = Math.min(config.maxAffixesPerSlot(), config.maxLootAffixes());
        double chance = config.extraAffixChance();
        while (picked.size() < cap && chance > 0.0D && random.nextDouble() < chance) {
            StyleAffix extra = AffixRegistry.weightedPick(page, random);
            if (extra == null || picked.contains(extra.serializedId())) {
                break;
            }
            picked.add(extra.serializedId());
            chance *= config.extraAffixFalloff();
        }
        return picked;
    }

    public static StylePage weightedPick(RandomSource random) {
        List<StylePage> pages = StylePool.natural(StyleRegistry.distinct());
        if (pages.isEmpty()) {
            return null;
        }
        List<StylePage> weighted = new ArrayList<>();
        for (StylePage page : pages) {
            int weight = weightOf(page.rarity());
            for (int i = 0; i < weight; i++) {
                weighted.add(page);
            }
        }
        if (weighted.isEmpty()) {
            return StyleRegistry.variantOf(pages.get(random.nextInt(pages.size())), random);
        }
        return StyleRegistry.variantOf(weighted.get(random.nextInt(weighted.size())), random);
    }

    public static ItemStack pageOf(StyleRarity rarity, RandomSource random) {
        if (rarity == null) {
            StylePage any = weightedPick(random);
            return any == null ? ItemStack.EMPTY : StylePageItem.of(any.styleId(), rollAffixes(any, random));
        }
        List<StylePage> pages = new ArrayList<>();
        for (StylePage page : StylePool.natural(StyleRegistry.distinct())) {
            if (page.rarity() == rarity) {
                pages.add(page);
            }
        }
        if (pages.isEmpty()) {
            return ItemStack.EMPTY;
        }
        StylePage page = StyleRegistry.variantOf(pages.get(random.nextInt(pages.size())), random);
        return StylePageItem.of(page.styleId(), rollAffixes(page, random));
    }

    private static int weightOf(StyleRarity rarity) {
        return CombatStylesConfig.get().rarityWeight(rarity.name());
    }

    public static StyleRarity weightedRarity(RandomSource random) {
        int total = 0;
        for (StyleRarity rarity : StyleRarity.values()) {
            total += weightOf(rarity);
        }
        if (total <= 0) {
            return StyleRarity.COMMON;
        }
        int target = random.nextInt(total);
        for (StyleRarity rarity : StyleRarity.values()) {
            target -= weightOf(rarity);
            if (target < 0) {
                return rarity;
            }
        }
        return StyleRarity.COMMON;
    }
}
