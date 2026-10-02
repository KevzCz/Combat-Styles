package net.pixeldreamstudios.combat_styles.affix;

import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.fx.ParticlePlacement;
import net.bettercombat.logic.PlayerAttackHelper;
import net.pixeldreamstudios.combat_styles.api.AttackInfo;
import net.pixeldreamstudios.combat_styles.api.AttackQuery;
import net.pixeldreamstudios.combat_styles.api.event.CombatStylesEvents;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.entity.ModEntities;
import net.pixeldreamstudios.combat_styles.entity.SlashWaveEntity;
import net.pixeldreamstudios.combat_styles.entity.WaveShape;
import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.HumanoidArm;

final class AffixSlashWaves {

    private static final int DEFAULT_WAVE_COLOUR = 0xFFFFFFFF;
    private static final double EYE_DROP = 0.44D;
    private static final double FORWARD_OFFSET = 0.8D;
    private static final float DEFAULT_REACH = 3.0F;
    private static final float TRAIL_RANGE_DROP = 0.25F;
    private static final int WAVE_STAGGER_TICKS = 3;
    private static final float VERTICAL_ROLL = 90.0F;
    private static final float DIAGONAL_ROLL = 45.0F;

    private record PendingWave(Player player, StyleAffix affix, AffixEffects.SlashWave wave,
                               float damage, ResolvedAffixes resolved, long dueAt,
                               WeaponAttributes.Attack attack) {
    }

    private static final Set<PendingWave> PENDING_WAVES = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Integer> WAVES_THIS_SWING = new ThreadLocal<>();

    private AffixSlashWaves() {
    }

    static void endSwing() {
        WAVES_THIS_SWING.remove();
    }

    static void tick(long gameTime) {
        PENDING_WAVES.removeIf(pending -> {
            if (gameTime < pending.dueAt()) {
                return false;
            }
            Player player = pending.player();
            if (player != null && player.isAlive()
                    && player.level() instanceof ServerLevel serverLevel) {
                releaseSlashWave(serverLevel, player, pending.affix(), pending.wave(),
                        pending.damage(), pending.resolved(), pending.attack());
            }
            return true;
        });
    }

    private static float waveDamage(Player player, AffixEffects.SlashWave wave, float total) {
        return DamageSpec.resolve(wave.damageSpec(), player, total,
                Math.max(0.0F, wave.ratio()));
    }

    private static int waveLife(AffixEffects.SlashWave wave) {
        float speed = Math.max(0.1F, wave.speed());
        AffixEffects.WaveSpeed curve = wave.speedCurve().orElse(null);
        if (curve == null || curve.isFlat()) {
            return Math.max(2, (int) (wave.range() / speed));
        }
        float travelled = 0.0F;
        int ticks = 0;
        int limit = (int) (wave.range() / speed) + curve.stallTicks() + curve.recoverTicks() + 2;
        while (travelled < wave.range() && ticks < limit) {
            travelled += speed * curve.multiplierAt(ticks);
            ticks++;
        }
        return Math.max(2, ticks);
    }

    private static float swingReachOf(Player player, WeaponAttributes.Attack attack) {
        WeaponAttributes attributes = WeaponRegistryAccess.rawAttributesFor(AffixDamageDealer.swingingWeapon(player));
        double base = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        double combined = attributes == null
                ? base
                : PlayerAttackHelper.combineAttackRange(attributes, base);
        float reach = (float) combined - TRAIL_RANGE_DROP;
        return reach <= 0.0F ? DEFAULT_REACH : reach;
    }

    static void queue(ServerLevel level, Player player, StyleAffix affix,
                                       AffixEffects.SlashWave wave, float total,
                                       ResolvedAffixes resolved) {
        Integer fired = WAVES_THIS_SWING.get();
        int index = fired == null ? 0 : fired;
        WAVES_THIS_SWING.set(index + 1);

        int delay = wave.delayTicks() > 0
                ? wave.delayTicks()
                : index * WAVE_STAGGER_TICKS;
        if (delay <= 0) {
            releaseSlashWave(level, player, affix, wave, total, resolved);
            return;
        }
        PENDING_WAVES.add(new PendingWave(player, affix, wave, total, resolved,
                level.getGameTime() + delay, AffixDamageDealer.attackOf(player)));
    }

    private static void releaseSlashWave(ServerLevel level, Player player, StyleAffix affix,
                                         AffixEffects.SlashWave wave, float total,
                                         ResolvedAffixes resolved) {
        releaseSlashWave(level, player, affix, wave, total, resolved, AffixDamageDealer.attackOf(player));
    }

