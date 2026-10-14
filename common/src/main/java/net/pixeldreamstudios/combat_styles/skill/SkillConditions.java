package net.pixeldreamstudios.combat_styles.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.combat_styles.api.CombatStylesAPI;
import net.pixeldreamstudios.combat_styles.api.SummonAPI;
import net.pixeldreamstudios.combat_styles.compat.SpellEngineCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SkillConditions {

    @FunctionalInterface
    public interface Condition {
        boolean test(ServerPlayer player);
    }

    public static final Condition ALWAYS = player -> true;

    private static final String KEY_TYPE = "type";
    private static final String KEY_CONDITIONS = "conditions";
    private static final String KEY_CONDITION = "condition";
    private static final String KEY_EFFECT = "effect";
    private static final String KEY_VALUE = "value";
    private static final String KEY_SPELL = "spell";
    private static final String KEY_RADIUS = "radius";
    private static final String KEY_COUNT = "count";
    private static final String KEY_SCHOOL = "school";
    private static final String KEY_CHOICE = "choice";
    private static final String KEY_OPTION = "option";
    private static final String KEY_COUNTER = "counter";
    private static final String KEY_MIN = "min";
    private static final String KEY_WEAPON_CATEGORY = "weapon_category";
    private static final String KEY_ITEM_TAG = "item_tag";
    private static final String KEY_TAG = "tag";
    private static final String TAG_PREFIX = "#";
    private static final int DEFAULT_COUNT = 1;
    private static final double DEFAULT_RADIUS = 8.0D;

    private SkillConditions() {
    }

    public static Condition parse(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return ALWAYS;
        }
        if (element.isJsonArray()) {
            return allOf(parseList(element.getAsJsonArray()));
        }
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("A condition must be an object or a list of conditions");
        }
        JsonObject object = element.getAsJsonObject();
        String type = string(object, KEY_TYPE).toLowerCase(Locale.ROOT);
        return switch (type) {
            case "all_of" -> allOf(parseList(array(object, KEY_CONDITIONS)));
            case "any_of" -> anyOf(parseList(array(object, KEY_CONDITIONS)));
            case "not" -> negate(parse(object.get(KEY_CONDITION)));
            case "has_effect" -> hasEffect(effect(string(object, KEY_EFFECT)));
            case "health_below" -> healthBelow(number(object, KEY_VALUE));
            case "health_above" -> healthAbove(number(object, KEY_VALUE));
            case "casting" -> casting(optionalString(object, KEY_SPELL));
            case "allies_within" -> alliesWithin(radius(object), count(object));
            case "pets_within" -> petsWithin(radius(object), count(object));
            case "summons" -> summons(count(object));
            case "enemy_with_effect" -> enemyWithEffect(effect(string(object, KEY_EFFECT)), radius(object));
            case "last_school" -> lastSchool(string(object, KEY_SCHOOL));
            case "choice" -> choice(string(object, KEY_CHOICE), string(object, KEY_OPTION));
            case "counter" -> counter(string(object, KEY_COUNTER), (int) number(object, KEY_MIN));
            case "tag" -> tag(string(object, KEY_TAG));
            case "holding" -> holding(optionalString(object, KEY_WEAPON_CATEGORY), optionalString(object, KEY_ITEM_TAG));
            default -> throw new IllegalArgumentException("Unknown condition type " + type);
        };
    }

    private static List<Condition> parseList(JsonArray array) {
        List<Condition> conditions = new ArrayList<>();
        for (JsonElement entry : array) {
            conditions.add(parse(entry));
        }
        return conditions;
    }

    private static Condition allOf(List<Condition> conditions) {
        return player -> {
            for (Condition condition : conditions) {
                if (!condition.test(player)) {
                    return false;
                }
            }
            return true;
        };
    }

    private static Condition anyOf(List<Condition> conditions) {
        return player -> {
            for (Condition condition : conditions) {
                if (condition.test(player)) {
                    return true;
                }
            }
            return false;
        };
    }

    private static Condition negate(Condition condition) {
        return player -> !condition.test(player);
    }

    private static Condition hasEffect(Holder<MobEffect> effect) {
        return player -> player.hasEffect(effect);
    }

    private static Condition healthBelow(double fraction) {
        return player -> player.getHealth() < player.getMaxHealth() * fraction;
    }

    private static Condition healthAbove(double fraction) {
        return player -> player.getHealth() > player.getMaxHealth() * fraction;
    }

    private static Condition casting(String spell) {
        return player -> {
            if (!SpellEngineCompat.isLoaded() || !SpellEngineCompat.isCasting(player)) {
                return false;
            }
            if (spell == null) {
                return true;
            }
            ResourceLocation current = SpellEngineCompat.castingSpell(player);
            return current != null && current.toString().equals(spell);
        };
    }

    private static Condition alliesWithin(double radius, int count) {
        return player -> player.level().getEntitiesOfClass(Player.class,
                player.getBoundingBox().inflate(radius),
                other -> other != player && other.isAlive() && !other.isSpectator()).size() >= count;
    }

    private static Condition petsWithin(double radius, int count) {
        return player -> player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                pet -> SkillActions.isPet(pet, player)).size() >= count;
    }

    private static Condition summons(int count) {
        return player -> SummonAPI.summonCount(player) >= count;
    }

    private static Condition enemyWithEffect(Holder<MobEffect> effect, double radius) {
        return player -> !player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity instanceof Enemy && entity.isAlive() && entity.hasEffect(effect)).isEmpty();
    }

    private static Condition lastSchool(String school) {
        String wanted = school.toLowerCase(Locale.ROOT);
        return player -> wanted.equals(SkillEvents.lastSchool(player));
    }

    private static Condition choice(String choice, String option) {
        return player -> option.equals(SkillChoices.selected(player, choice));
    }

    private static Condition counter(String counter, int min) {
        return player -> SkillCounters.get(player, counter) >= min;
    }

    private static Condition tag(String tag) {
        return player -> player.getTags().contains(tag);
    }

    private static Condition holding(String weaponCategory, String itemTag) {
        TagKey<Item> tag = itemTag == null ? null
                : TagKey.create(Registries.ITEM, ResourceLocation.parse(itemTag.startsWith(TAG_PREFIX)
                ? itemTag.substring(TAG_PREFIX.length()) : itemTag));
        return player -> {
            ItemStack held = player.getMainHandItem();
            if (held.isEmpty()) {
                return false;
            }
            if (tag != null && !held.is(tag)) {
                return false;
            }
            return weaponCategory == null || weaponCategory.equals(CombatStylesAPI.weaponCategory(held));
        };
    }

    public static Holder<MobEffect> effect(String id) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown effect " + id));
    }

    public static Holder<Attribute> attribute(String id) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown attribute " + id));
    }

    private static double radius(JsonObject object) {
        return object.has(KEY_RADIUS) ? number(object, KEY_RADIUS) : DEFAULT_RADIUS;
    }

    private static int count(JsonObject object) {
        return object.has(KEY_COUNT) ? (int) number(object, KEY_COUNT) : DEFAULT_COUNT;
    }

    static String string(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing \"" + key + "\"");
        }
        return object.get(key).getAsString();
    }

    static String optionalString(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
    }

    static double number(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing number \"" + key + "\"");
        }
        return object.get(key).getAsDouble();
    }

    static JsonArray array(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            throw new IllegalArgumentException("Missing list \"" + key + "\"");
        }
        return object.getAsJsonArray(key);
    }
}
