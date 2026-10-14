package net.pixeldreamstudios.combat_styles.skill;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.pixeldreamstudios.combat_styles.entity.SummonOwner;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SkillActions {

    public enum Target {
        SELF, OTHER, ALLIES, PETS, ENEMIES;

        static Target byId(String id) {
            for (Target target : values()) {
                if (target.name().toLowerCase(Locale.ROOT).equals(id)) {
                    return target;
                }
            }
            throw new IllegalArgumentException("Unknown effect target " + id);
        }
    }

    public record EffectAction(Holder<MobEffect> effect, int duration, int amplifier, Target target, double radius) {
    }

    public record CounterAction(String counter, int add, boolean reset) {
    }

    public record BurstAction(double radius, float base, Holder<Attribute> scaling, float perPoint,
                              ResourceKey<DamageType> damageType) {
    }

    public record TagAction(String tag, int ticks) {
    }

    public record Actions(List<EffectAction> effects, ResourceLocation function, List<CounterAction> counters,
                          float heal, float selfDamage, List<BurstAction> bursts, List<TagAction> tags) {

        public static final Actions NONE = new Actions(List.of(), null, List.of(), 0.0F, 0.0F, List.of(), List.of());

        public boolean isEmpty() {
            return effects.isEmpty() && function == null && counters.isEmpty() && heal == 0.0F
                    && selfDamage == 0.0F && bursts.isEmpty() && tags.isEmpty();
        }
    }

    private static final String KEY_EFFECTS = "effects";
    private static final String KEY_EFFECT = "effect";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_AMPLIFIER = "amplifier";
    private static final String KEY_TARGET = "target";
    private static final String KEY_RADIUS = "radius";
    private static final String KEY_FUNCTION = "function";
    private static final String KEY_COUNTERS = "counters";
    private static final String KEY_COUNTER = "counter";
    private static final String KEY_ADD = "add";
    private static final String KEY_RESET = "reset";
    private static final String KEY_HEAL = "heal";
    private static final String KEY_SELF_DAMAGE = "self_damage";
    private static final String KEY_BURSTS = "bursts";
    private static final String KEY_BASE = "base";
    private static final String KEY_SCALING = "scaling";
    private static final String KEY_PER_POINT = "per_point";
    private static final String KEY_DAMAGE_TYPE = "damage_type";
    private static final String KEY_TAGS = "tags";
    private static final String KEY_TAG = "tag";
    private static final String KEY_TICKS = "ticks";
    private static final String DEFAULT_DAMAGE_TYPE = "minecraft:player_attack";
    private static final String DEFAULT_TARGET = "self";
    private static final double DEFAULT_RADIUS = 8.0D;
    private static final int FUNCTION_PERMISSION = 2;

    private SkillActions() {
    }

    public static Actions parse(JsonObject object) {
        if (object == null) {
            return Actions.NONE;
        }
        List<EffectAction> effects = new ArrayList<>();
        if (object.has(KEY_EFFECTS)) {
            for (JsonElement entry : SkillConditions.array(object, KEY_EFFECTS)) {
                JsonObject effect = entry.getAsJsonObject();
                String target = SkillConditions.optionalString(effect, KEY_TARGET);
                effects.add(new EffectAction(
                        SkillConditions.effect(SkillConditions.string(effect, KEY_EFFECT)),
                        (int) SkillConditions.number(effect, KEY_DURATION),
                        effect.has(KEY_AMPLIFIER) ? (int) SkillConditions.number(effect, KEY_AMPLIFIER) : 0,
                        Target.byId(target == null ? DEFAULT_TARGET : target),
                        effect.has(KEY_RADIUS) ? SkillConditions.number(effect, KEY_RADIUS) : DEFAULT_RADIUS));
            }
        }
        String function = SkillConditions.optionalString(object, KEY_FUNCTION);
        List<CounterAction> counters = new ArrayList<>();
        if (object.has(KEY_COUNTERS)) {
            for (JsonElement entry : SkillConditions.array(object, KEY_COUNTERS)) {
                JsonObject counter = entry.getAsJsonObject();
                counters.add(new CounterAction(SkillConditions.string(counter, KEY_COUNTER),
                        counter.has(KEY_ADD) ? (int) SkillConditions.number(counter, KEY_ADD) : 0,
                        counter.has(KEY_RESET) && counter.get(KEY_RESET).getAsBoolean()));
            }
        }
        List<BurstAction> bursts = new ArrayList<>();
        if (object.has(KEY_BURSTS)) {
            for (JsonElement entry : SkillConditions.array(object, KEY_BURSTS)) {
                JsonObject burst = entry.getAsJsonObject();
                String scaling = SkillConditions.optionalString(burst, KEY_SCALING);
                String damageType = SkillConditions.optionalString(burst, KEY_DAMAGE_TYPE);
                bursts.add(new BurstAction(
                        burst.has(KEY_RADIUS) ? SkillConditions.number(burst, KEY_RADIUS) : DEFAULT_RADIUS,
                        burst.has(KEY_BASE) ? (float) SkillConditions.number(burst, KEY_BASE) : 0.0F,
                        scaling == null ? null : BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(scaling))
                                .orElseThrow(() -> new IllegalArgumentException("Unknown attribute " + scaling)),
                        burst.has(KEY_PER_POINT) ? (float) SkillConditions.number(burst, KEY_PER_POINT) : 0.0F,
                        ResourceKey.create(Registries.DAMAGE_TYPE,
                                ResourceLocation.parse(damageType == null ? DEFAULT_DAMAGE_TYPE : damageType))));
            }
        }
        List<TagAction> tags = new ArrayList<>();
        if (object.has(KEY_TAGS)) {
            for (JsonElement entry : SkillConditions.array(object, KEY_TAGS)) {
                JsonObject tag = entry.getAsJsonObject();
                tags.add(new TagAction(SkillConditions.string(tag, KEY_TAG), (int) SkillConditions.number(tag, KEY_TICKS)));
            }
        }
        return new Actions(effects, function == null ? null : ResourceLocation.parse(function), counters,
                object.has(KEY_HEAL) ? (float) SkillConditions.number(object, KEY_HEAL) : 0.0F,
                object.has(KEY_SELF_DAMAGE) ? (float) SkillConditions.number(object, KEY_SELF_DAMAGE) : 0.0F,
                bursts, tags);
    }

    public static void run(Actions actions, ServerPlayer player, LivingEntity other) {
        if (actions == null || actions.isEmpty() || player == null) {
            return;
        }
        for (EffectAction effect : actions.effects()) {
            for (LivingEntity target : targets(effect, player, other)) {
                target.addEffect(new MobEffectInstance(effect.effect(), effect.duration(), effect.amplifier()), player);
            }
        }
        for (CounterAction counter : actions.counters()) {
            if (counter.reset()) {
                SkillCounters.reset(player, counter.counter());
            } else {
                SkillCounters.add(player, counter.counter(), counter.add());
            }
        }
        if (actions.heal() > 0.0F) {
            player.heal(player.getMaxHealth() * actions.heal());
        }
        if (actions.selfDamage() > 0.0F) {
            player.hurt(player.damageSources().magic(), player.getMaxHealth() * actions.selfDamage());
        }
        for (BurstAction burst : actions.bursts()) {
            burst(player, burst);
        }
        for (TagAction tag : actions.tags()) {
            SkillTags.add(player, tag.tag(), tag.ticks());
        }
        if (actions.function() != null) {
            runFunction(player, actions.function());
        }
    }

    private static void burst(ServerPlayer player, BurstAction burst) {
        Holder<DamageType> type = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(burst.damageType()).orElse(null);
        if (type == null) {
            return;
        }
        float amount = burst.base();
        if (burst.scaling() != null && player.getAttribute(burst.scaling()) != null) {
            amount += (float) player.getAttributeValue(burst.scaling()) * burst.perPoint();
        }
        if (amount <= 0.0F) {
            return;
        }
        DamageSource source = new DamageSource(type, player);
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(burst.radius()),
                entity -> entity instanceof Enemy && entity.isAlive())) {
            target.hurt(source, amount);
        }
    }

    private static List<LivingEntity> targets(EffectAction effect, ServerPlayer player, LivingEntity other) {
        return switch (effect.target()) {
            case SELF -> List.of(player);
            case OTHER -> other == null || !other.isAlive() ? List.of() : List.of(other);
            case ALLIES -> new ArrayList<>(player.level().getEntitiesOfClass(Player.class,
                    player.getBoundingBox().inflate(effect.radius()),
                    ally -> ally.isAlive() && !ally.isSpectator()));
            case PETS -> new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(effect.radius()),
                    pet -> isPet(pet, player)));
            case ENEMIES -> player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(effect.radius()),
                    entity -> entity instanceof Enemy && entity.isAlive());
        };
    }

    public static boolean isPet(LivingEntity entity, Player player) {
        if (entity == null || !entity.isAlive() || entity == player) {
            return false;
        }
        return entity instanceof TamableAnimal tamed && tamed.isOwnedBy(player) || SummonOwner.ownedBy(entity, player);
    }

    private static void runFunction(ServerPlayer player, ResourceLocation id) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        CommandSourceStack source = player.createCommandSourceStack().withSuppressedOutput()
                .withPermission(FUNCTION_PERMISSION);
        server.getFunctions().get(id).ifPresent(function -> server.getFunctions().execute(function, source));
    }
}
