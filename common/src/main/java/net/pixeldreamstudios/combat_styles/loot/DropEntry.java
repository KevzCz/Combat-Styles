package net.pixeldreamstudios.combat_styles.loot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.pixeldreamstudios.combat_styles.CombatStyles;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record DropEntry(Optional<String> item, Optional<JsonObject> stack, int weight,
                        Map<String, Integer> tableBonus, boolean roll) {

    public static final String KEY_ITEM = "item";
    public static final String KEY_STACK = "stack";
    public static final String KEY_WEIGHT = "weight";
    public static final String KEY_TABLE_BONUS = "table_bonus";
    public static final String KEY_ROLL = "roll";
    public static final String KEY_STACK_ID = "id";
    public static final int DEFAULT_WEIGHT = 10;
    private static final int NO_WEIGHT = 0;
    private static final int SINGLE = 1;
    private static final String NOT_AN_OBJECT = "A drop stack must be an object";

    private static final Codec<JsonObject> STACK_CODEC = ExtraCodecs.JSON.comapFlatMap(
            element -> element.isJsonObject()
                    ? DataResult.success(element.getAsJsonObject().deepCopy())
                    : DataResult.error(() -> NOT_AN_OBJECT),
            object -> (JsonElement) object.deepCopy());

    private static final Codec<DropEntry> FULL = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf(KEY_ITEM).forGetter(DropEntry::item),
            STACK_CODEC.optionalFieldOf(KEY_STACK).forGetter(DropEntry::stack),
            Codec.INT.optionalFieldOf(KEY_WEIGHT, DEFAULT_WEIGHT).forGetter(DropEntry::weight),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf(KEY_TABLE_BONUS, Map.of())
                    .forGetter(DropEntry::tableBonus),
            Codec.BOOL.optionalFieldOf(KEY_ROLL, true).forGetter(DropEntry::roll)
    ).apply(instance, DropEntry::new));

    public static final Codec<DropEntry> CODEC = Codec.either(Codec.STRING, FULL).xmap(
            either -> either.map(DropEntry::of, entry -> entry),
            Either::right);

    public DropEntry {
        item = item == null ? Optional.empty() : item.filter(id -> !id.isBlank());
        stack = stack == null ? Optional.empty() : stack;
        tableBonus = tableBonus == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(tableBonus));
    }

    public static DropEntry of(String item) {
        return new DropEntry(Optional.of(item), Optional.empty(), DEFAULT_WEIGHT, Map.of(), true);
    }

    public boolean isPlain() {
        return stack.isEmpty() && weight == DEFAULT_WEIGHT && tableBonus.isEmpty() && roll;
    }

    public boolean isInstance() {
        return stack.isPresent();
    }

    public String itemId() {
        return stack.map(json -> json.has(KEY_STACK_ID) ? json.get(KEY_STACK_ID).getAsString() : "")
                .orElseGet(() -> item.orElse(""));
    }

    public DropEntry withItem(String id) {
        return new DropEntry(Optional.of(id), stack, weight, tableBonus, roll);
    }

    public int weightFor(String tableId) {
        int total = weight;
        for (Map.Entry<String, Integer> bonus : tableBonus.entrySet()) {
            String where = bonus.getKey();
            if (where.equalsIgnoreCase(tableId) || (!where.isEmpty() && tableId.contains(where))) {
                total += bonus.getValue();
            }
        }
        return Math.max(NO_WEIGHT, total);
    }

    public ItemStack create(HolderLookup.Provider registries) {
        if (stack.isPresent()) {
            return ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries), stack.get())
                    .resultOrPartial(error -> CombatStyles.LOGGER.warn("Skipped drop stack {}: {}",
                            stack.get(), error))
                    .map(parsed -> parsed.copyWithCount(SINGLE))
                    .orElse(ItemStack.EMPTY);
        }
        ResourceLocation id = ResourceLocation.tryParse(item.orElse(""));
        Item found = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return found == null ? ItemStack.EMPTY : new ItemStack(found);
    }

    public static Optional<JsonObject> encodeStack(ItemStack stack, HolderLookup.Provider registries) {
        return ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries),
                        stack.copyWithCount(SINGLE))
                .result()
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject);
    }
}
