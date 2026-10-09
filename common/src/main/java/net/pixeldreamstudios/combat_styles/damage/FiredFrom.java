package net.pixeldreamstudios.combat_styles.damage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public interface FiredFrom {

    String TAG_FIRED_FROM = "FiredFrom";
    String TAG_FIRED_FROM_NOTHING = "FiredFromNothing";

    void combat_styles$setFiredFrom(ItemStack stack);

    static void save(CompoundTag tag, ItemStack firedFrom, HolderLookup.Provider registries) {
        if (firedFrom == null) {
            return;
        }
        if (firedFrom.isEmpty()) {
            tag.putBoolean(TAG_FIRED_FROM_NOTHING, true);
        } else {
            tag.put(TAG_FIRED_FROM, firedFrom.save(registries));
        }
    }

    static ItemStack read(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.getBoolean(TAG_FIRED_FROM_NOTHING)) {
            return ItemStack.EMPTY;
        }
        if (!tag.contains(TAG_FIRED_FROM, Tag.TAG_COMPOUND)) {
            return null;
        }
        return ItemStack.parse(registries, tag.getCompound(TAG_FIRED_FROM)).orElse(null);
    }
}
