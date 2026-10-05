package net.pixeldreamstudios.combat_styles.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.style.CustomStyle;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StylePool;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StyleLoot {

    private static final String CHEST_PREFIX = "chests/";
    private static final String ENTITY_PREFIX = "entities/";
    private static final String GAMEPLAY_PREFIX = "gameplay/";
    private static final String WILDCARD = "*";

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

    public static boolean isChestTable(ResourceLocation table) {
        return table != null && table.getPath().startsWith(CHEST_PREFIX);
    }

    public static boolean isEntityTable(ResourceLocation table) {
        return table != null && table.getPath().startsWith(ENTITY_PREFIX);
    }

    public static boolean isGameplayTable(ResourceLocation table) {
        return table != null && table.getPath().startsWith(GAMEPLAY_PREFIX);
    }

    public static boolean isBlocked(ResourceLocation table) {
        if (table == null) {
            return true;
        }
        String id = table.toString().toLowerCase(Locale.ROOT);
        for (String blocked : CombatStylesConfig.get().lootTableBlocklist()) {
            String entry = blocked.toLowerCase(Locale.ROOT);
            if (entry.equals(id)
                    || (entry.endsWith(WILDCARD)
                            && id.startsWith(entry.substring(0, entry.length() - 1)))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEligible(ResourceLocation table) {
        return CombatStylesConfig.get().lootEnabled()
                && !StyleRegistry.isEmpty()
                && !isBlocked(table)
                && (isChestTable(table) || isEntityTable(table)
                        || (CombatStylesConfig.get().gameplayLootEnabled() && isGameplayTable(table)));
    }

    public static double chanceFor(ResourceLocation table) {
        CombatStylesConfig config = CombatStylesConfig.get();
        return isChestTable(table) ? config.chestLootChance() : config.mobDropChance();
    }

    public static StylePage weightedPick(RandomSource random) {
        List<StylePage> pages = StylePool.natural(StyleRegistry.all());
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
            return pages.get(random.nextInt(pages.size()));
        }
        return weighted.get(random.nextInt(weighted.size()));
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
