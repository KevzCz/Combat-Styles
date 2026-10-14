package net.pixeldreamstudios.combat_styles.skill;

import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

import java.util.Locale;

public record SkillEventFilter(SkillEvents.Type type, String spell, String school, boolean schoolChanged,
                               Holder<MobEffect> targetEffect, double targetHealthBelow, double selfHealthBelow,
                               double selfHealthAbove, String counter, double chance, int every,
                               SkillConditions.Condition condition) {

    private static final String KEY_EVENT = "event";
    private static final String KEY_SPELL = "spell";
    private static final String KEY_SCHOOL = "school";
    private static final String KEY_SCHOOL_CHANGED = "school_changed";
    private static final String KEY_TARGET_EFFECT = "target_has_effect";
    private static final String KEY_TARGET_HEALTH_BELOW = "target_health_below";
    private static final String KEY_SELF_HEALTH_BELOW = "self_health_below";
    private static final String KEY_SELF_HEALTH_ABOVE = "self_health_above";
    private static final String KEY_COUNTER = "counter";
    private static final String KEY_CHANCE = "chance";
    private static final String KEY_EVERY = "every";
    private static final String KEY_CONDITION = "condition";
    private static final double UNSET = -1.0D;
    private static final double ALWAYS = 1.0D;

    public static SkillEventFilter parse(JsonObject object) {
        String event = SkillConditions.string(object, KEY_EVENT);
        SkillEvents.Type type = SkillEvents.Type.byId(event.toLowerCase(Locale.ROOT));
        if (type == null) {
            throw new IllegalArgumentException("Unknown event " + event);
        }
        String effect = SkillConditions.optionalString(object, KEY_TARGET_EFFECT);
        String school = SkillConditions.optionalString(object, KEY_SCHOOL);
        return new SkillEventFilter(type,
                SkillConditions.optionalString(object, KEY_SPELL),
                school == null ? null : school.toLowerCase(Locale.ROOT),
                object.has(KEY_SCHOOL_CHANGED) && object.get(KEY_SCHOOL_CHANGED).getAsBoolean(),
                effect == null ? null : SkillConditions.effect(effect),
                optional(object, KEY_TARGET_HEALTH_BELOW),
                optional(object, KEY_SELF_HEALTH_BELOW),
                optional(object, KEY_SELF_HEALTH_ABOVE),
                SkillConditions.optionalString(object, KEY_COUNTER),
                object.has(KEY_CHANCE) ? SkillConditions.number(object, KEY_CHANCE) : ALWAYS,
                object.has(KEY_EVERY) ? (int) SkillConditions.number(object, KEY_EVERY) : 0,
                object.has(KEY_CONDITION) ? SkillConditions.parse(object.get(KEY_CONDITION)) : SkillConditions.ALWAYS);
    }

    public boolean matches(SkillEvents.Event event) {
        if (event.type() != type) {
            return false;
        }
        if (spell != null && !spell.equals(event.spell())) {
            return false;
        }
        if (school != null && !school.equals(event.school())) {
            return false;
        }
        if (schoolChanged && (event.school() == null
                || event.school().equals(SkillEvents.lastSchool(event.player())))) {
            return false;
        }
        if (targetEffect != null && (event.other() == null || !event.other().hasEffect(targetEffect))) {
            return false;
        }
        if (targetHealthBelow != UNSET && (event.other() == null
                || event.other().getHealth() >= event.other().getMaxHealth() * targetHealthBelow)) {
            return false;
        }
        if (selfHealthBelow != UNSET && event.player().getHealth() >= event.player().getMaxHealth() * selfHealthBelow) {
            return false;
        }
        if (selfHealthAbove != UNSET && event.player().getHealth() <= event.player().getMaxHealth() * selfHealthAbove) {
            return false;
        }
        if (counter != null && !counter.equals(event.counter())) {
            return false;
        }
        if (every > 0 && event.player().level().getGameTime() % every != 0L) {
            return false;
        }
        if (chance < ALWAYS && event.player().getRandom().nextDouble() >= chance) {
            return false;
        }
        return condition.test(event.player());
    }

    private static double optional(JsonObject object, String key) {
        return object.has(key) ? SkillConditions.number(object, key) : UNSET;
    }
}
