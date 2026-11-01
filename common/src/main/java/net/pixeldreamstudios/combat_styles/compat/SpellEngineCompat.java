package net.pixeldreamstudios.combat_styles.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.platform.Platform;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.affix.CastSpell;
import net.pixeldreamstudios.combat_styles.entity.SummonCastTarget;
import net.pixeldreamstudios.combat_styles.equipment.SpellTuning;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public final class SpellEngineCompat {

    public static final String MOD_ID = "spell_engine";
    public static final String POWER_MOD_ID = "spell_power";
    public static final float INHERIT_DAMAGE = -1.0F;
    private static final float CERTAIN_CHANCE = 1.0F;
    private static final float NORMAL_SCALE = 1.0F;
    private static final float NO_SPIN = 0.0F;

    private static Boolean loaded;

    private SpellEngineCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = Platform.isModLoaded(MOD_ID) && Platform.isModLoaded(POWER_MOD_ID);
        }
        return loaded;
    }

    public static Collection<ResourceLocation> spellIds(Level level) {
        return isLoaded() ? SpellEngineBridge.spellIds(level) : List.of();
    }

    public static List<String> impactTypeNames() {
        return isLoaded() ? SpellEngineBridge.impactTypeNames() : List.of();
    }

    public static String castTypeOf(ResourceLocation spellId, Level level) {
        return isLoaded() ? SpellEngineBridge.castTypeOf(spellId, level) : null;
    }

    public static List<String> castTypeNames() {
        return isLoaded() ? SpellEngineBridge.castTypeNames() : List.of();
    }

    public static boolean isSpellProjectile(Entity entity) {
        return isLoaded() && SpellEngineBridge.isSpellProjectile(entity);
    }

    public static ResourceLocation spellOf(Entity projectile) {
        return isLoaded() ? SpellEngineBridge.spellOf(projectile) : null;
    }

    public static String schoolOf(ResourceLocation spellId, Level level) {
        return isLoaded() ? SpellEngineBridge.schoolOf(spellId, level) : null;
    }

    public static boolean hasSpell(ItemStack stack) {
        return isLoaded() && SpellEngineBridge.hasSpell(stack);
    }

    public static List<ResourceLocation> spellsOn(ItemStack stack) {
        return isLoaded() ? SpellEngineBridge.spellsOn(stack) : List.of();
    }

    public static boolean isCasting(Player player) {
        return isLoaded() && SpellEngineBridge.isCasting(player);
    }

    public static ResourceLocation castingSpell(Player player) {
        return isLoaded() ? SpellEngineBridge.castingSpell(player) : null;
    }

    public static Object shootSpell(Player player, ResourceLocation spellId,
                                    Entity target, float damage, SpellTuning tuning,
                                    Consumer<Object> beforeLaunch) {
        return isLoaded() ? SpellEngineBridge.shootSpell(player, spellId, target, damage, tuning, beforeLaunch) : null;
    }

    public static void faceAlong(Entity shot, Vec3 direction) {
        if (isLoaded()) {
            SpellEngineBridge.faceAlong(shot, direction);
        }
    }

    public static Object shotTagOf(Entity projectile) {
        return isLoaded() ? SpellEngineBridge.shotTagOf(projectile) : null;
    }

    public static boolean isSpellCloud(Entity entity) {
        return isLoaded() && SpellEngineBridge.isSpellCloud(entity);
    }

    public static boolean prepareSummon(Entity spawned, Player owner) {
        return isLoaded() && SpellEngineBridge.prepareSummon(spawned, owner);
    }

    public static boolean reownSpellSummon(Entity spawned, Player owner) {
        return isLoaded() && SpellEngineBridge.reownSpellSummon(spawned, owner);
    }

    public static void init() {
        if (isLoaded()) {
            SpellEngineBridge.init();
        }
    }

    public static void afterContainerUpdate(Player player) {
        if (isLoaded()) {
            SpellEngineBridge.afterContainerUpdate(player);
        }
    }

    public static Object castSpell(Player player, ResourceLocation spellId, CastSpell.Target target,
                                   Entity hit, float damage, SpellTuning tuning) {
        return isLoaded() ? SpellEngineBridge.castSpell(player, spellId, target, hit, damage, tuning) : null;
    }

    public static Object castSpellAs(LivingEntity caster, Player owner, ResourceLocation spellId,
                                     LivingEntity target, float damage, SpellTuning tuning,
                                     SummonCastTarget mode) {
        return isLoaded() ? SpellEngineBridge.castSpellAs(caster, owner, spellId, target, damage, tuning, mode) : null;
    }

    public static float triggerChance(ResourceLocation spellId, Level level) {
        return isLoaded() ? SpellEngineBridge.triggerChance(spellId, level) : CERTAIN_CHANCE;
    }

    public static boolean deliversProjectile(ResourceLocation spellId, Level level) {
        return isLoaded() && SpellEngineBridge.deliversProjectile(spellId, level);
    }

    public static SpellChannel channelOf(ResourceLocation spellId, Level level, SpellTuning tuning) {
        return isLoaded() ? SpellEngineBridge.channelOf(spellId, level, tuning) : SpellChannel.NONE;
    }

    public static List<String> describeSpell(ResourceLocation spellId, Level level) {
        return isLoaded() ? SpellEngineBridge.describeSpell(spellId, level) : List.of();
    }

    public static boolean hasProjectileModel(ResourceLocation spellId, Level level) {
        return isLoaded() && SpellEngineBridge.hasProjectileModel(spellId, level);
    }

    public static float projectileScaleOf(ResourceLocation spellId, Level level) {
        return isLoaded() ? SpellEngineBridge.projectileScaleOf(spellId, level) : NORMAL_SCALE;
    }

    public static float projectileSpinOf(ResourceLocation spellId, Level level) {
        return isLoaded() ? SpellEngineBridge.projectileSpinOf(spellId, level) : NO_SPIN;
    }

    public static void renderProjectileModel(ResourceLocation spellId, Level level,
                                             PoseStack poses, MultiBufferSource buffers,
                                             int light) {
        if (isLoaded()) {
            SpellEngineBridge.renderProjectileModel(spellId, level, poses, buffers, light);
        }
    }
}
