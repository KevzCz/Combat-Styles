package net.pixeldreamstudios.combat_styles.fabric;

import net.fabricmc.api.ModInitializer;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;

public class CombatStylesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CombatStyles.init();
        NetworkManager.register();
    }
}
