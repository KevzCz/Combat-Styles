package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.entity.SummonData;
import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class SummonableEntityMixin implements SummonData {

    @Unique
    private static final String TAG_OWNER = "CombatStylesSummonOwner";

    @Unique
    private static final String TAG_DAMAGE = "CombatStylesSummonDamage";

    @Unique
    private static final int ADOPT_WINDOW = 20;

    @Unique
    private UUID combat_styles$entityOwner;

    @Unique
    private float combat_styles$entityDamage;

    @Unique
    private DamageSpec combat_styles$entitySpec;

    @Override
    public UUID combat_styles$summonOwner() {
        return combat_styles$entityOwner;
    }

    @Override
    public void combat_styles$setSummonOwner(Player owner) {
        combat_styles$entityOwner = owner == null ? null : owner.getUUID();
    }

    @Override
    public void combat_styles$setSummonOwnerId(UUID owner) {
        combat_styles$entityOwner = owner;
    }

    @Override
    public float combat_styles$summonDamage() {
        return combat_styles$entityDamage;
    }

    @Override
    public void combat_styles$setSummonDamage(float damage) {
        combat_styles$entityDamage = damage;
    }

    @Override
    public DamageSpec combat_styles$summonSpec() {
        return combat_styles$entitySpec;
    }

    @Override
    public void combat_styles$setSummonSpec(DamageSpec spec) {
        combat_styles$entitySpec = spec;
    }

    @Override
    public void combat_styles$applySummonGoals() {
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void combat_styles$adoptOwner(CallbackInfo callback) {
        Entity self = (Entity) (Object) this;
        if (combat_styles$entityOwner != null || self.level().isClientSide()
                || self.tickCount > ADOPT_WINDOW) {
            return;
        }
        UUID inherited = SummonOwner.inheritedOwner(self);
        if (inherited != null) {
            combat_styles$entityOwner = inherited;
            combat_styles$applySummonGoals();
        }
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void combat_styles$save(CompoundTag tag,
                                    CallbackInfoReturnable<CompoundTag> callback) {
        if (combat_styles$entityOwner != null) {
            tag.putUUID(TAG_OWNER, combat_styles$entityOwner);
        }
        if (combat_styles$entityDamage > 0.0F) {
            tag.putFloat(TAG_DAMAGE, combat_styles$entityDamage);
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void combat_styles$load(CompoundTag tag, CallbackInfo callback) {
        if (tag.hasUUID(TAG_OWNER)) {
            combat_styles$entityOwner = tag.getUUID(TAG_OWNER);
        }
        combat_styles$entityDamage = tag.getFloat(TAG_DAMAGE);
    }
}
