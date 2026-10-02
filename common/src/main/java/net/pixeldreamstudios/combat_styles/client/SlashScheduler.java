package net.pixeldreamstudios.combat_styles.client;

import net.bettercombat.api.fx.ParticlePlacement;
import net.bettercombat.api.fx.TrailAppearance;
import net.bettercombat.client.particle.SlashParticleUtil;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SlashScheduler {

    private record Pending(AbstractClientPlayer player, boolean offhand, float range,
                           ParticlePlacement placement, TrailAppearance appearance,
                           long dueAt) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private SlashScheduler() {
    }

    public static void schedule(AbstractClientPlayer player, boolean offhand, float range,
                                ParticlePlacement placement, TrailAppearance appearance,
                                int delayTicks) {
        if (player == null || placement == null || delayTicks <= 0) {
            return;
        }
        PENDING.add(new Pending(player, offhand, range, placement, appearance,
                player.level().getGameTime() + delayTicks));
    }

    public static void tick(long gameTime) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Pending> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            if (pending.player().isRemoved() || !pending.player().isAlive()) {
                iterator.remove();
                continue;
            }
            if (gameTime < pending.dueAt()) {
                continue;
            }
            iterator.remove();
            SlashParticleUtil.spawnParticles(pending.player(), pending.offhand(),
                    pending.range(), List.of(pending.placement()), pending.appearance());
        }
    }

    public static void clear() {
        PENDING.clear();
    }
}
