package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;

import java.util.List;

public record CastGate(boolean enabled, CastPhase phase, List<String> spells,
                       List<String> schools, List<String> castTypes,
                       boolean includeTriggered) {

    public static final String KEY = "cast_spell";

    public static final CastGate ANY = new CastGate(false, CastPhase.CASTING, List.of(),
            List.of(), List.of(), false);

    public static final MapCodec<CastGate> EXPANDED = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("enabled", Boolean.TRUE)
                            .forGetter(CastGate::enabled),
                    CastPhase.CODEC.optionalFieldOf("phase", CastPhase.CASTING)
                            .forGetter(CastGate::phase),
                    Codec.STRING.listOf().optionalFieldOf("spell", List.of())
                            .forGetter(CastGate::spells),
                    Codec.STRING.listOf().optionalFieldOf("school", List.of())
                            .forGetter(CastGate::schools),
                    Codec.STRING.listOf().optionalFieldOf("cast_type", List.of())
                            .forGetter(CastGate::castTypes),
                    Codec.BOOL.optionalFieldOf("include_triggered", Boolean.FALSE)
                            .forGetter(CastGate::includeTriggered)
            ).apply(instance, CastGate::new));

    public static final Codec<CastGate> CODEC = Codec.either(EXPANDED.codec(), Codec.BOOL)
            .xmap(either -> either.map(value -> value, CastGate::of), Either::left);

    public static CastGate of(boolean enabled) {
        return new CastGate(enabled, CastPhase.CASTING, List.of(), List.of(), List.of(), false);
    }

    public CastGate {
        phase = phase == null ? CastPhase.CASTING : phase;
        spells = meaningful(spells);
        schools = meaningful(schools);
        castTypes = meaningful(castTypes);
    }

    private static List<String> meaningful(List<String> entries) {
        return entries == null
                ? List.of()
                : entries.stream().filter(entry -> entry != null && !entry.isBlank()).toList();
    }

    public boolean isEmpty() {
        return !enabled;
    }

    public boolean reactsToCast() {
        return !isEmpty() && phase.event();
    }

    public boolean live() {
        return !isEmpty() && !phase.event();
    }

    public boolean castingNow(Player player) {
        if (!live()) {
            return true;
        }
        if (player == null || !SpellEngineCompat.isCasting(player)) {
            return false;
        }
        ResourceLocation spell = SpellEngineCompat.castingSpell(player);
        return matches(spell, player);
    }

    private boolean matches(ResourceLocation spell, Player player) {
        if (!SpellMatch.names(spells, spell)) {
            return false;
        }
        if (schools.isEmpty() && castTypes.isEmpty()) {
            return true;
        }
        if (spell == null || player == null) {
            return false;
        }
        return SpellMatch.inSchool(schools, SpellEngineCompat.schoolOf(spell, player.level()))
                && SpellMatch.ofType(castTypes,
                SpellEngineCompat.castTypeOf(spell, player.level()));
    }

    public String rejects(EquipmentContext context) {
        if (isEmpty()) {
            return null;
        }
        if (!SpellEngineCompat.isLoaded()) {
            return KEY + " (Spell Engine is not installed)";
        }
        if (live()) {
            return castingNow(context.player()) ? null : KEY + " (not casting)";
        }
        SpellCastEvent cast = context.cast();
        if (cast == null) {
            return KEY + " (this is not a spell cast)";
        }
        if (cast.phase() != phase) {
            return KEY + " phase (" + cast.phase().serializedName() + ")";
        }
        if (cast.triggered() && !includeTriggered) {
            return KEY + " (cast by a trigger)";
        }
        if (!SpellMatch.names(spells, cast.spell())) {
            return "spell (" + cast.spell() + ")";
        }
        if (!SpellMatch.inSchool(schools, cast.school())) {
            return "school (" + cast.school() + ")";
        }
        if (!SpellMatch.ofType(castTypes, cast.castType())) {
            return "cast_type (" + cast.castType() + ")";
        }
        return null;
    }
}
