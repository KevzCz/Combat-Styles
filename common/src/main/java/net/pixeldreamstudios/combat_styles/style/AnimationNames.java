package net.pixeldreamstudios.combat_styles.style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AnimationNames {

    private static final Map<String, String> WEAPONS = new LinkedHashMap<>();
    private static final Map<String, String> PHRASES = new LinkedHashMap<>();
    private static final Map<String, String> TOKENS = new LinkedHashMap<>();

    static {
        WEAPONS.put("greathammer", "Great Hammer");
        WEAPONS.put("meat_shredder", "Meat Shredder");
        WEAPONS.put("battlestaff", "Battlestaff");
        WEAPONS.put("greatsword", "Greatsword");
        WEAPONS.put("doubleaxe", "Twin Axe");
        WEAPONS.put("warglaive", "Warglaive");
        WEAPONS.put("dual_handed", "Dual");
        WEAPONS.put("two_handed", "Two-Handed");
        WEAPONS.put("one_handed", "One-Handed");
        WEAPONS.put("greataxe", "Great Axe");
        WEAPONS.put("claymore", "Claymore");
        WEAPONS.put("er_sword", "Sword");
        WEAPONS.put("chakram", "Chakram");
        WEAPONS.put("halberd", "Halberd");
        WEAPONS.put("trident", "Trident");
        WEAPONS.put("cutlass", "Cutlass");
        WEAPONS.put("katana", "Katana");
        WEAPONS.put("rapier", "Rapier");
        WEAPONS.put("dagger", "Dagger");
        WEAPONS.put("glaive", "Glaive");
        WEAPONS.put("spear", "Spear");
        WEAPONS.put("sword", "Sword");
        WEAPONS.put("claw", "Claw");
        WEAPONS.put("fist", "Fist");
        WEAPONS.put("axe", "Axe");

        PHRASES.put("slash_switch_blade", "Reverse-Grip Slash");
        PHRASES.put("double_cross_slash", "Crossing Flurry");
        PHRASES.put("swipe_horizontal", "Wide Swipe");
        PHRASES.put("slash_horizontal", "Horizontal Slash");
        PHRASES.put("swipe_vertical", "Vertical Swipe");
        PHRASES.put("slash_vertical", "Overhead Cleave");
        PHRASES.put("slash_uncross", "Uncrossing Slash");
        PHRASES.put("chakram_throw", "Chakram Throw");
        PHRASES.put("stab_mounted", "Mounted Thrust");
        PHRASES.put("updown_slash", "Descending Slash");
        PHRASES.put("downup_slash", "Rising Slash");
        PHRASES.put("slash_cross", "Crossing Slash");
        PHRASES.put("double_updown", "Twin Descending");
        PHRASES.put("double_stab", "Double Thrust");
        PHRASES.put("spin_slash", "Spinning Slash");
        PHRASES.put("slamslash", "Slamming Slash");
        PHRASES.put("slam_heavy", "Heavy Slam");
        PHRASES.put("rleg_lead", "Right-Foot Lead");
        PHRASES.put("lleg_lead", "Left-Foot Lead");
        PHRASES.put("double_ii", "Parallel Rake");
        PHRASES.put("double_x", "Cross Rake");
        PHRASES.put("double_u", "Sweeping Rake");
        PHRASES.put("heavy_up", "Heavy Rise");
        PHRASES.put("stabup", "Rising Thrust");
        PHRASES.put("updown", "Descending");
        PHRASES.put("downup", "Rising");
        PHRASES.put("rotate", "Spinning");
        PHRASES.put("hori", "Horizontal");
        PHRASES.put("vert", "Vertical");

        TOKENS.put("rl", "Right-to-Left");
        TOKENS.put("lr", "Left-to-Right");
        TOKENS.put("lrl", "Triple Left");
        TOKENS.put("rlr", "Triple Right");
        TOKENS.put("stab", "Thrust");
        TOKENS.put("poke", "Jab");
        TOKENS.put("kendo", "Kendo Stance");
        TOKENS.put("style", "Flourish");
        TOKENS.put("grounded", "Grounded");
        TOKENS.put("long", "Extended");
        TOKENS.put("up", "Rising");
        TOKENS.put("down", "Downward");
    }

    private AnimationNames() {
    }

    public static String readable(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String work = path.toLowerCase(Locale.ROOT);
        List<String> words = new ArrayList<>();

        for (Map.Entry<String, String> weapon : WEAPONS.entrySet()) {
            String key = weapon.getKey();
            if (work.equals(key) || work.startsWith(key + "_")) {
                words.add(weapon.getValue());
                work = work.substring(key.length());
                break;
            }
        }

        for (Map.Entry<String, String> phrase : PHRASES.entrySet()) {
            if (work.contains(phrase.getKey())) {
                words.add(phrase.getValue());
                work = work.replace(phrase.getKey(), "_");
            }
        }

        for (String part : work.split("_")) {
            if (part.isBlank()) {
                continue;
            }
            words.add(TOKENS.getOrDefault(part, capitalise(part)));
        }

        Set<String> seen = new LinkedHashSet<>(words);
        return String.join(" ", seen).trim();
    }

    private static String capitalise(String word) {
        return word.isEmpty()
                ? word
                : Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}
