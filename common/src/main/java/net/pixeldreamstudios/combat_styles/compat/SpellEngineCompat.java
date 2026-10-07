package net.pixeldreamstudios.combat_styles.compat;

import com.mojang.blaze3d.vertex.PoseStack;
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

    private SpellEngineCompat() {
    }

    public static boolean isLoaded() {
        return SpellEngineBridge.isLoaded();
    }

    public static Collection<ResourceLocation> spellIds(Level level) {
        return SpellEngineBridge.spellIds(level);
    }

    public static List<String> impactTypeNames() {
        return SpellEngineBridge.impactTypeNames();
    }

    public static String castTypeOf(ResourceLocation spellId, Level level) {
        return SpellEngineBridge.castTypeOf(spellId, level);
    }

    public static List<String> castTypeNames() {
        return SpellEngineBridge.castTypeNames();
    }

    public static boolean isSpellProjectile(Entity entity) {
        return SpellEngineBridge.isSpellProjectile(entity);
    }

    public static ResourceLocation spellOf(Entity projectile) {
        return SpellEngineBridge.spellOf(projectile);
    }

    public static String schoolOf(ResourceLocation spellId, Level level) {
        return SpellEngineBridge.schoolOf(spellId, level);
    }

    public static boolean hasSpell(ItemStack stack) {
        return SpellEngineBridge.hasSpell(stack);
    }

    public static List<ResourceLocation> spellsOn(ItemStack stack) {
        return SpellEngineBridge.spellsOn(stack);
    }

    public static boolean isCasting(Player player) {
        return SpellEngineBridge.isCasting(player);
    }

    public static ResourceLocation castingSpell(Player player) {
        return SpellEngineBridge.castingSpell(player);
    }

    public static final float INHERIT_DAMAGE = -1.0F;

    public static Object shootSpell(Player player, ResourceLocation spellId,
                                    Entity target, float damage, SpellTuning tuning,
                                    Consumer<Object> beforeLaunch) {
        return SpellEngineBridge.shootSpell(player, spellId, target, damage, tuning, beforeLaunch);
    }

    public static void faceAlong(Entity shot, Vec3 direction) {
        SpellEngineBridge.faceAlong(shot, direction);
    }

    public static Object shotTagOf(Entity projectile) {
        return SpellEngineBridge.shotTagOf(projectile);
    }

    public static boolean isSpellCloud(Entity entity) {
        return SpellEngineBridge.isSpellCloud(entity);
    }

    public static boolean prepareSummon(Entity spawned, Player owner) {
        return SpellEngineBridge.prepareSummon(spawned, owner);
    }

    public static boolean reownSpellSummon(Entity spawned, Player owner) {
        return SpellEngineBridge.reownSpellSummon(spawned, owner);
    }

    public static void init() {
        SpellEngineBridge.init();
    }

    public static void afterContainerUpdate(Player player) {
        SpellEngineBridge.afterContainerUpdate(player);
    }

    public static Object castSpell(Player player, ResourceLocation spellId, CastSpell.Target target,
                                   Entity hit, float damage, SpellTuning tuning) {
        return SpellEngineBridge.castSpell(player, spellId, target, hit, damage, tuning);
    }

    public static Object castSpellAs(LivingEntity caster, Player owner, ResourceLocation spellId,
                                     LivingEntity target, float damage, SpellTuning tuning,
                                     SummonCastTarget mode) {
        return SpellEngineBridge.castSpellAs(caster, owner, spellId, target, damage, tuning, mode);
    }

    public static float triggerChance(ResourceLocation spellId, Level level) {
        return SpellEngineBridge.triggerChance(spellId, level);
    }

    public static boolean deliversProjectile(ResourceLocation spellId, Level level) {
        return SpellEngineBridge.deliversProjectile(spellId, level);
    }

    public static SpellChannel channelOf(ResourceLocation spellId, Level level, SpellTuning tuning) {
        return SpellEngineBridge.channelOf(spellId, level, tuning);
    }

    public static List<String> describeSpell(ResourceLocation spellId, Level level) {
        return SpellEngineBridge.describeSpell(spellId, level);
    }

    public static boolean hasProjectileModel(ResourceLocation spellId, Level level) {
        return SpellEngineBridge.hasProjectileModel(spellId, level);
    }

    public static float projectileScaleOf(ResourceLocation spellId, Level level) {
        return SpellEngineBridge.projectileScaleOf(spellId, level);
    }

    public static float projectileSpinOf(ResourceLocation spellId, Level level) {
        return SpellEngineBridge.projectileSpinOf(spellId, level);
    }

    public static void renderProjectileModel(ResourceLocation spellId, Level level,
                                             PoseStack poses, MultiBufferSource buffers,
                                             int light) {
        SpellEngineBridge.renderProjectileModel(spellId, level, poses, buffers, light);
    }
}
