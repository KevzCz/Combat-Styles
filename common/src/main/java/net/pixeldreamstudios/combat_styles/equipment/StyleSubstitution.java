package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.pixeldreamstudios.combat_styles.style.AttackOverride;

public record StyleSubstitution(List<String> styles, List<Integer> slots, String mode,
                                List<String> weaponCategories, List<String> itemTags,
                                List<Integer> borrowSlots, int lingerTicks,
                                int chargeDecayTicks,
                                Map<String, AttackOverride> tuning,
                                boolean restartCombo,
                                String apply) {

    public StyleSubstitution(List<String> styles, List<Integer> slots, String mode,
                             List<String> weaponCategories, List<String> itemTags,
                             List<Integer> borrowSlots, int lingerTicks,
                             int chargeDecayTicks,
                             Map<String, AttackOverride> tuning) {
        this(styles, slots, mode, weaponCategories, itemTags, borrowSlots, lingerTicks,
                chargeDecayTicks, tuning, false, APPLY_ON_SLOT);
    }

    public StyleSubstitution(List<String> styles, List<Integer> slots, String mode,
                             List<String> weaponCategories, List<String> itemTags,
                             List<Integer> borrowSlots, int lingerTicks,
                             int chargeDecayTicks,
                             Map<String, AttackOverride> tuning,
                             boolean restartCombo) {
        this(styles, slots, mode, weaponCategories, itemTags, borrowSlots, lingerTicks,
                chargeDecayTicks, tuning, restartCombo, APPLY_ON_SLOT);
    }

    public static final String APPLY_ON_SLOT = "on_slot";
    public static final String APPLY_AFTER_SLOT = "after_slot";

    public boolean appliesAfterSlot() {
        return APPLY_AFTER_SLOT.equals(apply) && replacesWholeSequence();
    }

    public static final int DEFAULT_LINGER_TICKS = 200;
    public static final int NO_CHARGE_DECAY = -1;
    public static final int DEFAULT_CHARGE_DECAY_TICKS = 60;

    public boolean holdsCharge() {
        return styles.size() > 1;
    }

    public boolean decays() {
        return chargeDecayTicks != NO_CHARGE_DECAY;
    }

    public int decayTicks() {
        return chargeDecayTicks <= 0 ? DEFAULT_CHARGE_DECAY_TICKS : chargeDecayTicks;
    }

    public static final String MODE_REPLACE_ALL = "replace_all";
    public static final String MODE_REPLACE_SLOT = "replace_slot";
    public static final String MODE_APPEND = "append";
    public static final String MODE_PREPEND = "prepend";
    public static final String MODE_INSERT = "insert";

    private static final Codec<List<String>> STRINGS = Codec.either(
                    Codec.STRING, Codec.STRING.listOf())
            .xmap(either -> either.map(List::of, list -> list),
                    list -> list.size() == 1
                            ? Either.left(list.getFirst())
                            : Either.right(list));

    private static final Codec<List<Integer>> SLOTS = Codec.either(
                    Codec.INT, Codec.INT.listOf())
            .xmap(either -> either.map(List::of, list -> list),
                    list -> list.size() == 1
                            ? Either.left(list.getFirst())
                            : Either.right(list));

    public static final Codec<StyleSubstitution> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    STRINGS.fieldOf("styles").forGetter(StyleSubstitution::styles),
                    SLOTS.optionalFieldOf("slot", List.of()).forGetter(StyleSubstitution::slots),
                    Codec.STRING.optionalFieldOf("mode", MODE_REPLACE_ALL)
                            .forGetter(StyleSubstitution::mode),
                    STRINGS.optionalFieldOf("weapon_categories", List.of())
                            .forGetter(StyleSubstitution::weaponCategories),
                    STRINGS.optionalFieldOf("item_tags", List.of())
                            .forGetter(StyleSubstitution::itemTags),
                    SLOTS.optionalFieldOf("borrow_slots", List.of())
                            .forGetter(StyleSubstitution::borrowSlots),
                    Codec.INT.optionalFieldOf("linger_ticks", DEFAULT_LINGER_TICKS)
                            .forGetter(StyleSubstitution::lingerTicks),
                    Codec.INT.optionalFieldOf("charge_decay_ticks", 0)
                            .forGetter(StyleSubstitution::chargeDecayTicks),
                    Codec.unboundedMap(Codec.STRING, AttackOverride.CODEC)
                            .optionalFieldOf("tuning", Map.of())
                            .forGetter(StyleSubstitution::tuning),
                    Codec.BOOL.optionalFieldOf("restart_combo", Boolean.FALSE)
                            .forGetter(StyleSubstitution::restartCombo),
                    Codec.STRING.optionalFieldOf("apply", APPLY_ON_SLOT)
                            .forGetter(StyleSubstitution::apply)
            ).apply(instance, StyleSubstitution::new));

    public static final StyleSubstitution SAMPLE = new StyleSubstitution(
            List.of(""), List.of(0), MODE_REPLACE_ALL, List.of(""), List.of(""),
            List.of(0), DEFAULT_LINGER_TICKS, NO_CHARGE_DECAY, Map.of());

    public StyleSubstitution {
        styles = styles == null ? List.of() : List.copyOf(styles);
        slots = slots == null ? List.of() : List.copyOf(slots);
        weaponCategories = weaponCategories == null ? List.of() : List.copyOf(weaponCategories);
        itemTags = itemTags == null ? List.of() : List.copyOf(itemTags);
        borrowSlots = borrowSlots == null ? List.of() : List.copyOf(borrowSlots);
        mode = mode == null ? MODE_REPLACE_ALL : mode.toLowerCase(Locale.ROOT);
        apply = apply == null ? APPLY_ON_SLOT : apply.toLowerCase(Locale.ROOT);
        tuning = tuning == null ? Map.of() : Map.copyOf(tuning);
    }

    public boolean replacesWholeSequence() {
        return MODE_REPLACE_ALL.equals(mode);
    }

    public boolean inserts() {
        return MODE_INSERT.equals(mode);
    }

    public boolean keepsOriginalSlots() {
        return MODE_APPEND.equals(mode) || MODE_PREPEND.equals(mode)
                || replacesWholeSequence();
    }

    public int originalSlotOf(int slot, int originalSize) {
        return originalSlotOf(slot, originalSize, 0);
    }

    public int originalSlotOf(int slot, int originalSize, int lead) {
        if (originalSize <= 0 || !keepsOriginalSlots()) {
            return NO_ORIGINAL_SLOT;
        }
        if (replacesWholeSequence()) {
            return slot >= 0 && slot < lead && slot < originalSize
                    ? slot
                    : NO_ORIGINAL_SLOT;
        }
        int index = MODE_PREPEND.equals(mode) ? slot - styles.size() : slot;
        return index >= 0 && index < originalSize ? index : NO_ORIGINAL_SLOT;
    }

    public static final int NO_ORIGINAL_SLOT = -1;

    public boolean isEmpty() {
        return styles.isEmpty() && borrowSlots.isEmpty();
    }

    public static int resolveSlot(int slot, int size) {
        return slot < 0 ? size + slot : slot;
    }

    public boolean targets(int index, int size) {
        if (slots.isEmpty()) {
            return true;
        }
        for (int slot : slots) {
            if (resolveSlot(slot, size) == index) {
                return true;
            }
        }
        return false;
    }
}
