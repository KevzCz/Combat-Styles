package net.pixeldreamstudios.combat_styles.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.network.NetworkManager;

@Mod(CombatStyles.MOD_ID)
public class CombatStylesNeoForge {

    public CombatStylesNeoForge(IEventBus eventBus, ModContainer container) {
        CombatStyles.init();
        NetworkManager.register();
    }
}
