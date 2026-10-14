package net.pixeldreamstudios.combat_styles.skill;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillScaling {

    public record PerStack(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
    }

    private record Entry(String counter, List<PerStack> perStack) {
    }

    private static final String KEY_ATTRIBUTE = "attribute";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_OPERATION = "operation";
    private static final String MODIFIER_PREFIX = "skill_stack/";
    private static final String MODIFIER_SEPARATOR = "/";

    private static final Map<UUID, Map<String, Entry>> ENTRIES = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Integer>> APPLIED = new ConcurrentHashMap<>();

    private SkillScaling() {
    }

    public static List<PerStack> parse(Iterable<JsonElement> elements) {
        List<PerStack> parsed = new ArrayList<>();
        for (JsonElement element : elements) {
            JsonObject object = element.getAsJsonObject();
            String attributeId = SkillConditions.string(object, KEY_ATTRIBUTE);
            Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attributeId))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown attribute " + attributeId));
            String operation = SkillConditions.optionalString(object, KEY_OPERATION);
            parsed.add(new PerStack(attribute, SkillConditions.number(object, KEY_AMOUNT), operationOf(operation)));
        }
        return parsed;
    }

    public static void define(ServerPlayer player, String source, String counter, List<PerStack> perStack) {
        Map<String, Entry> entries = ENTRIES.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
        if (perStack == null || perStack.isEmpty()) {
            Entry removed = entries.remove(source);
            if (removed != null) {
                apply(player, source, removed, 0);
            }
        } else {
            entries.put(source, new Entry(counter, List.copyOf(perStack)));
        }
    }

    public static void tick(ServerPlayer player) {
        Map<String, Entry> entries = ENTRIES.get(player.getUUID());
        if (entries == null || entries.isEmpty()) {
            return;
        }
        Map<String, Integer> applied = APPLIED.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            int stacks = SkillCounters.get(player, entry.getValue().counter());
            Integer previous = applied.get(entry.getKey());
            if (previous == null || previous != stacks) {
                apply(player, entry.getKey(), entry.getValue(), stacks);
            }
        }
    }

    public static void forget(UUID playerId) {
        ENTRIES.remove(playerId);
        APPLIED.remove(playerId);
    }

    private static void apply(ServerPlayer player, String source, Entry entry, int stacks) {
        for (int index = 0; index < entry.perStack().size(); index++) {
            PerStack perStack = entry.perStack().get(index);
            AttributeInstance instance = player.getAttribute(perStack.attribute());
            if (instance == null) {
                continue;
            }
            ResourceLocation id = CombatStyles.id(MODIFIER_PREFIX + source + MODIFIER_SEPARATOR + index);
            instance.removeModifier(id);
            if (stacks > 0) {
                instance.addTransientModifier(new AttributeModifier(id, perStack.amount() * stacks, perStack.operation()));
            }
        }
        APPLIED.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>()).put(source, stacks);
    }

    private static AttributeModifier.Operation operationOf(String raw) {
        if (raw == null) {
            return AttributeModifier.Operation.ADD_VALUE;
        }
        for (AttributeModifier.Operation operation : AttributeModifier.Operation.values()) {
            if (operation.getSerializedName().equals(raw.toLowerCase(Locale.ROOT))) {
                return operation;
            }
        }
        throw new IllegalArgumentException("Unknown attribute operation " + raw);
    }
}
