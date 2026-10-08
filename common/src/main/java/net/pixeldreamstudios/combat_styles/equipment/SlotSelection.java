package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.pixeldreamstudios.combat_styles.custom.Customs;

import java.util.List;
import java.util.Optional;

public record SlotSelection(Optional<Range> range, List<Integer> indices) {

    public static final int FIRST = 0;
    public static final int LAST = -1;

    public record Range(int from, int to, List<Integer> except) {

        public static final Codec<Range> CODEC = Customs.wrap(Range.class, RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("from", FIRST).forGetter(Range::from),
                Codec.INT.optionalFieldOf("to", LAST).forGetter(Range::to),
                Codec.INT.listOf().optionalFieldOf("except", List.of()).forGetter(Range::except)
        ).apply(instance, Range::new)));

        public Range {
            except = except == null ? List.of() : List.copyOf(except);
        }

        public Range(int from, int to) {
            this(from, to, List.of());
        }
    }

    public static final Codec<SlotSelection> CODEC = Customs.wrap(SlotSelection.class, Codec.either(
            Range.CODEC, Codec.INT.listOf()).xmap(
            either -> either.map(
                    range -> new SlotSelection(Optional.of(range), List.of()),
                    indices -> new SlotSelection(Optional.empty(), indices)),
            selection -> selection.range().isPresent()
                    ? Either.left(selection.range().get())
                    : Either.right(selection.indices())));

    public SlotSelection {
        indices = indices == null ? List.of() : List.copyOf(indices);
    }

    public boolean matches(int slotIndex, int sequenceSize) {
        if (sequenceSize <= 0 || slotIndex < 0) {
            return false;
        }
        if (range.isPresent()) {
            int from = resolve(range.get().from(), sequenceSize);
            int to = resolve(range.get().to(), sequenceSize);
            return slotIndex >= Math.min(from, to) && slotIndex <= Math.max(from, to)
                    && !listed(range.get().except(), slotIndex, sequenceSize);
        }
        return listed(indices, slotIndex, sequenceSize);
    }

    private static boolean listed(List<Integer> indices, int slotIndex, int sequenceSize) {
        for (int index : indices) {
            if (resolve(index, sequenceSize) == slotIndex) {
                return true;
            }
        }
        return false;
    }

    public static int resolve(int index, int sequenceSize) {
        return index < 0 ? sequenceSize + index : index;
    }
}
