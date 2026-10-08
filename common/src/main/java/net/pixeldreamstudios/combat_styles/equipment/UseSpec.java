package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.UseAnim;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.Locale;
import java.util.Optional;

public record UseSpec(UseKind kind, int holdTicks, int thresholdTicks,
                      int channelStart, int channelTicks,
                      UseAnim pose,
                      Optional<String> channelAnimation,
                      Optional<String> releaseAnimation,
                      EffectCooldown cooldown) {

    public static final UseAnim DEFAULT_POSE = UseAnim.BOW;

    public static final Codec<UseAnim> POSE_CODEC = Codec.STRING.xmap(
            UseSpec::poseByName, pose -> pose.name().toLowerCase(Locale.ROOT));

    public static final int NO_HOLD = 0;
    public static final int INFINITE = -1;
    public static final int OPEN_ENDED = 72000;
    public static final int NO_CHANNEL = 0;
    public static final int NO_THRESHOLD = 0;
    public static final int IMMEDIATE = 0;
    public static final int DEFAULT_CHANNEL_TICKS = 10;

    public static final UseSpec NONE = new UseSpec(UseKind.PRESS, NO_HOLD, NO_THRESHOLD,
            IMMEDIATE, NO_CHANNEL, DEFAULT_POSE,
            Optional.empty(), Optional.empty(), EffectCooldown.NONE);

    public static final Codec<UseSpec> CODEC = Customs.wrap(UseSpec.class, RecordCodecBuilder.create(instance -> instance.group(
            UseKind.CODEC.optionalFieldOf("mode", UseKind.PRESS).forGetter(UseSpec::kind),
            Codec.INT.optionalFieldOf("hold_ticks", NO_HOLD).forGetter(UseSpec::holdTicks),
            Codec.INT.optionalFieldOf("threshold_ticks", NO_THRESHOLD)
                    .forGetter(UseSpec::thresholdTicks),
            Codec.INT.optionalFieldOf("channel_start", IMMEDIATE)
                    .forGetter(UseSpec::channelStart),
            Codec.INT.optionalFieldOf("channel_ticks", NO_CHANNEL)
                    .forGetter(UseSpec::channelTicks),
            POSE_CODEC.optionalFieldOf("pose", DEFAULT_POSE).forGetter(UseSpec::pose),
            Codec.STRING.optionalFieldOf("channel_animation")
                    .forGetter(UseSpec::channelAnimation),
            Codec.STRING.optionalFieldOf("release_animation")
                    .forGetter(UseSpec::releaseAnimation),
            EffectCooldown.CODEC.optionalFieldOf("cooldown", EffectCooldown.NONE)
                    .forGetter(UseSpec::cooldown)
    ).apply(instance, UseSpec::new)));

    public static final StreamCodec<ByteBuf, UseSpec> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(
                    raw -> CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(raw))
                            .result().orElse(NONE),
                    spec -> CODEC.encodeStart(JsonOps.INSTANCE, spec)
                            .result().map(JsonElement::toString).orElse(""));

    public UseSpec {
        kind = kind == null ? UseKind.PRESS : kind;
        pose = pose == null ? DEFAULT_POSE : pose;
        cooldown = cooldown == null ? EffectCooldown.NONE : cooldown;
        if (kind == UseKind.PRESS) {
            holdTicks = NO_HOLD;
            thresholdTicks = NO_THRESHOLD;
            channelStart = IMMEDIATE;
            channelTicks = NO_CHANNEL;
        } else {
            holdTicks = holdTicks < NO_HOLD ? INFINITE : holdTicks;
            thresholdTicks = Math.max(NO_THRESHOLD, thresholdTicks);
            channelStart = Math.max(IMMEDIATE, channelStart);
            channelTicks = kind == UseKind.CHANNEL
                    ? Math.max(1, channelTicks)
                    : NO_CHANNEL;
        }
    }

    public boolean isEmpty() {
        return kind == UseKind.PRESS && pose == DEFAULT_POSE
                && channelAnimation.isEmpty() && releaseAnimation.isEmpty()
                && cooldown.isEmpty();
    }

    public boolean channels() {
        return kind.holds();
    }

    public boolean infinite() {
        return holdTicks == INFINITE;
    }

    public boolean repeats() {
        return kind.repeats();
    }

    public boolean emits(UsePhase phase) {
        return kind.emits(phase);
    }

    public UsePhase completion() {
        return kind.completion();
    }

    public int duration() {
        if (!channels()) {
            return NO_HOLD;
        }
        return infinite() ? OPEN_ENDED : holdTicks;
    }

    public int required() {
        return kind.completesOnRelease() ? thresholdTicks : NO_THRESHOLD;
    }

    public boolean started(int elapsed) {
        return elapsed >= channelStart;
    }

    public static UseAnim poseByName(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_POSE;
        }
        String wanted = name.trim().toUpperCase(Locale.ROOT);
        for (UseAnim option : UseAnim.values()) {
            if (option.name().equals(wanted)) {
                return option;
            }
        }
        return DEFAULT_POSE;
    }

    public UseAnim useAnimation() {
        return pose == null || pose == UseAnim.NONE ? DEFAULT_POSE : pose;
    }
}
