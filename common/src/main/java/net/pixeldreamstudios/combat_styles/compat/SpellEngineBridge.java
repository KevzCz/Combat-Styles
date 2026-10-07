package net.pixeldreamstudios.combat_styles.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.platform.Platform;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.affix.CastSpell;
import net.pixeldreamstudios.combat_styles.entity.SummonCastTarget;
import net.pixeldreamstudios.combat_styles.equipment.CastPhase;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentActivation;
import net.pixeldreamstudios.combat_styles.equipment.SpellCastEvent;
import net.pixeldreamstudios.combat_styles.equipment.SpellLaunchTuning;
import net.pixeldreamstudios.combat_styles.equipment.SpellLockState;
import net.pixeldreamstudios.combat_styles.equipment.SpellLocks;
import net.pixeldreamstudios.combat_styles.equipment.SpellTuning;
import net.pixeldreamstudios.combat_styles.mixin.SpellCloudAccessor;

import net.spell_engine.api.render.CustomModels;
import net.spell_engine.api.render.LightEmission;
import net.spell_engine.api.spell.fx.ModelEffect;
import net.spell_engine.client.render.SpellModelHelper;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.container.SpellContainerHelper;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.entity.SpellCloud;
import net.spell_engine.entity.SpellProjectile;
import net.spell_engine.entity.SummonedEntity;
import net.spell_engine.api.spell.summon.SpellSummoned;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.SpellModifiers;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.delivery.ProjectileLauncher;
import net.spell_engine.internals.delivery.SpellDelivery;
import net.spell_engine.internals.target.SpellIntents;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.utils.VectorHelper;
import net.spell_power.api.SpellPower;
import net.spell_engine.internals.casting.SpellCaster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

final class SpellEngineBridge {

    private static final float CERTAIN_CHANCE = 1.0F;
    private static final float QUARTER_TURN = 90.0F;
    private static final int KEEP_LAUNCH_MOD = -1;
    private static final String GRANT_SOURCE = "combat_styles:granted";
    private static final String NO_PARAMETER = "";
    private static final String KEY_DISABLED_BY = "message.combat_styles.spell_disabled_by";
    private static final Set<Spell.Modifier> TUNED = Collections.newSetFromMap(new WeakHashMap<>());

    private static Boolean loaded;

