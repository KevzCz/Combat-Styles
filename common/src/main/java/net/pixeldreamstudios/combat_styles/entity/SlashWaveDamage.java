package net.pixeldreamstudios.combat_styles.entity;

import net.pixeldreamstudios.combat_styles.damage.DamageDealer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.affix.AffixEffects;
import net.pixeldreamstudios.combat_styles.damage.DamageHurt;
import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;

import java.util.Optional;
import java.util.stream.Stream;

public final class SlashWaveDamage {

    private SlashWaveDamage() {
    }

    public static void apply(SlashWaveEntity wave, LivingEntity target, float damage) {
        if (damage <= 0.0F) {
            return;
        }
        Entity owner = wave.getOwner();
        if (!(owner instanceof LivingEntity shooter) || shooter == target) {
            return;
        }
        DamageSource source = sourceFor(wave, shooter);

        StyleAffix affix = AffixRegistry.get(wave.affixId());
        DamageSpec spec = affix == null ? null : specOf(affix);
        if (spec != null && shooter instanceof Player player) {
            DamageDealer.dealSpec(player, target, spec, damage, source);
            return;
        }
        DamageHurt.bypassingIFrames(target, source, damage);
    }

    private static DamageSpec specOf(StyleAffix affix) {
        AffixEffects.SlashWave wave = affix.onSwing().slashWave()
                .or(() -> affix.onHit().slashWave())
                .orElse(null);
        return wave == null ? null : wave.damageSpec().orElse(null);
    }

    private static Optional<AffixEffects.Damage> typedDamageOf(StyleAffix affix) {
        return Stream.concat(affix.onSwing().allDamage().stream(),
                        affix.onHit().allDamage().stream())
                .filter(damage -> !damage.amplifies())
                .findFirst();
    }

    private static ResourceLocation damageTypeOf(StyleAffix affix) {
        AffixEffects.SlashWave wave = affix.onSwing().slashWave()
                .or(() -> affix.onHit().slashWave())
                .orElse(null);

        if (wave != null && wave.damageType().filter(type -> !type.isEmpty()).isPresent()) {
            return ResourceLocation.tryParse(wave.damageType().get());
        }
        if (wave != null && !wave.inherit()) {
            return null;
        }

        return typedDamageOf(affix).map(AffixEffects.Damage::damageType)
                .filter(type -> !type.isEmpty())
                .map(ResourceLocation::tryParse)
                .orElse(null);
    }

    private static Optional<String> schoolOf(StyleAffix affix) {
        AffixEffects.SlashWave wave = affix.onSwing().slashWave()
                .or(() -> affix.onHit().slashWave())
                .orElse(null);

        if (wave != null && wave.school().isPresent()) {
            return wave.school();
        }
        if (wave != null && !wave.inherit()) {
            return Optional.empty();
        }

        return typedDamageOf(affix).flatMap(AffixEffects.Damage::school);
    }

    private static DamageSource sourceFor(SlashWaveEntity wave, LivingEntity shooter) {
        StyleAffix affix = AffixRegistry.get(wave.affixId());
        ResourceLocation typeId = affix == null ? null : damageTypeOf(affix);
        if (typeId == null && !wave.inheritedType().isEmpty()) {
            typeId = SpellPowerAccess.damageTypeOf(wave.inheritedType());
        }
        if (typeId == null && affix != null) {
            typeId = schoolOf(affix).map(SpellPowerAccess::damageTypeOf).orElse(null);
        }

        if (typeId != null) {
            ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, typeId);
            var holder = wave.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolder(key)
                    .orElse(null);
            if (holder != null) {
                return new DamageSource(holder, wave, shooter);
            }
        }
        return shooter instanceof Player player
                ? player.damageSources().playerAttack(player)
                : wave.damageSources().mobAttack(shooter);
    }
}
