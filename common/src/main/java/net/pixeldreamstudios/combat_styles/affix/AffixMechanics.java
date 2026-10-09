package net.pixeldreamstudios.combat_styles.affix;
import net.pixeldreamstudios.combat_styles.entity.AbilityHosts;

import net.pixeldreamstudios.combat_styles.damage.CritRolls;
import net.pixeldreamstudios.combat_styles.damage.DamageSpec;
import net.pixeldreamstudios.combat_styles.damage.ScaledNumber;
import net.pixeldreamstudios.combat_styles.damage.DamageDealer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.debug.CombatDebug;
import net.pixeldreamstudios.combat_styles.equipment.ClientSwing;
import net.pixeldreamstudios.combat_styles.entity.AbilityRegistry;
import net.pixeldreamstudios.combat_styles.entity.SummonOrders;
import net.pixeldreamstudios.combat_styles.entity.SummonLifecycle;
import net.pixeldreamstudios.combat_styles.entity.SummonSpawner;
import net.pixeldreamstudios.combat_styles.equipment.CombatState;
import net.pixeldreamstudios.combat_styles.equipment.DodgeState;
import net.pixeldreamstudios.combat_styles.equipment.IFrameState;
import net.pixeldreamstudios.combat_styles.equipment.DashGlide;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AffixMechanics {

    private static final String KEY_SEPARATOR = "|";
    private static final int BLINK_SEARCH_DEPTH = 4;
    private static final double BLINK_WALL_MARGIN = 0.35D;
    private static final int COMMAND_PERMISSION = 2;

    public static final double MAX_DASH_FORWARD = 128.0D;
    public static final double MAX_DASH_UPWARD = 64.0D;
    public static final double MAX_DASH_SPEED = 4.0D;
    private static final int DASH_SPREAD_TICKS = 6;
    private static final double PLAYER_GRAVITY = 0.08D;

    private static final int SECOND_IN_TICKS = 20;
    private static final float MINIMUM_DAMAGE = 0.01F;

    private record Stacks(int count, long lastTick) {
    }

    private static final int STALE_MOMENTUM_TICKS = 600;
    private static final float MAX_SHIELD = 20.0F;

    private static final Map<UUID, Stacks> MOMENTUM = new ConcurrentHashMap<>();
    private static final Map<String, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static final List<Expiry> EXPIRING = new CopyOnWriteArrayList<>();

    private record Expiry(LivingEntity holder, Holder<Attribute> attribute,
                          ResourceLocation modifierId, long expiresAt,
                          double perStack, AttributeModifier.Operation operation,
                          int stacks, int decayTicks) {
    }

    private record ShieldExpiry(Player player, long expiresAt, float granted, float priorAbsorption) {
    }

    private static final Set<ShieldExpiry> SHIELDS = ConcurrentHashMap.newKeySet();

    private AffixMechanics() {
    }

    public static boolean slotRuleMet(AffixEffects.SlotRule rule, SwingContext context) {
        if (rule == null || rule.isEmpty()) {
            return true;
        }
        if (!context.isKnown()) {
            return false;
        }

        if (!rule.slots().isEmpty()) {
            boolean matched = false;
            for (int slot : rule.slots()) {
                boolean hit = slot < 0
                        ? context.slotCount() + slot == context.livePosition()
                        : slot == context.slotIndex();
                if (hit) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        if (rule.first().isPresent() && context.isFirst() != rule.first().get()) {
            return false;
        }
        if (rule.last().isPresent() && context.isLast() != rule.last().get()) {
            return false;
        }
        if (rule.afterStyle().isPresent()
                && !StyleRegistry.sameStyle(rule.afterStyle().get(), context.previousStyleId())) {
            return false;
        }
        if (rule.afterCategory().isPresent()
                && !rule.afterCategory().get().equals(context.previousCategory())) {
            return false;
        }
        return requiresMet(rule.requires(), context);
    }

    public static boolean requiresMet(AffixEffects.Requires requires, SwingContext context) {
        if (requires == null || requires.isEmpty()) {
            return true;
        }

        List<String> preceding = context.precedingStyleIds();
        if (preceding.size() < requires.minAttacks()) {
            return false;
        }

        if (!requires.styles().isEmpty()
                && preceding.stream().noneMatch(styleId -> requires.styles().stream()
                        .anyMatch(required -> StyleRegistry.sameStyle(required, styleId)))) {
            return false;
        }

        if (!requires.categories().isEmpty()) {
            List<String> categories = context.precedingCategories();
            if (categories.isEmpty()) {
                return false;
            }
            for (String category : categories) {
                if (!requires.categories().contains(category)) {
                    return false;
                }
            }
        }

        if (!requires.weaponCategories().isEmpty()
                && (context.weaponCategory() == null
                        || !requires.weaponCategories().contains(context.weaponCategory()))) {
            return false;
        }

        if (context.hitsLanded() < requires.hitsLanded()) {
            return false;
        }

        if (requires.sameTarget().isPresent()
                && requires.sameTarget().get() != context.sameTarget()) {
            return false;
        }

        if (requires.sameStyle().isPresent() && !preceding.isEmpty()) {
            boolean allSame = preceding.stream().distinct().count() == 1L;
            if (allSame != requires.sameStyle().get()) {
                return false;
            }
        }
        return true;
    }

    private static boolean critLanded(String group, Player player) {
        if (player.level().isClientSide()) {
            return false;
        }
        ScaledNumber.Crit crit = new ScaledNumber.Crit(Optional.of(group), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty()).effective();
        float chance = crit.chanceFor(player);
        return CritRolls.outcome(crit, () -> player.getRandom().nextFloat() < chance);
    }

    public static boolean conditionMet(AffixEffects.Condition condition, Player player,
                                       LivingEntity target) {
        return conditionMet(condition, player, target, SwingContext.UNKNOWN);
    }

    public static boolean conditionMet(AffixEffects.Condition condition, Player player,
                                       LivingEntity target, SwingContext context) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        if (!slotRuleMet(condition.slot(), context)) {
            return false;
        }
        if (!condition.damageType().isEmpty() && !DamageTypeMatch.matches(
                condition.damageType(), player.damageSources().playerAttack(player))) {
            return false;
        }

        float selfHealth = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        if (condition.selfHealthBelow().isPresent() && selfHealth >= condition.selfHealthBelow().get()) {
            return false;
        }
        if (condition.selfHealthAbove().isPresent() && selfHealth <= condition.selfHealthAbove().get()) {
            return false;
        }
        if (condition.targetHealthBelow().isPresent()) {
            if (target == null) {
                return false;
            }
            float targetHealth = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
            if (targetHealth >= condition.targetHealthBelow().get()) {
                return false;
            }
        }
        if (condition.targetHealthAbove().isPresent()) {
            if (target == null) {
                return false;
            }
            float targetHealth = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
            if (targetHealth <= condition.targetHealthAbove().get()) {
                return false;
            }
        }
        if (!condition.state().matches(player, target)) {
            return false;
        }
        if (!condition.targetEffects().isEmpty() && !afflicted(target, condition.targetEffects())) {
            return false;
        }
        if (condition.targetBurning().isPresent()
                && (target == null || target.isOnFire() != condition.targetBurning().get())) {
            return false;
        }
        if (!DamageTypeMatch.matches(condition.takenDamageType(),
                CombatState.lastDamageType(player))) {
            return false;
        }
        if (condition.crit().isPresent() && !critLanded(condition.crit().get(), player)) {
            return false;
        }
        if (condition.critMissed().isPresent() && critLanded(condition.critMissed().get(), player)) {
            return false;
        }
        return condition.onMiss().isEmpty()
                || condition.onMiss().get() == CombatState.lastSwingMissed(player);
    }

    private static boolean afflicted(LivingEntity target, List<String> effects) {
        if (target == null) {
            return false;
        }
        for (String raw : effects) {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id != null && BuiltInRegistries.MOB_EFFECT.getHolder(id)
                    .map(target::hasEffect).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    public static boolean selfConditionMet(AffixEffects.Condition condition, Player player) {
        return selfConditionMet(condition, player, null);
    }

    public static boolean selfConditionMet(AffixEffects.Condition condition, Player player,
                                           DamageSource incoming) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        if (!condition.damageType().isEmpty()
                && !DamageTypeMatch.matches(condition.damageType(), incoming)) {
            return false;
        }
        float selfHealth = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
        if (condition.selfHealthBelow().isPresent()
                && selfHealth >= condition.selfHealthBelow().get()) {
            return false;
        }
        if (condition.selfHealthAbove().isPresent()
                && selfHealth <= condition.selfHealthAbove().get()) {
            return false;
        }
        if (!condition.state().withoutTarget().matches(player, null)) {
            return false;
        }
        return DamageTypeMatch.matches(condition.takenDamageType(),
                CombatState.lastDamageType(player));
    }

    private record SwingTally(long swingId, int struck) {
    }

    private static final Map<String, SwingTally> SWING_TARGETS = new ConcurrentHashMap<>();

    public static boolean targetLimitReached(Player player, StyleAffix affix,
                                             AffixEffects.Mechanics mechanics) {
        if (mechanics == null || !mechanics.capsTargets()) {
            return false;
        }
        long swing = CombatState.currentSwingId(player);
        String key = player.getUUID() + KEY_SEPARATOR + affix.serializedId();
        SwingTally tally = SWING_TARGETS.get(key);
        int struck = tally != null && tally.swingId() == swing ? tally.struck() : 0;
        if (struck >= mechanics.maxTargetsPerSwing()) {
            return true;
        }
        SWING_TARGETS.put(key, new SwingTally(swing, struck + 1));
        return false;
    }

    public static boolean onCooldown(Player player, StyleAffix affix,
                                     AffixEffects.Cooldown cooldown, int slot) {
        if (cooldown == null || cooldown.ticks() <= 0) {
            return false;
        }
        Long until = COOLDOWNS.get(cooldownKey(player, affix, cooldown, slot));
        return until != null && player.level().getGameTime() < until;
    }

    public static void startCooldown(Player player, StyleAffix affix,
                                     AffixEffects.Cooldown cooldown, int slot) {
        if (cooldown == null || cooldown.ticks() <= 0) {
            return;
        }
        COOLDOWNS.put(cooldownKey(player, affix, cooldown, slot),
                player.level().getGameTime() + cooldown.ticks());
    }

    private static String cooldownKey(Player player, StyleAffix affix,
                                      AffixEffects.Cooldown cooldown, int slot) {
        String base = player.getUUID() + KEY_SEPARATOR + affix.serializedId();
        return cooldown.shared() ? base : base + KEY_SEPARATOR + slot;
    }

    public static boolean canAffordCost(Player player, AffixEffects.Resource cost) {
        if (cost == null || player.getAbilities().instabuild) {
            return true;
        }
        if (!cost.cancelIfShort()) {
            return true;
        }
        boolean shortOnHealth = cost.health() > 0.0F && player.getHealth() <= cost.health();
        boolean shortOnLevels = cost.experienceLevels() > 0
                && player.experienceLevel < cost.experienceLevels();
        return !shortOnHealth && !shortOnLevels;
    }

    public static boolean payCost(Player player, AffixEffects.Resource cost) {
        if (cost == null) {
            return true;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }

        if (!canAffordCost(player, cost)) {
            return false;
        }

        if (cost.health() > 0.0F) {
            player.setHealth(Math.max(1.0F, player.getHealth() - cost.health()));
        }
        if (cost.hunger() > 0) {
            player.getFoodData().setFoodLevel(
                    Math.max(0, player.getFoodData().getFoodLevel() - cost.hunger()));
        }
        if (cost.experienceLevels() > 0) {
            player.giveExperienceLevels(-cost.experienceLevels());
        }
        return true;
    }

    public static void applyAttributes(Player player, LivingEntity target, StyleAffix affix,
                                       List<AffixEffects.AttributeBonus> bonuses) {
        for (AffixEffects.AttributeBonus bonus : bonuses) {
            LivingEntity holder = bonus.onTarget() ? target : player;
            if (holder == null) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(bonus.attribute());
            if (id == null) {
                if (CombatDebug.enabled(player)) {
                    CombatDebug.skipped(player, "attribute", "unreadable id " + bonus.attribute());
                }
                continue;
            }
            Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE
                    .getHolder(ResourceKey.create(Registries.ATTRIBUTE, id))
                    .orElse(null);
            if (attribute == null) {
                CombatStyles.LOGGER.warn("Affix {} references unknown attribute {}",
                        affix.serializedId(), id);
                if (CombatDebug.enabled(player)) {
                    CombatDebug.skipped(player, "attribute", id + " is not registered");
                }
                continue;
            }
            AttributeInstance instance = holder.getAttribute(attribute);
            if (instance == null) {
                continue;
            }

            ResourceLocation modifierId = CombatStyles.id(
                    "affix/" + affix.serializedId().replace(':', '_') + "/" + id.getPath());
            AttributeModifier existing = instance.getModifier(modifierId);
            int stacks = 1;
            if (existing != null) {
                stacks = bonus.amount() == 0.0D
                        ? 1
                        : Math.min(bonus.maxStacks(),
                                (int) Math.round(existing.amount() / bonus.amount()) + 1);
                instance.removeModifier(modifierId);
            }

            AttributeModifier.Operation operation = operationOf(bonus.operation());
            instance.addTransientModifier(new AttributeModifier(modifierId,
                    bonus.amount() * stacks, operation));
            if (CombatDebug.enabled(player)) {
                CombatDebug.fired(player, "attribute", id.getPath() + " "
                        + (bonus.amount() > 0 ? "+" : "") + bonus.amount()
                        + " x" + stacks + " for " + bonus.duration() + "t");
            }
            EXPIRING.removeIf(expiry -> expiry.holder() == holder
                    && expiry.modifierId().equals(modifierId));
            EXPIRING.add(new Expiry(holder, attribute, modifierId,
                    holder.level().getGameTime() + bonus.duration(),
                    bonus.amount(), operation, stacks, bonus.duration()));
        }
    }

    public static AttributeModifier.Operation operationOf(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "multiply_base", "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "multiply_total", "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }

    public static void forget(UUID playerId) {
        MOMENTUM.remove(playerId);
        HELD.remove(playerId);
        SWING_TARGETS.keySet().removeIf(key -> key.startsWith(playerId + KEY_SEPARATOR));
    }

    public static void tickExpiries(long gameTime) {
        COOLDOWNS.values().removeIf(until -> gameTime >= until);
        MOMENTUM.values().removeIf(stacks -> gameTime - stacks.lastTick() > STALE_MOMENTUM_TICKS);
        tickAbsorption();
        tickShields(gameTime);

        List<Expiry> decayed = new ArrayList<>();
        EXPIRING.removeIf(expiry -> {
            if (gameTime < expiry.expiresAt()) {
                return false;
            }
            AttributeInstance instance = expiry.holder().getAttribute(expiry.attribute());
            if (instance != null) {
                instance.removeModifier(expiry.modifierId());
                int remaining = expiry.stacks() - 1;
                if (remaining > 0 && expiry.perStack() != 0.0D) {
                    instance.addTransientModifier(new AttributeModifier(expiry.modifierId(),
                            expiry.perStack() * remaining, expiry.operation()));
                    decayed.add(new Expiry(expiry.holder(), expiry.attribute(),
                            expiry.modifierId(), gameTime + expiry.decayTicks(),
                            expiry.perStack(), expiry.operation(), remaining,
                            expiry.decayTicks()));
                }
            }
            return true;
        });
        EXPIRING.addAll(decayed);

        SummonSpawner.tickDespawns(gameTime);
        SummonLifecycle.tick(gameTime);
        AbilityHosts.tick(gameTime);
    }

    private static Vec3 flatten(Vec3 aim) {
        double length = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
        if (length <= 1.0E-4D) {
            return null;
        }
        return new Vec3(aim.x / length, 0.0D, aim.z / length);
    }

    private static double liftVelocity(double blocks) {
        double magnitude = Math.sqrt(2.0D * PLAYER_GRAVITY * Math.abs(blocks));
        return Math.clamp(Math.copySign(magnitude, blocks),
                -MAX_DASH_SPEED, MAX_DASH_SPEED);
    }

    public static boolean applyDash(Player player, AffixEffects.Dash dash, LivingEntity target) {
        if (!EffectChance.rolls(player, dash.chance())) {
            return false;
        }
        Vec3 direction;
        if (dash.towardTarget() && target != null) {
            direction = flatten(target.getBoundingBox().getCenter()
                    .subtract(player.position()));
        } else {
            direction = ClientSwing.declaredAim(player);
            if (direction == null) {
                direction = flatten(player.getLookAngle());
            }
        }
        if (direction == null) {
            return false;
        }

        double forward = Math.clamp(dash.forward(), -MAX_DASH_FORWARD, MAX_DASH_FORWARD);
        double upward = Math.clamp(dash.upward(), -MAX_DASH_UPWARD, MAX_DASH_UPWARD);
        double perTick = Math.clamp(forward / DASH_SPREAD_TICKS,
                -MAX_DASH_SPEED, MAX_DASH_SPEED);
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(direction.x * perTick,
                upward != 0.0D ? liftVelocity(upward) : velocity.y,
                direction.z * perTick);
        DashGlide.begin(player, direction, perTick,
                (int) Math.ceil(Math.abs(forward) / Math.max(1.0E-3D, Math.abs(perTick))));
        player.hurtMarked = true;
        IFrameState.grant(player, dash.iframes());
        return dash.towardTarget() && target != null && target.isAlive();
    }

    public static float executeBonus(AffixEffects.Execute execute, LivingEntity target, float dealt) {
        float ratio = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
        if (ratio > execute.healthThreshold()) {
            return 0.0F;
        }
        if (execute.instantKill()) {
            return target.getHealth();
        }
        return dealt * execute.bonusRatio();
    }

    public static List<LivingEntity> chainTargets(Player player, LivingEntity origin,
                                                  AffixEffects.Chain chain) {
        List<LivingEntity> found = new ArrayList<>();
        Set<LivingEntity> seen = new HashSet<>();
        seen.add(origin);

        LivingEntity current = origin;
        for (int i = 0; i < chain.maxTargets(); i++) {
            AABB area = current.getBoundingBox().inflate(chain.range());
            LivingEntity nearest = null;
            double nearestDistance = Double.MAX_VALUE;

            for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (candidate == player || seen.contains(candidate) || !candidate.isAlive()
                        || !AffixTargets.canHit(player, candidate)) {
                    continue;
                }
                double distance = candidate.distanceToSqr(current);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = candidate;
                }
            }
            if (nearest == null) {
                break;
            }
            found.add(nearest);
            seen.add(nearest);
            current = nearest;
        }
        return found;
    }

    public static float currentMomentum(Player player, AffixEffects.Momentum momentum) {
        Stacks stacks = MOMENTUM.get(player.getUUID());
        if (stacks == null
                || player.level().getGameTime() - stacks.lastTick() > momentum.decayTicks()) {
            return 1.0F;
        }
        return 1.0F + momentum.perHitRatio() * (stacks.count() - 1);
    }

    public static float momentumMultiplier(Player player, AffixEffects.Momentum momentum) {
        long now = player.level().getGameTime();
        Stacks previous = MOMENTUM.get(player.getUUID());

        int count = 1;
        if (previous != null && now - previous.lastTick() <= momentum.decayTicks()) {
            count = Math.min(momentum.maxStacks(), previous.count() + 1);
        }
        MOMENTUM.put(player.getUUID(), new Stacks(count, now));
        return 1.0F + momentum.perHitRatio() * (count - 1);
    }

    public static void applyHeal(Player player, AffixEffects.Heal heal, float dealt) {
        if (!EffectChance.rolls(player, heal.chance())) {
            return;
        }
        float amount = heal.flat() + dealt * heal.ratio();
        if (amount < MINIMUM_DAMAGE) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.skipped(player, "heal",
                        "amount " + amount + " below the minimum");
            }
            return;
        }
        if (heal.overheal()) {
            grantAbsorption(player, amount, DEFAULT_ABSORPTION_TICKS);
        } else {
            player.heal(amount);
            if (CombatDebug.enabled(player)) {
                CombatDebug.fired(player, "heal", "healed " + amount);
            }
        }
    }

    public static void applyStatus(LivingEntity target, Holder<MobEffect> holder, int duration,
                                   int amplifier, boolean stacks,
                                   Optional<Integer> maxAmplifier) {
        if (target == null || holder == null) {
            return;
        }
        int level = Math.max(0, amplifier);
        if (stacks) {
            MobEffectInstance active = target.getEffect(holder);
            if (active != null) {
                level += active.getAmplifier() + 1;
            }
        }
        if (maxAmplifier.isPresent()) {
            level = Math.min(level, Math.max(0, maxAmplifier.get()));
        }
        target.addEffect(new MobEffectInstance(holder, duration, level));
    }

    public static void applyDisarm(Player player, LivingEntity target, AffixEffects.Disarm disarm) {
        if (!EffectChance.rolls(player, disarm.chance())) {
            return;
        }
        ItemStack held = target.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!held.isEmpty() && disarm.durabilityDamage() > 0 && held.isDamageableItem()) {
            held.hurtAndBreak(disarm.durabilityDamage(), target, EquipmentSlot.MAINHAND);
        }
        if (disarm.slownessSeconds() > 0.0F) {
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
                    (int) (disarm.slownessSeconds() * SECOND_IN_TICKS), 1));
        }
    }

    private static final Set<String> WARNED = new HashSet<>();

    public static void applyOrder(ServerLevel level, Player player, LivingEntity target,
                                  AffixEffects.Order order, float dealtDamage) {
        order.quicken().ifPresent(quicken -> {
            if (EffectChance.rolls(player, quicken.chance())) {
                AbilityHosts.quicken(player, quicken.ratio());
            }
        });
        if (!order.ordersAbility() || !EffectChance.rolls(player, order.chance())) {
            return;
        }
        if (!AbilityRegistry.has(order.ability())) {
            if (WARNED.add(order.ability())) {
                CombatStyles.LOGGER.warn("An affix orders unknown ability {}", order.ability());
            }
            return;
        }
        float damage = order.damageRatio() <= 0.0F ? 0.0F : dealtDamage * order.damageRatio();
        LivingEntity aimed = order.aim()
                .map(aim -> aim.resolve(player, player, target))
                .orElse(target);

        SummonOrders.command(player, aimed, order.ability(), damage, order.targets(),
                order.tuning().orElse(null));
    }

    public static void runCommands(ServerLevel level, Player player,
                                   List<AffixEffects.Command> commands) {
        if (commands.isEmpty() || level.getServer() == null) {
            return;
        }
        CommandSourceStack base = player.createCommandSourceStack().withSuppressedOutput();
        for (AffixEffects.Command command : commands) {
            if (command.command().isBlank()) {
                continue;
            }
            CommandSourceStack source = command.asServer()
                    ? base.withPermission(COMMAND_PERMISSION)
                    : base.withPermission(0);
            try {
                level.getServer().getCommands().performPrefixedCommand(source, command.command());
            } catch (Throwable throwable) {
                CombatStyles.LOGGER.error("Affix command failed: {}", command.command(), throwable);
            }
        }
    }

    public static void applyVault(Player player, LivingEntity origin, AffixEffects.Vault vault) {
        if (!EffectChance.rolls(player, vault.chance())) {
            return;
        }
        AABB area = origin.getBoundingBox().inflate(vault.radius());
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity == player || !entity.isAlive()) {
                continue;
            }
            Vec3 away = entity.position().subtract(origin.position()).normalize();
            entity.push(away.x * vault.strength(),
                    vault.upward() ? vault.strength() : 0.0D,
                    away.z * vault.strength());
            entity.hurtMarked = true;
        }
    }

    public static void applyBlink(Player player, LivingEntity target, AffixEffects.Blink blink) {
        boolean hasTarget = target != null && target.isAlive();
        Vec3 destination;
        if (!hasTarget) {
            Vec3 facing = ClientSwing.declaredAim(player);
            if (facing == null) {
                facing = flatten(player.getLookAngle());
            }
            if (facing == null || facing.lengthSqr() < 1.0E-4D) {
                return;
            }
            destination = player.position().add(facing.scale(blink.distance()));
        } else if (blink.distance() < 0.0F) {
            Vec3 facing = ClientSwing.declaredAim(player);
            if (facing == null) {
                facing = flatten(player.getLookAngle());
            }
            if (facing == null || facing.lengthSqr() < 1.0E-4D) {
                return;
            }
            destination = player.position().add(facing.scale(blink.distance()));
        } else if (blink.behindTarget()) {
            Vec3 facing = Vec3.directionFromRotation(0.0F, target.getYRot()).normalize();
            destination = target.position().subtract(facing.scale(blink.distance()));
        } else {
            Vec3 toward = target.position().subtract(player.position()).normalize();
            destination = player.position().add(toward.scale(blink.distance()));
        }
        Vec3 safe = safeDestination(player, visibleDestination(player, destination));
        if (safe == null) {
            return;
        }
        player.teleportTo(safe.x, safe.y, safe.z);
        player.resetFallDistance();
        IFrameState.grant(player, blink.iframes());
    }

    private static Vec3 visibleDestination(Player player, Vec3 wanted) {
        Vec3 from = player.getEyePosition();
        Vec3 to = wanted.add(0.0D, player.getEyeHeight(), 0.0D);
        BlockHitResult hit = player.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return wanted;
        }
        Vec3 travel = to.subtract(from);
        double reached = hit.getLocation().distanceTo(from);
        double length = travel.length();
        if (length < 1.0E-4D) {
            return wanted;
        }
        double allowed = Math.max(0.0D, reached - BLINK_WALL_MARGIN);
        return player.position().add(travel.scale(allowed / length));
    }

    private static Vec3 safeDestination(Player player, Vec3 wanted) {
        Level level = player.level();
        AABB size = player.getBoundingBox();
        for (int drop = 0; drop <= BLINK_SEARCH_DEPTH; drop++) {
            Vec3 candidate = wanted.subtract(0.0D, drop, 0.0D);
            if (candidate.y < level.getMinBuildHeight()) {
                break;
            }
            AABB box = size.move(candidate.subtract(player.position()));
            if (level.noCollision(player, box)) {
                return candidate;
            }
        }
        for (int rise = 1; rise <= BLINK_SEARCH_DEPTH; rise++) {
            Vec3 candidate = wanted.add(0.0D, rise, 0.0D);
            if (candidate.y > level.getMaxBuildHeight()) {
                break;
            }
            AABB box = size.move(candidate.subtract(player.position()));
            if (level.noCollision(player, box)) {
                return candidate;
            }
        }
        return null;
    }

    public static void applyShield(Player player, AffixEffects.Shield shield) {
        float granted = Math.max(0.0F, shield.amount());
        if (granted <= 0.0F) {
            CombatDebug.skipped(player, "shield", "amount is zero");
            DodgeState.grant(player, shield.dodgeChance(), shield.dodgeDuration());
            return;
        }
        float prior = player.getAbsorptionAmount();
        float capped = Math.min(granted, MAX_SHIELD - prior);
        if (capped <= 0.0F) {
            if (CombatDebug.enabled(player)) {
                CombatDebug.skipped(player, "shield",
                        "already at the " + MAX_SHIELD + " absorption cap");
            }
            DodgeState.grant(player, shield.dodgeChance(), shield.dodgeDuration());
            return;
        }
        grantAbsorption(player, capped, shield.duration());
        DodgeState.grant(player, shield.dodgeChance(), shield.dodgeDuration());
        if (shield.duration() > 0) {
            SHIELDS.add(new ShieldExpiry(player,
                    player.level().getGameTime() + shield.duration(),
                    capped, prior));
        }
    }

    private static final float ABSORPTION_PER_LEVEL = 4.0F;
    private static final int DEFAULT_ABSORPTION_TICKS = 200;

    private static void grantAbsorption(Player player, float amount, int durationTicks) {
        if (player == null || player.level().isClientSide() || amount <= 0.0F) {
            return;
        }
        int level = Math.max(1, (int) Math.ceil(amount / ABSORPTION_PER_LEVEL));
        int ticks = durationTicks > 0 ? durationTicks : DEFAULT_ABSORPTION_TICKS;
        MobEffectInstance existing = player.getEffect(MobEffects.ABSORPTION);
        if (existing != null && existing.getAmplifier() >= level - 1
                && existing.getDuration() >= ticks) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, level - 1,
                false, false, true));
        if (CombatDebug.enabled(player)) {
            CombatDebug.fired(player, "absorption",
                    amount + " (level " + level + ") for " + ticks + "t");
        }
    }

    private static void tickAbsorption() {
        HELD.forEach((id, held) -> {
            for (ShieldExpiry expiry : SHIELDS) {
                Player holder = expiry.player();
                if (holder == null || !holder.isAlive()
                        || !id.equals(holder.getUUID())) {
                    continue;
                }
                if (holder.getAbsorptionAmount() < held) {
                    holder.setAbsorptionAmount(held);
                    pushHealth(holder);
                }
                return;
            }
        });
    }

    private static final Map<UUID, Float> HELD = new ConcurrentHashMap<>();

    private static void pushHealth(Player player) {
        if (player instanceof ServerPlayer served) {
            served.connection.send(new ClientboundSetHealthPacket(
                    served.getHealth(),
                    served.getFoodData().getFoodLevel(),
                    served.getFoodData().getSaturationLevel()));
        }
    }

    private static void tickShields(long gameTime) {
        SHIELDS.removeIf(expiry -> {
            if (gameTime < expiry.expiresAt()) {
                return false;
            }
            Player holder = expiry.player();
            if (holder != null && holder.isAlive()) {
                float current = holder.getAbsorptionAmount();
                float reclaim = Math.min(expiry.granted(), current);
                holder.setAbsorptionAmount(Math.max(0.0F, current - reclaim));
                pushHealth(holder);
                HELD.computeIfPresent(holder.getUUID(),
                        (id, held) -> held - reclaim <= 0.0F ? null : held - reclaim);
            }
            return true;
        });
    }

    public static void applyCleave(Player player, LivingEntity origin, float damage,
                                   AffixEffects.Cleave cleave) {
        applyCleave(player, origin, damage, cleave, null);
    }

    public static void applyCleave(Player player, LivingEntity origin, float damage,
                                   AffixEffects.Cleave cleave, DamageSource source) {
        if (origin == null || cleave.maxTargets() <= 0) {
            return;
        }
        DamageSource dealt = source == null
                ? player.damageSources().playerAttack(player)
                : source;
        AABB area = origin.getBoundingBox().inflate(cleave.radius());
        int struck = 0;
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity == player || entity == origin || !entity.isAlive()
                    || !AffixTargets.canHit(player, entity)) {
                continue;
            }
            DamageDealer.dealSpec(player, entity, cleave.damageSpec().orElse(null),
                    DamageSpec.resolve(cleave.damageSpec(), player, damage, cleave.ratio()),
                    dealt);
            if (++struck >= cleave.maxTargets()) {
                return;
            }
        }
    }
}
