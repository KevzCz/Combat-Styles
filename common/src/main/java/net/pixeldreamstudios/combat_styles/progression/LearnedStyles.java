package net.pixeldreamstudios.combat_styles.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import net.pixeldreamstudios.combat_styles.CombatStyles;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LearnedStyles {

    public static final String NBT_KEY = "CombatStylesLearned";
    private static final String LIST_KEY = "Styles";
    private static final String ID_KEY = "Id";
    private static final String COUNT_KEY = "Count";
    private static final int MAX_COPIES = 64;

    private static final String VARIANTS_KEY = "Variants";
    private static final String ENTRY_KEY = "Entry";

    private final Map<String, Integer> copies = new LinkedHashMap<>();
    private final Map<String, StyleEntry> variants = new LinkedHashMap<>();

    public boolean has(String styleId) {
        return count(styleId) > 0;
    }

    public StyleEntry variant(String key) {
        return key == null ? null : variants.get(key);
    }

    public Map<String, StyleEntry> allVariants() {
        return Map.copyOf(variants);
    }

    public boolean learn(StyleEntry entry) {
        if (entry == null) {
            return false;
        }
        StyleEntry resolved = CustomStyles.resolve(entry);
        String key = resolved.learnedKey();
        if (!learn(key)) {
            return false;
        }
        if (!resolved.isPlain()) {
            variants.put(key, resolved);
        }
        return true;
    }

    public int count(String styleId) {
        if (styleId == null) {
            return 0;
        }
        Integer own = copies.get(styleId);
        return own != null ? own : copies.getOrDefault(StyleRegistry.representativeOf(styleId), 0);
    }

    public boolean learn(String styleId) {
        if (styleId == null) {
            return false;
        }
        int current = copies.getOrDefault(styleId, 0);
        if (current >= MAX_COPIES) {
            return false;
        }
        copies.put(styleId, current + 1);
        return true;
    }

    public boolean forget(String styleId) {
        if (styleId == null) {
            return false;
        }
        Integer current = copies.get(styleId);
        if (current == null) {
            return false;
        }
        if (current <= 1) {
            copies.remove(styleId);
            variants.remove(styleId);
        } else {
            copies.put(styleId, current - 1);
        }
        return true;
    }

    public void clear() {
        copies.clear();
        variants.clear();
    }

    public Map<String, Integer> all() {
        return Map.copyOf(copies);
    }

    public int distinctCount() {
        return copies.size();
    }

    public int distinctStyleCount(boolean includeCustom) {
        return countDistinctStyles(copies, includeCustom);
    }

    public static int countDistinctStyles(Map<String, Integer> copies, boolean includeCustom) {
        Set<String> styles = new HashSet<>();
        for (Map.Entry<String, Integer> entry : copies.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            String key = entry.getKey();
            if (key.startsWith(StyleEntry.CUSTOM_PREFIX)) {
                if (includeCustom) {
                    styles.add(key);
                }
                continue;
            }
            String styleId = StyleEntry.styleIdOfKey(key);
            if (styleId != null && !styleId.isBlank()) {
                styles.add(styleId);
            }
        }
        return styles.size();
    }

    public int totalCopies() {
        int total = 0;
        for (int value : copies.values()) {
            total += value;
        }
        return total;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<String, Integer> entry : copies.entrySet()) {
            CompoundTag element = new CompoundTag();
            element.putString(ID_KEY, entry.getKey());
            element.putInt(COUNT_KEY, entry.getValue());
            list.add(element);
        }
        tag.put(LIST_KEY, list);

        ListTag variantList = new ListTag();
        for (Map.Entry<String, StyleEntry> entry : variants.entrySet()) {
            StyleEntry.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue())
                    .resultOrPartial(error -> CombatStyles.LOGGER.error(
                            "Failed to save learned variant {}: {}", entry.getKey(), error))
                    .ifPresent(encoded -> {
                        CompoundTag element = new CompoundTag();
                        element.putString(ID_KEY, entry.getKey());
                        element.put(ENTRY_KEY, encoded);
                        variantList.add(element);
                    });
        }
        tag.put(VARIANTS_KEY, variantList);
        return tag;
    }

    public void load(CompoundTag tag) {
        copies.clear();
        variants.clear();
        if (tag == null) {
            return;
        }

        ListTag variantList = tag.getList(VARIANTS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < variantList.size(); i++) {
            CompoundTag element = variantList.getCompound(i);
            String key = element.getString(ID_KEY);
            if (key.isBlank()) {
                continue;
            }
            StyleEntry.CODEC.parse(NbtOps.INSTANCE, element.get(ENTRY_KEY))
                    .resultOrPartial(error -> CombatStyles.LOGGER.error(
                            "Failed to load learned variant {}: {}", key, error))
                    .ifPresent(entry -> variants.put(key, entry));
        }
        ListTag legacy = tag.getList(LIST_KEY, Tag.TAG_STRING);
        for (int i = 0; i < legacy.size(); i++) {
            String styleId = legacy.getString(i);
            if (!styleId.isBlank()) {
                copies.put(styleId, 1);
            }
        }
        if (copies.isEmpty()) {
            ListTag list = tag.getList(LIST_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag element = list.getCompound(i);
                String styleId = element.getString(ID_KEY);
                int count = Math.max(1, element.getInt(COUNT_KEY));
                if (!styleId.isBlank()) {
                    copies.put(styleId, Math.min(MAX_COPIES, count));
                }
            }
        }
        migrate();
    }

    public void migrate() {
        migrateVariantKeys();
        migrateCopyKeys();
        variants.keySet().removeIf(key -> copies.getOrDefault(key, 0) <= 0);
    }

    private void migrateVariantKeys() {
        if (variants.isEmpty()) {
            return;
        }
        Map<String, StyleEntry> migrated = new LinkedHashMap<>();
        for (Map.Entry<String, StyleEntry> entry : variants.entrySet()) {
            String stale = entry.getKey();
            StyleEntry resolved = CustomStyles.resolve(entry.getValue());
            String current = resolved.learnedKey();
            migrated.put(current, resolved);
            if (current.equals(stale)) {
                continue;
            }
            Integer owned = copies.remove(stale);
            if (owned != null && owned > 0) {
                copies.merge(current, owned, (a, b) -> Math.min(MAX_COPIES, a + b));
            }
        }
        variants.clear();
        variants.putAll(migrated);
    }

    private void migrateCopyKeys() {
        if (copies.isEmpty()) {
            return;
        }
        Map<String, Integer> migrated = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : copies.entrySet()) {
            migrated.merge(canonicalKey(entry.getKey()), entry.getValue(),
                    (a, b) -> Math.min(MAX_COPIES, a + b));
        }
        copies.clear();
        copies.putAll(migrated);
    }

    private String canonicalKey(String key) {
        if (key.startsWith(StyleEntry.CUSTOM_PREFIX)
                || key.contains(StyleEntry.NAME_SEPARATOR)
                || variants.containsKey(key)) {
            return key;
        }
        String styleId = StyleEntry.styleIdOfKey(key);
        String representative = StyleRegistry.representativeOf(styleId);
        if (representative == null || representative.equals(styleId)) {
            return key;
        }
        return StyleEntry.key(representative, StyleEntry.affixIdsOfKey(key));
    }
}
