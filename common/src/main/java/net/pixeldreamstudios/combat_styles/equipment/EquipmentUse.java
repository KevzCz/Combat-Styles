package net.pixeldreamstudios.combat_styles.equipment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class EquipmentUse {

    private static final Map<UUID, Integer> CHANNELLING = new ConcurrentHashMap<>();

    private static volatile BiConsumer<Player, ItemStack> pressHandler;
    private static volatile BiConsumer<Player, ItemStack> channelHandler;

    public static void setPressHandler(BiConsumer<Player, ItemStack> handler) {
        pressHandler = handler;
    }

    public static void setChannelHandler(BiConsumer<Player, ItemStack> handler) {
        channelHandler = handler;
    }

    public static void pressed(Player player, ItemStack stack) {
        BiConsumer<Player, ItemStack> handler = pressHandler;
        if (handler != null) {
            handler.accept(player, stack);
        }
    }

    public static void channelStarted(Player player, ItemStack stack) {
        BiConsumer<Player, ItemStack> handler = channelHandler;
        if (handler != null) {
            handler.accept(player, stack);
        }
    }

    private EquipmentUse() {
    }

    public static void forget(UUID playerId) {
        CHANNELLING.remove(playerId);
    }

    public static UseSpec specOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || EquipmentRegistry.isEmpty()) {
            return UseSpec.NONE;
        }
        EquipmentDefinition definition = EquipmentProviders.definitionFor(stack);
        return definition == null ? UseSpec.NONE : definition.use();
    }

    public static boolean declaresUse(ItemStack stack) {
        return !specOf(stack).isEmpty();
    }

    public static int durationOf(ItemStack stack) {
        return specOf(stack).duration();
    }

    public static boolean blocked(Player player, ItemStack stack) {
        UseSpec spec = specOf(stack);
        if (spec.isEmpty() || spec.cooldown().ticks() <= 0) {
            return false;
        }
        return player.getCooldowns().isOnCooldown(stack.getItem());
    }

    public static void begin(Player player, ItemStack stack) {
        UseSpec spec = specOf(stack);
        if (spec.isEmpty() || blocked(player, stack)) {
            return;
        }
        if (!spec.channels()) {
            trigger(player, stack, spec, 0, UsePhase.START);
            spend(player, stack, spec);
            return;
        }
        CHANNELLING.put(player.getUUID(), 0);
        trigger(player, stack, spec, 0, UsePhase.START);
    }

    public static void tick(Player player, ItemStack stack, int remaining) {
        UseSpec spec = specOf(stack);
        if (spec.isEmpty() || !spec.channels()) {
            return;
        }
        Integer held = CHANNELLING.get(player.getUUID());
        if (held == null) {
            return;
        }
        int elapsed = held + 1;
        CHANNELLING.put(player.getUUID(), elapsed);
        if (!spec.repeats() || !spec.started(elapsed)) {
            return;
        }
        if ((elapsed - spec.channelStart()) % spec.channelTicks() == 0) {
            trigger(player, stack, spec, elapsed, UsePhase.CHANNEL);
            spend(player, stack, spec);
        }
    }

    public static void release(Player player, ItemStack stack, int remaining) {
        UseSpec spec = specOf(stack);
        Integer held = CHANNELLING.remove(player.getUUID());
        if (spec.isEmpty() || !spec.channels()) {
            return;
        }
        int elapsed = held == null ? 0 : held;
        if (elapsed < spec.required()) {
            return;
        }
        trigger(player, stack, spec, elapsed, UsePhase.RELEASE);
        if (spec.completion() == UsePhase.RELEASE) {
            spend(player, stack, spec);
        }
    }

    private static void trigger(Player player, ItemStack stack, UseSpec spec, int heldTicks,
                                UsePhase phase) {
        EquipmentActivation.used(player, new UseEvent(stack, spec.kind(), heldTicks, phase));
    }

    private static void spend(Player player, ItemStack stack, UseSpec spec) {
        applyCooldown(player, stack, spec);
    }

    private static void applyCooldown(Player player, ItemStack stack, UseSpec spec) {
        int ticks = spec.cooldown().ticks();
        if (ticks <= 0) {
            return;
        }
        Item item = stack.getItem();
        player.getCooldowns().addCooldown(item, ticks);
    }
}
