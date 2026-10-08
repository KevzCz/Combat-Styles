package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.config.SummonTuning;
import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.Optional;

public record SummonFollow(Optional<Double> startDistance, Optional<Double> stopDistance,
                           Optional<Double> teleportDistance) {

    public static final SummonFollow DEFAULTS = new SummonFollow(Optional.empty(),
            Optional.empty(), Optional.empty());

    public static final Codec<SummonFollow> CODEC = Customs.wrap(SummonFollow.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("start_distance").forGetter(SummonFollow::startDistance),
            Codec.DOUBLE.optionalFieldOf("stop_distance").forGetter(SummonFollow::stopDistance),
            Codec.DOUBLE.optionalFieldOf("teleport_distance")
                    .forGetter(SummonFollow::teleportDistance)
    ).apply(instance, SummonFollow::new)));

    public SummonFollow {
        startDistance = startDistance == null ? Optional.empty() : startDistance;
        stopDistance = stopDistance == null ? Optional.empty() : stopDistance;
        teleportDistance = teleportDistance == null ? Optional.empty() : teleportDistance;
    }

    public boolean isDefault() {
        return startDistance.isEmpty() && stopDistance.isEmpty() && teleportDistance.isEmpty();
    }

    public double start() {
        return Math.max(0.0D, startDistance.orElse(SummonTuning.get().followStartDistance));
    }

    public double stop() {
        return Math.max(0.0D, stopDistance.orElse(SummonTuning.get().followStopDistance));
    }

    public double teleport() {
        return Math.max(0.0D, teleportDistance.orElse(SummonTuning.get().teleportDistance));
    }

    public boolean teleports() {
        return teleport() > 0.0D;
    }
}