    private static void releaseSlashWave(ServerLevel level, Player player, StyleAffix affix,
                                         AffixEffects.SlashWave wave, float total,
                                         ResolvedAffixes resolved,
                                         WeaponAttributes.Attack swungAttack) {
        if (!EffectChance.rolls(player, wave.chance())) {
            return;
        }

        SlashWaveEntity entity = ModEntities.SLASH_WAVE.get().create(level);
        if (entity == null) {
            return;
        }

        String animation = swungAttack == null
                ? animationOf(player)
                : swungAttack.animation();
        ParticlePlacement placement = wave.followsSwing()
                ? TrailPlacements.get(animation)
                : null;

        float yaw = player.getYRot();
        float pitch = player.getXRot();
        Vec3 sideways = Vec3.directionFromRotation(0.0F, yaw + 90.0F).normalize();
        Vec3 facing = Vec3.directionFromRotation(pitch, yaw).normalize();

        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
        float mirror = leftHanded ? -1.0F : 1.0F;

        double x = player.getX();
        double y = player.getEyeY() - EYE_DROP;
        double z = player.getZ();

        if (placement != null) {
            y += placement.y_addition();
        }

        Vec3 origin = new Vec3(x, y, z);
        if (placement != null) {
            origin = origin
                    .add(facing.scale(placement.z_addition()))
                    .add(sideways.scale(placement.x_addition() * mirror));
        }
        origin = origin.add(facing.scale(FORWARD_OFFSET));

        entity.setPos(origin.x, origin.y, origin.z);
        entity.setOwner(player);
        entity.setFacing(yaw, pitch + (placement == null ? 0.0F : placement.pitch_addition()));
        entity.setDeltaMovement(facing.scale(wave.speed()));
        entity.setSpeedCurve(wave.speedCurve().orElse(null));

        int colour = wave.colour()
                .map(AffixEffects.TrailColour::argb)
                .orElseGet(() -> wave.inherit()
                        ? affix.trail()
                                .or(resolved::blendedTrail)
                                .map(StyleAffix.TrailColor::argb)
                                .orElse(DEFAULT_WAVE_COLOUR)
                        : DEFAULT_WAVE_COLOUR);
        WeaponAttributes.Attack attack = swungAttack;
        AttackInfo info = AttackQuery.of(attack);
        boolean thrusting = info.isKnown()
                ? info.isThrust()
                : TrailPlacements.isStab(animation);
        entity.configure(waveDamage(player, wave, total),
                swingReachOf(player, attack) * Math.max(0.1F, wave.scale()),
                rollFor(swungAttack, player, wave.followsSwing()),
                placement == null ? 0.0F : placement.local_yaw(),
                colour,
                waveLife(wave),
                affix.serializedId());
        String texture = wave.texture()
                .orElse(thrusting ? SlashWaveEntity.THRUST_TEXTURE : null);
        int frames = wave.texture().isPresent() || !thrusting
                ? wave.frames()
                : SlashWaveEntity.THRUST_FRAMES;
        if (CombatDebug.enabled(player)) {
            CombatDebug.note(player, "wave from "
                    + (attack == null ? "no attack" : attack.animation())
                    + ", hitbox " + (attack == null || attack.hitbox() == null
                            ? "none" : attack.hitbox().name())
                    + ", thrusting " + thrusting
                    + ", shape " + info.shape()
                    + ", affix " + affix.serializedId()
                    + ", colour " + String.format("%08X", colour)
                    + ", blend " + resolved.blendedTrail()
                            .map(trail -> String.format("%08X", trail.argb())).orElse("none")
                    + ", own " + affix.trail()
                            .map(trail -> String.format("%08X", trail.argb())).orElse("none"));
        }
        entity.setAppearance(texture, frames, WaveShape.forShape(info.shape()));
        entity.setArc(attack == null ? 0.0F : (float) attack.angle());
        entity.setAnimation(animation);
        entity.setPierces(wave.pierces());
        if (wave.inherit() && wave.damageType().isEmpty() && wave.school().isEmpty()) {
            resolved.elementalPartner(affix)
                    .flatMap(AffixEffects.Damage::school)
                    .ifPresent(entity::setInheritedType);
        }

        if (!CombatStylesEvents.WAVE.isEmpty()) {
            CombatStylesEvents.WaveEvent event = new CombatStylesEvents.WaveEvent(
                    player, affix, waveDamage(player, wave, total), texture, frames, thrusting);
            if (!CombatStylesEvents.WAVE.allow(listener -> listener.onWave(event))) {
                return;
            }
        }

        level.addFreshEntity(entity);
    }

    private static String animationOf(Player player) {
        WeaponAttributes.Attack attack = AffixDamageDealer.swingAttackOf(player);
        return attack == null ? null : attack.animation();
    }

    private static float rollFor(WeaponAttributes.Attack swung, Player player,
                                 boolean followsSwing) {
        if (!followsSwing) {
            return 0.0F;
        }
        WeaponAttributes.Attack attack = swung == null ? AffixDamageDealer.swingAttackOf(player) : swung;
        if (attack == null) {
            return 0.0F;
        }

        ParticlePlacement placement = TrailPlacements.get(attack.animation());
        if (placement != null) {
            return placement.roll_set();
        }
        return attack.hitbox() == null ? 0.0F : switch (attack.hitbox()) {
            case VERTICAL_PLANE -> VERTICAL_ROLL;
            case FORWARD_BOX -> DIAGONAL_ROLL;
            default -> 0.0F;
        };
    }
}
