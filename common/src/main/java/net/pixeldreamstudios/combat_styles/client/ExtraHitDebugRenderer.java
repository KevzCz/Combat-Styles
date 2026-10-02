package net.pixeldreamstudios.combat_styles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.MinecraftClient_BetterCombat;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.client.BetterCombatClientMod;
import net.bettercombat.client.collision.OrientedBoundingBox;
import net.bettercombat.client.collision.TargetFinder;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.equipment.ClientSwing;
import net.pixeldreamstudios.combat_styles.equipment.PendingExtraHit;
import net.pixeldreamstudios.combat_styles.equipment.SequenceSlots;
import net.pixeldreamstudios.combat_styles.network.HitboxClaimPayload;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.AttackShaping;
import net.pixeldreamstudios.combat_styles.style.ExtraAttack;
import net.pixeldreamstudios.combat_styles.style.HitboxGeometry;
import net.pixeldreamstudios.combat_styles.style.HitboxSet;
import net.pixeldreamstudios.combat_styles.style.ExtraAttackScope;

import java.util.List;

public final class ExtraHitDebugRenderer {

    private static final float ALPHA = 0.85F;
    private static final float[] HIT_COLOUR = {1.0F, 0.15F, 0.15F};
    private static final float[] OWN_COLOUR = {0.95F, 0.85F, 0.30F};
    private static final float[][] COLOURS = {
            {1.0F, 0.45F, 0.15F},
            {0.35F, 0.75F, 1.0F},
            {1.0F, 0.25F, 0.85F},
            {0.55F, 1.0F, 0.35F}
    };

    private static boolean drewReplacementBoxes;

    private ExtraHitDebugRenderer() {
    }

    public static boolean muteBaseCollider() {
        if (!drewReplacementBoxes || BetterCombatClientMod.config == null
                || !BetterCombatClientMod.config.isDebugOBBEnabled) {
            return false;
        }
        BetterCombatClientMod.config.isDebugOBBEnabled = false;
        return true;
    }

    public static void unmuteBaseCollider() {
        if (BetterCombatClientMod.config != null) {
            BetterCombatClientMod.config.isDebugOBBEnabled = true;
        }
    }

    public static void render(PoseStack poses, MultiBufferSource.BufferSource buffers,
                              double cameraX, double cameraY, double cameraZ) {
        drewReplacementBoxes = false;
        if (BetterCombatClientMod.config == null
                || !BetterCombatClientMod.config.isDebugOBBEnabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || !(client instanceof MinecraftClient_BetterCombat combat)) {
            return;
        }
        if (!client.getEntityRenderDispatcher().shouldRenderHitBoxes()
                || player.getMainHandItem().isEmpty()) {
            return;
        }
        AttackHand hand = PlayerAttackHelper.getCurrentAttack(player, combat.getComboCount());
        if (hand == null || hand.attributes() == null) {
            return;
        }
        WeaponAttributes.Attack attack = hand.attack();
        PendingClaims.beginPreview();
        SequenceSlots.beginPreview(hand.combo() == null ? ClientSwing.NO_SLOT : hand.combo().current() - 1);
        try {
            draw(poses, buffers, player, combat, hand, attack, new Vec3(cameraX, cameraY, cameraZ));
        } finally {
            SequenceSlots.endPreview();
            PendingClaims.endPreview();
        }
    }

