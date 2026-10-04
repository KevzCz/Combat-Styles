package net.pixeldreamstudios.combat_styles.client;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.network.NetworkManager;
import net.pixeldreamstudios.combat_styles.network.SummonCommandPayload;

public final class KeybindHandler {

    public static final String CATEGORY = "key.categories.combat_styles";
    public static final String OPEN_CUSTOMIZER = "key.combat_styles.open_customizer";
    public static final String OPEN_EQUIPMENT = "key.combat_styles.open_equipment";
    public static final String SUMMON_RECALL = "key.combat_styles.summon_recall";
    public static final String SUMMON_FOCUS = "key.combat_styles.summon_focus";

    private static final double FOCUS_RANGE = 48.0D;
    private static final double PICK_INFLATE = 1.0D;
    private static final float CURRENT_FRAME = 1.0F;

    private KeybindHandler() {
    }

    @ExpectPlatform
    public static void register() {
        throw new AssertionError();
    }

    public static void openEquipment(Minecraft client) {
        if (client.player == null || client.screen != null
                || !client.player.getAbilities().instabuild) {
            return;
        }
        client.setScreen(new LootMenuScreen(null, true));
    }

    public static void openCustomizer(Minecraft client) {
        if (client.player == null || client.screen != null) {
            return;
        }
        client.setScreen(new CustomizerScreen(new CustomizerState(client.player)));
    }

    public static void recallSummons(Minecraft client) {
        if (client.player == null || client.screen != null) {
            return;
        }
        NetworkManager.sendSummonCommand(new SummonCommandPayload(
                SummonCommandPayload.Command.RECALL, SummonCommandPayload.NO_TARGET));
    }

    public static void focusSummons(Minecraft client) {
        if (client.player == null || client.screen != null) {
            return;
        }
        Entity target = pickTarget(client);
        if (target == null) {
            return;
        }
        NetworkManager.sendSummonCommand(new SummonCommandPayload(
                SummonCommandPayload.Command.FOCUS, target.getId()));
    }

    private static Entity pickTarget(Minecraft client) {
        Vec3 eye = client.player.getEyePosition();
        Vec3 reach = client.player.getViewVector(CURRENT_FRAME).scale(FOCUS_RANGE);
        Vec3 end = eye.add(reach);
        AABB area = client.player.getBoundingBox().expandTowards(reach).inflate(PICK_INFLATE);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(client.player, eye, end, area,
                candidate -> candidate instanceof LivingEntity && candidate.isPickable()
                        && !candidate.isSpectator(),
                FOCUS_RANGE * FOCUS_RANGE);
        return hit == null ? null : hit.getEntity();
    }
}
