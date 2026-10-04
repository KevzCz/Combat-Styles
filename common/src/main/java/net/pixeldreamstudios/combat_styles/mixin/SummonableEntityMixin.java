package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.entity.SummonData;
import net.pixeldreamstudios.combat_styles.entity.SummonFollow;
import net.pixeldreamstudios.combat_styles.entity.SummonOwner;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class SummonableEntityMixin implements SummonData {

    @Unique
    private UUID combat_styles$entityOwner;

    @Unique
    private float combat_styles$entityDamage;

    @Unique
    private DamageSpec combat_styles$entitySpec;

    @Unique
    private boolean combat_styles$entitySpectral;

    @Unique
    private boolean combat_styles$stale;

    @Unique
    private SummonFollow combat_styles$entityFollow = SummonFollow.DEFAULTS;

    @Unique
    private UUID combat_styles$entityCastBy;

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
    public boolean combat_styles$spectral() {
        return combat_styles$entitySpectral;
    }

    @Override
    public void combat_styles$setSpectral(boolean spectral) {
        combat_styles$entitySpectral = spectral;
    }

    @Override
    public UUID combat_styles$castBy() {
        return combat_styles$entityCastBy;
    }

    @Override
    public void combat_styles$setCastBy(UUID caster) {
        combat_styles$entityCastBy = caster;
    }

    @Override
    public SummonFollow combat_styles$follow() {
        return combat_styles$entityFollow;
    }

    @Override
    public void combat_styles$setFollow(SummonFollow follow) {
        combat_styles$entityFollow = follow == null ? SummonFollow.DEFAULTS : follow;
    }

    @Override
    public void combat_styles$applySummonGoals() {
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void combat_styles$ownerPassesThrough(Entity other, CallbackInfo callback) {
        if (SummonOwner.ownerPair((Entity) (Object) this, other)) {
            callback.cancel();
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void combat_styles$markStale(CompoundTag tag, CallbackInfo callback) {
        combat_styles$stale = SummonOwner.isSummon((Entity) (Object) this);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void combat_styles$clearStale(CallbackInfo callback) {
        Entity self = (Entity) (Object) this;
        if (combat_styles$stale && !self.level().isClientSide()) {
            self.discard();
            callback.cancel();
        }
    }

    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void combat_styles$dropNothing(ItemStack stack, float offsetY,
                                           CallbackInfoReturnable<ItemEntity> callback) {
        if (SummonOwner.isSummon((Entity) (Object) this) && !stack.is(Items.LEAD)) {
            callback.setReturnValue(null);
        }
    }
}
