package net.pixeldreamstudios.combat_styles.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EntityGetter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.pixeldreamstudios.combat_styles.entity.SummonBrains;
import net.pixeldreamstudios.combat_styles.entity.SummonData;
import net.pixeldreamstudios.combat_styles.entity.SummonFollowOwnerGoal;
import net.pixeldreamstudios.combat_styles.entity.SummonAi;
import net.pixeldreamstudios.combat_styles.entity.SummonOwner;
import net.pixeldreamstudios.combat_styles.entity.SummonOwnerTargetGoal;

import java.util.UUID;

@Mixin(Mob.class)
public abstract class SummonableMobMixin implements SummonData, OwnableEntity {

    @Unique
    private static final int FOLLOW_PRIORITY = 5;

    @Unique
    private static final int PET_TARGET_PRIORITY = 1;

    @Shadow
    @Final
    protected GoalSelector goalSelector;

    @Shadow
    @Final
    protected GoalSelector targetSelector;

    @Override
    public EntityGetter level() {
        return ((Mob) (Object) this).level();
    }

    @Override
    public UUID getOwnerUUID() {
        return combat_styles$summonOwner();
    }

    @Override
    public LivingEntity getOwner() {
        Mob self = (Mob) (Object) this;
        UUID owner = getOwnerUUID();
        return owner == null ? null : self.level().getPlayerByUUID(owner);
    }

    @Override
    public void combat_styles$applySummonGoals() {
        Mob self = (Mob) (Object) this;
        targetSelector.removeAllGoals(goal -> true);
        targetSelector.addGoal(PET_TARGET_PRIORITY, new SummonOwnerTargetGoal(self));
        goalSelector.removeAllGoals(goal -> goal instanceof FollowOwnerGoal
                || goal instanceof SummonFollowOwnerGoal);
        goalSelector.addGoal(FOLLOW_PRIORITY, new SummonFollowOwnerGoal(self));
    }

    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void combat_styles$forgetForbiddenTargets(CallbackInfo callback) {
        Mob self = (Mob) (Object) this;
        if (SummonOwner.isSummon(self)) {
            SummonBrains.forgetForbiddenTargets(self);
        }
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void combat_styles$refuseFriendlyTarget(LivingEntity target, CallbackInfo callback) {
        Mob self = (Mob) (Object) this;
        if (target != null && SummonOwner.isSummon(self) && SummonAi.friendlyTo(self, target)) {
            callback.cancel();
        }
    }

    @Inject(method = "convertTo", at = @At("HEAD"), cancellable = true)
    private <T extends Mob> void combat_styles$neverConvert(EntityType<T> type, boolean keepEquipment,
                                                            CallbackInfoReturnable<T> callback) {
        if (SummonOwner.isSummon((Mob) (Object) this)) {
            callback.setReturnValue(null);
        }
    }

    @Inject(method = "canPickUpLoot", at = @At("HEAD"), cancellable = true)
    private void combat_styles$neverPickUp(CallbackInfoReturnable<Boolean> callback) {
        if (SummonOwner.isSummon((Mob) (Object) this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "wantsToPickUp", at = @At("HEAD"), cancellable = true)
    private void combat_styles$neverWant(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (SummonOwner.isSummon((Mob) (Object) this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void combat_styles$neverBurn(CallbackInfoReturnable<Boolean> callback) {
        if (SummonOwner.isSummon((Mob) (Object) this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void combat_styles$guardInteraction(Player player, InteractionHand hand,
                                                CallbackInfoReturnable<InteractionResult> callback) {
        Mob self = (Mob) (Object) this;
        if (!SummonOwner.isSummon(self)) {
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        boolean harmless = held.isEmpty() || held.is(Items.LEAD) || held.is(Items.NAME_TAG);
        if (!harmless || self instanceof AbstractVillager) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }
}
