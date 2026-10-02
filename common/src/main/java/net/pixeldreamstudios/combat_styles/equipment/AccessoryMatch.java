package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record AccessoryMatch(String slot, ItemMatch match) {

    public static final String SLOT = "slot";
    public static final String ANY_SLOT = "*";

    public static final Codec<AccessoryMatch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf(SLOT, "").forGetter(AccessoryMatch::slot),
            ItemMatch.MAP_CODEC.forGetter(AccessoryMatch::match)
    ).apply(instance, AccessoryMatch::new));

    public AccessoryMatch {
        slot = slot == null ? "" : slot;
        match = match == null ? ItemMatch.NONE : match;
    }

    public boolean isEmpty() {
        return slot.isBlank() || match.isEmpty();
    }

    public boolean anySlot() {
        return ANY_SLOT.equals(slot.trim());
    }

    public List<ItemStack> stacks(Player player) {
        return anySlot()
                ? AccessoryCompat.equippedThisTick(player)
                : AccessoryCompat.inSlot(player, slot);
    }
}
