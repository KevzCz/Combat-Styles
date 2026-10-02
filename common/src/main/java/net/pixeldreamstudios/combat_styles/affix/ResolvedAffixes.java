package net.pixeldreamstudios.combat_styles.affix;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.style.StylePage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public record ResolvedAffixes(List<StyleAffix> affixes,
                              Optional<StyleAffix> trailSource,
                              Optional<StyleAffix> conversionSource) {

    public static final ResolvedAffixes EMPTY =
            new ResolvedAffixes(List.of(), Optional.empty(), Optional.empty());

    private static final Comparator<StyleAffix> BY_RARITY =
            Comparator.comparingInt((StyleAffix affix) -> affix.rarity().ordinal()).reversed();

    public ResolvedAffixes restrictedTo(StylePage page) {
        if (page == null || affixes.isEmpty()) {
            return this;
        }
        List<StyleAffix> allowed = affixes.stream()
                .filter(affix -> affix.canApplyTo(page))
                .toList();
        if (allowed.size() == affixes.size()) {
            return this;
        }
        if (allowed.isEmpty()) {
            return EMPTY;
        }
        return new ResolvedAffixes(List.copyOf(allowed),
                trailSource.filter(affix -> affix.canApplyTo(page)),
                conversionSource.filter(affix -> affix.canApplyTo(page)));
    }

    public static ResolvedAffixes of(List<String> affixIds) {
        return of(affixIds, Map.of());
    }

    public static ResolvedAffixes of(List<String> affixIds, Map<String, AffixTuning> tuning) {
        if (affixIds == null || affixIds.isEmpty()) {
            return EMPTY;
        }

        List<StyleAffix> resolved = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String id : affixIds) {
            StyleAffix affix = AffixRegistry.get(id);
            if (affix == null || !seen.add(affix.serializedId())) {
                continue;
            }
            AffixTuning tuned = tuning == null ? null : tuning.get(affix.serializedId());
            resolved.add(tuned == null ? affix : tuned.applyTo(affix));
        }
        if (resolved.isEmpty()) {
            return EMPTY;
        }

        Optional<StyleAffix> trail = resolved.stream()
                .filter(affix -> affix.trail().isPresent())
                .min(BY_RARITY);

        List<StyleAffix> converters = resolved.stream()
                .filter(StyleAffix::convertsDamage)
                .sorted(BY_RARITY)
                .toList();

        if (converters.size() > 1) {
            CombatStyles.LOGGER.debug("Multiple converting affixes on one attack ({}); {} wins",
                    converters.size(), converters.getFirst().serializedId());
        }

        return new ResolvedAffixes(List.copyOf(resolved), trail,
                converters.isEmpty() ? Optional.empty() : Optional.of(converters.getFirst()));
    }

    public Optional<AffixEffects.Damage> elementalPartner(StyleAffix exclude) {
        AffixEffects.Damage best = null;
        int bestRarity = -1;
        for (StyleAffix affix : affixes) {
            if (affix == exclude) {
                continue;
            }
            AffixEffects.Damage damage = Stream.concat(
                            affix.onHit().allDamage().stream(),
                            affix.onSwing().allDamage().stream())
                    .filter(entry -> entry.school().isPresent())
                    .findFirst()
                    .orElse(null);
            if (damage == null) {
                continue;
            }
            int rarity = affix.rarity().ordinal();
            if (rarity > bestRarity) {
                bestRarity = rarity;
                best = damage;
            }
        }
        return Optional.ofNullable(best);
    }

    public ResolvedAffixes tunedBy(Map<String, AffixTuning> tuning) {
        if (tuning == null || tuning.isEmpty() || affixes.isEmpty()) {
            return this;
        }
        List<String> ids = new ArrayList<>(affixes.size());
        for (StyleAffix affix : affixes) {
            ids.add(affix.serializedId());
        }
        return of(ids, tuning);
    }

    public boolean isEmpty() {
        return affixes.isEmpty();
    }

    public Optional<StyleAffix.TrailColor> blendedTrail() {
        return blend(affixes.stream()
                .map(StyleAffix::trail)
                .flatMap(Optional::stream)
                .toList());
    }

    public static Optional<StyleAffix.TrailColor> blend(List<StyleAffix.TrailColor> colours) {
        if (colours.isEmpty()) {
            return Optional.empty();
        }
        if (colours.size() == 1) {
            return Optional.of(colours.getFirst());
        }

        long red = 0L;
        long green = 0L;
        long blue = 0L;
        long alpha = 0L;
        boolean glows = false;
        for (StyleAffix.TrailColor colour : colours) {
            int argb = colour.argb();
            alpha += (argb >> 24) & 0xFF;
            red += (argb >> 16) & 0xFF;
            green += (argb >> 8) & 0xFF;
            blue += argb & 0xFF;
            glows |= colour.glows();
        }

        int count = colours.size();
        String hex = String.format("#%02X%02X%02X%02X",
                red / count, green / count, blue / count, alpha / count);
        return Optional.of(new StyleAffix.TrailColor(hex, glows));
    }

    public boolean convertsDamage() {
        return conversionSource.isPresent();
    }

    public double costMultiplier() {
        double total = 1.0D;
        for (StyleAffix affix : affixes) {
            total *= affix.costModifier();
        }
        return total;
    }

    public double damageMultiplier() {
        double total = 1.0D;
        for (StyleAffix affix : affixes) {
            total *= affix.damageMultiplier();
        }
        return total;
    }
}
