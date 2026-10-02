package net.pixeldreamstudios.combat_styles.client;

import net.minecraft.client.Minecraft;

import net.pixeldreamstudios.combat_styles.ServerDataReset;
import net.pixeldreamstudios.combat_styles.config.ConfigSnapshot;

public final class ClientConfigSync {

    private static volatile String received;

    private ClientConfigSync() {
    }

    public static void accept(String json) {
        if (Minecraft.getInstance().isLocalServer()) {
            return;
        }
        if (ConfigSnapshot.acceptRemote(json)) {
            received = json;
        }
    }

    public static void revert() {
        String last = received;
        if (last != null) {
            ConfigSnapshot.acceptRemote(last);
        }
    }

    public static void restoreLocal() {
        if (received == null) {
            return;
        }
        received = null;
        ConfigSnapshot.restoreLocal();
        ServerDataReset.clear();
    }
}
