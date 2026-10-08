package net.pixeldreamstudios.combat_styles.style;

import com.google.common.collect.MapMaker;
import net.bettercombat.api.AttributesContainer;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class StyleRegistry {

    private static final Map<String, StylePage> PAGES = new ConcurrentHashMap<>();
    private static final Map<String, StylePage> RAW_PAGES = new ConcurrentHashMap<>();
    private static final Map<String, List<StylePage>> BY_CATEGORY = new ConcurrentHashMap<>();
    private static volatile Map<String, String> aliases = Map.of();
    private static volatile Map<String, StylePage> firstByAnimation = Map.of();
    private static volatile List<StylePage> ordered = List.of();
    private static volatile Map<String, String> signatures = Map.of();
    private static volatile Map<String, Map<String, StylePage>> bySignature = Map.of();
    private static volatile Map<String, String> representatives = Map.of();
    private static volatile Map<String, List<String>> families = Map.of();
    private static volatile Map<String, List<StylePage>> distinctByCategory = Map.of();
    private static volatile List<StylePage> distinct = List.of();
    private static final Map<WeaponAttributes, String> OWNERS = new MapMaker().weakKeys().makeMap();
    private static final Map<WeaponAttributes, List<StylePage>> NODES = new MapMaker().weakKeys().makeMap();
    private static volatile List<String> categories = List.of();
    private static volatile List<WeaponAttributes.Sound> swingSounds = List.of();
    private static final double BASELINE_ARC = 120.0D;
    private static final String KEY_SEPARATOR = "|";
    private static final int SELF_CHECK_EXAMPLES = 5;

    private static final class Draft {
        private final String category;
        private final WeaponAttributes.Attack attack;
        private final StyleRarity rarity;
        private final List<StyleIdRule.Candidate> candidates = new ArrayList<>();

        private Draft(String category, WeaponAttributes.Attack attack, StyleRarity rarity) {
            this.category = category;
            this.attack = attack;
            this.rarity = rarity;
        }
    }

    private StyleRegistry() {
    }

    public static void rebuild() {
        long startedAt = System.nanoTime();
        boolean debug = CombatStylesConfig.get().debugLogging();
        int deduped = 0;
        int disabled = 0;
        Map<ResourceLocation, WeaponAttributes> source = WeaponRegistryAccess.indexable();
        if (source == null || source.isEmpty()) {
            CombatStyles.LOGGER.warn("BetterCombat weapon registry is empty; no combat styles indexed");
            PAGES.clear();
            RAW_PAGES.clear();
            BY_CATEGORY.clear();
            categories = List.of();
            aliases = Map.of();
            firstByAnimation = Map.of();
            ordered = List.of();
            signatures = Map.of();
            bySignature = Map.of();
            representatives = Map.of();
            families = Map.of();
            distinctByCategory = Map.of();
            distinct = List.of();
            OWNERS.clear();
            NODES.clear();
            swingSounds = List.of();
            return;
        }

        Map<ResourceLocation, AttributesContainer> containers = WeaponRegistryAccess.containers();
        Set<String> presets = presetsOf(containers);
        Map<String, Draft> drafts = new LinkedHashMap<>();
        Map<WeaponAttributes, String> owners = new IdentityHashMap<>();
        Map<String, WeaponAttributes.Sound> sounds = new TreeMap<>();
        Set<String> indexedAnimations = new HashSet<>();

        List<Map.Entry<ResourceLocation, WeaponAttributes>> sorted = new ArrayList<>(source.entrySet());
        sorted.sort(Comparator.comparing(entry -> entry.getKey().toString()));

        for (Map.Entry<ResourceLocation, WeaponAttributes> entry : sorted) {
            WeaponAttributes attributes = entry.getValue();
            if (attributes == null || attributes.attacks() == null) {
                continue;
            }
            String category = attributes.category();
            if (category == null || category.isBlank()) {
                CombatStyles.LOGGER.warn("Skipping {}: no category", entry.getKey());
                continue;
            }
            String sourceId = entry.getKey().toString();
            owners.put(attributes, sourceId);
            boolean preset = presets.contains(sourceId);
            WeaponAttributes.Attack[] own = ownAttacksOf(containers.get(entry.getKey()));

            WeaponAttributes.Attack[] attacks = attributes.attacks();
            double baselineDamage = averageDamage(attacks);
            double baselineUpswing = averageUpswing(attacks);

            for (int slot = 0; slot < attacks.length; slot++) {
                WeaponAttributes.Attack attack = attacks[slot];
                if (attack == null || attack.animation() == null || attack.animation().isBlank()) {
                    CombatStyles.LOGGER.warn("Skipping an attack in {}: no animation", entry.getKey());
                    continue;
                }
                WeaponAttributes.Sound swing = attack.swingSound();
                if (swing != null && swing.id() != null && !swing.id().isBlank()) {
                    sounds.putIfAbsent(swing.id(), swing);
                }
                String key = category + KEY_SEPARATOR + signatureOf(attack);
                boolean defines = own != null && slot < own.length && own[slot] != null;
                Draft draft = drafts.get(key);
                if (draft == null) {
                    draft = new Draft(category, attack,
                            StyleRarity.fromDeviation(deviation(attack, baselineDamage, baselineUpswing)));
                    drafts.put(key, draft);
                    indexedAnimations.add(attack.animation());
                } else {
                    deduped++;
                }
                draft.candidates.add(new StyleIdRule.Candidate(sourceId, slot, defines, preset));
            }
        }

        Map<String, StylePage> pages = new LinkedHashMap<>();
        Map<String, StylePage> raw = new LinkedHashMap<>();
        Map<String, List<StylePage>> byCategory = new TreeMap<>();
        Map<String, Integer> nextIndex = new HashMap<>();
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, String> signatureMap = new LinkedHashMap<>();

        Map<String, String> representativeMap = new HashMap<>();
        Map<String, List<String>> familyMap = new HashMap<>();
        Map<String, List<StylePage>> distinctMap = new TreeMap<>();
        for (Draft draft : drafts.values()) {
            List<StyleIdRule.Candidate> members = StyleIdRule.ordered(draft.candidates);
            String representative = null;
            List<String> family = new ArrayList<>();
            for (StyleIdRule.Candidate candidate : members) {
                String styleId = candidate.id();
                if (pages.containsKey(styleId)) {
                    CombatStyles.LOGGER.error("Two combat styles resolved to the same id {}", styleId);
                    continue;
                }
                if (AttackOverrides.isDisabled(styleId)) {
                    disabled++;
                    continue;
                }
                int index = nextIndex.merge(draft.category, 1, Integer::sum) - 1;
                ResourceLocation sourceWeapon = ResourceLocation.tryParse(candidate.source());
                StylePage page = new StylePage(styleId, draft.category, index, draft.attack,
                        sourceWeapon, draft.rarity);
                raw.put(styleId, page);
                page = AttackOverrides.apply(page);
                pages.put(styleId, page);
                byCategory.computeIfAbsent(draft.category, key -> new ArrayList<>()).add(page);
                signatureMap.put(styleId, signatureOf(draft.attack));
                if (representative == null) {
                    representative = styleId;
                    distinctMap.computeIfAbsent(draft.category, key -> new ArrayList<>()).add(page);
                }
                family.add(styleId);
                representativeMap.put(styleId, representative);
            }
            if (representative != null) {
                familyMap.put(representative, List.copyOf(family));
            }
        }

        for (CustomAttack custom : CustomAttacks.all()) {
            String styleId = custom.id();
            if (pages.containsKey(styleId) || aliasMap.containsKey(styleId)) {
                CombatStyles.LOGGER.warn("Custom attack {} collides with an existing style", styleId);
                continue;
            }
            String category = custom.categoryOrDefault();
            WeaponAttributes.Attack attack = custom.attack().toAttack();
            WeaponAttributes.Sound swing = attack.swingSound();
            if (swing != null && swing.id() != null && !swing.id().isBlank()) {
                sounds.putIfAbsent(swing.id(), swing);
            }
            int index = nextIndex.merge(category, 1, Integer::sum) - 1;
            StylePage page = new StylePage(styleId, category, index, attack, null,
                    custom.rarityOrDefault());
            pages.put(styleId, page);
            byCategory.computeIfAbsent(category, key -> new ArrayList<>()).add(page);
            distinctMap.computeIfAbsent(category, key -> new ArrayList<>()).add(page);
        }

        Map<String, StylePage> animationIndex = animationIndexOf(pages);
        for (CustomStyle custom : CustomStyles.all()) {
            String styleId = custom.id();
            if (pages.containsKey(styleId) || aliasMap.containsKey(styleId)) {
                CombatStyles.LOGGER.warn("Custom style {} collides with an existing style",
                        styleId);
                continue;
            }
            String baseId = custom.entry().rawStyleId();
            StylePage base = lookup(pages, aliasMap, animationIndex, baseId);
            if (base == null) {
                CombatStyles.LOGGER.warn("Custom style {} references unknown style {}",
                        styleId, baseId);
                continue;
            }
            String category = custom.category().orElse(base.category());
            WeaponAttributes.Attack attack = custom.entry().attackOverride()
                    .map(override -> AttackOverrides.applyTo(base, override).attack())
                    .orElse(base.attack());
            int index = nextIndex.merge(category, 1, Integer::sum) - 1;
            StylePage page = new StylePage(styleId, category, index, attack, null,
                    custom.rarity().orElse(base.rarity()));
            pages.put(styleId, page);
            byCategory.computeIfAbsent(category, key -> new ArrayList<>()).add(page);
            distinctMap.computeIfAbsent(category, key -> new ArrayList<>()).add(page);
        }

        byCategory.replaceAll((category, list) -> List.copyOf(list));
        distinctMap.replaceAll((category, list) -> List.copyOf(list));

        PAGES.clear();
        PAGES.putAll(pages);
        RAW_PAGES.clear();
        RAW_PAGES.putAll(pages);
        RAW_PAGES.putAll(raw);
        BY_CATEGORY.clear();
        BY_CATEGORY.putAll(byCategory);
        aliases = Map.copyOf(aliasMap);
        ordered = List.copyOf(pages.values());
        firstByAnimation = Map.copyOf(animationIndexOf(pages));
        signatures = Collections.unmodifiableMap(signatureMap);
        representatives = Map.copyOf(representativeMap);
        families = Map.copyOf(familyMap);
        distinctByCategory = Map.copyOf(distinctMap);
        List<StylePage> distinctPages = new ArrayList<>();
        for (StylePage page : pages.values()) {
            if (representativeOf(page.styleId()).equals(page.styleId())) {
                distinctPages.add(page);
            }
        }
        distinct = List.copyOf(distinctPages);
        bySignature = signaturesOf(distinctByCategory);
        OWNERS.clear();
        OWNERS.putAll(owners);
        NODES.clear();
        categories = List.copyOf(byCategory.keySet());
        swingSounds = List.copyOf(sounds.values());

        selfCheck();
        if (debug) {
            CombatStyles.LOGGER.info("Indexed {} combat styles across {} weapon categories "
                            + "with {} swing sounds in {}ms",
                    PAGES.size(), categories.size(), swingSounds.size(),
                    (System.nanoTime() - startedAt) / 1_000_000L);
            CombatStyles.LOGGER.info("Indexed {} distinct animations ({} identical copies grouped, {} disabled, {} aliases)",
                    indexedAnimations.size(), deduped, disabled, aliases.size());
        }
    }

    private static Set<String> presetsOf(Map<ResourceLocation, AttributesContainer> containers) {
        Set<String> presets = new HashSet<>();
        if (containers == null) {
            return presets;
        }
        for (AttributesContainer container : containers.values()) {
            String parent = container == null ? null : container.parent();
            if (parent == null || parent.isBlank()) {
                continue;
            }
            ResourceLocation parsed = ResourceLocation.tryParse(parent);
            presets.add(parsed == null ? parent : parsed.toString());
        }
        return presets;
    }

    private static WeaponAttributes.Attack[] ownAttacksOf(AttributesContainer container) {
        if (container == null || container.attributes() == null) {
            return null;
        }
        return container.attributes().attacks();
    }

    private static Map<String, StylePage> animationIndexOf(Map<String, StylePage> pages) {
        Map<String, StylePage> index = new HashMap<>();
        for (StylePage page : pages.values()) {
            String animation = page.animation();
            if (animation != null && !animation.isBlank()) {
                index.putIfAbsent(animation, page);
            }
        }
        return index;
    }

    private static StylePage lookup(Map<String, StylePage> pages, Map<String, String> aliasMap,
                                    Map<String, StylePage> animationIndex, String styleId) {
        if (styleId == null || styleId.isBlank()) {
            return null;
        }
        StylePage direct = pages.get(styleId);
        if (direct != null) {
            return direct;
        }
        String canonical = aliasMap.get(styleId);
        if (canonical != null) {
            return pages.get(canonical);
        }
        return styleId.indexOf(':') < 0 ? null : animationIndex.get(styleId);
    }

    private static void selfCheck() {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, StylePage> entry : PAGES.entrySet()) {
            StylePage resolved = get(entry.getKey());
            if (resolved == null || !resolved.styleId().equals(entry.getKey())) {
                problems.add(entry.getKey() + " does not resolve to itself");
            }
        }
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            StylePage resolved = get(entry.getKey());
            if (resolved == null || !resolved.styleId().equals(entry.getValue())) {
                problems.add("alias " + entry.getKey() + " does not resolve to " + entry.getValue());
            }
        }
        if (!problems.isEmpty()) {
            CombatStyles.LOGGER.error("Style id self-check found {} problems, for example {}",
                    problems.size(), problems.subList(0, Math.min(SELF_CHECK_EXAMPLES, problems.size())));
        } else if (CombatStylesConfig.get().debugLogging()) {
            CombatStyles.LOGGER.info("Style id self-check passed: {} styles, {} aliases",
                    PAGES.size(), aliases.size());
        }
    }

    private static Map<String, Map<String, StylePage>> signaturesOf(Map<String, List<StylePage>> byCategory) {
        Map<String, Map<String, StylePage>> signed = new HashMap<>();
        for (Map.Entry<String, List<StylePage>> category : byCategory.entrySet()) {
            Map<String, StylePage> pages = new HashMap<>();
            for (StylePage page : category.getValue()) {
                if (page.attack() != null) {
                    pages.putIfAbsent(signatureOf(page.attack()), page);
                }
            }
            signed.put(category.getKey(), Map.copyOf(pages));
        }
        return Map.copyOf(signed);
    }

    private static String signatureOf(WeaponAttributes.Attack attack) {
        StringBuilder builder = new StringBuilder(attack.animation());
        builder.append('|').append(attack.hitbox())
                .append('|').append(attack.damageMultiplier())
                .append('|').append(attack.angle())
                .append('|').append(attack.upswing())
                .append('|').append(attack.rangeMultiplier())
                .append('|').append(attack.movementSpeedMultiplier())
                .append('|').append(soundSignature(attack.swingSound()))
                .append('|').append(soundSignature(attack.impactSound()))
                .append('|').append(attack.trailParticles());
        if (attack.conditions() != null) {
            for (WeaponAttributes.Condition condition : attack.conditions()) {
                builder.append('|').append(condition);
            }
        }
        return builder.toString();
    }

    private static String soundSignature(WeaponAttributes.Sound sound) {
        if (sound == null) {
            return "";
        }
        return sound.id() + KEY_SEPARATOR + sound.volume() + KEY_SEPARATOR + sound.pitch()
                + KEY_SEPARATOR + sound.randomness();
    }

    public static String representativeOf(String styleId) {
        if (styleId == null) {
            return null;
        }
        String canonical = canonicalOf(styleId);
        return representatives.getOrDefault(canonical, canonical);
    }

    public static List<StylePage> familyOf(String styleId) {
        List<String> members = families.get(representativeOf(styleId));
        if (members == null) {
            StylePage page = get(styleId);
            return page == null ? List.of() : List.of(page);
        }
        List<StylePage> found = new ArrayList<>(members.size());
        for (String member : members) {
            StylePage page = PAGES.get(member);
            if (page != null) {
                found.add(page);
            }
        }
        return found;
    }

    public static StylePage variantOf(StylePage page, RandomSource random) {
        if (page == null) {
            return null;
        }
        List<StylePage> family = familyOf(page.styleId());
        return family.size() <= 1 ? page : family.get(random.nextInt(family.size()));
    }

    public static List<StylePage> distinct() {
        return distinct;
    }

    public static List<StylePage> distinctFor(String category) {
        return distinctByCategory.getOrDefault(category, List.of());
    }

    public static Map<String, String> nativeSignatures() {
        return signatures;
    }

    public static List<WeaponAttributes.Sound> swingSounds() {
        return swingSounds;
    }

    public static List<WeaponAttributes.Sound> swingSoundsForCategory(String category) {
        if (category == null) {
            return List.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        List<WeaponAttributes.Sound> matching = new ArrayList<>();
        for (StylePage page : forCategory(category)) {
            WeaponAttributes.Sound sound = page.swingSound();
            if (sound != null && sound.id() != null && ids.add(sound.id())) {
                matching.add(sound);
            }
        }
        return List.copyOf(matching);
    }

    public static WeaponAttributes.Sound swingSound(String soundId) {
        if (soundId == null) {
            return null;
        }
        for (WeaponAttributes.Sound sound : swingSounds) {
            if (soundId.equals(sound.id())) {
                return sound;
            }
        }
        return null;
    }

    private static double averageDamage(WeaponAttributes.Attack[] attacks) {
        double total = 0.0D;
        int counted = 0;
        for (WeaponAttributes.Attack attack : attacks) {
            if (attack != null) {
                total += attack.damageMultiplier();
                counted++;
            }
        }
        return counted == 0 ? 1.0D : total / counted;
    }

    private static double averageUpswing(WeaponAttributes.Attack[] attacks) {
        double total = 0.0D;
        int counted = 0;
        for (WeaponAttributes.Attack attack : attacks) {
            if (attack != null) {
                total += attack.upswing();
                counted++;
            }
        }
        return counted == 0 ? 0.5D : total / counted;
    }

    private static double deviation(WeaponAttributes.Attack attack, double baselineDamage, double baselineUpswing) {
        double damageDelta = baselineDamage <= 0.0D
                ? 0.0D
                : (attack.damageMultiplier() - baselineDamage) / baselineDamage;
        double upswingDelta = baselineUpswing <= 0.0D
                ? 0.0D
                : (baselineUpswing - attack.upswing()) / baselineUpswing;
        double conditionPenalty = attack.conditions() != null && attack.conditions().length > 0
                ? -0.15D
                : 0.0D;
        double rangeDelta = attack.rangeMultiplier() - 1.0F;
        double arcDelta = (Math.abs(attack.angle()) - BASELINE_ARC) / BASELINE_ARC;
        return damageDelta
                + (upswingDelta * 0.5D)
                + rangeDelta
                + (arcDelta * 0.25D)
                + conditionPenalty;
    }

    public static List<StylePage> nodesOf(WeaponAttributes attributes) {
        if (attributes == null || attributes.attacks() == null || attributes.category() == null) {
            return List.of();
        }
        List<StylePage> cached = NODES.get(attributes);
        if (cached != null) {
            return cached;
        }
        Map<String, StylePage> signed = bySignature.getOrDefault(attributes.category(), Map.of());
        String owner = OWNERS.get(attributes);
        List<StylePage> nodes = new ArrayList<>();
        WeaponAttributes.Attack[] attacks = attributes.attacks();
        for (int slot = 0; slot < attacks.length; slot++) {
            WeaponAttributes.Attack attack = attacks[slot];
            if (attack == null || attack.animation() == null) {
                continue;
            }
            String signature = signatureOf(attack);
            StylePage own = owner == null ? null : PAGES.get(StyleIdRule.idOf(owner, slot));
            if (own != null && signature.equals(signatures.get(own.styleId()))) {
                nodes.add(own);
                continue;
            }
            StylePage page = signed.get(signature);
            if (page != null) {
                nodes.add(page);
            }
        }
        List<StylePage> found = List.copyOf(nodes);
        NODES.put(attributes, found);
        return found;
    }

    public static StylePage raw(String styleId) {
        if (styleId == null) {
            return null;
        }
        StylePage direct = RAW_PAGES.get(styleId);
        if (direct != null) {
            return direct;
        }
        String canonical = aliases.get(styleId);
        return canonical == null ? null : RAW_PAGES.get(canonical);
    }

    public static StylePage get(String styleId) {
        if (styleId == null) {
            return null;
        }
        StylePage direct = PAGES.get(styleId);
        if (direct != null) {
            return direct;
        }
        String canonical = aliases.get(styleId);
        if (canonical != null) {
            return PAGES.get(canonical);
        }
        return byAnimation(styleId, null);
    }

    public static String canonicalOf(String styleId) {
        if (styleId == null || styleId.isBlank()) {
            return styleId;
        }
        if (PAGES.containsKey(styleId)) {
            return styleId;
        }
        StylePage page = get(styleId);
        return page == null ? styleId : page.styleId();
    }

    public static boolean sameStyle(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        return first.equals(second) || representativeOf(first).equals(representativeOf(second));
    }

    public static StylePage byAnimation(String animation, String category) {
        if (animation == null || !animation.contains(":")) {
            return null;
        }
        if (category == null) {
            return firstByAnimation.get(animation);
        }
        for (StylePage page : ordered) {
            if (animation.equals(page.animation()) && category.equals(page.category())) {
                return page;
            }
        }
        return null;
    }

    public static boolean contains(String styleId) {
        return get(styleId) != null;
    }

    public static List<StylePage> forCategory(String category) {
        return BY_CATEGORY.getOrDefault(category, List.of());
    }

    public static List<String> animations() {
        Set<String> found = new TreeSet<>();
        for (StylePage page : PAGES.values()) {
            String animation = page.animation();
            if (animation != null && !animation.isBlank()) {
                found.add(animation);
            }
        }
        return List.copyOf(found);
    }

    public static List<String> categories() {
        return categories;
    }

    public static List<StylePage> all() {
        List<StylePage> all = new ArrayList<>(PAGES.values());
        Collections.sort(all, (a, b) -> {
            int byCategory = a.category().compareTo(b.category());
            return byCategory != 0 ? byCategory : Integer.compare(a.attackIndex(), b.attackIndex());
        });
        return all;
    }

    public static int size() {
        return PAGES.size();
    }

    public static boolean isEmpty() {
        return PAGES.isEmpty();
    }
}
