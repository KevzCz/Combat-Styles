package net.pixeldreamstudios.combat_styles.affix;

import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;

import java.util.ArrayList;
import java.util.List;

public record SwingContext(int slotIndex, int slotCount, String previousStyleId,
                           List<String> precedingStyleIds, String weaponCategory,
                           int hitsLanded, boolean sameTarget) {

    public static final SwingContext UNKNOWN = new SwingContext(-1, 0, null, List.of(),
            null, 0, false);

    public static SwingContext of(List<StyleEntry> entries, int index) {
        return of(entries, index, entries == null ? 0 : entries.size());
    }

    public static SwingContext of(List<StyleEntry> entries, int index, int usableCount) {
        if (entries == null || entries.isEmpty() || index < 0 || index >= entries.size()) {
            return UNKNOWN;
        }
        String previous = index == 0
                ? entries.getLast().styleId()
                : entries.get(index - 1).styleId();

        List<String> preceding = new ArrayList<>();
        for (int i = 0; i < index; i++) {
            preceding.add(entries.get(i).styleId());
        }
        return new SwingContext(index, usableCount <= 0 ? entries.size() : usableCount,
                previous, List.copyOf(preceding), null, 0, false);
    }

    public SwingContext withWeaponCategory(String category) {
        return new SwingContext(slotIndex, slotCount, previousStyleId, precedingStyleIds,
                category, hitsLanded, sameTarget);
    }

    public SwingContext withHistory(int hits, boolean repeatedTarget) {
        return new SwingContext(slotIndex, slotCount, previousStyleId, precedingStyleIds,
                weaponCategory, hits, repeatedTarget);
    }

    public List<String> precedingCategories() {
        List<String> categories = new ArrayList<>();
        for (String styleId : precedingStyleIds) {
            StylePage page = StyleRegistry.get(styleId);
            if (page != null) {
                categories.add(page.category());
            }
        }
        return categories;
    }

    public boolean isKnown() {
        return slotIndex >= 0 && slotCount > 0;
    }

    public boolean isFirst() {
        return slotIndex == 0;
    }

    public boolean isLast() {
        return slotCount > 0 && slotIndex == slotCount - 1;
    }

    public String previousCategory() {
        StylePage page = StyleRegistry.get(previousStyleId);
        return page == null ? null : page.category();
    }
}
