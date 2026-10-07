package net.pixeldreamstudios.combat_styles.entity;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.network.BeamPayload;

public final class BeamCast {

    private BeamCast() {
    }

    public static void show(ServerLevel level, Entity source, Vec3 from, Vec3 to, BeamLook look) {
        if (level == null || source == null || from == null || to == null) {
            return;
        }
        BeamLook shown = look == null ? BeamLook.DEFAULT : look;
        BeamPayload payload = new BeamPayload(source.getId(), from.subtract(source.position()), to,
                shown.rgb(), shown.width(), shown.alpha(), shown.durationTicks());
        level.getChunkSource().broadcastAndSend(source, new ClientboundCustomPayloadPacket(payload));
    }
}
