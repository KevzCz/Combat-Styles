package net.pixeldreamstudios.combat_styles.kit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class KitState {

    public static final String NBT_KEY = "CombatStylesKitState";
    private static final String KEY_BOUND = "bound";

    public static final class Data {

        private boolean bound;

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean(KEY_BOUND, bound);
            return tag;
        }

        public void load(CompoundTag tag) {
            bound = tag.getBoolean(KEY_BOUND);
        }

        public void copyFrom(Data other) {
            load(other.save());
        }
    }

    public interface Holder {
        Data combat_styles$kitState();
    }

    private KitState() {
    }

    public static boolean isBound(Player player) {
        Data data = dataOf(player);
        return data != null && data.bound;
    }

    public static void markBound(Player player) {
        Data data = dataOf(player);
        if (data != null) {
            data.bound = true;
        }
    }

    public static void copy(Player from, Player to) {
        Data source = dataOf(from);
        Data target = dataOf(to);
        if (source != null && target != null) {
            target.copyFrom(source);
        }
    }

    private static Data dataOf(Player player) {
        return player instanceof Holder holder ? holder.combat_styles$kitState() : null;
    }
}
