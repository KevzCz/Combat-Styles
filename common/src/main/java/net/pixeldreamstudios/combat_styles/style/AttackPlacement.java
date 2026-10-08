package net.pixeldreamstudios.combat_styles.style;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.List;

public record AttackPlacement(Mode mode, float range, boolean stopAtEntities,
                              float stopMargin) {

    public static final float DEFAULT_RANGE = 12.0F;
    public static final float MAX_RANGE = 64.0F;
    public static final float DEFAULT_MARGIN = 0.5F;

    public static final AttackPlacement NONE =
            new AttackPlacement(Mode.NORMAL, DEFAULT_RANGE, true, DEFAULT_MARGIN);

    public static final Codec<AttackPlacement> CODEC = Customs.wrap(AttackPlacement.class, RecordCodecBuilder.create(
            instance -> instance.group(
                    Mode.CODEC.optionalFieldOf("mode", Mode.NORMAL)
                            .forGetter(AttackPlacement::mode),
                    Codec.FLOAT.optionalFieldOf("range", DEFAULT_RANGE)
                            .forGetter(AttackPlacement::range),
                    Codec.BOOL.optionalFieldOf("stop_at_entities", true)
                            .forGetter(AttackPlacement::stopAtEntities),
                    Codec.FLOAT.optionalFieldOf("stop_margin", DEFAULT_MARGIN)
                            .forGetter(AttackPlacement::stopMargin)
            ).apply(instance, AttackPlacement::new)));

    public boolean followsCursor() {
        return mode == Mode.CURSOR;
    }

    public boolean isEmpty() {
        return mode == Mode.NORMAL;
    }

    public float clampedRange() {
        return Math.clamp(range, 0.0F, MAX_RANGE);
    }

    public String signature() {
        return isEmpty() ? "" : "p" + mode.getSerializedName() + clampedRange()
                + (stopAtEntities ? "e" : "") + stopMargin;
    }

    public enum Mode implements StringRepresentable {

        NORMAL("normal"),
        CURSOR("cursor");

        public static final List<Mode> ALL = List.of(values());

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