    private SpellEngineBridge() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = Platform.isModLoaded(SpellEngineCompat.MOD_ID);
        }
        return loaded;
    }

    public static Collection<ResourceLocation> spellIds(Level level) {
        if (!isLoaded() || level == null) {
            return List.of();
        }
        try {
            return SpellRegistry.from(level).keySet();
        } catch (Throwable throwable) {
            return List.of();
        }
    }

    public static boolean isSpellProjectile(Entity entity) {
        return isLoaded() && entity instanceof SpellProjectile;
    }

    public static ResourceLocation spellOf(Entity projectile) {
        if (!isSpellProjectile(projectile)) {
            return null;
        }
        try {
            SpellProjectile shot = (SpellProjectile) projectile;
            return shot.getSpellEntry() == null
                    ? null
                    : shot.getSpellEntry().unwrapKey().map(key -> key.location()).orElse(null);
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static String schoolOf(ResourceLocation spellId, Level level) {
        if (!isLoaded() || spellId == null || level == null) {
            return null;
        }
        try {
            Spell spell = SpellRegistry.from(level).get(spellId);
            if (spell == null || spell.school == null || spell.school.id == null) {
                return null;
            }
            return spell.school.id.getPath();
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static String castTypeOf(ResourceLocation spellId, Level level) {
        if (!isLoaded() || spellId == null || level == null) {
            return null;
        }
        try {
            return castTypeOf(SpellRegistry.from(level).get(spellId));
        } catch (Throwable throwable) {
            return null;
        }
    }

    private static String castTypeOf(Spell spell) {
        if (spell == null || spell.active == null || spell.active.cast == null
                || spell.active.cast.type == null) {
            return null;
        }
        return spell.active.cast.type.name().toLowerCase(Locale.ROOT);
    }

    public static List<String> castTypeNames() {
        if (!isLoaded()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Spell.Active.Cast.Type type : Spell.Active.Cast.Type.values()) {
            names.add(type.name().toLowerCase(Locale.ROOT));
        }
        return List.copyOf(names);
    }

    private static void spellCast(SpellEvents.SpellCastEvent.Args args) {
        Player caster = args.caster();
        if (caster == null || caster.level().isClientSide() || args.spell() == null) {
            return;
        }
        Spell spell = args.spell().value();
        ResourceLocation id = args.spell().unwrapKey().map(ResourceKey::location).orElse(null);
        if (id == null) {
            return;
        }
        boolean channel = args.action() == SpellCast.Action.CHANNEL;
        LivingEntity target = null;
        for (Entity entity : args.targets()) {
            if (entity instanceof LivingEntity living && living != caster) {
                target = living;
                break;
            }
        }
        String school = spell.school == null || spell.school.id == null
                ? null
                : spell.school.id.getPath();
        EquipmentActivation.cast(caster, new SpellCastEvent(id, school, castTypeOf(spell),
                channel ? CastPhase.CHANNEL : CastPhase.CAST, args.progress(), target,
                args.action() == SpellCast.Action.TRIGGER));
    }

    public static boolean hasSpell(ItemStack stack) {
        if (!isLoaded() || stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            return SpellContainerHelper.hasUsableContainer(stack);
        } catch (Throwable throwable) {
            return false;
        }
    }

    public static List<ResourceLocation> spellsOn(ItemStack stack) {
        if (!isLoaded() || stack == null || stack.isEmpty()) {
            return List.of();
        }
        try {
            SpellContainer container = SpellContainerHelper.containerFromItemStack(stack);
            if (container == null || container.spell_ids() == null) {
                return List.of();
            }
            List<ResourceLocation> ids = new ArrayList<>();
            for (String id : container.spell_ids()) {
                ResourceLocation parsed = ResourceLocation.tryParse(id);
                if (parsed != null) {
                    ids.add(parsed);
                }
            }
            return List.copyOf(ids);
        } catch (Throwable throwable) {
            return List.of();
        }
    }

    public static boolean isCasting(Player player) {
        if (!isLoaded() || !(player instanceof SpellCaster.Entity caster)) {
            return false;
        }
        try {
            return caster.isCastingSpell();
        } catch (Throwable throwable) {
            return false;
        }
    }

    public static List<String> describeSpell(ResourceLocation spellId, Level level) {
        if (!isLoaded() || spellId == null || level == null) {
            return List.of();
        }
        try {
            Spell spell = SpellRegistry.from(level).get(spellId);
            if (spell == null) {
                return List.of();
            }
            List<String> lines = new ArrayList<>();
            if (spell.school != null && spell.school.id != null) {
                lines.add("school: " + spell.school.id.getPath());
            }
            describeProjectile(spell, lines);
            describeImpacts(spell, lines);
            return List.copyOf(lines);
        } catch (Throwable throwable) {
            return List.of();
        }
    }

    private static Spell.ProjectileData projectileDataOf(Spell spell) {
        if (spell == null || spell.deliver == null) {
            return null;
        }
        if (spell.deliver.projectile != null && spell.deliver.projectile.projectile != null) {
            return spell.deliver.projectile.projectile;
        }
        if (spell.deliver.meteor != null && spell.deliver.meteor.projectile != null) {
            return spell.deliver.meteor.projectile;
        }
        return null;
    }

    private static ModelEffect cloudEffect(Spell spell) {
        if (spell == null || spell.deliver == null || spell.deliver.clouds == null) {
            return null;
        }
        for (Spell.Delivery.Cloud cloud : spell.deliver.clouds) {
            if (cloud == null || cloud.client_data == null
                    || cloud.client_data.model_fx == null
                    || cloud.client_data.model_fx.isEmpty()) {
                continue;
            }
            for (ModelEffect effect : cloud.client_data.model_fx) {
                if (effect != null && effect.model_id != null && !effect.model_id.isBlank()) {
                    return effect;
                }
            }
        }
        return null;
    }

    private static Spell.ProjectileModelComposite.Model firstModel(ResourceLocation spellId,
                                                                   Level level) {
        if (!isLoaded() || spellId == null || level == null) {
            return null;
        }
        try {
            Spell.ProjectileData data = projectileDataOf(SpellRegistry.from(level).get(spellId));
            if (data == null || data.client_data == null
                    || data.client_data.composite_model == null
                    || data.client_data.composite_model.models == null) {
                return null;
            }
            for (Spell.ProjectileModelComposite.Model model
                    : data.client_data.composite_model.models) {
                if (model != null && model.fx != null && model.fx.model_id != null
                        && !model.fx.model_id.isBlank()) {
                    return model;
                }
            }
            return null;
        } catch (Throwable throwable) {
            return null;
        }
    }

    private static ModelEffect effectOf(ResourceLocation spellId, Level level) {
        Spell.ProjectileModelComposite.Model model = firstModel(spellId, level);
        if (model != null) {
            return model.fx;
        }
        if (!isLoaded() || spellId == null || level == null) {
            return null;
        }
        try {
            return cloudEffect(SpellRegistry.from(level).get(spellId));
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static boolean hasProjectileModel(ResourceLocation spellId, Level level) {
        return effectOf(spellId, level) != null;
    }

    public static float projectileScaleOf(ResourceLocation spellId, Level level) {
        ModelEffect effect = effectOf(spellId, level);
        return effect == null || effect.scale <= 0.0F ? 1.0F : effect.scale;
    }

    public static float projectileSpinOf(ResourceLocation spellId, Level level) {
        Spell.ProjectileModelComposite.Model model = firstModel(spellId, level);
        return model == null ? 0.0F : model.rotate_degrees_per_tick;
    }

    public static void renderProjectileModel(ResourceLocation spellId, Level level,
                                             PoseStack poses, MultiBufferSource buffers,
                                             int light) {
        ModelEffect effect = effectOf(spellId, level);
        if (effect == null) {
            return;
        }
        try {
            RenderType layer = SpellModelHelper.LAYERS.get(effect.light_emission);
            if (layer == null) {
                layer = SpellModelHelper.LAYERS.get(LightEmission.NONE);
            }
            CustomModels.render(layer, Minecraft.getInstance().getItemRenderer(),
                    ResourceLocation.parse(effect.model_id), ItemDisplayContext.GROUND,
                    poses, buffers, light, OverlayTexture.NO_OVERLAY);
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.debug("Spell model render failed for {}", spellId, throwable);
        }
    }

    private static void describeProjectile(Spell spell, List<String> lines) {
        if (spell.deliver == null || spell.deliver.projectile == null
                || spell.deliver.projectile.projectile == null) {
            return;
        }
        Spell.ProjectileData data = spell.deliver.projectile.projectile;
        if (data.perks == null) {
            return;
        }
        if (data.perks.ricochet > 0) {
            lines.add("ricochet: " + data.perks.ricochet);
        }
        if (data.perks.pierce > 0) {
            lines.add("pierce: " + data.perks.pierce);
        }
        if (data.perks.bounce > 0) {
            lines.add("bounce: " + data.perks.bounce);
        }
        if (data.homing_angle > 0.0F) {
            lines.add("homing: " + data.homing_angle);
        }
    }

    private static void describeImpacts(Spell spell, List<String> lines) {
        if (spell.impacts == null) {
            return;
        }
        for (Spell.Impact impact : spell.impacts) {
            if (impact == null || impact.action == null || impact.action.type == null) {
                continue;
            }
            lines.add(describeAction(impact.action));
        }
    }

    private static String describeAction(Spell.Impact.Action action) {
        String label = action.type.toString().toLowerCase(Locale.ROOT);
        return switch (action.type) {
            case DAMAGE -> action.damage == null
                    ? label
                    : label + ": x" + action.damage.spell_power_coefficient;
            case HEAL -> action.heal == null
                    ? label
                    : label + ": x" + action.heal.spell_power_coefficient;
            case STATUS_EFFECT -> action.status_effect == null
                    ? label
                    : label + ": " + action.status_effect.effect_id
                            + " " + action.status_effect.duration + "s";
            case FIRE -> action.fire == null
                    ? label
                    : label + ": " + action.fire.duration + "s";
            default -> label;
        };
    }

    private static Spell.Modifier modifierFor(SpellTuning tuning) {
        Spell.Modifier modifier = new Spell.Modifier();
        if (tuning.hasPower()) {
            Spell.Impact.Modifier power = new Spell.Impact.Modifier();
            power.power_multiplier = tuning.powerMultiplier();
            power.critical_chance_bonus = tuning.criticalChanceBonus();
            power.critical_damage_bonus = tuning.criticalDamageBonus();
            modifier.power_modifier = power;
        }
        if (tuning.hasPerks()) {
            Spell.ProjectileData.Perks perks = Spell.ProjectileData.Perks.EMPTY();
            perks.ricochet = tuning.ricochet();
            perks.pierce = tuning.pierce();
            perks.bounce = tuning.bounce();
            perks.ricochet_range = tuning.ricochetRange();
            perks.chain_reaction_size = tuning.chainReaction().size();
            perks.chain_reaction_triggers = tuning.chainReaction().triggers();
            perks.chain_reaction_increment = tuning.chainReaction().increment();
            modifier.projectile_perks = perks;
        }
        modifier.knockback_multiply_base = tuning.knockbackMultiply();
        modifier.projectile_scale_multiply = tuning.launch().scaleAdd();
        modifier.meteor_launch_radius_add = tuning.launch().meteorRadiusAdd();
        modifier.range_add = tuning.rangeAdd();
        modifier.channel_ticks_add = tuning.channelTicksAdd();
        modifier.effect_duration_add = tuning.effectDurationAdd();
        modifier.effect_amplifier_add = tuning.effectAmplifierAdd();
        modifier.cooldown_duration_deduct = tuning.cooldownReduction();
        if (tuning.filtersImpacts()) {
            modifier.impact_filters = filtersFor(tuning);
        }
        return modifier;
    }

    private static List<Spell.Modifier.ImpactFilter> filtersFor(SpellTuning tuning) {
        Set<String> schools = new HashSet<>();
        for (String school : tuning.impactSchools()) {
            if (school != null && !school.isBlank()) {
                schools.add(SpellLockState.schoolKey(school.trim()));
            }
        }
        Set<Spell.Impact.Action.Type> types = new HashSet<>();
        for (String name : tuning.impactTypes()) {
            Spell.Impact.Action.Type type = impactType(name);
            if (type != null) {
                types.add(type);
            }
        }
        return List.of(new TunedImpactFilter(schools, types));
    }

    public static List<String> impactTypeNames() {
        if (!isLoaded()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Spell.Impact.Action.Type type : Spell.Impact.Action.Type.values()) {
            names.add(type.name().toLowerCase(Locale.ROOT));
        }
        return List.copyOf(names);
    }

    private static Spell.Impact.Action.Type impactType(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String wanted = name.trim().toUpperCase(Locale.ROOT);
        for (Spell.Impact.Action.Type type : Spell.Impact.Action.Type.values()) {
            if (type.name().equals(wanted)) {
                return type;
            }
        }
        return null;
    }

    public static void faceAlong(Entity shot, Vec3 direction) {
        if (!isLoaded() || shot == null || direction == null) {
            return;
        }
        float pitch = (float) VectorHelper.pitchFromNormalized(direction);
        float yaw = (float) VectorHelper.yawFromNormalized(direction);
        shot.setXRot(pitch);
        shot.setYRot(yaw);
        shot.xRotO = pitch;
        shot.yRotO = yaw;
    }

    public static Object shotTagOf(Entity projectile) {
        if (!isLoaded()) {
            return null;
        }
        SpellExecution.ImpactContext context = null;
        if (projectile instanceof SpellProjectile shot) {
            context = shot.getImpactContext();
        } else if (projectile instanceof SpellCloud cloud) {
            context = ((SpellCloudAccessor) cloud).combat_styles$context();
        }
        return context == null ? null : context.power();
    }

    public static Object shootSpell(Player player, ResourceLocation spellId,
                                    Entity target, float damage, SpellTuning tuning,
                                    Consumer<Object> beforeLaunch) {
        if (!isLoaded() || player == null || spellId == null
                || player.level() == null || player.level().isClientSide()) {
            return null;
        }
        try {
            Holder<Spell> spell = SpellRegistry.from(player.level())
                    .getHolder(ResourceKey.create(SpellRegistry.KEY, spellId))
                    .map(holder -> (Holder<Spell>) holder)
                    .orElse(null);
            if (spell == null || spell.value().deliver == null) {
                return null;
            }
            Spell.Delivery deliver = spell.value().deliver;
            boolean meteor = deliver.meteor != null;
            if (!meteor && deliver.projectile == null) {
                return null;
            }
            Spell.LaunchProperties launch = meteor
                    ? deliver.meteor.launch_properties
                    : deliver.projectile.launch_properties;
            Spell.Modifier modifier = tuning == null || tuning.isEmpty()
                    ? new Spell.Modifier()
                    : modifierFor(tuning);
            modifier.projectile_launch = launchFor(player, spell, launch, tuning);
            SpellExecution.ImpactContext context = contextFor(player, spell, modifier, damage);
            if (beforeLaunch != null) {
                beforeLaunch.accept(context.power());
            }
            if (meteor) {
                Vec3 at = target == null ? player.position() : target.position();
                return ProjectileLauncher.fallProjectile(player.level(), player, target,
                        at, spell, context) ? context.power() : null;
            }
            ProjectileLauncher.shootProjectile(player.level(), player, target, spell, context);
            return context.power();
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.warn("Shooting {} failed", spellId, throwable);
            return null;
        }
    }

    private static SpellExecution.ImpactContext contextFor(Player player, Holder<Spell> spell,
                                                           Spell.Modifier modifier, float damage) {
        SpellPower.Result natural = SpellPower.getSpellPower(spell.value().school, player);
        SpellPower.Result marker = new SpellPower.Result(natural.school(),
                natural.baseValue(), natural.criticalChance(), natural.criticalDamage());
        if (damage >= 0.0F) {
            SpellPowerOverrides.register(marker, damage);
        }
        return new SpellExecution.ImpactContext().chargeModifier(modifier).power(marker);
    }

    private static Holder<Spell> holderOf(Level level, ResourceLocation spellId) {
        return SpellRegistry.from(level)
                .getHolder(ResourceKey.create(SpellRegistry.KEY, spellId))
                .map(holder -> (Holder<Spell>) holder)
                .orElse(null);
    }

    public static boolean isSpellCloud(Entity entity) {
        return isLoaded() && entity instanceof SpellCloud;
    }

    public static boolean prepareSummon(Entity spawned, Player owner) {
        if (!isLoaded() || owner == null || !(spawned instanceof SpellSummoned summoned)) {
            return false;
        }
        try {
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(spawned.getType());
            Registry<Spell> spells = SpellRegistry.from(owner.level());
            for (Spell spell : spells) {
                Spell.Impact.Action.Summon summon = summonOf(spell, entityId);
                if (summon != null) {
                    summoned.onSummonedBySpell(new SpellSummoned.Args(owner, spells.wrapAsHolder(spell),
                            summon.behaviour, summon.attribute_scaling,
                            new SpellExecution.ImpactContext()));
                    return true;
                }
            }
            if (spawned instanceof SummonedEntity entity) {
                entity.setOwnerUuid(owner.getUUID());
            }
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.error("Unable to prepare Spell Engine summon {}", spawned, throwable);
        }
        return false;
    }

    public static boolean reownSpellSummon(Entity spawned, Player owner) {
        if (!isLoaded() || owner == null || !(spawned instanceof SummonedEntity entity)) {
            return false;
        }
        entity.setOwnerUuid(owner.getUUID());
        return true;
    }

    private static Spell.Impact.Action.Summon summonOf(Spell spell, ResourceLocation entityId) {
        if (spell == null || spell.impacts == null || entityId == null) {
            return null;
        }
        for (Spell.Impact impact : spell.impacts) {
            Spell.Impact.Action.Summon summon = impact == null || impact.action == null
                    ? null
                    : impact.action.summon;
            if (summon != null && summon.behaviour != null
                    && entityId.toString().equals(summon.entity_type_id)) {
                return summon;
            }
        }
        return null;
    }

    public static Object castSpell(Player player, ResourceLocation spellId, CastSpell.Target target,
                                   Entity hit, float damage, SpellTuning tuning) {
        if (!isLoaded() || player == null || spellId == null
                || player.level() == null || player.level().isClientSide()) {
            return null;
        }
        try {
            Holder<Spell> spell = holderOf(player.level(), spellId);
            if (spell == null) {
                return null;
            }
            Spell.Modifier modifier = tunedCastModifier(player, spell, tuning);
            SpellExecution.ImpactContext context = contextFor(player, spell, modifier, damage);
            SpellTarget.SearchResult targets = targetsFor(player, spell, target, hit);
            boolean delivered = SpellDelivery.resolveAndDeliver(player.level(), player, spell,
                    targets, context, completion -> {
                    });
            return delivered ? context.power() : null;
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.debug("Casting {} failed", spellId, throwable);
            return null;
        }
    }

    public static Object castSpellAs(LivingEntity caster, Player owner, ResourceLocation spellId,
                                     LivingEntity target, float damage, SpellTuning tuning,
                                     SummonCastTarget mode) {
        if (!isLoaded() || caster == null || owner == null || spellId == null
                || caster.level().isClientSide()) {
            return null;
        }
        try {
            Holder<Spell> spell = holderOf(caster.level(), spellId);
            if (spell == null) {
                return null;
            }
            SpellTarget.SearchResult targets = summonTargets(caster, owner, spell, target, mode);
            if (targets == null) {
                return null;
            }
            Spell.Modifier modifier = tunedCastModifier(caster, spell, tuning);
            SpellExecution.ImpactContext context = contextFor(owner, spell, modifier, damage);
            boolean delivered = SpellDelivery.resolveAndDeliver(caster.level(), caster, spell,
                    targets, context, completion -> {
                    });
            return delivered ? context.power() : null;
        } catch (Throwable throwable) {
            CombatStyles.LOGGER.debug("Summon casting {} failed", spellId, throwable);
            return null;
        }
    }

    private static SpellTarget.SearchResult summonTargets(LivingEntity caster, Player owner,
                                                          Holder<Spell> spell, LivingEntity target,
                                                          SummonCastTarget mode) {
        SummonCastTarget chosen = mode == null ? SummonCastTarget.AUTO : mode;
        LivingEntity facing = switch (chosen) {
            case SELF -> null;
            case OWNER -> owner;
            default -> target;
        };
        if (facing != null && facing != caster) {
            faceToward(caster, facing);
        }
        Spell.Target.Type type = spell.value().target == null ? null : spell.value().target.type;
        boolean engineSearches = type == Spell.Target.Type.AIM || type == Spell.Target.Type.BEAM
                || type == Spell.Target.Type.AREA;
        return switch (chosen) {
            case SELF -> engineSearches ? searched(caster, spell) : SpellTarget.SearchResult.of(caster);
            case OWNER -> SpellTarget.SearchResult.of(owner);
            case HIT -> {
                if (target == null) {
                    yield null;
                }
                SpellTarget.SearchResult found = engineSearches && type != Spell.Target.Type.AREA
                        ? searched(caster, spell)
                        : null;
                yield found != null ? found : SpellTarget.SearchResult.of(target);
            }
            case AIM -> searched(caster, spell);
            case AUTO -> autoTargets(caster, owner, spell, target);
        };
    }

    private static void faceToward(LivingEntity caster, Entity toward) {
        Vec3 delta = toward.getEyePosition().subtract(caster.getEyePosition());
        double flat = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - QUARTER_TURN;
        float pitch = (float) -(Mth.atan2(delta.y, flat) * Mth.RAD_TO_DEG);
        caster.setYRot(yaw);
        caster.setXRot(pitch);
        caster.yRotO = yaw;
        caster.xRotO = pitch;
        caster.setYHeadRot(yaw);
        caster.setYBodyRot(yaw);
    }

    private static SpellTarget.SearchResult autoTargets(LivingEntity caster, Player owner,
                                                        Holder<Spell> spell, LivingEntity target) {
        if (beneficial(spell.value())) {
            return SpellTarget.SearchResult.of(owner);
        }
        Spell.Target.Type type = spell.value().target == null ? null : spell.value().target.type;
        if (type == null || type == Spell.Target.Type.NONE
                || type == Spell.Target.Type.FROM_TRIGGER) {
            return target == null ? null : SpellTarget.SearchResult.of(target);
        }
        if (type == Spell.Target.Type.CASTER) {
            return SpellTarget.SearchResult.of(caster);
        }
        SpellTarget.SearchResult found = searched(caster, spell);
        if (found != null) {
            return found;
        }
        return target == null ? null : SpellTarget.SearchResult.of(target);
    }

    private static SpellTarget.SearchResult searched(LivingEntity caster, Holder<Spell> spell) {
        SpellTarget.SearchResult found = SpellTarget.findTargets(caster, spell,
                SpellTarget.SearchResult.empty(), true);
        return found != null && (!found.entities().isEmpty() || found.location() != null)
                ? found
                : null;
    }

    private static boolean beneficial(Spell spell) {
        if (spell == null) {
            return false;
        }
        return SpellIntents.deliveryIntent(spell)
                .map(intent -> intent == SpellTarget.Intent.HELPFUL)
                .orElseGet(() -> SpellIntents.impactIntents(spell)
                        .equals(EnumSet.of(SpellTarget.Intent.HELPFUL)));
    }

    public static float triggerChance(ResourceLocation spellId, Level level) {
        if (!isLoaded() || spellId == null || level == null) {
            return CERTAIN_CHANCE;
        }
        try {
            Holder<Spell> spell = holderOf(level, spellId);
            Spell.Passive passive = spell == null ? null : spell.value().passive;
            if (passive == null || passive.triggers == null || passive.triggers.isEmpty()) {
                return CERTAIN_CHANCE;
            }
            float best = 0.0F;
            for (Spell.Trigger trigger : passive.triggers) {
                if (trigger != null) {
                    best = Math.max(best, trigger.chance);
                }
            }
            return best;
        } catch (Throwable throwable) {
            return CERTAIN_CHANCE;
        }
    }

    public static boolean deliversProjectile(ResourceLocation spellId, Level level) {
        if (!isLoaded() || spellId == null || level == null) {
            return false;
        }
        try {
            Holder<Spell> spell = holderOf(level, spellId);
            Spell.Delivery deliver = spell == null ? null : spell.value().deliver;
            return deliver != null && (deliver.projectile != null || deliver.meteor != null);
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static SpellTarget.SearchResult targetsFor(Player player, Holder<Spell> spell,
                                                      CastSpell.Target target, Entity hit) {
        if (target == CastSpell.Target.SELF) {
            return SpellTarget.SearchResult.of(player);
        }
        if (target == CastSpell.Target.HIT && hit != null) {
            return SpellTarget.SearchResult.of(hit);
        }
        SpellTarget.SearchResult found = SpellTarget.findTargets(player, spell,
                SpellTarget.SearchResult.empty(), true);
        if (found != null && (!found.entities().isEmpty() || found.location() != null)) {
            return found;
        }
        return SpellTarget.SearchResult.of(aimPoint(player, spell.value().range));
    }

    private static Vec3 aimPoint(Player player, float spellRange) {
        double reach = spellRange > 0.0F
                ? spellRange
                : player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(reach));
        BlockHitResult blocked = player.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return blocked.getType() == HitResult.Type.BLOCK ? blocked.getLocation() : to;
    }

    public static SpellChannel channelOf(ResourceLocation spellId, Level level, SpellTuning tuning) {
        if (!isLoaded() || spellId == null || level == null) {
            return SpellChannel.NONE;
        }
        try {
            Spell spell = SpellRegistry.from(level).get(spellId);
            if (spell == null || spell.active == null || spell.active.cast == null
                    || spell.active.cast.type != Spell.Active.Cast.Type.CHANNEL) {
                return SpellChannel.NONE;
            }
            int added = tuning == null ? 0 : tuning.channelTicksAdd();
            int interval = Math.max(1, spell.active.cast.channelTicks() + added);
            int duration = Math.round(spell.active.cast.duration * SharedConstants.TICKS_PER_SECOND);
            return new SpellChannel(interval, duration);
        } catch (Throwable throwable) {
            return SpellChannel.NONE;
        }
    }

    public static void init() {
        if (!isLoaded()) {
            return;
        }
        SpellContainerSource.addSource(new SpellContainerSource.Entry(GRANT_SOURCE,
                SpellEngineBridge::grantedContainers, SpellLockState::versionOf));
        SpellEvents.CASTING_ATTEMPT.PRE.register(SpellEngineBridge::attemptLocked);
        SpellEvents.SPELL_CAST.register(SpellEngineBridge::spellCast);
    }

    private static List<SpellContainerSource.SourcedContainer> grantedContainers(Player player,
                                                                                String name) {
        List<String> granted = SpellLockState.grantsFor(player);
        if (granted.isEmpty()) {
            return List.of();
        }
        SpellContainer container = new SpellContainer(SpellContainer.ContentType.NONE,
                NO_PARAMETER, NO_PARAMETER, NO_PARAMETER, granted.size(), granted);
        return List.of(new SpellContainerSource.SourcedContainer(name, null, container));
    }

    private static SpellCast.Attempt attemptLocked(SpellEvents.CastingAttemptEvent.Args args) {
        Player caster = args.caster();
        SpellLockState.Lock lock = lockOf(caster, args.spell());
        if (lock == null) {
            return null;
        }
        if (lock.mode() == SpellLocks.Mode.BLOCK && caster.level().isClientSide()) {
            caster.displayClientMessage(Component.translatable(KEY_DISABLED_BY,
                    itemName(lock.item())), true);
        }
        return SpellCast.Attempt.none();
    }

    private static SpellLockState.Lock lockOf(Player player, Holder<Spell> spell) {
        if (spell == null) {
            return null;
        }
        String id = spell.unwrapKey().map(key -> key.location().toString()).orElse(null);
        Spell value = spell.value();
        String school = value.school == null || value.school.id == null
                ? null
                : value.school.id.getPath();
        return SpellLockState.lockFor(player, id, school);
    }

    private static Component itemName(String itemId) {
        ResourceLocation parsed = ResourceLocation.tryParse(itemId);
        return parsed == null
                ? Component.literal(itemId)
                : BuiltInRegistries.ITEM.get(parsed).getDescription();
    }

    public static void afterContainerUpdate(Player player) {
        if (!isLoaded() || !(player instanceof SpellContainerSource.Owner owner)) {
            return;
        }
        SpellContainerSource.Result result = owner.getSpellContainers();
        if (result == null) {
            return;
        }
        List<Holder<Spell>> actives = kept(player, result.actives());
        List<Holder<Spell>> passives = kept(player, result.passives());
        List<Holder<Spell>> modifiers = kept(player, result.modifiers());
        for (String granted : SpellLockState.grantsFor(player)) {
            ResourceLocation id = ResourceLocation.tryParse(granted);
            Holder<Spell> spell = id == null ? null : holderOf(player.level(), id);
            if (spell == null || removed(player, spell)) {
                continue;
            }
            Spell.Type type = spell.value().type;
            List<Holder<Spell>> into = type == Spell.Type.PASSIVE
                    ? passives
                    : (type == Spell.Type.MODIFIER ? modifiers : actives);
            if (!containsSpell(into, spell)) {
                into.add(spell);
            }
        }
        if (sameSpells(actives, result.actives()) && sameSpells(passives, result.passives())
                && sameSpells(modifiers, result.modifiers())) {
            applyTunes(player, owner);
            return;
        }
        owner.setSpellContainers(new SpellContainerSource.Result(result.activeContainer(),
                List.copyOf(actives), List.copyOf(passives), List.copyOf(modifiers),
                result.sources()));
        owner.spellModifierCache().clear();
        if (!player.level().isClientSide() && player instanceof SpellCaster.Player caster
                && caster.getInteractor() != null) {
            caster.getInteractor().invalidateOptions();
        }
        applyTunes(player, owner);
    }

    private static void applyTunes(Player player, SpellContainerSource.Owner owner) {
        Map<String, SpellTuning> tunes = SpellLockState.tunesFor(player);
        if (tunes.isEmpty()) {
            return;
        }
        Map<ResourceLocation, List<Spell.Modifier>> cache = owner.spellModifierCache();
        for (Map.Entry<String, SpellTuning> entry : tunes.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            Holder<Spell> spell = id == null ? null : holderOf(player.level(), id);
            if (spell == null || entry.getValue().isEmpty()) {
                continue;
            }
            List<Spell.Modifier> cached = cache.get(id);
            if (cached != null && cached.stream().anyMatch(TUNED::contains)) {
                continue;
            }
            List<Spell.Modifier> modifiers = new ArrayList<>(SpellModifiers.of(player, spell));
            modifiers.removeIf(TUNED::contains);
            Spell.Modifier tuned = tunedModifier(entry.getValue());
            TUNED.add(tuned);
            modifiers.add(tuned);
            cache.put(id, modifiers);
        }
    }

    private static Spell.Modifier tunedModifier(SpellTuning tuning) {
        Spell.Modifier modifier = modifierFor(tuning);
        SpellLaunchTuning launch = tuning.launch();
        if (!launch.isEmpty()) {
            Spell.LaunchProperties added = new Spell.LaunchProperties();
            added.velocity = launch.velocityAdd();
            added.extra_launch_mod = KEEP_LAUNCH_MOD;
            added.extra_launch_count = launch.extraLaunches();
            added.extra_launch_delay = launch.launchDelay() == SpellLaunchTuning.SPELL_DELAY
                    ? 0
                    : launch.launchDelay();
            modifier.projectile_launch = added;
        }
        return modifier;
    }

    private static List<Holder<Spell>> kept(Player player, List<Holder<Spell>> spells) {
        List<Holder<Spell>> kept = new ArrayList<>();
        if (spells == null) {
            return kept;
        }
        for (Holder<Spell> spell : spells) {
            if (!removed(player, spell)) {
                kept.add(spell);
            }
        }
        return kept;
    }

    private static boolean removed(Player player, Holder<Spell> spell) {
        SpellLockState.Lock lock = lockOf(player, spell);
        return lock != null && lock.mode() == SpellLocks.Mode.REMOVE;
    }

    private static boolean containsSpell(List<Holder<Spell>> spells, Holder<Spell> spell) {
        for (Holder<Spell> existing : spells) {
            if (existing.value() == spell.value()) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameSpells(List<Holder<Spell>> first, List<Holder<Spell>> second) {
        if (second == null || first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            if (first.get(index).value() != second.get(index).value()) {
                return false;
            }
        }
        return true;
    }

    private static Spell.Modifier tunedCastModifier(LivingEntity caster, Holder<Spell> spell,
                                                    SpellTuning tuning) {
        Spell.Modifier modifier = tuning == null || tuning.isEmpty()
                ? new Spell.Modifier()
                : modifierFor(tuning);
        if (tuning != null && !tuning.launch().equals(SpellLaunchTuning.NONE)
                && spell.value().deliver != null) {
            Spell.Delivery deliver = spell.value().deliver;
            Spell.LaunchProperties launch = deliver.meteor != null
                    ? deliver.meteor.launch_properties
                    : (deliver.projectile != null ? deliver.projectile.launch_properties : null);
            modifier.projectile_launch = launchFor(caster, spell, launch, tuning);
        }
        return modifier;
    }

    private static Spell.LaunchProperties launchFor(LivingEntity caster, Holder<Spell> spell,
                                                    Spell.LaunchProperties base,
                                                    SpellTuning tuning) {
        int extras = base == null ? 0 : base.extra_launch_count;
        int delay = base == null ? 0 : base.extra_launch_delay;
        if (caster instanceof Player player) {
            for (Spell.Modifier active : SpellModifiers.of(player, spell)) {
                if (active != null && active.projectile_launch != null) {
                    extras += active.projectile_launch.extra_launch_count;
                    delay += active.projectile_launch.extra_launch_delay;
                }
            }
        }
        SpellLaunchTuning wanted = tuning == null ? SpellLaunchTuning.NONE : tuning.launch();
        Spell.LaunchProperties adjust = new Spell.LaunchProperties();
        adjust.velocity = wanted.velocityAdd();
        adjust.extra_launch_mod = KEEP_LAUNCH_MOD;
        adjust.extra_launch_count = wanted.extraLaunches() - extras;
        adjust.extra_launch_delay = wanted.launchDelay() == SpellLaunchTuning.SPELL_DELAY
                ? 0
                : wanted.launchDelay() - delay;
        return adjust;
    }

    public static ResourceLocation castingSpell(Player player) {
        if (!isCasting(player)) {
            return null;
        }
        try {
            Spell spell = ((SpellCaster.Entity) player).getCastedSpell();
            if (spell == null || player.level() == null) {
                return null;
            }
            return SpellRegistry.from(player.level()).getKey(spell);
        } catch (Throwable throwable) {
            return null;
        }
    }
}
