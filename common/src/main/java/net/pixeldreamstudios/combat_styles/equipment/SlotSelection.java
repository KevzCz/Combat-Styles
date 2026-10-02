package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public record SlotSelection(Optional<Range> range, List<Integer> indices) {

    public record Range(int from, int to) {

        public static final Codec<Range> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("from").forGetter(Range::from),
                Codec.INT.fieldOf("to").forGetter(Range::to)
        ).apply(instance, Range::new));
    }

    public static final Codec<SlotSelection> CODEC = Codec.either(
            Range.CODEC, Codec.INT.listOf()).xmap(
            either -> either.map(
                    range -> new SlotSelection(Optional.of(range), List.of()),
                    indices -> new SlotSelection(Optional.empty(), indices)),
            selection -> selection.range().isPresent()
                    ? Either.left(selection.range().get())
                    : Either.right(selection.indices()));

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
            return slotIndex >= Math.min(from, to) && slotIndex <= Math.max(from, to);
        }
        for (int index : indices) {
            if (resolve(index, sequenceSize) == slotIndex) {
                return true;
            }
        }
        return false;
    }

    private static int resolve(int index, int sequenceSize) {
        return index < 0 ? sequenceSize + index : index;
    }
}
