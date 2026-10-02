package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.config.SummonTuning;

import java.util.ArrayList;
import java.util.List;

public final class SummonRoster {

    private SummonRoster() {
    }

    public static List<ConfigurableSummon> of(Player player) {
        if (player == null) {
            return List.of();
        }
        List<ConfigurableSummon> found = new ArrayList<>();
        for (Entity entity : player.level().getEntities(player,
                player.getBoundingBox().inflate(SummonTuning.get().rosterRadius),
                candidate -> candidate instanceof ConfigurableSummon)) {
            ConfigurableSummon summon = (ConfigurableSummon) entity;
            if (!entity.isRemoved() && !summon.fading() && summon.ownedBy(player)) {
                found.add(summon);
            }
        }
        return List.copyOf(found);
    }

    public static List<ConfigurableSummon> available(Player player, SummonTargeting targeting) {
        SummonTargeting mode = targeting == null ? SummonTargeting.ALL : targeting;
        List<ConfigurableSummon> free = new ArrayList<>();
        for (ConfigurableSummon summon : of(player)) {
            if (summon.canAct() && mode.accepts(summon)) {
                free.add(summon);
            }
        }
        return List.copyOf(free);
    }

    public static <T extends ConfigurableSummon> List<T> ofType(Player player, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (ConfigurableSummon summon : of(player)) {
            if (type.isInstance(summon)) {
                found.add(type.cast(summon));
            }
        }
        return List.copyOf(found);
    }

    public static int count(Player player) {
        return of(player).size();
    }
}
