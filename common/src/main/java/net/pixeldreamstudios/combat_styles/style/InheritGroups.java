package net.pixeldreamstudios.combat_styles.style;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record InheritGroups(InheritMode shape, InheritMode meta, InheritMode timing,
                            InheritMode behaviour, InheritMode affixes) {

    private static final String GROUP_PREFIX = "group.combat_styles.";

    public static final String KEY_SHAPE = "shape";
    public static final String KEY_META = "meta";
    public static final String KEY_TIMING = "timing";
    public static final String KEY_BEHAVIOUR = "behaviour";
    public static final String KEY_AFFIXES = "affixes";

    public static final List<String> GROUPS =
            List.of(KEY_SHAPE, KEY_META, KEY_TIMING, KEY_BEHAVIOUR, KEY_AFFIXES);

    public static final InheritGroups ALL = of(InheritMode.PARENT);
    public static final InheritGroups NONE = of(InheritMode.OWN);

    private static final Codec<InheritMode> MODE_CODEC = Codec.either(
            InheritMode.CODEC, Codec.BOOL).xmap(
            either -> either.map(mode -> mode,
                    legacy -> legacy ? InheritMode.PARENT : InheritMode.OWN),
            Either::left);

    public static final Codec<InheritGroups> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MODE_CODEC.optionalFieldOf(KEY_SHAPE, InheritMode.PARENT).forGetter(InheritGroups::shape),
            MODE_CODEC.optionalFieldOf(KEY_META, InheritMode.PARENT).forGetter(InheritGroups::meta),
            MODE_CODEC.optionalFieldOf(KEY_TIMING, InheritMode.PARENT).forGetter(InheritGroups::timing),
            MODE_CODEC.optionalFieldOf(KEY_BEHAVIOUR, InheritMode.PARENT).forGetter(InheritGroups::behaviour),
            MODE_CODEC.optionalFieldOf(KEY_AFFIXES, InheritMode.PARENT).forGetter(InheritGroups::affixes)
    ).apply(instance, InheritGroups::new));

    public InheritGroups {
        shape = shape == null ? InheritMode.PARENT : shape;
        meta = meta == null ? InheritMode.PARENT : meta;
        timing = timing == null ? InheritMode.PARENT : timing;
        behaviour = behaviour == null ? InheritMode.PARENT : behaviour;
        affixes = affixes == null ? InheritMode.PARENT : affixes;
    }

    public static InheritGroups of(InheritMode mode) {
        return new InheritGroups(mode, mode, mode, mode, mode);
    }

    public static InheritGroups of(boolean value) {
        return value ? ALL : NONE;
    }

    public boolean any() {
        return shape.inherits() || meta.inherits() || timing.inherits()
                || behaviour.inherits() || affixes.inherits();
    }

    public boolean all() {
        return shape == InheritMode.PARENT && meta == InheritMode.PARENT
                && timing == InheritMode.PARENT && behaviour == InheritMode.PARENT
                && affixes == InheritMode.PARENT;
    }

    public InheritMode mode(String group) {
        return switch (group) {
            case KEY_SHAPE -> shape;
            case KEY_META -> meta;
            case KEY_TIMING -> timing;
            case KEY_BEHAVIOUR -> behaviour;
            case KEY_AFFIXES -> affixes;
            default -> InheritMode.OWN;
        };
    }

    public boolean get(String group) {
        return mode(group).inherits();
    }

    public InheritGroups with(String group, InheritMode value) {
        InheritMode applied = value == null ? InheritMode.OWN : value;
        return switch (group) {
            case KEY_SHAPE -> new InheritGroups(applied, meta, timing, behaviour, affixes);
            case KEY_META -> new InheritGroups(shape, applied, timing, behaviour, affixes);
            case KEY_TIMING -> new InheritGroups(shape, meta, applied, behaviour, affixes);
            case KEY_BEHAVIOUR -> new InheritGroups(shape, meta, timing, applied, affixes);
            case KEY_AFFIXES -> new InheritGroups(shape, meta, timing, behaviour, applied);
            default -> this;
        };
    }

    public InheritGroups toggle(String group) {
        return with(group, mode(group).next());
    }

    public static String translationKey(String group) {
        return GROUP_PREFIX + group;
    }

    public String signature() {
        StringBuilder builder = new StringBuilder();
        for (String group : GROUPS) {
            builder.append(mode(group).getSerializedName().charAt(0));
        }
        return builder.toString();
    }
}
