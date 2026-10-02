package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public record HealthGate(Optional<Float> selfAbove, Optional<Float> selfBelow,
                         Optional<Float> targetAbove, Optional<Float> targetBelow) {

    public static final HealthGate ANY = new HealthGate(Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

    public static final Codec<HealthGate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("self_above").forGetter(HealthGate::selfAbove),
            Codec.FLOAT.optionalFieldOf("self_below").forGetter(HealthGate::selfBelow),
            Codec.FLOAT.optionalFieldOf("target_above").forGetter(HealthGate::targetAbove),
            Codec.FLOAT.optionalFieldOf("target_below").forGetter(HealthGate::targetBelow)
    ).apply(instance, HealthGate::new));

    public boolean isEmpty() {
        return selfAbove.isEmpty() && selfBelow.isEmpty()
                && targetAbove.isEmpty() && targetBelow.isEmpty();
    }

    public boolean needsTarget() {
        return targetAbove.isPresent() || targetBelow.isPresent();
    }

    public String rejects(EquipmentContext context) {
        if (isEmpty()) {
            return null;
        }
        String selfRejection = rejectsSelf(context.player());
        if (selfRejection != null) {
            return selfRejection;
        }
        if (!needsTarget()) {
            return null;
        }
        LivingEntity target = context.target();
        if (target == null) {
            return "health.target (this attack has no target)";
        }
        float ratio = ratioOf(target);
        if (targetAbove.isPresent() && ratio <= targetAbove.get()) {
            return "health.target_above";
        }
        if (targetBelow.isPresent() && ratio >= targetBelow.get()) {
            return "health.target_below";
        }
        return null;
    }

    public String rejectsSelf(Player player) {
        if (selfAbove.isEmpty() && selfBelow.isEmpty()) {
            return null;
        }
        float ratio = ratioOf(player);
        if (selfAbove.isPresent() && ratio <= selfAbove.get()) {
            return "health.self_above";
        }
        if (selfBelow.isPresent() && ratio >= selfBelow.get()) {
            return "health.self_below";
        }
        return null;
    }

    private static float ratioOf(LivingEntity entity) {
        return entity == null || entity.getMaxHealth() <= 0.0F
                ? 1.0F
                : entity.getHealth() / entity.getMaxHealth();
    }
}
