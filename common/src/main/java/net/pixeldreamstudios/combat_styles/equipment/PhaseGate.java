package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record PhaseGate(Optional<Boolean> onMiss, Optional<Boolean> onKill,
                        Optional<Boolean> onSummonHit, Optional<Boolean> onSwing,
                        Optional<Boolean> onPlayerHit, StateGate state,
                        Optional<Float> chance) {

    public static final String CHANCE = "chance";

    public static final PhaseGate ANY = new PhaseGate(Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), StateGate.ANY, Optional.empty());

    public static final MapCodec<PhaseGate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("on_miss").forGetter(PhaseGate::onMiss),
            Codec.BOOL.optionalFieldOf("on_kill").forGetter(PhaseGate::onKill),
            Codec.BOOL.optionalFieldOf("on_summon_hit").forGetter(PhaseGate::onSummonHit),
            Codec.BOOL.optionalFieldOf("on_swing").forGetter(PhaseGate::onSwing),
            Codec.BOOL.optionalFieldOf("on_player_hit").forGetter(PhaseGate::onPlayerHit),
            StateGate.CODEC.forGetter(PhaseGate::state),
            Codec.FLOAT.optionalFieldOf(CHANCE).forGetter(PhaseGate::chance)
    ).apply(instance, PhaseGate::new));

    public PhaseGate {
        state = state == null ? StateGate.ANY : state;
        chance = chance == null
                ? Optional.empty()
                : chance.map(value -> Math.clamp(value, 0.0F, 1.0F));
    }

    public boolean isEmpty() {
        return onMiss.isEmpty() && onKill.isEmpty() && onSummonHit.isEmpty()
                && onSwing.isEmpty() && onPlayerHit.isEmpty() && state.isEmpty();
    }

    public PhaseGate withChance(Optional<Float> value) {
        return new PhaseGate(onMiss, onKill, onSummonHit, onSwing, onPlayerHit, state, value);
    }

    public boolean gatesPass() {
        return onSwing.isPresent() || onPlayerHit.isPresent();
    }

    public boolean wantsHitPass() {
        return onPlayerHit.orElse(false);
    }

    public String rejectsPass(boolean hitPass) {
        if (onSwing.orElse(false) && hitPass) {
            return "on_swing (the hit pass is running)";
        }
        if (onPlayerHit.orElse(false) && !hitPass) {
            return "on_player_hit (the swing pass is running)";
        }
        return null;
    }
}
