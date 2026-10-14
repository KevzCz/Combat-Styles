package net.pixeldreamstudios.combat_styles.kit;

import dev.architectury.platform.Platform;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.compat.PuffishCompat;
import net.pixeldreamstudios.combat_styles.equipment.AccessoryCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KitHooks {

    public static final String STARTER_KIT_MOD_ID = "starterkit";
    private static final String STARTER_KIT_GIVE_COMMAND = "starterkit give %s";
    private static final int FUNCTION_PERMISSION_LEVEL = 2;
    private static final int MIGRATION_INTERVAL = 20;

    private static final Map<UUID, Boolean> RECHOOSING = new ConcurrentHashMap<>();

    private KitHooks() {
    }

    public static boolean beforeGive(ServerPlayer player, String kit) {
        Boolean givesItems = RECHOOSING.get(player.getUUID());
        if (givesItems == null || kit == null || kit.isBlank()) {
            return false;
        }
        KitHook hook = KitHookLoader.hooks().get(kit);
        boolean skipItems = hook == null ? !givesItems : !(givesItems || hook.rechooseGivesItems());
        if (!skipItems) {
            return false;
        }
        RECHOOSING.remove(player.getUUID());
        if (hook != null) {
            bind(player, hook);
        }
        return true;
    }

    public static void afterGive(ServerPlayer player, String kit) {
        if (kit == null || kit.isBlank()) {
            return;
        }
        RECHOOSING.remove(player.getUUID());
        KitHook hook = KitHookLoader.hooks().get(kit);
        if (hook != null) {
            bind(player, hook);
        }
    }

    public static void tick(MinecraftServer server) {
        Map<String, KitHook> hooks = KitHookLoader.hooks();
        if (hooks.isEmpty() || server.getTickCount() % MIGRATION_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!KitState.isBound(player) && !RECHOOSING.containsKey(player.getUUID())) {
                migrate(server, player, hooks);
            }
        }
    }

    private static void migrate(MinecraftServer server, ServerPlayer player, Map<String, KitHook> hooks) {
        for (KitHook hook : hooks.values()) {
            if (hasGivenAdvancement(server, player, hook)) {
                KitState.markBound(player);
                return;
            }
        }
        List<ItemStack> candidates = new ArrayList<>(AccessoryCompat.equipped(player));
        candidates.addAll(player.getInventory().items);
        candidates.addAll(player.getInventory().offhand);
        candidates.addAll(player.getInventory().armor);
        for (ItemStack stack : candidates) {
            for (KitHook hook : hooks.values()) {
                if (matchesLegacy(stack, hook)) {
                    bind(player, hook);
                    CombatStyles.LOGGER.info("Bound legacy kit {} for {} from their signature item",
                            hook.kit(), player.getGameProfile().getName());
                    return;
                }
            }
        }
    }

    private static boolean hasGivenAdvancement(MinecraftServer server, ServerPlayer player, KitHook hook) {
        for (KitAction action : hook.onGiven()) {
            if (action.type() != KitAction.Type.ADVANCEMENT || !action.mode().equals(KitAction.MODE_GRANT)) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(action.value());
            AdvancementHolder holder = id == null ? null : server.getAdvancements().get(id);
            if (holder != null && player.getAdvancements().getOrStartProgress(holder).isDone()) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesLegacy(ItemStack stack, KitHook hook) {
        if (stack.isEmpty() || hook.legacyCustomData().isEmpty()) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        CompoundTag tag = data.copyTag();
        for (Map.Entry<String, String> entry : hook.legacyCustomData().entrySet()) {
            if (!entry.getValue().equals(tag.getString(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static void bind(ServerPlayer player, KitHook hook) {
        KitState.markBound(player);
        run(player, hook.onGiven());
    }

    public static int reset(ServerPlayer player) {
        int hooks = 0;
        for (KitHook hook : KitHookLoader.hooks().values()) {
            run(player, hook.onReset());
            hooks++;
        }
        PuffishCompat.checkTreesNow(player);
        return hooks;
    }

    public static boolean rechoose(ServerPlayer player, boolean giveItems) {
        if (!Platform.isModLoaded(STARTER_KIT_MOD_ID)) {
            return false;
        }
        reset(player);
        RECHOOSING.put(player.getUUID(), giveItems);
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, String.format(STARTER_KIT_GIVE_COMMAND, player.getGameProfile().getName()));
        return true;
    }

    public static List<String> kits() {
        return new ArrayList<>(KitHookLoader.hooks().keySet());
    }

    public static void forget(UUID playerId) {
        RECHOOSING.remove(playerId);
    }

    private static void run(ServerPlayer player, List<KitAction> actions) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        for (KitAction action : actions) {
            switch (action.type()) {
                case ADVANCEMENT -> advancement(server, player, action);
                case FUNCTION -> function(server, player, action);
                case COMMAND -> server.getCommands().performPrefixedCommand(source(player), action.value());
                case TAG -> {
                    if (action.mode().equals(KitAction.MODE_ADD)) {
                        player.addTag(action.value());
                    } else {
                        player.removeTag(action.value());
                    }
                }
                case PUFFISH -> {
                    ResourceLocation id = ResourceLocation.tryParse(action.value());
                    if (id != null && !PuffishCompat.setCategoryUnlocked(player, id, action.mode().equals(KitAction.MODE_UNLOCK))) {
                        CombatStyles.LOGGER.warn("Starter kit hook wants Puffish category {}, but Puffish Skills is not installed", id);
                    }
                }
            }
        }
    }

    private static void advancement(MinecraftServer server, ServerPlayer player, KitAction action) {
        ResourceLocation id = ResourceLocation.tryParse(action.value());
        AdvancementHolder holder = id == null ? null : server.getAdvancements().get(id);
        if (holder == null) {
            CombatStyles.LOGGER.warn("Starter kit hook names unknown advancement {}", action.value());
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        boolean grant = action.mode().equals(KitAction.MODE_GRANT);
        List<String> criteria = new ArrayList<>();
        (grant ? progress.getRemainingCriteria() : progress.getCompletedCriteria()).forEach(criteria::add);
        for (String criterion : criteria) {
            if (grant) {
                player.getAdvancements().award(holder, criterion);
            } else {
                player.getAdvancements().revoke(holder, criterion);
            }
        }
    }

    private static void function(MinecraftServer server, ServerPlayer player, KitAction action) {
        ResourceLocation id = ResourceLocation.tryParse(action.value());
        if (id == null) {
            return;
        }
        server.getFunctions().get(id).ifPresentOrElse(fn -> server.getFunctions().execute(fn, source(player)),
                () -> CombatStyles.LOGGER.warn("Starter kit hook names unknown function {}", id));
    }

    private static CommandSourceStack source(ServerPlayer player) {
        return player.createCommandSourceStack().withPermission(FUNCTION_PERMISSION_LEVEL).withSuppressedOutput();
    }
}
