package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.equipment.SummonProfile;

import java.util.List;
import java.util.Optional;

public record RaiseRules(MobFilter filter, int lifespanTicks, boolean keepGear, boolean spectral,
                         Optional<Integer> maxActive, float chance, RaiseProfile profile,
                         List<SummonProfile> profiles) {

    public static final String KEY = "raise_dead";
    public static final String KEY_LIFESPAN_TICKS = "lifespan_ticks";
    public static final String KEY_KEEP_GEAR = "keep_gear";
    public static final String KEY_SPECTRAL = "spectral";
    public static final String KEY_MAX_ACTIVE = "max_active";
    public static final String KEY_CHANCE = "chance";
    public static final String KEY_PROFILE = "profile";
    public static final String KEY_PROFILES = "profiles";
    public static final Codec<List<SummonProfile>> PROFILES_CODEC =
            Codec.lazyInitialized(() -> SummonProfile.CODEC.listOf());
    public static final int DEFAULT_LIFESPAN = 400;
    public static final float ALWAYS = 1.0F;

    public static final RaiseRules DEFAULT = new RaiseRules(MobFilter.ANY, DEFAULT_LIFESPAN, true, false,
            Optional.empty(), ALWAYS, RaiseProfile.MATCHING, List.of());

    public static final Codec<RaiseRules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MobFilter.MAP_CODEC.forGetter(RaiseRules::filter),
            Codec.INT.optionalFieldOf(KEY_LIFESPAN_TICKS, DEFAULT_LIFESPAN).forGetter(RaiseRules::lifespanTicks),
            Codec.BOOL.optionalFieldOf(KEY_KEEP_GEAR, true).forGetter(RaiseRules::keepGear),
            Codec.BOOL.optionalFieldOf(KEY_SPECTRAL, false).forGetter(RaiseRules::spectral),
            Codec.INT.optionalFieldOf(KEY_MAX_ACTIVE).forGetter(RaiseRules::maxActive),
            Codec.FLOAT.optionalFieldOf(KEY_CHANCE, ALWAYS).forGetter(RaiseRules::chance),
            RaiseProfile.CODEC.optionalFieldOf(KEY_PROFILE, RaiseProfile.MATCHING).forGetter(RaiseRules::profile),
            PROFILES_CODEC.optionalFieldOf(KEY_PROFILES, List.of()).forGetter(RaiseRules::profiles)
    ).apply(instance, RaiseRules::new));

    public RaiseRules {
        filter = filter == null ? MobFilter.ANY : filter;
        maxActive = maxActive == null ? Optional.empty() : maxActive;
        profile = profile == null ? RaiseProfile.MATCHING : profile;
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }
}
