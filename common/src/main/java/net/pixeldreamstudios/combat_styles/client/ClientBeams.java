package net.pixeldreamstudios.combat_styles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.network.BeamPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientBeams {

    private static final Map<Integer, List<Shown>> ACTIVE = new HashMap<>();

    private record Shown(BeamPayload beam, long startedAt) {
        private boolean expired(long now) {
            return now - startedAt >= beam.durationTicks();
        }
    }

    private ClientBeams() {
    }

    public static void accept(BeamPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (payload == null || client.level == null) {
            return;
        }
        ACTIVE.computeIfAbsent(payload.sourceId(), ignored -> new ArrayList<>())
                .add(new Shown(payload, client.level.getGameTime()));
    }

    public static boolean has(Entity entity) {
        return entity != null && ACTIVE.containsKey(entity.getId());
    }

    public static void draw(Entity entity, double x, double y, double z, float partialTick,
                            PoseStack poses, MultiBufferSource buffers) {
        List<Shown> shown = entity == null ? null : ACTIVE.get(entity.getId());
        if (shown == null || entity.level() == null) {
            return;
        }
        long now = entity.level().getGameTime();
        shown.removeIf(beam -> beam.expired(now));
        if (shown.isEmpty()) {
            ACTIVE.remove(entity.getId());
            return;
        }
        Vec3 at = entity.getPosition(partialTick);
        Vec3 camera = new Vec3(x, y, z).subtract(at);
        for (Shown beam : shown) {
            Vec3 from = at.add(beam.beam().offset()).add(camera);
            Vec3 to = beam.beam().end().add(camera);
            BeamRenderer.draw(poses, buffers, from, to, beam.beam().width(), beam.beam().rgb(),
                    beam.beam().alpha(), now + partialTick);
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
