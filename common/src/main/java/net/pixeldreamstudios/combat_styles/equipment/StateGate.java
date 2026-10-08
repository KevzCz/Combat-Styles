package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;
import net.pixeldreamstudios.combat_styles.entity.SummonRoster;
import net.pixeldreamstudios.combat_styles.platform.WeaponRegistryAccess;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record StateGate(Optional<Boolean> sprinting, Optional<Boolean> airborne,
                        Optional<Boolean> behindTarget, Optional<Boolean> night,
                        Optional<Integer> damagedWithin, Optional<Boolean> hasSpell,
                        Optional<Boolean> castingSpell, Optional<Boolean> crouching,
                        Optional<Boolean> dualWielding, Optional<Boolean> twoHanded,
                        ItemGate items, List<String> playerTags,
                        Optional<Integer> playerTagsMin, List<String> missingPlayerTags,
                        Optional<Integer> summonsActive, List<String> heldSpellSchools) {

    public static final String PLAYER_TAGS = "player_tags";
    public static final String PLAYER_TAGS_MIN = "player_tags_min";
    public static final String MISSING_PLAYER_TAGS = "missing_player_tags";
    public static final String SUMMONS_ACTIVE = "summons_active";
    public static final String HAS_SPELL_SCHOOL = "has_spell_school";

    public static final long NIGHT_START = 13000L;
    public static final long NIGHT_END = 23000L;
    private static final long DAY_LENGTH = 24000L;
    private static final double BEHIND_DOT = -0.2D;

    public static final StateGate ANY = new StateGate(Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), ItemGate.NONE,
            List.of(), Optional.empty(), List.of(), Optional.empty(), List.of());

    public static final MapCodec<StateGate> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("sprinting").forGetter(StateGate::sprinting),
                    Codec.BOOL.optionalFieldOf("airborne").forGetter(StateGate::airborne),
                    Codec.BOOL.optionalFieldOf("behind_target")
                            .forGetter(StateGate::behindTarget),
                    Codec.BOOL.optionalFieldOf("night").forGetter(StateGate::night),
                    Codec.INT.optionalFieldOf("damaged_within")
                            .forGetter(StateGate::damagedWithin),
                    Codec.BOOL.optionalFieldOf("has_spell").forGetter(StateGate::hasSpell),
                    Codec.BOOL.optionalFieldOf("casting_spell")
                            .forGetter(StateGate::castingSpell),
                    Codec.BOOL.optionalFieldOf("crouching").forGetter(StateGate::crouching),
                    Codec.BOOL.optionalFieldOf("dual_wielding").forGetter(StateGate::dualWielding),
                    Codec.BOOL.optionalFieldOf("two_handed").forGetter(StateGate::twoHanded),
                    ItemGate.CODEC.optionalFieldOf("items", ItemGate.NONE).forGetter(StateGate::items),
                    Codec.STRING.listOf().optionalFieldOf(PLAYER_TAGS, List.of())
                            .forGetter(StateGate::playerTags),
                    Codec.INT.optionalFieldOf(PLAYER_TAGS_MIN).forGetter(StateGate::playerTagsMin),
                    Codec.STRING.listOf().optionalFieldOf(MISSING_PLAYER_TAGS, List.of())
                            .forGetter(StateGate::missingPlayerTags),
                    Codec.INT.optionalFieldOf(SUMMONS_ACTIVE).forGetter(StateGate::summonsActive),
                    Codec.STRING.listOf().optionalFieldOf(HAS_SPELL_SCHOOL, List.of())
                            .forGetter(StateGate::heldSpellSchools)
            ).apply(instance, StateGate::new));

    public StateGate {
        items = items == null ? ItemGate.NONE : items;
        playerTags = meaningful(playerTags);
        playerTagsMin = playerTagsMin == null ? Optional.empty() : playerTagsMin;
        missingPlayerTags = meaningful(missingPlayerTags);
        summonsActive = summonsActive == null ? Optional.empty() : summonsActive;
        heldSpellSchools = meaningful(heldSpellSchools);
    }

    public int tagsNeeded() {
        return playerTagsMin.map(min -> Math.max(1, Math.min(min, playerTags.size())))
                .orElse(playerTags.size());
    }

    private static List<String> meaningful(List<String> tags) {
        return tags == null
                ? List.of()
                : tags.stream().filter(tag -> tag != null && !tag.isBlank())
                        .map(String::trim).toList();
    }

    public boolean isEmpty() {
        return sprinting.isEmpty() && airborne.isEmpty() && behindTarget.isEmpty()
                && night.isEmpty() && damagedWithin.isEmpty() && hasSpell.isEmpty()
                && castingSpell.isEmpty() && crouching.isEmpty() && dualWielding.isEmpty() && twoHanded.isEmpty()
                && items.isEmpty() && playerTags.isEmpty() && missingPlayerTags.isEmpty()
                && summonsActive.isEmpty() && heldSpellSchools.isEmpty();
    }

    public boolean needsTarget() {
        return behindTarget.isPresent();
    }

    public StateGate withoutTarget() {
        return behindTarget.isEmpty()
                ? this
                : new StateGate(sprinting, airborne, Optional.empty(), night, damagedWithin,
                        hasSpell, castingSpell, crouching, dualWielding, twoHanded, items,
                        playerTags, playerTagsMin, missingPlayerTags, summonsActive, heldSpellSchools);
    }

    public String rejects(Player player, LivingEntity target) {
        return rejects(player, target, true);
    }

    public String rejectsAtRest(Player player) {
        return rejectsLive(player, null);
    }

    public String rejectsLive(Player player, LivingEntity target) {
        return rejects(player, target, false);
    }

    private String rejects(Player player, LivingEntity target, boolean atSwing) {
        if (isEmpty() || player == null) {
            return null;
        }
        boolean sprintingState = atSwing ? sprintingNow(player) : player.isSprinting();
        if (sprinting.isPresent() && sprintingState != sprinting.get()) {
            return "sprinting";
        }
        if (airborne.isPresent() && player.onGround() == airborne.get()) {
            return "airborne";
        }
        if (crouching.isPresent() && player.isShiftKeyDown() != crouching.get()) {
            return "crouching";
        }
        if (behindTarget.isPresent() && behindTarget.get()
                && (target == null || !behind(player, target))) {
            return "behind_target";
        }
        if (damagedWithin.isPresent()
                && !CombatState.damagedWithin(player, damagedWithin.get())) {
            return "damaged_within";
        }
        if (night.isPresent() && nightNow(player) != night.get()) {
            return "night";
        }
        if (hasSpell.isPresent()
                && SpellEngineCompat.hasSpell(player.getMainHandItem()) != hasSpell.get()) {
            return "has_spell";
        }
        if (!heldSpellSchools.isEmpty() && !holdsSpellSchool(player)) {
            return HAS_SPELL_SCHOOL;
        }
        if (castingSpell.isPresent()
                && SpellEngineCompat.isCasting(player) != castingSpell.get()) {
            return "casting_spell";
        }
        if (dualWielding.isPresent()
                && PlayerAttackHelper.isDualWielding(player) != dualWielding.get()) {
            return "dual_wielding";
        }
        if (twoHanded.isPresent() && !twoHandedMatches(player, twoHanded.get())) {
            return "two_handed";
        }
        if (!playerTags.isEmpty() || !missingPlayerTags.isEmpty()) {
            Set<String> owned = PlayerTags.of(player);
            long held = playerTags.stream().filter(owned::contains).count();
            if (held < tagsNeeded()) {
                return PLAYER_TAGS + " (has " + held + " of " + tagsNeeded() + ")";
            }
            for (String tag : missingPlayerTags) {
                if (owned.contains(tag)) {
                    return MISSING_PLAYER_TAGS + " (has " + tag + ")";
                }
            }
        }
        if (summonsActive.isPresent()
                && SummonRoster.count(player, true) < summonsActive.get()) {
            return SUMMONS_ACTIVE + " (has " + SummonRoster.count(player, true) + ")";
        }
        return items.rejects(player);
    }

    public boolean matches(Player player, LivingEntity target) {
        return rejects(player, target) == null;
    }

    private boolean holdsSpellSchool(Player player) {
        for (ResourceLocation spell : SpellEngineCompat.spellsOn(player.getMainHandItem())) {
            if (SpellMatch.inSchool(heldSpellSchools, SpellEngineCompat.schoolOf(spell, player.level()))) {
                return true;
            }
        }
        return false;
    }

    public static boolean twoHandedMatches(Player player, boolean wanted) {
        WeaponAttributes attributes = WeaponRegistryAccess.rawAttributesFor(player.getMainHandItem());
        return attributes != null && attributes.isTwoHanded() == wanted;
    }

    public static boolean sprintingNow(Player player) {
        return player.isSprinting() || CombatState.sprintingAtSwing(player);
    }

    public static boolean nightNow(Player player) {
        long time = player.level().getDayTime() % DAY_LENGTH;
        return time >= NIGHT_START && time <= NIGHT_END;
    }

    public static boolean behind(Player player, LivingEntity target) {
        Vec3 facing = target.getLookAngle().normalize();
        Vec3 toAttacker = player.position().subtract(target.position()).normalize();
        return facing.dot(toAttacker) < BEHIND_DOT;
    }
}
