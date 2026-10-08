package net.pixeldreamstudios.combat_styles.equipment;

import com.google.common.collect.MapMaker;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.Set;

public final class ProjectileCopies {

    private static final Set<Entity> COPIES =
            Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    private ProjectileCopies() {
    }

    public static void mark(Entity copy) {
        if (copy != null) {
            COPIES.add(copy);
        }
    }

    public static boolean isCopy(Entity projectile) {
        return projectile != null && COPIES.contains(projectile);
    }
}
