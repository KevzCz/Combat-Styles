package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.api.CombatScheduler;
import net.pixeldreamstudios.combat_styles.platform.SpellPowerAccess;

import java.util.Locale;

public record EffectStacking(String source, double per, int minStacks, int maxStacks, int delayTicks,
                             Mode mode) {

    public static final String KEY = "stacking";
    public static final String KEY_SOURCE = "source";
    public static final String KEY_PER = "per";
    public static final String KEY_MIN_STACKS = "min_stacks";
    public static final String KEY_MAX_STACKS = "max_stacks";
    public static final String KEY_DELAY_TICKS = "delay_ticks";
    public static final String KEY_MODE = "mode";
    public static final double DEFAULT_PER = 1.0D;
    public static final int DEFAULT_MIN_STACKS = 0;
    public static final int DEFAULT_MAX_STACKS = 1;
    public static final int NO_DELAY = 0;
    public static final int SINGLE = 1;
    public static final float UNSCALED = 1.0F;
    public static final String SAMPLE_SOURCE = "enchantment:minecraft:sharpness";
    private static final double MIN_PER = 1.0E-4D;

    public enum Mode implements StringRepresentable {
        REPEAT,
        MULTIPLY;

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EffectStacking SAMPLE = new EffectStacking(SAMPLE_SOURCE, DEFAULT_PER,
            DEFAULT_MIN_STACKS, DEFAULT_MAX_STACKS, NO_DELAY, Mode.REPEAT);

    public static final Codec<EffectStacking> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf(KEY_SOURCE).forGetter(EffectStacking::source),
            Codec.DOUBLE.optionalFieldOf(KEY_PER, DEFAULT_PER).forGetter(EffectStacking::per),
            Codec.INT.optionalFieldOf(KEY_MIN_STACKS, DEFAULT_MIN_STACKS).forGetter(EffectStacking::minStacks),
            Codec.INT.optionalFieldOf(KEY_MAX_STACKS, DEFAULT_MAX_STACKS).forGetter(EffectStacking::maxStacks),
            Codec.INT.optionalFieldOf(KEY_DELAY_TICKS, NO_DELAY).forGetter(EffectStacking::delayTicks),
            Mode.CODEC.optionalFieldOf(KEY_MODE, Mode.REPEAT).forGetter(EffectStacking::mode)
    ).apply(instance, EffectStacking::new));

    public EffectStacking {
        source = source == null ? "" : source.trim();
        per = Math.max(MIN_PER, per);
        minStacks = Math.max(0, minStacks);
        maxStacks = Math.max(minStacks, maxStacks);
        delayTicks = Math.max(NO_DELAY, delayTicks);
        mode = mode == null ? Mode.REPEAT : mode;
    }

    public int count(Player player) {
        double value = SpellPowerAccess.valueOf(player, source);
        int stacks = (int) Math.floor(value / per);
        return Math.max(minStacks, Math.min(maxStacks, stacks));
    }

    public static int stacksFor(Player player, TriggeredEffect entry) {
        return entry.stacking().map(stacking -> stacking.count(player)).orElse(SINGLE);
    }

    public static boolean allows(Player player, TriggeredEffect entry) {
        return stacksFor(player, entry) > 0;
    }

    public static float multiplier(Player player, TriggeredEffect entry, ItemStack owner) {
        if (entry.stacking().map(stacking -> stacking.mode() != Mode.MULTIPLY).orElse(true)) {
            return UNSCALED;
        }
        int[] stacks = new int[1];
        OwnerScope.run(owner, () -> stacks[0] = stacksFor(player, entry));
        return stacks[0];
    }

    public static void fire(Player player, TriggeredEffect entry, ItemStack owner, String label,
                            Runnable fire) {
        int[] counted = new int[1];
        OwnerScope.run(owner, () -> counted[0] = stacksFor(player, entry));
        boolean repeats = entry.stacking().map(stacking -> stacking.mode() == Mode.REPEAT).orElse(false);
        int times = repeats ? counted[0] : Math.min(SINGLE, counted[0]);
        int spacing = entry.stacking().map(EffectStacking::delayTicks).orElse(NO_DELAY);
        Runnable scoped = () -> OwnerScope.run(owner, fire);
        for (int stack = 0; stack < times; stack++) {
            int delay = entry.delayTicks() + stack * spacing;
            if (!CombatScheduler.after(player, delay, label, scoped)) {
                scoped.run();
            }
        }
    }
}
