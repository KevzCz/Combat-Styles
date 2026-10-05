package net.pixeldreamstudios.combat_styles.loot;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;
import net.pixeldreamstudios.combat_styles.config.DefinitionLayer;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StylePool;
import net.pixeldreamstudios.combat_styles.style.StyleRarity;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BookRolls {

    private static final int GLOBAL_PICK_ATTEMPTS = 8;

    private static final Map<StyleRarity, List<BookRoll>> ROLLS =
            new EnumMap<>(StyleRarity.class);
    private static final String LABEL = "style book";

    public static final DefinitionLayer<BookRoll> DEFINITIONS = new DefinitionLayer<>(LABEL,
            ConfigPaths.STYLE_BOOKS, BookRoll.CODEC, BookRolls::rebuild);

    private BookRolls() {
    }

    private static void rebuild() {
        rebuild(List.copyOf(DEFINITIONS.authoritative().values()));
    }

    private static synchronized void rebuild(List<BookRoll> loaded) {
        ROLLS.clear();
        if (loaded == null) {
            return;
        }
        for (BookRoll roll : loaded) {
            if (roll.mode() == BookRoll.Mode.VARIANT) {
                ROLLS.computeIfAbsent(roll.rarity(), key -> new ArrayList<>()).add(roll);
            }
        }
        for (BookRoll roll : loaded) {
            if (roll.mode() != BookRoll.Mode.INJECT) {
                continue;
            }
            List<BookRoll> variants = ROLLS.get(roll.rarity());
            if (variants == null || variants.isEmpty()) {
                CombatStyles.LOGGER.warn(
                        "Style book for {} uses mode 'inject' but no variant defines that rarity",
                        roll.rarity());
                continue;
            }
            for (int i = 0; i < variants.size(); i++) {
                variants.set(i, variants.get(i).withInjected(roll));
            }
        }
    }

    private static BookRoll variantFor(StyleRarity rarity, RandomSource random) {
        List<BookRoll> variants = ROLLS.get(rarity);
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        if (variants.size() == 1) {
            return variants.getFirst();
        }
        int total = 0;
        for (BookRoll variant : variants) {
            total += Math.max(0, variant.rollWeight());
        }
        if (total <= 0) {
            return variants.get(random.nextInt(variants.size()));
        }
        int target = random.nextInt(total);
        for (BookRoll variant : variants) {
            target -= Math.max(0, variant.rollWeight());
            if (target < 0) {
                return variant;
            }
        }
        return variants.getLast();
    }

    public static boolean isEmpty() {
        return ROLLS.isEmpty();
    }

    public static int count() {
        int total = 0;
        for (List<BookRoll> variants : ROLLS.values()) {
            total += variants.size();
        }
        return total;
    }

    public static List<BookRoll> all() {
        List<BookRoll> flattened = new ArrayList<>();
        for (List<BookRoll> variants : ROLLS.values()) {
            flattened.addAll(variants);
        }
        return List.copyOf(flattened);
    }

    public static ItemStack create(StyleRarity rarity, RandomSource random) {
        ItemStack book = new ItemStack(BookProvider.bookFor(rarity));
        fill(book, rarity, random);
        return book;
    }

    public static void fill(ItemStack book, StyleRarity rarity, RandomSource random) {
        BookRoll roll = variantFor(rarity, random);
        if (roll == null) {
            return;
        }

        int min = Math.max(1, roll.minAttacks());
        int max = Math.max(min, roll.maxAttacks());
        int count = min + random.nextInt(max - min + 1);

        List<StyleEntry> chosen = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            StyleEntry entry = pick(roll, random);
            if (entry == null) {
                continue;
            }
            if (!roll.allowDuplicates() && containsStyle(chosen, entry)) {
                continue;
            }
            chosen.add(entry);
        }
        BookProvider.setContents(book, chosen);
    }

    private static boolean containsStyle(List<StyleEntry> chosen, StyleEntry candidate) {
        for (StyleEntry entry : chosen) {
            if (entry.styleId().equals(candidate.styleId())
                    && entry.affixIds().equals(candidate.affixIds())) {
                return true;
            }
        }
        return false;
    }

    private static StyleEntry pick(BookRoll roll, RandomSource random) {
        StyleEntry base = rollSource(roll, random);
        if (base == null) {
            return null;
        }

        StylePage page = StyleRegistry.get(base.styleId());
        if (page == null
                || (roll.affixPools().isEmpty() && roll.affixChance() <= 0.0F)) {
            return base;
        }

        List<String> affixIds = new ArrayList<>(base.affixIds());
        if (roll.affixPools().isEmpty()) {
            rollFlatAffixes(roll, page, affixIds, random);
        } else {
            rollSlottedAffixes(roll, page, affixIds, random);
        }
        if (affixIds.size() == base.affixIds().size()) {
            return base;
        }
        return new StyleEntry(base.styleId(), base.soundId(), StyleEntry.sorted(affixIds),
                base.attackOverride(), base.name(), base.nameKey(),
                base.affixTuning(), base.customStyleId());
    }

    private static void rollFlatAffixes(BookRoll roll, StylePage page, List<String> affixIds,
                                        RandomSource random) {
        int cap = Math.max(0, roll.maxAffixes());
        List<BookRoll.WeightedAffix> affixPool = roll.affixPool();
        while (affixIds.size() < cap && random.nextFloat() < roll.affixChance()) {
            String picked = pickAffix(affixPool, page, affixIds, random);
            if (picked == null) {
                break;
            }
            affixIds.add(picked);
        }
    }

    private static void rollSlottedAffixes(BookRoll roll, StylePage page, List<String> affixIds,
                                           RandomSource random) {
        for (BookRoll.AffixSlot slot : roll.affixPools()) {
            if (slot.chance() < 1.0F && random.nextFloat() >= slot.chance()) {
                continue;
            }
            int span = slot.max() - slot.min();
            int wanted = slot.min() + (span <= 0 ? 0 : random.nextInt(span + 1));
            for (int i = 0; i < wanted; i++) {
                String picked = pickAffix(slot.affixes(), page, affixIds, random);
                if (picked == null) {
                    break;
                }
                affixIds.add(picked);
            }
        }
    }

    private static String pickAffix(List<BookRoll.WeightedAffix> pool, StylePage page,
                                    List<String> taken, RandomSource random) {
        if (!pool.isEmpty()) {
            return fromAffixPool(pool, page, taken, random);
        }
        for (int attempt = 0; attempt < GLOBAL_PICK_ATTEMPTS; attempt++) {
            String candidate = serializedId(AffixRegistry.weightedPick(page, random));
            if (candidate != null && !taken.contains(candidate)) {
                return candidate;
            }
        }
        for (StyleAffix affix : AffixRegistry.applicableTo(page)) {
            if (!taken.contains(affix.serializedId())) {
                return affix.serializedId();
            }
        }
        return null;
    }

    private static String serializedId(StyleAffix affix) {
        return affix == null ? null : affix.serializedId();
    }

    private static String fromAffixPool(List<BookRoll.WeightedAffix> affixPool, StylePage page,
                                        List<String> taken, RandomSource random) {
        List<StyleAffix> allowed = AffixRegistry.applicableTo(page);
        List<BookRoll.WeightedAffix> available = new ArrayList<>();
        int total = 0;
        for (BookRoll.WeightedAffix weighted : affixPool) {
            int weight = Math.max(0, weighted.weight());
            if (weight <= 0) {
                continue;
            }
            boolean usable = false;
            for (String id : weighted.affixes()) {
                StyleAffix affix = AffixRegistry.get(id);
                if (affix != null && !taken.contains(id) && allowed.contains(affix)) {
                    usable = true;
                    break;
                }
            }
            if (!usable) {
                continue;
            }
            available.add(weighted);
            total += weight;
        }
        if (total <= 0) {
            return null;
        }
        int target = random.nextInt(total);
        for (BookRoll.WeightedAffix weighted : available) {
            target -= Math.max(0, weighted.weight());
            if (target < 0) {
                return usableAffix(weighted, page, taken, random);
            }
        }
        return usableAffix(available.getLast(), page, taken, random);
    }

    private static String usableAffix(BookRoll.WeightedAffix weighted, StylePage page,
                                      List<String> taken, RandomSource random) {
        List<StyleAffix> allowed = AffixRegistry.applicableTo(page);
        List<String> options = new ArrayList<>();
        for (String id : weighted.affixes()) {
            StyleAffix affix = AffixRegistry.get(id);
            if (affix != null && !taken.contains(id) && allowed.contains(affix)) {
                options.add(id);
            }
        }
        if (options.isEmpty()) {
            return null;
        }
        return options.get(random.nextInt(options.size()));
    }

    private static StyleEntry rollSource(BookRoll roll, RandomSource random) {
        if (roll.pool().isEmpty()) {
            return fromRegistry(roll, random);
        }
        int authored = 0;
        for (BookRoll.Weighted weighted : roll.pool()) {
            if (isResolvable(weighted.entry())) {
                authored += Math.max(0, weighted.weight());
            }
        }
        int randomWeight = Math.max(0, roll.randomWeight());
        if (authored <= 0) {
            return fromRegistry(roll, random);
        }
        if (randomWeight > 0 && random.nextInt(authored + randomWeight) < randomWeight) {
            StyleEntry fromRegistry = fromRegistry(roll, random);
            if (fromRegistry != null) {
                return fromRegistry;
            }
        }
        return fromPool(roll, random);
    }

    private static boolean isResolvable(StyleEntry entry) {
        return entry != null
                && StyleRegistry.get(CustomStyles.resolve(entry).styleId()) != null;
    }

    private static StyleEntry fromPool(BookRoll roll, RandomSource random) {
        List<BookRoll.Weighted> available = new ArrayList<>();
        int total = 0;
        for (BookRoll.Weighted weighted : roll.pool()) {
            int weight = Math.max(0, weighted.weight());
            if (weight <= 0 || !isResolvable(weighted.entry())) {
                continue;
            }
            available.add(weighted);
            total += weight;
        }
        if (total <= 0) {
            return null;
        }
        int target = random.nextInt(total);
        for (BookRoll.Weighted weighted : available) {
            target -= Math.max(0, weighted.weight());
            if (target < 0) {
                return rolled(weighted, roll, random);
            }
        }
        return rolled(available.getLast(), roll, random);
    }

    private static StyleEntry rolled(BookRoll.Weighted weighted, BookRoll roll,
                                     RandomSource random) {
        StyleEntry entry = CustomStyles.resolve(weighted.entry());
        BookRoll.Rolled ranges = weighted.roll().orElse(roll.roll());
        if (ranges.isEmpty()) {
            return entry;
        }
        return entry.withAttackOverride(
                ranges.applyTo(entry.attackOverride().orElse(null), random));
    }

    private static StyleEntry fromRegistry(BookRoll roll, RandomSource random) {
        List<StylePage> pages = new ArrayList<>();
        if (roll.categories().isEmpty()) {
            pages.addAll(StylePool.natural(StyleRegistry.all()));
        } else {
            for (String category : roll.categories()) {
                pages.addAll(StylePool.natural(StyleRegistry.forCategory(category)));
            }
        }
        if (pages.isEmpty()) {
            return null;
        }
        StyleEntry entry = SequenceService.entryFor(pages.get(random.nextInt(pages.size())).styleId());
        if (entry == null || roll.roll().isEmpty()) {
            return entry;
        }
        return entry.withAttackOverride(
                roll.roll().applyTo(entry.attackOverride().orElse(null), random));
    }
}
