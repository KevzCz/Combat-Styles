package net.pixeldreamstudios.combat_styles.entity;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public enum AbilityPick implements StringRepresentable {

    IN_ORDER,
    RANDOM,
    CYCLE;

    public static final Codec<AbilityPick> CODEC = StringRepresentable.fromEnum(AbilityPick::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static final int NONE_PICKED = -1;

    public String pick(List<String> pool, int slot, int step, RandomSource random,
                       Predicate<String> ready) {
        int index = pickIndex(pool, slot, step, random, ready);
        return index == NONE_PICKED ? null : pool.get(index);
    }

    public int pickIndex(List<String> pool, int slot, int step, RandomSource random,
                         Predicate<String> ready) {
        if (pool == null || pool.isEmpty()) {
            return NONE_PICKED;
        }
        if (this == RANDOM) {
            List<Integer> usable = new ArrayList<>();
            for (int index = 0; index < pool.size(); index++) {
                if (ready.test(pool.get(index))) {
                    usable.add(index);
                }
            }
            return usable.isEmpty() ? NONE_PICKED : usable.get(random.nextInt(usable.size()));
        }
        int start = this == CYCLE ? slot + step : slot;
        if (this == IN_ORDER) {
            int own = Math.floorMod(start, pool.size());
            return ready.test(pool.get(own)) ? own : NONE_PICKED;
        }
        for (int offset = 0; offset < pool.size(); offset++) {
            int candidate = Math.floorMod(start + offset, pool.size());
            if (ready.test(pool.get(candidate))) {
                return candidate;
            }
        }
        return NONE_PICKED;
    }
}
