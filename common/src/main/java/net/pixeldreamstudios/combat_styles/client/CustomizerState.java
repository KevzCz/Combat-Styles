package net.pixeldreamstudios.combat_styles.client;

import net.bettercombat.api.WeaponAttributes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.affix.AffixRegistry;
import net.pixeldreamstudios.combat_styles.affix.AffixTuning;
import net.pixeldreamstudios.combat_styles.affix.ResolvedAffixes;
import net.pixeldreamstudios.combat_styles.affix.StyleAffix;
import net.pixeldreamstudios.combat_styles.config.CombatStylesConfig;
import net.pixeldreamstudios.combat_styles.style.AttackOverride;
import net.pixeldreamstudios.combat_styles.style.BudgetSources;
import net.pixeldreamstudios.combat_styles.style.CategoryAffinity;
import net.pixeldreamstudios.combat_styles.style.ClientHolder;
import net.pixeldreamstudios.combat_styles.style.CustomStyle;
import net.pixeldreamstudios.combat_styles.style.CustomStyles;
import net.pixeldreamstudios.combat_styles.style.DefaultStyles;
import net.pixeldreamstudios.combat_styles.style.SequenceLimits;
import net.pixeldreamstudios.combat_styles.equipment.SubstitutionScope;
import net.pixeldreamstudios.combat_styles.style.SequenceService;
import net.pixeldreamstudios.combat_styles.style.StyleCost;
import net.pixeldreamstudios.combat_styles.style.StyleEntry;
import net.pixeldreamstudios.combat_styles.style.StylePage;
import net.pixeldreamstudios.combat_styles.style.StyleRegistry;
import net.pixeldreamstudios.combat_styles.style.StyleSequence;
import net.pixeldreamstudios.combat_styles.style.StyleText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CustomizerState {

    public enum Sort {
        NAME("name"),
        DAMAGE("damage"),
        ARC("arc"),
        COST("cost"),
        RARITY("rarity");

        private final String key;

        Sort(String key) {
            this.key = key;
        }

        public String translationKey() {
            return "screen.combat_styles.sort." + key;
        }

        public Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public static final String NATIVE_CATEGORY = "combat_styles:native";

    private final LocalPlayer player;
    private final Set<String> nativeIds = new HashSet<>();
    private final List<StyleEntry> nativeVariants = new ArrayList<>();
    private List<StylePage> nativeNodes = List.of();
    private EquipmentSlot slot = EquipmentSlot.MAINHAND;
    private StyleSequence sequence = StyleSequence.EMPTY;
    private boolean substituted;
    private final Map<String, AttackOverride> attackOverrides = new LinkedHashMap<>();
    private String weaponCategory;
    private String activeCategory;
    private String search = "";
    private Sort sort = Sort.NAME;

    public CustomizerState(LocalPlayer player) {
        this.player = player;
        refresh();
    }

    public void refresh() {
        ItemStack weapon = weapon();
        weaponCategory = SequenceService.categoryOf(weapon);

        List<StylePage> combined = new ArrayList<>();
        nativeIds.clear();
        for (StylePage page : StyleRegistry.nodesOf(SequenceService.baseAttributes(weapon))) {
            if (nativeIds.add(page.styleId())) {
                combined.add(page);
            }
        }
        nativeVariants.clear();
        for (StyleEntry entry : SequenceService.rawBaselineOf(weapon).entries()) {
            StyleEntry resolved = CustomStyles.resolve(entry);
            StylePage page = StyleRegistry.get(resolved.styleId());
            if (page == null) {
                continue;
            }
            if (entry.customStyleId().isPresent()) {
                nativeVariants.add(entry);
            }
            if (!nativeIds.add(page.styleId())) {
                continue;
            }
            nativeIds.add(resolved.styleId());
            combined.add(page);
        }
        nativeNodes = List.copyOf(combined);
        activeCategory = authorMode()
                ? (nativeNodes.isEmpty() ? weaponCategory : NATIVE_CATEGORY)
                : null;

        sequence = SubstitutionScope.withoutSubstitution(
                () -> SequenceService.sequenceOf(weapon, ClientHolder.localId()));
        substituted = SubstitutionScope.activeFor(
                ClientHolder.localPlayer(), weapon) != null;
        attackOverrides.clear();
        attackOverrides.putAll(SequenceService.attackOverridesOf(weapon));
        budgetOverride = BudgetSources.overrideOf(weapon);
        if (sequence.isEmpty()) {
            sequence = defaultSequence();
        }
    }

    private StyleSequence defaultSequence() {
        StyleSequence defined = SequenceService.baselineOf(weapon());
        if (!defined.isEmpty()) {
            return defined;
        }
        if (nativeNodes.isEmpty()) {
            return StyleSequence.EMPTY;
        }
        int limit = Math.min(nativeNodes.size(), maxAttacks());
        List<StyleEntry> entries = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            entries.add(StyleEntry.of(nativeNodes.get(i).styleId()));
        }
        return new StyleSequence("", entries);
    }

    public boolean matchesDefault() {
        return sequence.equals(defaultSequence());
    }

    public boolean isNative(String styleId) {
        return nativeIds.contains(styleId);
    }

    public boolean isNativeVariant(StyleEntry variant) {
        if (variant == null) {
            return false;
        }
        String key = variant.learnedKey();
        for (StyleEntry native_ : nativeVariants) {
            if (native_.learnedKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public List<StylePage> nativeNodes() {
        return nativeNodes;
    }

    public ItemStack weapon() {
        return player.getItemBySlot(slot);
    }

    public EquipmentSlot slot() {
        return slot;
    }

    public void toggleSlot() {
        setSlot(slot == EquipmentSlot.MAINHAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
    }

    public void setSlot(EquipmentSlot target) {
        if (target != EquipmentSlot.MAINHAND && target != EquipmentSlot.OFFHAND) {
            return;
        }
        slot = target;
        refresh();
    }

    public boolean weaponSupported() {
        return SequenceService.isSupported(weapon());
    }

    public boolean substituted() {
        return substituted;
    }

    public StyleSequence sequence() {
        return sequence;
    }

    public void setSequence(StyleSequence next) {
        sequence = next;
    }

    public boolean isLearned(String styleId) {
        return ClientStyleData.hasLearned(styleId);
    }

    public boolean creative() {
        return player.getAbilities().instabuild;
    }

    public boolean authorMode() {
        return creative();
    }

    public boolean ownsPlain(String styleId) {
        if (isNative(styleId) || creative() || !CombatStylesConfig.get().requireLearnedStyles()) {
            return true;
        }
        return ClientStyleData.copies(styleId) > 0;
    }

    public boolean ownsPlainExplicitly(String styleId) {
        if (creative() || !CombatStylesConfig.get().requireLearnedStyles()) {
            return true;
        }
        return ClientStyleData.copies(styleId) > 0;
    }

    public boolean canUse(String styleId) {
        if (isNative(styleId) || creative() || !CombatStylesConfig.get().requireLearnedStyles()) {
            return true;
        }
        return isLearned(styleId);
    }

    public int copiesOwned(String styleId) {
        return copiesOwned(styleId, List.of());
    }

    public int copiesOwned(String styleId, String affixId) {
        return copiesOwned(styleId,
                affixId == null || affixId.isBlank() ? List.of() : List.of(affixId));
    }

    public int copiesOwned(String styleId, List<String> affixIds) {
        return ClientStyleData.copies(StyleEntry.key(styleId, affixIds));
    }

    public boolean isGranted(String styleId, List<String> affixIds) {
        if (creative() || !CombatStylesConfig.get().requireLearnedStyles()) {
            return true;
        }
        return isNative(styleId)
                && (affixIds.isEmpty() || isNativeCombination(styleId, affixIds));
    }

    public int copiesUsed(String styleId) {
        return copiesUsed(styleId, List.of());
    }

    public int copiesUsed(String styleId, String affixId) {
        return copiesUsed(styleId,
                affixId == null || affixId.isBlank() ? List.of() : List.of(affixId));
    }

    public int copiesUsed(String styleId, List<String> affixIds) {
        List<String> wanted = StyleEntry.sorted(affixIds);
        int used = 0;
        for (StyleEntry entry : sequence.entries()) {
            if (entry.styleId().equals(styleId) && entry.affixIds().equals(wanted)) {
                used++;
            }
        }
        return used;
    }

    public boolean hasSpareCopy(String styleId) {
        return hasSpareCopy(styleId, List.of());
    }

    public boolean hasSpareCopy(String styleId, String affixId) {
        return hasSpareCopy(styleId,
                affixId == null || affixId.isBlank() ? List.of() : List.of(affixId));
    }

    public int grantedCopies(String styleId) {
        return SequenceLimits.allowedCopies(weapon(), StyleEntry.of(styleId),
                copiesOwned(styleId, List.of()));
    }

    public int availableCopies(StyleEntry entry) {
        if (entry == null) {
            return 0;
        }
        int owned = copiesOwned(entry.styleId(), entry.affixIds());
        return SequenceLimits.allowedCopies(weapon(), entry, owned);
    }

    public int styleUsed(String styleId) {
        int used = 0;
        for (StyleEntry entry : sequence.entries()) {
            if (entry.customStyleId().isEmpty() && entry.styleId().equals(styleId)) {
                used++;
            }
        }
        return used;
    }

    public boolean hasSpareCopy(String styleId, List<String> affixIds) {
        if (creative() || !CombatStylesConfig.get().requireLearnedStyles()) {
            return true;
        }
        int owned = copiesOwned(styleId, affixIds);
        if (copiesUsed(styleId, affixIds) < owned) {
            return true;
        }
        int granted = SequenceLimits.grantedBy(weapon(), StyleEntry.of(styleId));
        return granted > 0 && styleUsed(styleId) < granted;
    }

    private boolean isNativeCombination(String styleId, List<String> affixIds) {
        for (StyleEntry entry : SequenceService.baselineOf(weapon()).entries()) {
            StylePage page = StyleRegistry.get(entry.styleId());
            String resolved = page == null ? entry.styleId() : page.styleId();
            if ((resolved.equals(styleId) || entry.styleId().equals(styleId))
                    && StyleEntry.sorted(entry.affixIds()).equals(StyleEntry.sorted(affixIds))) {
                return true;
            }
        }
        return false;
    }

    public List<List<String>> learnedCombinationsFor(String styleId, StyleEntry current) {
        List<List<String>> combinations = learnedCombinationsFor(styleId);
        if (current != null) {
            List<String> owned = StyleEntry.sorted(current.affixIds());
            if (!owned.isEmpty() && !combinations.contains(owned)
                    && !ResolvedAffixes.of(owned).affixes().isEmpty()) {
                combinations.add(1, owned);
            }
        }
        return combinations;
    }

    public List<List<String>> learnedCombinationsFor(String styleId) {
        List<List<String>> combinations = new ArrayList<>();
        combinations.add(List.of());

        if (creative() || !CombatStylesConfig.get().requireLearnedStyles()) {
            for (StyleAffix affix : AffixRegistry.applicableTo(StyleRegistry.get(styleId))) {
                combinations.add(List.of(affix.serializedId()));
            }
            return combinations;
        }

        for (StyleEntry entry : SequenceService.baselineOf(weapon()).entries()) {
            StylePage page = StyleRegistry.get(entry.styleId());
            String resolved = page == null ? entry.styleId() : page.styleId();
            if (!resolved.equals(styleId)) {
                continue;
            }
            List<String> ids = StyleEntry.sorted(entry.affixIds());
            if (!ids.isEmpty() && !combinations.contains(ids)) {
                combinations.add(ids);
            }
        }

        for (String key : ClientStyleData.all().keySet()) {
            if (!StyleEntry.styleIdOfKey(key).equals(styleId)) {
                continue;
            }
            List<String> ids = StyleEntry.affixIdsOfKey(key);
            if (!ids.isEmpty() && !combinations.contains(ids)
                    && ResolvedAffixes.of(ids).affixes().size() == ids.size()) {
                combinations.add(ids);
            }
        }
        return combinations;
    }

    public boolean isFull() {
        return sequence.size() >= maxAttacks();
    }

    public int maxAttacks() {
        return DefaultStyles.slotLimitFor(weapon());
    }

    private Optional<Double> budgetOverride = Optional.empty();

    public Optional<Double> budgetOverride() {
        return budgetOverride;
    }

    public void setBudgetOverride(Double value) {
        budgetOverride = value == null || value < 0.0D
                ? Optional.empty()
                : Optional.of(value);
    }

    public double budget() {
        return budgetOverride
                .map(value -> BudgetSources.totalWithOverride(player, weapon(), value))
                .orElseGet(() -> BudgetSources.totalFor(player, weapon()));
    }

    public double spent() {
        return StyleCost.totalOf(sequence, weaponCategory, nativeIds, attackOverrides);
    }

    public double costAt(int index) {
        return StyleCost.costAt(sequence, index, weaponCategory, nativeIds, attackOverrides);
    }

    public double remaining() {
        return budget() - spent();
    }

    public boolean canAfford(StylePage page) {
        return canAfford(page, null);
    }

    public boolean canAfford(StylePage page, String affixId) {
        if (creative() || !CombatStylesConfig.get().enforceStyleBudget()) {
            return true;
        }
        return spent() + nextCostOf(page, affixId) <= budget() + StyleCost.TOLERANCE;
    }

    public double nextCostOf(StylePage page) {
        return StyleCost.costOfAdding(sequence, page, null);
    }

    public double nextCostOf(StylePage page, String affixId) {
        double base = StyleCost.costOfAdding(sequence, page, affixId);
        if (page == null || isNative(page.styleId())) {
            return base;
        }
        return base * CategoryAffinity.costMultiplier(page.category(), weaponCategory);
    }

    public double nextCostOf(StyleEntry variant, StylePage page) {
        if (variant == null) {
            return nextCostOf(page, null);
        }
        String key = variant.learnedKey();
        int repeat = 0;
        for (StyleEntry entry : sequence.entries()) {
            if (!entry.learnedKey().equals(key) && entry.styleId().equals(variant.styleId())) {
                repeat++;
            }
        }
        double base = StyleCost.of(variant) * StyleCost.repeatMultiplier(repeat);
        if (page == null || isNative(page.styleId())) {
            return base;
        }
        return base * CategoryAffinity.costMultiplier(
                CustomStyles.categoryOf(variant, page), weaponCategory);
    }

    public boolean overBudget() {
        return CombatStylesConfig.get().enforceStyleBudget()
                && !creative()
                && spent() > budget() + StyleCost.TOLERANCE;
    }

    public String weaponCategory() {
        return weaponCategory;
    }

    public void refreshWeaponCategory() {
        weaponCategory = SequenceService.categoryOf(weapon());
    }

    public String activeCategory() {
        return activeCategory;
    }

    public void setActiveCategory(String category) {
        activeCategory = category;
    }

    public String search() {
        return search;
    }

    public void setSearch(String value) {
        search = value == null ? "" : value;
    }

    public Sort sort() {
        return sort;
    }

    public void cycleSort() {
        sort = sort.next();
    }

    public List<String> tabCategories() {
        List<String> tabs = new ArrayList<>();
        if (!nativeNodes.isEmpty()) {
            tabs.add(NATIVE_CATEGORY);
        }
        if (weaponCategory != null && !StyleRegistry.forCategory(weaponCategory).isEmpty()) {
            tabs.add(weaponCategory);
        }
        for (String category : StyleRegistry.categories()) {
            if (!category.equals(weaponCategory)) {
                tabs.add(category);
            }
        }
        return tabs;
    }

    public int categoryCount(String category) {
        if (NATIVE_CATEGORY.equals(category)) {
            return nativeNodes.size();
        }
        return category == null ? StyleRegistry.size() : StyleRegistry.forCategory(category).size();
    }

    public List<StylePage> visiblePages() {
        List<StylePage> source;
        if (NATIVE_CATEGORY.equals(activeCategory)) {
            source = nativeNodes;
        } else if (activeCategory == null) {
            source = StyleRegistry.all();
        } else {
            source = StyleRegistry.forCategory(activeCategory);
        }

        boolean showLocked = CombatStylesConfig.get().showLockedStyles();
        String needle = search.trim().toLowerCase(Locale.ROOT);

        List<StylePage> filtered = new ArrayList<>(source.size());
        for (StylePage page : source) {
            if (!showLocked && !canUse(page.styleId())) {
                continue;
            }
            if (!needle.isEmpty() && !matches(page, needle)) {
                continue;
            }
            filtered.add(page);
        }
        filtered.sort(this::compare);
        return filtered;
    }

    public boolean canUseVariant(StyleEntry variant) {
        if (variant == null) {
            return false;
        }
        if (isNativeVariant(variant)) {
            return true;
        }
        if (creative() || !CombatStylesConfig.get().requireLearnedStyles()) {
            return true;
        }
        return ClientStyleData.copies(variant.learnedKey()) > 0;
    }

    public boolean hasSpareVariant(StyleEntry variant) {
        if (variant == null) {
            return false;
        }
        if (creative() || !CombatStylesConfig.get().requireLearnedStyles()) {
            return true;
        }
        int allowed = SequenceLimits.allowedCopies(weapon(), variant,
                ClientStyleData.copies(variant.learnedKey()));
        return variantUsed(variant) < allowed;
    }

    public int variantCopies(StyleEntry variant) {
        return variant == null ? 0 : ClientStyleData.copies(variant.learnedKey());
    }

    public boolean variantAllowed(StyleEntry variant) {
        if (variant == null) {
            return false;
        }
        if (creative() || !CombatStylesConfig.get().requireLearnedStyles()) {
            return true;
        }
        int allowed = SequenceLimits.allowedCopies(weapon(), variant,
                ClientStyleData.copies(variant.learnedKey()));
        return variantUsed(variant) <= allowed;
    }

    public int variantUsed(StyleEntry variant) {
        if (variant == null) {
            return 0;
        }
        String key = variant.learnedKey();
        int used = 0;
        for (StyleEntry entry : sequence.entries()) {
            if (entry.learnedKey().equals(key)) {
                used++;
            }
        }
        return used;
    }

    public List<StyleEntry> visibleVariants() {
        List<StyleEntry> visible = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String needle = search.trim().toLowerCase(Locale.ROOT);
        for (StyleEntry raw : browsableVariants()) {
            if (!seen.add(raw.learnedKey())) {
                continue;
            }
            StyleEntry variant = CustomStyles.resolve(raw);
            StylePage page = StyleRegistry.get(variant.styleId());
            if (page == null) {
                continue;
            }
            if (NATIVE_CATEGORY.equals(activeCategory) && !isNative(page.styleId())) {
                continue;
            }
            if (activeCategory != null && !NATIVE_CATEGORY.equals(activeCategory)
                    && !page.category().equals(activeCategory)) {
                continue;
            }
            if (!needle.isEmpty() && !variantMatches(variant, page, needle)) {
                continue;
            }
            if (!creative() && CombatStylesConfig.get().requireLearnedStyles()
                    && !isNativeVariant(raw) && variantCopies(variant) <= 0) {
                continue;
            }
            visible.add(variant);
        }
        return visible;
    }

    private List<StyleEntry> browsableVariants() {
        List<StyleEntry> browsable = new ArrayList<>(nativeVariants);
        browsable.addAll(ClientStyleData.variants().values());
        for (CustomStyle style : CustomStyles.all()) {
            browsable.add(style.asEntry());
        }
        return browsable;
    }

    private boolean variantMatches(StyleEntry variant, StylePage page, String needle) {
        return StyleText.name(variant, page).getString().toLowerCase(Locale.ROOT).contains(needle)
                || matches(page, needle);
    }

    private boolean matches(StylePage page, String needle) {
        return StyleText.name(page).getString().toLowerCase(Locale.ROOT).contains(needle)
                || page.styleId().toLowerCase(Locale.ROOT).contains(needle)
                || page.category().toLowerCase(Locale.ROOT).contains(needle);
    }

    private int compare(StylePage a, StylePage b) {
        int usable = Boolean.compare(!canUse(a.styleId()), !canUse(b.styleId()));
        if (usable != 0) {
            return usable;
        }
        return switch (sort) {
            case NAME -> StyleText.name(a).getString().compareToIgnoreCase(StyleText.name(b).getString());
            case DAMAGE -> Double.compare(b.damageMultiplier(), a.damageMultiplier());
            case ARC -> Double.compare(b.angle(), a.angle());
            case COST -> Double.compare(StyleCost.of(a), StyleCost.of(b));
            case RARITY -> Integer.compare(b.rarity().ordinal(), a.rarity().ordinal());
        };
    }

    public int learnedCount() {
        return ClientStyleData.distinctStyles();
    }

    public int totalCount() {
        return StyleRegistry.size();
    }

    public void add(String styleId) {
        add(styleId, null);
    }

    public void add(String styleId, String affixId) {
        if (isFull() || !canUse(styleId) || !hasSpareCopy(styleId, affixId)) {
            return;
        }
        StyleEntry native_ = affixId == null ? nativeEntryOf(styleId) : null;
        if (native_ == null) {
            sequence = sequence.with(styleId, affixId);
            return;
        }
        List<StyleEntry> next = new ArrayList<>(sequence.entries());
        next.add(native_);
        sequence = new StyleSequence(sequence.name(), next);
    }

    public StyleEntry nativeEntryOf(String styleId) {
        if (styleId == null || !isNative(styleId)) {
            return null;
        }
        for (StyleEntry entry : SequenceService.baselineOf(weapon()).entries()) {
            if (entry.customStyleId().isPresent() || !entry.affixIds().isEmpty()) {
                continue;
            }
            StylePage resolved = StyleRegistry.get(entry.styleId());
            String resolvedId = resolved == null ? entry.styleId() : resolved.styleId();
            if (styleId.equals(resolvedId) && !entry.isPlain()) {
                return entry;
            }
        }
        return null;
    }

    public void add(StyleEntry entry) {
        if (entry == null || isFull() || !hasSpareVariant(entry)) {
            return;
        }
        List<StyleEntry> next = new ArrayList<>(sequence.entries());
        next.add(entry);
        sequence = new StyleSequence(sequence.name(), next);
    }

    public List<StyleAffix> ownedAffixesFor(StylePage page) {
        List<StyleAffix> owned = new ArrayList<>();
        for (StyleAffix affix : AffixRegistry.applicableTo(page)) {
            if (creative() || copiesOwned(page.styleId(), affix.serializedId()) > 0) {
                owned.add(affix);
            }
        }
        return owned;
    }

    public void remove(int index) {
        sequence = sequence.without(index);
        reindexOverrides(index, -1);
    }

    private void reindexOverrides(int from, int to) {
        if (attackOverrides.isEmpty()) {
            return;
        }
        List<AttackOverride> ordered = new ArrayList<>();
        int size = Math.max(sequence.size() + 1, attackOverrides.size());
        for (int i = 0; i < size; i++) {
            ordered.add(attackOverrides.get(String.valueOf(i)));
        }

        if (from >= 0 && from < ordered.size()) {
            AttackOverride moved = ordered.remove(from);
            if (to >= 0 && to <= ordered.size()) {
                ordered.add(to, moved);
            }
        }

        attackOverrides.clear();
        for (int i = 0; i < ordered.size() && i < sequence.size(); i++) {
            if (ordered.get(i) != null) {
                attackOverrides.put(String.valueOf(i), ordered.get(i));
            }
        }
    }

    public AttackOverride attackOverrideFor(int index) {
        AttackOverride stored = attackOverrides.get(String.valueOf(index));
        if (stored != null) {
            return stored;
        }
        StyleEntry entry = sequence.entry(index);
        return entry == null ? null : entry.attackOverride().orElse(null);
    }

    public Map<String, AttackOverride> attackOverrides() {
        return Map.copyOf(attackOverrides);
    }

    public void setAttackOverride(int index, AttackOverride override) {
        if (index < 0) {
            return;
        }
        String key = String.valueOf(index);
        if (override == null) {
            attackOverrides.remove(key);
        } else {
            attackOverrides.put(key, override);
        }
    }

    public void rename(int index, String name) {
        StyleEntry entry = sequence.entry(index);
        if (entry == null) {
            return;
        }
        List<StyleEntry> next = new ArrayList<>(sequence.entries());
        next.set(index, entry.withName(name));
        sequence = new StyleSequence(sequence.name(), next);
    }

    public Map<String, AffixTuning> affixTuningAt(int index) {
        StyleEntry entry = sequence.entry(index);
        return entry == null ? Map.of() : entry.affixTuning();
    }

    public void setAffixTuning(int index, Map<String, AffixTuning> tuning) {
        if (index < 0 || index >= sequence.size()) {
            return;
        }
        List<StyleEntry> next = new ArrayList<>(sequence.entries());
        StyleEntry entry = next.get(index);
        next.set(index, entry.withAffixTuning(tuning));
        sequence = new StyleSequence(sequence.name(), next);
    }

    public void setAffixes(int index, List<String> affixIds) {
        if (index < 0 || index >= sequence.size()) {
            return;
        }
        List<StyleEntry> next = new ArrayList<>(sequence.entries());
        StyleEntry entry = next.get(index);
        int cap = Math.max(1, CombatStylesConfig.get().maxAffixesPerSlot());
        List<String> merged = new ArrayList<>(baseAffixesOf(entry));
        for (String id : affixIds) {
            if (merged.size() >= cap) {
                break;
            }
            if (!merged.contains(id)) {
                merged.add(id);
            }
        }
        if (merged.size() > cap) {
            merged = new ArrayList<>(merged.subList(0, cap));
        }
        next.set(index, entry.withAffixes(StyleEntry.sorted(merged)));
        sequence = new StyleSequence(sequence.name(), next);
    }

    private List<String> baseAffixesOf(StyleEntry entry) {
        if (entry == null || entry.customStyleId().isEmpty()) {
            return List.of();
        }
        CustomStyle definition = CustomStyles.get(entry.customStyleId().get());
        return definition == null ? List.of() : definition.entry().affixIds();
    }

    public void move(int from, int to) {
        if (from < 0 || from >= sequence.size() || to < 0 || to >= sequence.size() || from == to) {
            return;
        }
        List<StyleEntry> next = new ArrayList<>(sequence.entries());
        next.add(to, next.remove(from));
        sequence = new StyleSequence(sequence.name(), next);
        reindexOverrides(from, to);
    }

    public void cycleSound(int index, int direction) {
        StyleEntry entry = sequence.entry(index);
        if (entry == null) {
            return;
        }
        StylePage page = StyleRegistry.get(entry.styleId());
        List<WeaponAttributes.Sound> sounds = StyleRegistry.swingSounds();
        if (page == null || sounds.isEmpty()) {
            return;
        }

        String current = entry.soundId().orElse(page.swingSoundId());
        int at = -1;
        for (int i = 0; i < sounds.size(); i++) {
            if (sounds.get(i).id().equals(current)) {
                at = i;
                break;
            }
        }
        int next = Math.floorMod(at + direction, sounds.size());
        String nextId = sounds.get(next).id();
        sequence = sequence.withSound(index,
                nextId.equals(page.swingSoundId()) ? null : nextId);
    }

    public void setSound(int index, String soundId) {
        sequence = sequence.withSound(index, soundId);
    }

    public String soundIdFor(int index) {
        StyleEntry entry = sequence.entry(index);
        if (entry == null) {
            return null;
        }
        AttackOverride override = attackOverrideFor(index);
        if (override != null && override.swingSound().isPresent()) {
            return override.swingSound().get();
        }
        StylePage page = StyleRegistry.get(entry.styleId());
        return entry.soundId().orElse(page == null ? null : page.swingSoundId());
    }

    public void clear() {
        sequence = defaultSequence();
        attackOverrides.clear();
    }
}