    private static void draw(PoseStack poses, MultiBufferSource.BufferSource buffers,
                             LocalPlayer player, MinecraftClient_BetterCombat combat,
                             AttackHand hand, WeaponAttributes.Attack attack, Vec3 camera) {
        List<ExtraAttack> hits = PendingExtraHit.hitsOf(player, attack);
        double range = PlayerAttackHelper.getRangeForItem(player, hand.itemStack())
                * attack.rangeMultiplier();
        AttackOverride parent = AttackShaping.overrideFor(player, attack);
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (int index = 0; index < hits.size(); index++) {
            AttackOverride shape = hits.get(index).shapeFrom(parent);
            TargetFinder.TargetResult result = resultFor(player, combat, attack, shape, range);
            if (result == null || result.obb == null) {
                continue;
            }
            List<Entity> struck = result.entities == null ? List.of() : result.entities;
            float[] idle = COLOURS[index % COLOURS.length];
            HitboxSet boxes = shape.behaviourOrDefault().boxesOrNone();
            if (boxes.isEmpty()) {
                outline(poses, lines, result.obb.copy().offset(camera.reverse()).updateVertex(),
                        struck.isEmpty() ? idle : HIT_COLOUR);
                continue;
            }
            drewReplacementBoxes = true;
            for (HitboxSet.Shape box : boxes.shapes()) {
                outlineShape(poses, lines, player, result.obb, box, struck, idle, camera);
            }
        }

        HitboxSet own = AttackShaping.behaviourFor(player, attack).boxesOrNone();
        if (own.staggered()) {
            own = own.atDelay(HitboxClaimPayload.FIRST_STEP);
        }
        if (own.isEmpty()) {
            return;
        }
        TargetFinder.TargetResult base = TargetFinder.findAttackTargetResult(
                player, combat.getCursorTarget(), attack, range);
        if (base == null || base.obb == null) {
            return;
        }
        drewReplacementBoxes = true;
        List<Entity> struck = base.entities == null ? List.of() : base.entities;
        for (HitboxSet.Shape shape : own.shapes()) {
            outlineShape(poses, lines, player, base.obb, shape, struck, OWN_COLOUR, camera);
        }
    }

    private static void outlineShape(PoseStack poses, VertexConsumer lines, LocalPlayer player,
                                     OrientedBoundingBox base, HitboxSet.Shape shape,
                                     List<Entity> struck, float[] idle, Vec3 camera) {
        OrientedBoundingBox box = HitboxGeometry.shaped(base, player, shape);
        boolean hit = false;
        for (Entity target : struck) {
            if (box.intersects(target.getBoundingBox())) {
                hit = true;
                break;
            }
        }
        outline(poses, lines, box.copy().offset(camera.reverse()).updateVertex(),
                hit ? HIT_COLOUR : idle);
    }

    private static TargetFinder.TargetResult resultFor(LocalPlayer player,
                                                       MinecraftClient_BetterCombat combat,
                                                       WeaponAttributes.Attack attack,
                                                       AttackOverride shape, double range) {
        ExtraAttackScope.push(shape.behaviourOrDefault());
        try {
            return TargetFinder.findAttackTargetResult(
                    player, combat.getCursorTarget(), attack, range);
        } finally {
            ExtraAttackScope.pop();
        }
    }

    private static void outline(PoseStack poses, VertexConsumer lines,
                                OrientedBoundingBox box, float[] colour) {
        edge(poses, lines, box.vertex1, box.vertex2, colour);
        edge(poses, lines, box.vertex2, box.vertex3, colour);
        edge(poses, lines, box.vertex3, box.vertex4, colour);
        edge(poses, lines, box.vertex4, box.vertex1, colour);

        edge(poses, lines, box.vertex5, box.vertex6, colour);
        edge(poses, lines, box.vertex6, box.vertex7, colour);
        edge(poses, lines, box.vertex7, box.vertex8, colour);
        edge(poses, lines, box.vertex8, box.vertex5, colour);

        edge(poses, lines, box.vertex1, box.vertex5, colour);
        edge(poses, lines, box.vertex2, box.vertex6, colour);
        edge(poses, lines, box.vertex3, box.vertex7, colour);
        edge(poses, lines, box.vertex4, box.vertex8, colour);
    }

    private static void edge(PoseStack poses, VertexConsumer lines,
                             Vec3 from, Vec3 to, float[] colour) {
        if (from == null || to == null) {
            return;
        }
        Vec3 direction = to.subtract(from).normalize();
        lines.addVertex(poses.last(), (float) from.x, (float) from.y, (float) from.z)
                .setColor(colour[0], colour[1], colour[2], ALPHA)
                .setNormal(poses.last(), (float) direction.x, (float) direction.y,
                        (float) direction.z);
        lines.addVertex(poses.last(), (float) to.x, (float) to.y, (float) to.z)
                .setColor(colour[0], colour[1], colour[2], ALPHA)
                .setNormal(poses.last(), (float) direction.x, (float) direction.y,
                        (float) direction.z);
    }
}
