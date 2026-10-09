package net.pixeldreamstudios.combat_styles.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.equipment.OwnerScope;
import net.pixeldreamstudios.combat_styles.mixin.ArrowWeaponAccessor;

public final class FiringItem {

    private FiringItem() {
    }

    public static DamageSource stated(DamageSource source) {
        ItemStack stack = OwnerScope.current();
        if (stack == null || source == null || source instanceof Stated) {
            return source;
        }
        Entity causing = source.getEntity();
        if (causing == null || source.getDirectEntity() != causing
                || source.sourcePositionRaw() != null) {
            return source;
        }
        return new Stated(source, stack);
    }

    public static void stamp(Entity projectile) {
        stamp(projectile, OwnerScope.current());
    }

    public static void stamp(Entity projectile, ItemStack stack) {
        if (stack == null || projectile == null) {
            return;
        }
        if (projectile instanceof FiredFrom fired) {
            fired.combat_styles$setFiredFrom(stack.copy());
        } else if (projectile instanceof AbstractArrow arrow && !stack.isEmpty()) {
            ((ArrowWeaponAccessor) arrow).combat_styles$setFiredFromWeapon(stack.copy());
        }
    }

    private static final class Stated extends DamageSource {

        private final ItemStack weapon;

        private Stated(DamageSource source, ItemStack weapon) {
            super(source.typeHolder(), source.getDirectEntity(), source.getEntity());
            this.weapon = weapon;
        }

        @Override
        public ItemStack getWeaponItem() {
            return weapon;
        }
    }
}
