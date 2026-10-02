package net.pixeldreamstudios.combat_styles.affix;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

public final class DamageTypeMatch {

    private static final String TAG_PREFIX = "#";

    private DamageTypeMatch() {
    }

    public static boolean matches(List<String> accepted, DamageSource source) {
        return source != null && matches(accepted, source.typeHolder());
    }

    public static boolean matches(List<String> accepted, Holder<DamageType> type) {
        if (accepted == null || accepted.isEmpty()) {
            return true;
        }
        if (type == null) {
            return false;
        }
        for (String entry : accepted) {
            if (matchesOne(entry, type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesOne(String entry, Holder<DamageType> type) {
        if (entry == null || entry.isBlank()) {
            return false;
        }
        String trimmed = entry.trim();
        if (trimmed.startsWith(TAG_PREFIX)) {
            ResourceLocation tagId = ResourceLocation.tryParse(trimmed.substring(1));
            return tagId != null && type.is(TagKey.create(Registries.DAMAGE_TYPE, tagId));
        }
        ResourceLocation id = ResourceLocation.tryParse(trimmed);
        return id != null && type.unwrapKey()
                .map(key -> key.location().equals(id))
                .orElse(false);
    }
}
