package net.pixeldreamstudios.combat_styles.compat;

import dev.architectury.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.combat_styles.compat.puffish.PuffishBridge;

import java.util.UUID;

public final class PuffishCompat {

    public static final String MOD_ID = "puffish_skills";

    private static Boolean loaded;

    private PuffishCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = Platform.isModLoaded(MOD_ID);
        }
        return loaded;
    }

    public static void init() {
        if (isLoaded()) {
            PuffishBridge.init();
        }
    }

    public static void tick(MinecraftServer server) {
        if (isLoaded()) {
            PuffishBridge.tick(server);
        }
    }

    public static void checkTreesNow(ServerPlayer player) {
        if (isLoaded()) {
            PuffishBridge.checkTreesNow(player);
        }
    }

    public static boolean setCategoryUnlocked(ServerPlayer player, ResourceLocation categoryId, boolean unlocked) {
        if (!isLoaded()) {
            return false;
        }
        PuffishBridge.setCategoryUnlocked(player, categoryId, unlocked);
        return true;
    }

    public static void forget(UUID playerId) {
        if (isLoaded()) {
            PuffishBridge.forget(playerId);
        }
    }
}
