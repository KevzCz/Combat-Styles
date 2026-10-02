package net.pixeldreamstudios.combat_styles.mixin;

import net.bettercombat.client.particle.SlashParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.pixeldreamstudios.combat_styles.client.DraggedTrail;

@Mixin(SlashParticle.class)
public abstract class SlashParticleDragMixin extends Particle {

    @Unique
    private Player combat_styles$follows;

    @Unique
    private Vec3 combat_styles$offset;

    protected SlashParticleDragMixin() {
        super(null, 0.0D, 0.0D, 0.0D);
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 1)
    private void combat_styles$adopt(CallbackInfo callback) {
        Player owner = DraggedTrail.pendingOwner();
        if (owner == null) {
            return;
        }
        combat_styles$follows = owner;
        combat_styles$offset = new Vec3(x - owner.getX(), y - owner.getY(), z - owner.getZ());
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 1)
    private void combat_styles$follow(CallbackInfo callback) {
        if (combat_styles$follows == null || !combat_styles$follows.isAlive()) {
            return;
        }
        Vec3 origin = combat_styles$follows.position();
        setPos(origin.x + combat_styles$offset.x,
                origin.y + combat_styles$offset.y,
                origin.z + combat_styles$offset.z);
    }
}
