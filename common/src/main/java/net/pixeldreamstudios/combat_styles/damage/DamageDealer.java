package net.pixeldreamstudios.combat_styles.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;

import java.util.List;
import java.util.Optional;

public final class DamageDealer {

    public static final float MINIMUM_DAMAGE = 0.01F;

    private static final ThreadLocal<Boolean> DEALING =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Float> DEALT_DAMAGE = new ThreadLocal<>();
    private static final ThreadLocal<Float> BASE_DAMAGE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> CONNECTED = new ThreadLocal<>();

    private DamageDealer() {
    }

    public static boolean isDealing() {
        return DEALING.get();
    }

    public static void beginDealing() {
        DEALING.set(Boolean.TRUE);
    }

    public static void endDealing() {
        DEALING.set(Boolean.FALSE);
    }

    public static void recordBaseDamage(float amount) {
        BASE_DAMAGE.set(amount);
    }

    public static float baseDamage(float fallback) {
        Float recorded = BASE_DAMAGE.get();
        BASE_DAMAGE.remove();
        return recorded == null || recorded <= 0.0F ? fallback : recorded;
    }

    public static void recordDealtDamage(float amount) {
        DEALT_DAMAGE.set(amount);
    }

    public static float lastDealtDamage(float fallback) {
        Float recorded = DEALT_DAMAGE.get();
        DEALT_DAMAGE.remove();
        return recorded == null || recorded <= 0.0F ? fallback : recorded;
    }

    public static void recordConnected(boolean connected) {
        CONNECTED.set(connected);
    }

    public static boolean consumeConnected() {
        Boolean recorded = CONNECTED.get();
        CONNECTED.remove();
        return Boolean.TRUE.equals(recorded);
    }

    public static void dealSpec(Player player, LivingEntity target, DamageSpec spec,
                                float amount, DamageSource fallback) {
        if (target == null || amount < MINIMUM_DAMAGE) {
            return;
        }
        if (player == null) {
            if (fallback != null) {
                DamageHurt.bypassingIFrames(target, fallback, amount);
            }
            return;
        }
        List<DamageSpec.Share> shares = spec == null
                ? List.of()
                : spec.normalisedShares();
        if (shares.isEmpty()) {
            DamageSource source = spec == null
                    ? fallback
                    : specSourceFor(player, spec).orElse(fallback);
            if (source != null) {
                DamageHurt.bypassingIFrames(target, source, amount);
            }
            return;
        }
        for (DamageSpec.Share share : shares) {
            float portion = amount * share.share();
            if (portion < MINIMUM_DAMAGE) {
                continue;
            }
            DamageSource source = typedSource(player, share.damageType());
            DamageHurt.bypassingIFrames(target, source == null ? fallback : source, portion);
        }
    }

    public static Optional<DamageSource> specSourceFor(Player player, DamageSpec spec) {
        ResourceLocation typeId = spec.damageType()
                .map(ResourceLocation::tryParse)
                .orElseGet(() -> spec.school()
                        .map(SpellPowerAccess::damageTypeOf)
                        .orElse(null));
        return Optional.ofNullable(typeId == null ? null : typedSource(player, typeId));
    }

    public static DamageSource typedSource(Player player, String declared) {
        return typedSource(player, declared == null || declared.isEmpty()
                ? null
                : ResourceLocation.tryParse(declared));
    }

    public static DamageSource typedSource(Player player, ResourceLocation typeId) {
        if (typeId == null) {
            return player.damageSources().playerAttack(player);
        }
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, typeId);
        var holder = player.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(key)
                .orElse(null);
        if (holder == null) {
            CombatStyles.LOGGER.warn("Affix damage type {} does not exist", typeId);
            return player.damageSources().playerAttack(player);
        }
        return new DamageSource(holder, player);
    }
}
