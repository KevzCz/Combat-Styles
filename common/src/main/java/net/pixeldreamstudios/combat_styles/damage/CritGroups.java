package net.pixeldreamstudios.combat_styles.damage;

import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.affix.LibraryRef;
import net.pixeldreamstudios.combat_styles.config.ConfigPaths;
import net.pixeldreamstudios.combat_styles.config.DefinitionLayer;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentDefinition;
import net.pixeldreamstudios.combat_styles.equipment.EquipmentProviders;
import net.pixeldreamstudios.combat_styles.equipment.OwnerScope;

import java.util.List;

public final class CritGroups {

    private static final String LABEL = "crit group";

    public static final DefinitionLayer<ScaledNumber.Crit> DEFINITIONS = new DefinitionLayer<>(LABEL,
            ConfigPaths.CRIT_GROUPS, ScaledNumber.Crit.CODEC, CritGroups::changed);

    private CritGroups() {
    }

    private static void changed() {
    }

    public static ScaledNumber.Crit find(String ref) {
        String name = LibraryRef.nameOf(ref);
        ItemStack owner = OwnerScope.current();
        if (owner != null && !owner.isEmpty()) {
            EquipmentDefinition definition = EquipmentProviders.definitionFor(owner);
            ScaledNumber.Crit local = definition == null ? null : definition.crits().get(name);
            if (local != null) {
                return local;
            }
        }
        return DEFINITIONS.view().get(name);
    }

    public static List<String> globalIds() {
        return List.copyOf(DEFINITIONS.view().keySet());
    }
}
