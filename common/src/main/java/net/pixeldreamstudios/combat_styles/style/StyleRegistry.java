package net.pixeldreamstudios.combat_styles.style;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.resources.ResourceLocation;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
    private static volatile List<String> categories = List.of();
    private static volatile List<WeaponAttributes.Sound> swingSounds = List.of();
    private static final double BASELINE_ARC = 120.0D;

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
            swingSounds = List.of();
            return;
        }

        Map<String, StylePage> pages = new LinkedHashMap<>();
        Map<String, StylePage> raw = new LinkedHashMap<>();
        Map<String, List<StylePage>> byCategory = new TreeMap<>();
        Map<String, Set<String>> seenSignatures = new HashMap<>();
        Map<String, Integer> nextIndex = new HashMap<>();
        Map<String, WeaponAttributes.Sound> sounds = new TreeMap<>();
        Set<String> indexedAnimations = new HashSet<>();

        List<Map.Entry<ResourceLocation, WeaponAttributes>> ordered = new ArrayList<>(source.entrySet());
        ordered.sort(Comparator.comparing(entry -> entry.getKey().toString()));

        for (Map.Entry<ResourceLocation, WeaponAttributes> entry : ordered) {
            WeaponAttributes attributes = entry.getValue();
            if (attributes == null || attributes.attacks() == null) {
                continue;
            }
            String category = attributes.category();
            if (category == null || category.isBlank()) {
                CombatStyles.LOGGER.warn("Skipping {}: no category", entry.getKey());
                continue;
            }

            WeaponAttributes.Attack[] attacks = attributes.attacks();
            double baselineDamage = averageDamage(attacks);
            double baselineUpswing = averageUpswing(attacks);
            Set<String> signatures = seenSignatures.computeIfAbsent(category, key -> new HashSet<>());

            for (WeaponAttributes.Attack attack : attacks) {
                if (attack == null || attack.animation() == null || attack.animation().isBlank()) {
                    CombatStyles.LOGGER.warn("Skipping an attack in {}: no animation", entry.getKey());
                    continue;
                }
                WeaponAttributes.Sound swing = attack.swingSound();
                if (swing != null && swing.id() != null && !swing.id().isBlank()) {
                    sounds.putIfAbsent(swing.id(), swing);
                }
                String signature = signatureOf(attack);
                boolean fresh = signatures.add(signature);
                if (!fresh && indexedAnimations.contains(attack.animation())) {
                    deduped++;
                    continue;
                }
                indexedAnimations.add(attack.animation());
                int index = nextIndex.merge(category, 1, Integer::sum) - 1;
                String styleId = StylePage.idFor(category, index);
                StyleRarity rarity = StyleRarity.fromDeviation(deviation(attack, baselineDamage, baselineUpswing));
                StylePage page = new StylePage(styleId, category, index, attack, entry.getKey(), rarity);
                if (AttackOverrides.isDisabled(styleId)) {
                    disabled++;
                    continue;
                }
                raw.put(styleId, page);
                page = AttackOverrides.apply(page);
                pages.put(styleId, page);
                byCategory.computeIfAbsent(category, key -> new ArrayList<>()).add(page);
            }
        }

        for (CustomAttack custom : CustomAttacks.all()) {
            String styleId = custom.id();
            if (pages.containsKey(styleId)) {
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
        }

        for (CustomStyle custom : CustomStyles.all()) {
            String styleId = custom.id();
            if (pages.containsKey(styleId)) {
                CombatStyles.LOGGER.warn("Custom style {} collides with an existing style",
                        styleId);
                continue;
            }
            StylePage base = baseOf(pages, custom.entry().styleId());
            if (base == null) {
                CombatStyles.LOGGER.warn("Custom style {} references unknown style {}",
                        styleId, custom.entry().styleId());
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
        }

        byCategory.replaceAll((category, list) -> List.copyOf(list));

        PAGES.clear();
        PAGES.putAll(pages);
        RAW_PAGES.clear();
        RAW_PAGES.putAll(pages);
        RAW_PAGES.putAll(raw);
        BY_CATEGORY.clear();
        BY_CATEGORY.putAll(byCategory);
        categories = List.copyOf(byCategory.keySet());
        swingSounds = List.copyOf(sounds.values());

        if (debug) {
            CombatStyles.LOGGER.info("Indexed {} combat styles across {} weapon categories "
                            + "with {} swing sounds in {}ms",
                    PAGES.size(), categories.size(), swingSounds.size(),
                    (System.nanoTime() - startedAt) / 1_000_000L);
            CombatStyles.LOGGER.info("Indexed {} distinct animations ({} deduped, {} disabled)",
                    indexedAnimations.size(), deduped, disabled);
        }
    }

    private static StylePage baseOf(Map<String, StylePage> pages, String styleId) {
        if (styleId == null || styleId.isBlank()) {
            return null;
        }
        StylePage direct = pages.get(styleId);
        if (direct != null) {
            return direct;
        }
        if (styleId.indexOf(':') < 0) {
            return null;
        }
        for (StylePage page : pages.values()) {
            if (styleId.equals(page.animation())) {
                return page;
            }
        }
        return null;
    }

    private static String signatureOf(WeaponAttributes.Attack attack) {
        StringBuilder builder = new StringBuilder(attack.animation());
        builder.append('|').append(attack.hitbox())
                .append('|').append(attack.damageMultiplier())
                .append('|').append(attack.angle())
                .append('|').append(attack.upswing())
                .append('|').append(attack.rangeMultiplier())
                .append('|').append(attack.movementSpeedMultiplier());
        if (attack.conditions() != null) {
            for (WeaponAttributes.Condition condition : attack.conditions()) {
                builder.append('|').append(condition);
            }
        }
        return builder.toString();
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
        List<StylePage> pool = forCategory(attributes.category());
        if (pool.isEmpty()) {
            return List.of();
        }
        List<StylePage> nodes = new ArrayList<>();
        for (WeaponAttributes.Attack attack : attributes.attacks()) {
            if (attack == null || attack.animation() == null) {
                continue;
            }
            String signature = signatureOf(attack);
            for (StylePage page : pool) {
                if (signature.equals(signatureOf(page.attack()))) {
                    nodes.add(page);
                    break;
                }
            }
        }
        return List.copyOf(nodes);
    }

    public static StylePage raw(String styleId) {
        return styleId == null ? null : RAW_PAGES.get(styleId);
    }

    public static StylePage get(String styleId) {
        if (styleId == null) {
            return null;
        }
        StylePage direct = PAGES.get(styleId);
        if (direct != null) {
            return direct;
        }
        return byAnimation(styleId, null);
    }

    public static StylePage byAnimation(String animation, String category) {
        if (animation == null || !animation.contains(":")) {
            return null;
        }
        for (StylePage page : PAGES.values()) {
            if (animation.equals(page.animation())
                    && (category == null || category.equals(page.category()))) {
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
