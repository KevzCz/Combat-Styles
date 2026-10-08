package net.pixeldreamstudios.combat_styles.loot;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.component.ModComponents;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;
import net.pixeldreamstudios.combat_styles.config.DefinitionLayer;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.DefaultStyles;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StylePool;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;
import net.pixeldreamstudios.combat_styles.style.SyntheticAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public final class WeaponRolls {

    private static final List<WeaponRoll> ROLLS = new CopyOnWriteArrayList<>();
    private static final String WILDCARD = "*";
    private static final String LABEL = "weapon roll";

    public static final DefinitionLayer<WeaponRoll> DEFINITIONS = new DefinitionLayer<>(LABEL,
            ConfigPaths.WEAPON_ROLLS, WeaponRoll.CODEC, WeaponRolls::rebuild);

    private WeaponRolls() {
    }

    private static void rebuild() {
        ROLLS.clear();
        ROLLS.addAll(DEFINITIONS.authoritative().values());
    }

    public static boolean isEmpty() {
        return ROLLS.isEmpty();
    }

    public static int count() {
        return ROLLS.size();
    }

    public static List<WeaponRoll> all() {
        return List.copyOf(ROLLS);
    }

    public static boolean applyForced(ItemStack stack, RandomSource random) {
        if (stack == null || stack.isEmpty() || ROLLS.isEmpty()
                || !CombatStylesConfig.get().weaponRollsEnabled()) {
            return false;
        }
        return drawOnto(stack, null, random);
    }

    public static boolean apply(ItemStack stack, ResourceLocation table, RandomSource random) {
        return CombatStylesConfig.get().weaponRollsEnabled() && rollFresh(stack, table, random);
    }

    public static boolean applyCrafted(ItemStack stack, RandomSource random) {
        return CombatStylesConfig.get().rollCraftedWeapons() && rollFresh(stack, null, random);
    }

    private static boolean rollFresh(ItemStack stack, ResourceLocation table, RandomSource random) {
        if (stack == null || stack.isEmpty() || ROLLS.isEmpty()
                || !SequenceService.customSequenceOf(stack).isEmpty()
                || !SequenceService.rolledSequenceOf(stack).isEmpty()) {
            return false;
        }
        return drawOnto(stack, table, random);
    }

    private static boolean drawOnto(ItemStack stack, ResourceLocation table, RandomSource random) {
        if (!SequenceService.isSupported(stack) && !adoptable(stack)) {
            return false;
        }
        String category = SequenceService.categoryOf(stack);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        List<WeaponRoll> candidates = new ArrayList<>();
        for (WeaponRoll roll : ROLLS) {
            if (roll.weight() > 0.0F && matches(roll, stack, itemId, category, table)) {
                candidates.add(roll);
            }
        }
        while (!candidates.isEmpty()) {
            WeaponRoll roll = candidates.remove(weightedIndex(candidates, random));
            if (rollOnto(stack, roll, category, random)) {
                return true;
            }
        }
        return false;
    }

    private static int weightedIndex(List<WeaponRoll> candidates, RandomSource random) {
        double total = 0.0D;
        for (WeaponRoll roll : candidates) {
            total += roll.weight();
        }
        double pick = random.nextDouble() * total;
        for (int index = 0; index < candidates.size(); index++) {
            pick -= candidates.get(index).weight();
            if (pick < 0.0D) {
                return index;
            }
        }
        return candidates.size() - 1;
    }

    public static String diagnose(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "no stack";
        }
        String category = SequenceService.categoryOf(stack);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        StringBuilder report = new StringBuilder();
        for (WeaponRoll roll : ROLLS) {
            if (!matches(roll, stack, itemId, category, null)) {
                continue;
            }
            List<StyleEntry> pool = poolFor(roll, category);
            int baseline = SequenceService.baselineOf(stack).size();
            int natives = StyleRegistry.nodesOf(SequenceService.baseAttributes(stack)).size();
            report.append("pool=").append(pool.size())
                    .append(" baseline=").append(baseline)
                    .append(" natives=").append(natives)
                    .append(" slotLimit=").append(DefaultStyles.slotLimitFor(stack));
            if (pool.isEmpty()) {
                for (StyleEntry entry : roll.styles()) {
                    report.append(" | unresolved:")
                            .append(entry.customStyleId().orElse(entry.rawStyleId()))
                            .append("->").append(entry.styleId());
                }
            }
            break;
        }
        return report.isEmpty() ? "no matching roll" : report.toString();
    }

    public static int countMatching(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        String category = SequenceService.categoryOf(stack);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int matching = 0;
        for (WeaponRoll roll : ROLLS) {
            if (matches(roll, stack, itemId, category, null)) {
                matching++;
            }
        }
        return matching;
    }

    private static boolean matchesTag(WeaponRoll roll, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (String raw : roll.tags()) {
            ResourceLocation tagId = ResourceLocation.tryParse(raw);
            if (tagId == null) {
                continue;
            }
            if (stack.is(TagKey.create(Registries.ITEM, tagId))) {
                return true;
            }
        }
        return false;
    }

    private static boolean adoptable(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (WeaponRoll roll : ROLLS) {
            if (roll.affixesOnly() || roll.rewritesSlots() || roll.appends()
                    || roll.styles().isEmpty()) {
                continue;
            }
            boolean named = (!roll.items().isEmpty() && itemId != null
                    && namesExactly(roll.items(), itemId.toString()))
                    || (!roll.tags().isEmpty() && matchesTag(roll, stack));
            if (named) {
                return true;
            }
        }
        return false;
    }

    private static boolean namesExactly(List<String> values, String candidate) {
        String needle = candidate.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).equals(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(WeaponRoll roll, ItemStack stack, ResourceLocation itemId,
                                   String category, ResourceLocation table) {
        if (!roll.tags().isEmpty() && !matchesTag(roll, stack)) {
            return false;
        }
        if (!roll.items().isEmpty()
                && (itemId == null || !containsId(roll.items(), itemId.toString()))) {
            return false;
        }
        if (!roll.categories().isEmpty()
                && (category == null || !containsId(roll.categories(), category))) {
            return false;
        }
        return roll.tables().isEmpty()
                || table == null
                || containsId(roll.tables(), table.toString());
    }

    private static boolean containsId(List<String> values, String candidate) {
        String needle = candidate.toLowerCase(Locale.ROOT);
        for (String value : values) {
            String entry = value.toLowerCase(Locale.ROOT);
            if (entry.equals(WILDCARD) || entry.equals(needle)) {
                return true;
            }
            if (entry.endsWith(WILDCARD)
                    && needle.startsWith(entry.substring(0, entry.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    private static boolean rollOnto(ItemStack stack, WeaponRoll roll,
                                    String category, RandomSource random) {
        roll.slotLimit().ifPresent(slots -> {
            if (slots > 0) {
                stack.set(ModComponents.SLOT_LIMIT.get(), slots);
            }
        });

        List<StyleEntry> existing = new ArrayList<>(SequenceService.baselineOf(stack).entries());
        if (existing.isEmpty()) {
            for (StylePage native_ : StyleRegistry.nodesOf(SequenceService.baseAttributes(stack))) {
                existing.add(StyleEntry.of(native_.styleId()));
            }
        }
        List<StyleEntry> combined = roll.affixesOnly()
                ? affixOnto(existing, roll, random)
                : rewrite(existing, roll, category, random,
                        DefaultStyles.keepsThroughRolls(stack));
        if (combined == null || combined.isEmpty()) {
            return false;
        }

        int limit = DefaultStyles.slotLimitFor(stack);
        if (combined.size() > limit) {
            combined = combined.subList(0, limit);
        }

        boolean adopting = SequenceService.nativeAttributes(stack) == null;
        String name = roll.name().orElseGet(() -> SequenceService.baselineOf(stack).name());
        SequenceService.storeRolled(stack, new StyleSequence(name, combined));
        if (adopting) {
            SyntheticAttributes.setImprovised(stack, true);
        }
        return true;
    }

    private static List<StyleEntry> affixOnto(List<StyleEntry> existing, WeaponRoll roll,
                                              RandomSource random) {
        if (existing.isEmpty()) {
            return null;
        }
        List<Integer> targets = targetsIn(roll, existing.size(), random);
        if (targets.isEmpty()) {
            return null;
        }

        boolean changed = false;
        for (int index : targets) {
            StyleEntry entry = existing.get(index);
            StylePage page = StyleRegistry.get(CustomStyles.resolve(entry).styleId());
            if (page == null) {
                continue;
            }
            List<String> affixIds = new ArrayList<>(entry.affixIds());
            if (!addAffixes(affixIds, page, roll, random)) {
                continue;
            }
            StyleEntry updated = new StyleEntry(entry.rawStyleId(), entry.soundId(),
                    StyleEntry.sorted(affixIds), entry.attackOverride(),
                    entry.name(), entry.nameKey(), entry.affixTuning(), entry.customStyleId());
            existing.set(index, named(updated, roll));
            changed = true;
        }
        return changed ? existing : null;
    }

    private static List<StyleEntry> rewrite(List<StyleEntry> existing, WeaponRoll roll,
                                            String category, RandomSource random,
                                            boolean sticky) {
        List<StyleEntry> pool = poolFor(roll, category);
        if (pool.isEmpty()) {
            return null;
        }

        if (roll.rewritesSlots()) {
            if (existing.isEmpty()) {
                return null;
            }
            List<Integer> targets = targetsIn(roll, existing.size(), random);
            if (targets.isEmpty()) {
                return null;
            }
            for (int index : targets) {
                StyleEntry previous = existing.get(index);
                StyleEntry picked = pick(pool, roll, random);
                existing.set(index, sticky ? carryOver(previous, picked) : picked);
            }
            return existing;
        }

        int min = Math.max(1, roll.minSlots());
        int max = Math.max(min, roll.maxSlots());
        int count = min + random.nextInt(max - min + 1);

        List<StyleEntry> rolled = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rolled.add(pick(pool, roll, random));
        }

        List<StyleEntry> combined = new ArrayList<>();
        if (roll.appends()) {
            combined.addAll(existing);
        } else if (sticky) {
            for (int index = 0; index < rolled.size() && index < existing.size(); index++) {
                rolled.set(index, carryOver(existing.get(index), rolled.get(index)));
            }
        }
        combined.addAll(rolled);
        return combined;
    }

    private static StyleEntry carryOver(StyleEntry previous, StyleEntry picked) {
        StyleEntry result = picked;
        if (previous.attackOverride().isPresent() && picked.attackOverride().isEmpty()) {
            result = result.withAttackOverride(previous.attackOverride().get());
        }
        if (!previous.affixTuning().isEmpty() && picked.affixTuning().isEmpty()) {
            result = result.withAffixTuning(previous.affixTuning());
        }
        return result;
    }

    private static List<Integer> targetsIn(WeaponRoll roll, int size, RandomSource random) {
        List<Integer> targets = new ArrayList<>();
        if (roll.slots().isEmpty()) {
            targets.add(random.nextInt(size));
            return targets;
        }
        for (int slot : roll.slots()) {
            int index = slot < 0 ? size + slot : slot;
            if (index >= 0 && index < size && !targets.contains(index)) {
                targets.add(index);
            }
        }
        return targets;
    }

    private static StyleEntry pick(List<StyleEntry> pool, WeaponRoll roll, RandomSource random) {
        StyleEntry picked = pool.get(random.nextInt(pool.size()));
        StyleEntry template = CustomStyles.resolve(picked);
        StylePage page = StyleRegistry.get(template.styleId());
        if (page == null) {
            return picked;
        }
        if (roll.styles().isEmpty() && picked.customStyleId().isEmpty()) {
            page = StyleRegistry.variantOf(page, random);
        }

        List<String> affixIds = new ArrayList<>(template.affixIds());
        addAffixes(affixIds, page, roll, random);

        boolean custom = picked.customStyleId().isPresent();
        StyleEntry entry = new StyleEntry(
                custom ? picked.rawStyleId() : page.styleId(),
                custom ? picked.soundId() : template.soundId(),
                StyleEntry.sorted(affixIds),
                custom ? picked.attackOverride() : template.attackOverride(),
                custom ? picked.name() : template.name(),
                custom ? picked.nameKey() : template.nameKey(),
                custom ? picked.affixTuning() : template.affixTuning(),
                picked.customStyleId());
        return template.name().isPresent() || template.nameKey().isPresent()
                ? entry
                : named(entry, roll);
    }

    private static boolean addAffixes(List<String> affixIds, StylePage page,
                                      WeaponRoll roll, RandomSource random) {
        int cap = Math.max(0, roll.maxAffixes());
        int before = affixIds.size();

        List<String> requestedOrder = new ArrayList<>(roll.affixes());
        Util.shuffle(requestedOrder, random);
        for (String requested : requestedOrder) {
            if (affixIds.size() >= cap) {
                break;
            }
            StyleAffix affix = AffixRegistry.get(requested);
            if (affix != null && !affixIds.contains(affix.serializedId())) {
                affixIds.add(affix.serializedId());
            }
        }

        while (roll.affixes().isEmpty() && affixIds.size() < cap
                && random.nextFloat() < roll.affixChance()) {
            StyleAffix affix = AffixRegistry.weightedPick(page, random);
            if (affix == null || affixIds.contains(affix.serializedId())) {
                break;
            }
            affixIds.add(affix.serializedId());
        }
        return affixIds.size() > before;
    }

    private static StyleEntry named(StyleEntry entry, WeaponRoll roll) {
        if (roll.names().isEmpty()) {
            return entry;
        }
        String name = roll.names().get(entry.styleId());
        if (name == null) {
            StylePage page = StyleRegistry.get(entry.styleId());
            if (page != null) {
                name = roll.names().get(page.styleId());
                if (name == null) {
                    name = roll.names().get(page.animation());
                }
            }
        }
        return name == null ? entry : entry.withName(name);
    }

    private static List<StyleEntry> poolFor(WeaponRoll roll, String category) {
        List<StyleEntry> pool = new ArrayList<>();
        if (!roll.styles().isEmpty()) {
            for (StyleEntry entry : roll.styles()) {
                if (entry.customStyleId().isPresent()
                        || StyleRegistry.get(entry.styleId()) != null) {
                    pool.add(entry);
                }
            }
            return pool;
        }
        List<StylePage> pages = StylePool.natural(category == null
                ? StyleRegistry.distinct()
                : StyleRegistry.distinctFor(category));
        for (StylePage page : pages) {
            StyleEntry entry = SequenceService.entryFor(page.styleId());
            if (entry != null) {
                pool.add(entry);
            }
        }
        return pool;
    }
}
