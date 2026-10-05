package net.pixeldreamstudios.combat_styles.style;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.affix.ResolvedAffixes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record StyleEntry(String styleId, Optional<String> soundId, List<String> affixIds,
                         Optional<AttackOverride> attackOverride,
                         Optional<String> name, Optional<String> nameKey,
                         Map<String, AffixTuning> affixTuning,
                         Optional<String> customStyleId) {

    private static final int MAX_STREAM_AFFIXES = 32;

    public static final StyleEntry EMPTY = new StyleEntry("", Optional.empty(), List.of(),
            Optional.empty(), Optional.empty(), Optional.empty(), Map.of(), Optional.empty());

    public static final String KEY_SEPARATOR = StyleKeys.KEY_SEPARATOR;
    public static final String AFFIX_SEPARATOR = StyleKeys.AFFIX_SEPARATOR;

    private static final Codec<List<String>> AFFIX_LIST_CODEC = Codec.either(
                    Codec.STRING, Codec.STRING.listOf())
            .xmap(either -> either.map(List::of, list -> list),
                    list -> list.size() == 1
                            ? Either.left(list.getFirst())
                            : Either.right(list));

    public static final Codec<StyleEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("style", "").forGetter(StyleEntry::rawStyleId),
            Codec.STRING.optionalFieldOf("sound").forGetter(StyleEntry::soundId),
            AFFIX_LIST_CODEC.optionalFieldOf("affixes", List.of()).forGetter(StyleEntry::affixIds),
            AttackOverride.CODEC.optionalFieldOf("attack").forGetter(StyleEntry::attackOverride),
            Codec.STRING.optionalFieldOf("name").forGetter(StyleEntry::name),
            Codec.STRING.optionalFieldOf("name_key").forGetter(StyleEntry::nameKey),
            AffixTuning.MAP_CODEC.optionalFieldOf("affix_tuning", Map.of())
                    .forGetter(StyleEntry::affixTuning),
            Codec.STRING.optionalFieldOf("custom_style_id").forGetter(StyleEntry::customStyleId)
    ).apply(instance, StyleEntry::new));

    public static final StreamCodec<ByteBuf, StyleEntry> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public StyleEntry decode(ByteBuf buffer) {
            String styleId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            Optional<String> sound = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buffer);
            List<String> affixes = ByteBufCodecs
                    .collection(ArrayList::new, ByteBufCodecs.STRING_UTF8, MAX_STREAM_AFFIXES)
                    .decode(buffer);
            Optional<AttackOverride> override =
                    ByteBufCodecs.optional(AttackOverride.STREAM_CODEC).decode(buffer);
            Optional<String> name = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buffer);
            Optional<String> nameKey = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buffer);
            Map<String, AffixTuning> tuning = AffixTuning.MAP_STREAM_CODEC.decode(buffer);
            Optional<String> customId = ByteBufCodecs
                    .optional(ByteBufCodecs.STRING_UTF8).decode(buffer);
            return new StyleEntry(styleId, sound, affixes, override, name, nameKey,
                    tuning, customId);
        }

        @Override
        public void encode(ByteBuf buffer, StyleEntry entry) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, entry.rawStyleId());
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buffer, entry.soundId());
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8, MAX_STREAM_AFFIXES)
                    .encode(buffer, new ArrayList<>(entry.affixIds()));
            ByteBufCodecs.optional(AttackOverride.STREAM_CODEC)
                    .encode(buffer, entry.attackOverride());
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buffer, entry.name());
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buffer, entry.nameKey());
            AffixTuning.MAP_STREAM_CODEC.encode(buffer, entry.affixTuning());
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8)
                    .encode(buffer, entry.customStyleId());
        }
    };

    public StyleEntry {
        affixIds = affixIds == null ? List.of() : List.copyOf(affixIds);
        attackOverride = attackOverride == null ? Optional.empty() : attackOverride;
        name = name == null ? Optional.empty() : name;
        nameKey = nameKey == null ? Optional.empty() : nameKey;
        affixTuning = affixTuning == null ? Map.of() : Map.copyOf(affixTuning);
        customStyleId = customStyleId == null ? Optional.empty() : customStyleId;
        styleId = styleId == null ? "" : styleId;
    }

    public String styleId() {
        if (!styleId.isBlank() || customStyleId.isEmpty()) {
            return StyleRegistry.canonicalOf(styleId);
        }
        return StyleRegistry.canonicalOf(CustomStyles.styleIdOf(customStyleId.get()));
    }

    public String rawStyleId() {
        return styleId;
    }

    public StyleEntry(String styleId, Optional<String> soundId, List<String> affixIds,
                      Optional<AttackOverride> attackOverride,
                      Optional<String> name, Optional<String> nameKey,
                      Map<String, AffixTuning> affixTuning) {
        this(styleId, soundId, affixIds, attackOverride, name, nameKey, affixTuning,
                Optional.empty());
    }

    public StyleEntry(String styleId, Optional<String> soundId, List<String> affixIds,
                      Optional<AttackOverride> attackOverride,
                      Optional<String> name, Optional<String> nameKey) {
        this(styleId, soundId, affixIds, attackOverride, name, nameKey, Map.of(),
                Optional.empty());
    }

    public ResolvedAffixes resolveAffixes() {
        return ResolvedAffixes.of(affixIds, affixTuning);
    }

    public ResolvedAffixes resolveAffixes(StylePage page) {
        AttackOverride base = page == null ? null : AttackOverrides.get(page.styleId());
        if (base == null || base.affixes().isEmpty()) {
            return resolveAffixes();
        }
        List<String> merged = new ArrayList<>(base.affixes());
        for (String own : affixIds) {
            if (!merged.contains(own)) {
                merged.add(own);
            }
        }
        Map<String, AffixTuning> tuning = new LinkedHashMap<>(base.affixTuning());
        tuning.putAll(affixTuning);
        return ResolvedAffixes.of(merged, tuning);
    }

    public StyleEntry(String styleId, Optional<String> soundId, List<String> affixIds) {
        this(styleId, soundId, affixIds, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public StyleEntry(String styleId, Optional<String> soundId, List<String> affixIds,
                      Optional<AttackOverride> attackOverride) {
        this(styleId, soundId, affixIds, attackOverride, Optional.empty(), Optional.empty());
    }

    public StyleEntry withAffixTuning(Map<String, AffixTuning> tuning) {
        return new StyleEntry(styleId, soundId, affixIds, attackOverride, name, nameKey,
                tuning == null ? Map.of() : Map.copyOf(tuning), customStyleId);
    }

    public StyleEntry withAttackOverride(AttackOverride override) {
        return new StyleEntry(styleId, soundId, affixIds, Optional.ofNullable(override),
                name, nameKey, affixTuning, customStyleId);
    }

    public StyleEntry withName(String customName) {
        return new StyleEntry(styleId, soundId, affixIds, attackOverride,
                customName == null || customName.isBlank()
                        ? Optional.empty()
                        : Optional.of(customName),
                nameKey, affixTuning, customStyleId);
    }

    public boolean isPlain() {
        return soundId.isEmpty() && affixIds.isEmpty() && attackOverride.isEmpty()
                && name.isEmpty() && nameKey.isEmpty() && customStyleId.isEmpty()
                && affixTuning.isEmpty();
    }

    public StyleEntry withCustomStyleId(String id) {
        return new StyleEntry(styleId, soundId, affixIds, attackOverride, name, nameKey,
                affixTuning, id == null || id.isBlank() ? Optional.empty() : Optional.of(id));
    }

    public static StyleEntry of(String styleId) {
        return new StyleEntry(styleId, Optional.empty(), List.of());
    }

    public static StyleEntry of(String styleId, String affixId) {
        return new StyleEntry(styleId, Optional.empty(),
                affixId == null || affixId.isBlank() ? List.of() : List.of(affixId));
    }

    public static StyleEntry of(String styleId, List<String> affixIds) {
        return new StyleEntry(styleId, Optional.empty(),
                affixIds == null ? List.of() : sorted(affixIds));
    }

    public static String key(String styleId, String affixId) {
        return StyleKeys.key(styleId, affixId);
    }

    public static final String NAME_SEPARATOR = StyleKeys.NAME_SEPARATOR;

    public static String key(String styleId, List<String> affixIds) {
        return StyleKeys.key(styleId, affixIds);
    }

    public static final String CUSTOM_PREFIX = StyleKeys.CUSTOM_PREFIX;

    public boolean isNamed() {
        return name.isPresent() || nameKey.isPresent() || customStyleId.isPresent();
    }

    public boolean isSameNamed(StyleEntry other) {
        return other != null && isNamed() && styleId().equals(other.styleId())
                && name.equals(other.name()) && nameKey.equals(other.nameKey())
                && customStyleId.equals(other.customStyleId());
    }

    public String learnedKey() {
        if (customStyleId.isPresent()) {
            return CUSTOM_PREFIX + customStyleId.get();
        }
        StylePage page = StyleRegistry.get(styleId);
        String canonical = page == null ? styleId : page.styleId();
        String base = key(canonical, affixIds);
        if (isPlain()) {
            return base;
        }
        StringBuilder suffix = new StringBuilder();
        name.ifPresent(suffix::append);
        nameKey.ifPresent(value -> suffix.append('/').append(value));
        soundId.ifPresent(value -> suffix.append('/').append(value));
        attackOverride.ifPresent(override -> suffix.append('/').append(override.signature()));
        if (!affixTuning.isEmpty()) {
            List<String> keys = new ArrayList<>(affixTuning.keySet());
            keys.sort(String::compareTo);
            for (String tuned : keys) {
                suffix.append('/').append(tuned)
                        .append('=').append(affixTuning.get(tuned).hashCode());
            }
        }
        return base + NAME_SEPARATOR + suffix;
    }

    public static List<String> sorted(List<String> affixIds) {
        return StyleKeys.sorted(affixIds);
    }

    public static String styleIdOfKey(String key) {
        if (key.startsWith(CUSTOM_PREFIX)) {
            return CustomStyles.styleIdOf(key.substring(CUSTOM_PREFIX.length()));
        }
        return StyleKeys.styleIdOf(key);
    }

    public static List<String> affixIdsOfKey(String key) {
        return StyleKeys.affixIdsOf(key);
    }

    public String key() {
        return key(styleId, affixIds);
    }

    public StyleEntry withSound(String soundId) {
        return new StyleEntry(styleId, Optional.ofNullable(soundId), affixIds, attackOverride,
                name, nameKey, affixTuning, customStyleId);
    }

    public StyleEntry withoutSound() {
        return new StyleEntry(styleId, Optional.empty(), affixIds, attackOverride,
                name, nameKey, affixTuning, customStyleId);
    }

    public StyleEntry withAffixes(List<String> ids) {
        return new StyleEntry(styleId, soundId, ids == null ? List.of() : sorted(ids),
                attackOverride, name, nameKey, affixTuning, customStyleId);
    }

    public StyleEntry withAffix(String affixId) {
        return withAffixes(affixId == null || affixId.isBlank() ? List.of() : List.of(affixId));
    }

    public StyleEntry plusAffix(String affixId) {
        if (affixId == null || affixId.isBlank() || affixIds.contains(affixId)) {
            return this;
        }
        List<String> next = new ArrayList<>(affixIds);
        next.add(affixId);
        return withAffixes(next);
    }

    public StyleEntry minusAffix(String affixId) {
        if (affixId == null || !affixIds.contains(affixId)) {
            return this;
        }
        List<String> next = new ArrayList<>(affixIds);
        next.remove(affixId);
        return withAffixes(next);
    }

    public Optional<String> affixId() {
        return affixIds.isEmpty() ? Optional.empty() : Optional.of(affixIds.getFirst());
    }

    public boolean hasCustomSound() {
        return soundId.isPresent();
    }

    public boolean hasAffix() {
        return !affixIds.isEmpty();
    }
}
