package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;

import java.util.List;

public record SpellProjectileGate(boolean enabled, List<String> spells, List<String> schools) {

    public static final SpellProjectileGate ANY =
            new SpellProjectileGate(false, List.of(), List.of());

    public static final MapCodec<SpellProjectileGate> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("enabled", Boolean.TRUE)
                            .forGetter(SpellProjectileGate::enabled),
                    Codec.STRING.listOf().optionalFieldOf("spell", List.of())
                            .forGetter(SpellProjectileGate::spells),
                    Codec.STRING.listOf().optionalFieldOf("school", List.of())
                            .forGetter(SpellProjectileGate::schools)
            ).apply(instance, SpellProjectileGate::new));

    public SpellProjectileGate {
        spells = spells == null ? List.of() : List.copyOf(spells);
        schools = schools == null ? List.of() : List.copyOf(schools);
    }

    public boolean isEmpty() {
        return !enabled && spells.isEmpty() && schools.isEmpty();
    }

    public String rejects(EquipmentContext context) {
        if (isEmpty()) {
            return null;
        }
        if (!SpellEngineCompat.isLoaded()) {
            return "spell_projectile (Spell Engine is not installed)";
        }
        ProjectileHitEvent hit = context.projectileHit();
        Entity projectile = hit == null ? null : hit.projectile();
        if (!SpellEngineCompat.isSpellProjectile(projectile)) {
            return "spell_projectile (not a spell projectile)";
        }
        ResourceLocation spell = SpellEngineCompat.spellOf(projectile);
        if (spell == null) {
            return "spell_projectile (no spell attached)";
        }
        if (!SpellMatch.names(spells, spell)) {
            return "spell (" + spell + ")";
        }
        if (schools.isEmpty()) {
            return null;
        }
        String school = SpellEngineCompat.schoolOf(spell, context.player().level());
        return SpellMatch.inSchool(schools, school) ? null : "school (" + school + ")";
    }
}
