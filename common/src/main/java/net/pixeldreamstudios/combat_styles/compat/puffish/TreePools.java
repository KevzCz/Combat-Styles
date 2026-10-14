package net.pixeldreamstudios.combat_styles.compat.puffish;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TreePools {

    public static final String NBT_KEY = "CombatStylesTreePools";

    public static final class Data {

        private final Map<String, Integer> experience = new LinkedHashMap<>();

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            experience.forEach(tag::putInt);
            return tag;
        }

        public void load(CompoundTag tag) {
            experience.clear();
            for (String pool : tag.getAllKeys()) {
                experience.put(pool, tag.getInt(pool));
            }
        }

        public void copyFrom(Data other) {
            load(other.save());
        }
    }

    public interface Holder {
        Data combat_styles$treePools();
    }

    private TreePools() {
    }

    public static void add(Player player, String pool, int amount) {
        Data data = dataOf(player);
        if (data != null && amount > 0) {
            data.experience.merge(pool, amount, Integer::sum);
        }
    }

    public static int take(Player player, String pool) {
        Data data = dataOf(player);
        if (data == null) {
            return 0;
        }
        Integer stored = data.experience.remove(pool);
        return stored == null ? 0 : stored;
    }

    public static void copy(Player from, Player to) {
        Data source = dataOf(from);
        Data target = dataOf(to);
        if (source != null && target != null) {
            target.copyFrom(source);
        }
    }

    private static Data dataOf(Player player) {
        return player instanceof Holder holder ? holder.combat_styles$treePools() : null;
    }
}
