package net.pixeldreamstudios.combat_styles.equipment;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.ComboState;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import net.pixeldreamstudios.combat_styles.style.StyleEntry;

public record EquipmentContext(Player player,
                          StyleEntry entry,
                          WeaponAttributes.Attack attack,
                          int slotIndex,
                          int sequenceSize,
                          int swingCount,
                          boolean killed,
                          boolean summonHit,
                          LivingEntity target,
                          HurtEvent hurt,
                          ShotEvent shot,
                          BlockEvent blocked,
                          UseEvent used,
                          boolean hitPass,
                          float dealtDamage,
                          ProjectileHitEvent projectileHit,
                          SpellCastEvent cast,
                          SummonEvent summoned) {

    public EquipmentContext(Player player, StyleEntry entry, WeaponAttributes.Attack attack,
                       int slotIndex, int sequenceSize, int swingCount,
                       boolean killed, boolean summonHit, LivingEntity target,
                       HurtEvent hurt, ShotEvent shot, BlockEvent blocked, UseEvent used) {
        this(player, entry, attack, slotIndex, sequenceSize, swingCount, killed, summonHit,
                target, hurt, shot, blocked, used, false, 0.0F, null, null, null);
    }

    public EquipmentContext(Player player, StyleEntry entry, WeaponAttributes.Attack attack,
                       int slotIndex, int sequenceSize, int swingCount) {
        this(player, entry, attack, slotIndex, sequenceSize, swingCount, false, false, null,
                null, null, null, null);
    }

    public EquipmentContext(Player player, StyleEntry entry, WeaponAttributes.Attack attack,
                       int slotIndex, int sequenceSize, int swingCount, boolean killed) {
        this(player, entry, attack, slotIndex, sequenceSize, swingCount, killed, false, null,
                null, null, null, null);
    }

    public EquipmentContext(Player player, StyleEntry entry, WeaponAttributes.Attack attack,
                       int slotIndex, int sequenceSize, int swingCount,
                       boolean killed, boolean summonHit) {
        this(player, entry, attack, slotIndex, sequenceSize, swingCount, killed, summonHit, null,
                null, null, null, null);
    }

    public EquipmentContext withTarget(LivingEntity hit) {
        return hit == target
                ? this
                : new EquipmentContext(player, entry, attack, slotIndex, sequenceSize,
                        swingCount, killed, summonHit, hit, hurt, shot, blocked, used,
                        hitPass, dealtDamage, projectileHit, cast, summoned);
    }

    public static EquipmentContext hurtBy(Player player, HurtEvent hurt) {
        AttackHand hand = CombatState.swungHand(player);
        ComboState combo = hand == null ? null : hand.combo();
        int size = combo != null && combo.total() > 0 ? combo.total() : 0;
        int slot = size > 0 ? combo.current() - 1 : -1;
        LivingEntity attacker = hurt != null && hurt.attacker() instanceof LivingEntity living
                ? living
                : null;
        return new EquipmentContext(player, CombatState.swungEntry(player),
                hand == null ? null : hand.attack(), slot, size,
                EquipmentRuntime.swingCount(player), false, false, attacker, hurt, null, null, null);
    }

    public static EquipmentContext shotBy(Player player, ShotEvent shot) {
        return eventFor(player, null, shot, null, null);
    }

    public static EquipmentContext usedBy(Player player, UseEvent used) {
        return eventFor(player, null, null, null, used);
    }

    public static EquipmentContext blockedBy(Player player, BlockEvent blocked) {
        LivingEntity attacker = blocked != null && blocked.attacker() instanceof LivingEntity living
                ? living
                : null;
        return eventFor(player, attacker, null, blocked, null);
    }

    private static EquipmentContext eventFor(Player player, LivingEntity target,
                                             ShotEvent shot, BlockEvent blocked,
                                             UseEvent used) {
        AttackHand hand = CombatState.swungHand(player);
        ComboState combo = hand == null ? null : hand.combo();
        int size = combo != null && combo.total() > 0 ? combo.total() : 0;
        int slot = size > 0 ? combo.current() - 1 : -1;
        return new EquipmentContext(player, CombatState.swungEntry(player),
                hand == null ? null : hand.attack(), slot, size,
                EquipmentRuntime.swingCount(player), false, false, target, null, shot,
                blocked, used);
    }

    public static EquipmentContext projectileHitBy(Player player, ProjectileHitEvent hit) {
        return new EquipmentContext(player, CombatState.swungEntry(player), null, -1, 0,
                EquipmentRuntime.swingCount(player), false, false,
                hit == null ? null : hit.victim(), null, null, null, null,
                true, hit == null ? 0.0F : hit.amount(), hit, null, null);
    }

    public static EquipmentContext castBy(Player player, SpellCastEvent cast) {
        EquipmentContext base = eventFor(player, cast == null ? null : cast.target(), null, null,
                null);
        return new EquipmentContext(player, base.entry(), base.attack(), base.slotIndex(),
                base.sequenceSize(), base.swingCount(), false, false, base.target(), null, null,
                null, null, false, 0.0F, null, cast, null);
    }

    public static EquipmentContext summonedBy(Player player, SummonEvent event) {
        EquipmentContext base = eventFor(player, event == null ? null : event.other(), null, null,
                null);
        return new EquipmentContext(player, base.entry(), base.attack(), base.slotIndex(),
                base.sequenceSize(), base.swingCount(), false, false, base.target(), null, null,
                null, null, false, 0.0F, null, null, event);
    }

    public EquipmentContext withoutSummonHit() {
        return summonHit
                ? new EquipmentContext(player, entry, attack, slotIndex, sequenceSize,
                        swingCount, killed, false, target, hurt, shot, blocked, used,
                        hitPass, dealtDamage, projectileHit, cast, summoned)
                : this;
    }

    public EquipmentContext onHitPass(boolean hitPass, float dealt) {
        return new EquipmentContext(player, entry, attack, slotIndex, sequenceSize,
                swingCount, killed, summonHit, target, hurt, shot, blocked, used,
                hitPass, dealt, projectileHit, cast, summoned);
    }

    public boolean hasTarget() {
        return target != null;
    }

    public float reactionDamage() {
        if (dealtDamage > 0.0F) {
            return dealtDamage;
        }
        if (hurt != null) {
            return hurt.amount();
        }
        if (blocked != null) {
            return blocked.amount();
        }
        return player == null ? 0.0F : (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public boolean fromSwing() {
        return hurt == null && shot == null && blocked == null && used == null
                && projectileHit == null && cast == null && summoned == null;
    }

    public String hitbox() {
        return attack == null || attack.hitbox() == null ? null : attack.hitbox().name();
    }
}
