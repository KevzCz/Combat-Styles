package net.pixeldreamstudios.combat_styles.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import net.pixeldreamstudios.combat_styles.affix.EffectLines;
import net.pixeldreamstudios.combat_styles.custom.Customs;
import net.pixeldreamstudios.combat_styles.item.ItemTooltip;
import net.pixeldreamstudios.combat_styles.item.TooltipGem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record TooltipOverride(String header, List<String> lines, Map<String, String> edits,
                              List<String> order, Map<String, String> detail,
                              Map<String, Integer> indent, Map<String, String> icons) {

    public static final String HIDDEN = "";
    public static final String LINE_PLACEHOLDER = "line";
    public static final String PLACEHOLDER_OPEN = "{";
    public static final String PLACEHOLDER_CLOSE = "}";
    public static final String LINE_TOKEN = PLACEHOLDER_OPEN + LINE_PLACEHOLDER + PLACEHOLDER_CLOSE;
    public static final String LINE_REF = "#";
    public static final String VALUE_REF = ".";
    public static final String ADDED_PREFIX = "+";
    public static final String DETAIL_CTRL = "ctrl";
    public static final String DETAIL_BRIEF = "brief";
    private static final int FIRST_VALUE = 1;
    private static final Pattern PLACEHOLDER = Pattern.compile(
            Pattern.quote(PLACEHOLDER_OPEN) + "(" + LINE_PLACEHOLDER + "|\\d+(?:"
                    + Pattern.quote(VALUE_REF) + "\\d+)*|"
                    + Pattern.quote(LINE_REF) + "\\d+(?:" + Pattern.quote(VALUE_REF) + "\\d+)*)"
                    + Pattern.quote(PLACEHOLDER_CLOSE));
    public static final String HEADER_SECTION = "header";
    public static final String HEADER_KEY = EffectLines.keyOf(HEADER_SECTION, 0);

    public static final TooltipOverride NONE = new TooltipOverride("", List.of(), Map.of(), List.of(), Map.of(), Map.of(), Map.of());

    public static final Codec<Map<String, String>> EDITS_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING);
    public static final Codec<Map<String, Integer>> INDENT_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.INT);
    public static final int MAX_INDENT = 4;

    public static final Codec<TooltipOverride> CODEC = Customs.wrap(TooltipOverride.class, RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf(TriggeredEffect.TOOLTIP_HEADER, "")
                    .forGetter(TooltipOverride::header),
            Codec.STRING.listOf().optionalFieldOf(TriggeredEffect.TOOLTIP_LINES, List.of())
                    .forGetter(TooltipOverride::lines),
            EDITS_CODEC.optionalFieldOf(TriggeredEffect.TOOLTIP_EDITS, Map.of())
                    .forGetter(TooltipOverride::edits),
            Codec.STRING.listOf().optionalFieldOf(TriggeredEffect.TOOLTIP_ORDER, List.of())
                    .forGetter(TooltipOverride::order),
            EDITS_CODEC.optionalFieldOf(TriggeredEffect.TOOLTIP_DETAIL, Map.of())
                    .forGetter(TooltipOverride::detail),
            INDENT_CODEC.optionalFieldOf(TriggeredEffect.TOOLTIP_INDENT, Map.of())
                    .forGetter(TooltipOverride::indent),
            EDITS_CODEC.optionalFieldOf(TriggeredEffect.TOOLTIP_ICON, Map.of())
                    .forGetter(TooltipOverride::icons)
    ).apply(instance, TooltipOverride::new)));

    public interface Rewrite {
        Component apply(String raw, Component original, List<Component> generated, String icon);
    }

    public TooltipOverride(String header, List<String> lines) {
        this(header, lines, Map.of(), List.of(), Map.of(), Map.of(), Map.of());
    }

    public TooltipOverride {
        header = header == null ? "" : header.trim();
        lines = lines == null
                ? List.of()
                : lines.stream().filter(line -> line != null && !line.isBlank()).toList();
        edits = normalized(edits);
        order = order == null
                ? List.of()
                : order.stream().filter(key -> key != null && !key.isBlank()).distinct().toList();
        detail = normalized(detail);
        indent = indent == null ? Map.of() : Map.copyOf(indent);
        icons = normalized(icons);
    }

    public static Map<String, String> normalized(Map<String, String> edits) {
        if (edits == null || edits.isEmpty()) {
            return Map.of();
        }
        Map<String, String> kept = new LinkedHashMap<>();
        edits.forEach((key, value) -> {
            if (key != null && !key.isBlank()) {
                kept.put(key, value == null ? HIDDEN : value.trim());
            }
        });
        return Map.copyOf(kept);
    }

    public boolean isEmpty() {
        return header.isEmpty() && lines.isEmpty() && edits.isEmpty() && order.isEmpty()
                && detail.isEmpty() && indent.isEmpty() && icons.isEmpty();
    }

    public boolean hides(String key) {
        return HIDDEN.equals(edits.get(key));
    }

    public TooltipOverride withHeader(String value) {
        return new TooltipOverride(value, lines, edits, order, detail, indent, icons);
    }

    public TooltipOverride withLines(List<String> value) {
        return new TooltipOverride(header, value, edits, order, detail, indent, icons);
    }

    public TooltipOverride withOrder(List<String> value) {
        return new TooltipOverride(header, lines, edits, value, detail, indent, icons);
    }

    public TooltipOverride withEdit(String key, String value) {
        Map<String, String> next = new LinkedHashMap<>(edits);
        if (value == null) {
            next.remove(key);
        } else {
            next.put(key, value);
        }
        return new TooltipOverride(header, lines, next, order, detail, indent, icons);
    }

    public TooltipOverride withDetail(String key, String value) {
        Map<String, String> next = new LinkedHashMap<>(detail);
        if (value == null) {
            next.remove(key);
        } else {
            next.put(key, value);
        }
        return new TooltipOverride(header, lines, edits, order, next, indent, icons);
    }

    public String iconOf(String key) {
        return icons.get(key);
    }

    public TooltipOverride withIcon(String key, String icon) {
        Map<String, String> next = new LinkedHashMap<>(icons);
        if (icon == null) {
            next.remove(key);
        } else {
            next.put(key, icon);
        }
        return new TooltipOverride(header, lines, edits, order, detail, indent, next);
    }

    public int indentOf(String key) {
        return indent.getOrDefault(key, 0);
    }

    public TooltipOverride withIndent(String key, int level) {
        Map<String, Integer> next = new LinkedHashMap<>(indent);
        int clamped = Math.max(0, Math.min(MAX_INDENT, level));
        if (clamped == 0) {
            next.remove(key);
        } else {
            next.put(key, clamped);
        }
        return new TooltipOverride(header, lines, edits, order, detail, next, icons);
    }

    public String detailOf(String key) {
        return detail.get(key);
    }

    public boolean showsNow(String key) {
        String mode = detail.get(key);
        if (mode == null) {
            return true;
        }
        boolean detailed = ItemTooltip.detailed();
        return DETAIL_CTRL.equals(mode) ? detailed : !DETAIL_BRIEF.equals(mode) || !detailed;
    }

    public static String addedKey(int index) {
        return ADDED_PREFIX + index;
    }

    public List<String> naturalKeys(List<String> generatedKeys) {
        List<String> natural = new ArrayList<>(generatedKeys);
        for (int index = 0; index < lines.size(); index++) {
            natural.add(addedKey(index));
        }
        return natural;
    }

    public List<String> ordered(List<String> natural) {
        List<String> result = new ArrayList<>();
        for (String key : order) {
            if (natural.contains(key) && !result.contains(key)) {
                result.add(key);
            }
        }
        for (int index = 0; index < natural.size(); index++) {
            String key = natural.get(index);
            if (result.contains(key)) {
                continue;
            }
            int at = 0;
            for (int back = index - 1; back >= 0; back--) {
                int found = result.indexOf(natural.get(back));
                if (found >= 0) {
                    at = found + 1;
                    break;
                }
            }
            result.add(at, key);
        }
        return result;
    }

    public List<Component> apply(EffectLines generated, Rewrite rewrite) {
        List<String> keys = generated.keys();
        List<Component> originals = new ArrayList<>(generated);
        Map<String, Component> shown = new LinkedHashMap<>();
        for (int index = 0; index < originals.size(); index++) {
            String key = keys.get(index);
            String edit = edits.get(key);
            String icon = icons.get(key);
            if (edit == null) {
                shown.put(key, icon == null
                        ? originals.get(index)
                        : rewrite.apply(LINE_TOKEN, originals.get(index), originals, icon));
            } else if (!edit.equals(HIDDEN)) {
                shown.put(key, rewrite.apply(edit, originals.get(index), originals, icon));
            }
        }
        for (int index = 0; index < lines.size(); index++) {
            shown.put(addedKey(index), rewrite.apply(lines.get(index), null, originals,
                    icons.get(addedKey(index))));
        }
        List<Component> result = new ArrayList<>(shown.size());
        boolean withheld = false;
        for (String key : ordered(new ArrayList<>(shown.keySet()))) {
            if (!showsNow(key)) {
                withheld |= DETAIL_CTRL.equals(detail.get(key));
                continue;
            }
            result.add(indented(shown.get(key), indentOf(key)));
        }
        if (withheld && !result.isEmpty()) {
            int last = result.size() - 1;
            String mark = ItemTooltip.mark().getString();
            if (!result.get(last).getString().endsWith(mark)) {
                result.set(last, ItemTooltip.marked(result.get(last)));
            }
        }
        return result;
    }

    public static Component indented(Component line, int levels) {
        Component current = line;
        for (int level = 0; level < levels; level++) {
            current = TooltipGem.hang(current);
        }
        return current;
    }

    public static MutableComponent text(String raw) {
        return Component.translatableWithFallback(raw, raw);
    }

    public static MutableComponent filled(String raw, Component original, List<Component> generated) {
        String template = Language.getInstance().getOrDefault(raw, raw);
        Matcher matcher = PLACEHOLDER.matcher(template);
        if (!matcher.find()) {
            return text(raw);
        }
        MutableComponent result = Component.empty();
        int last = 0;
        matcher.reset();
        while (matcher.find()) {
            result.append(template.substring(last, matcher.start()));
            result.append(placeholder(matcher.group(1), matcher.group(), original, generated));
            last = matcher.end();
        }
        return result.append(template.substring(last));
    }

    public static String lineRef(int number) {
        return wrap(lineName(number));
    }

    public static Component bodyOf(Component line) {
        Component found = translatableIn(line);
        return found == null ? line : found;
    }

    public static List<Component> valuesOf(Component line) {
        Component body = translatableIn(line);
        if (body == null || !(body.getContents() instanceof TranslatableContents translatable)) {
            return List.of();
        }
        List<Component> values = new ArrayList<>();
        for (Object argument : translatable.getArgs()) {
            Component value = argument instanceof Component component
                    ? component
                    : Component.literal(String.valueOf(argument));
            values.add(Component.empty().withStyle(body.getStyle()).append(value));
        }
        return values;
    }

    private static Component placeholder(String name, String token, Component original,
                                         List<Component> generated) {
        if (name.startsWith(LINE_REF)) {
            String[] parts = name.substring(LINE_REF.length()).split(Pattern.quote(VALUE_REF));
            int line = Integer.parseInt(parts[0]) - FIRST_VALUE;
            if (generated == null || line < 0 || line >= generated.size()) {
                return Component.literal(token);
            }
            Component target = generated.get(line);
            return parts.length == 1
                    ? bodyOf(target).copy()
                    : valueAt(target, parts, FIRST_VALUE, token);
        }
        if (original == null) {
            return Component.literal(token);
        }
        if (LINE_PLACEHOLDER.equals(name)) {
            return bodyOf(original).copy();
        }
        return valueAt(original, name.split(Pattern.quote(VALUE_REF)), 0, token);
    }

    private static Component valueAt(Component line, String[] path, int from, String token) {
        Component current = line;
        for (int step = from; step < path.length; step++) {
            List<Component> values = valuesOf(current);
            int index = Integer.parseInt(path[step]) - FIRST_VALUE;
            if (index < 0 || index >= values.size()) {
                return Component.literal(token);
            }
            current = values.get(index);
        }
        return current;
    }

    public static String valueName(int index) {
        return String.valueOf(index + FIRST_VALUE);
    }

    public static String lineName(int number) {
        return LINE_REF + number;
    }

    public static String under(String parent, String path) {
        return parent + VALUE_REF + path;
    }

    public static String nestedRef(String parent, int index) {
        return parent + VALUE_REF + (index + FIRST_VALUE);
    }

    public static String wrap(String reference) {
        return PLACEHOLDER_OPEN + reference + PLACEHOLDER_CLOSE;
    }

    public static Component keepingStructure(Component original, Component body) {
        Component target = translatableIn(original);
        if (target == null) {
            return null;
        }
        return replaced(original, target, Component.empty().withStyle(target.getStyle()).append(body));
    }

    private static Component replaced(Component node, Component target, Component replacement) {
        if (node == target) {
            return replacement;
        }
        MutableComponent copy = node.plainCopy().setStyle(node.getStyle());
        for (Component sibling : node.getSiblings()) {
            copy.append(replaced(sibling, target, replacement));
        }
        return copy;
    }

    private static Component translatableIn(Component line) {
        if (line == null) {
            return null;
        }
        if (line.getContents() instanceof TranslatableContents) {
            return line;
        }
        for (Component sibling : line.getSiblings()) {
            Component found = translatableIn(sibling);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
