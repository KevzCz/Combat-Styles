package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.equipment.EquipmentUse;

import java.util.List;

public final class ClientActivationTick {

    private static boolean wasUsing;
    private static ItemStack wasUsingStack = ItemStack.EMPTY;
    private static int heldTicks;
    private static boolean bound;

    private ClientActivationTick() {
    }

    public static void tick() {
        if (!bound) {
            bound = true;
            EquipmentUse.setPressHandler(UseAnimationBinding::pressed);
            EquipmentUse.setChannelHandler(UseAnimationBinding::channelStarted);
        }
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = minecraft == null ? null : minecraft.player;
        if (player == null) {
            wasUsing = false;
            wasUsingStack = ItemStack.EMPTY;
            heldTicks = 0;
            return;
        }
        boolean using = player.isUsingItem();
        boolean released = wasUsing && !using;
        ItemStack releasedStack = wasUsingStack;
        int releasedAfter = heldTicks;
        wasUsing = using;
        wasUsingStack = using ? player.getUseItem() : ItemStack.EMPTY;
        heldTicks = using ? heldTicks + 1 : 0;
        if (released) {
            UseAnimationBinding.release(player, releasedStack, releasedAfter);
        } else {
            UseAnimationBinding.tick(player);
        }
        if (minecraft.level == null) {
            return;
        }
        List<AbstractClientPlayer> others = minecraft.level.players();
        for (AbstractClientPlayer other : others) {
            if (!other.getUUID().equals(player.getUUID())) {
                UseAnimationBinding.tick(other);
            }
        }
    }
}
