# Combat Styles

A Minecraft **1.21.1** mod (Fabric + NeoForge, via Architectury) that turns
[BetterCombat](https://modrinth.com/mod/better-combat) attack sequences into something you build.

A weapon's combo is no longer fixed. Every swing in the sequence is a **style** you can reorder,
replace, tune, and hang **affixes** on — and **rings** you wear can rewrite the whole thing.

Everything below is data-driven. You do not need to write Java to add content.

---

## Contents

- [How it fits together](#how-it-fits-together)
- [Quick start: your first datapack](#quick-start-your-first-datapack)
- [Datapack reference](#datapack-reference)
- [Addon API](#addon-api)
- [Commands](#commands)
- [Gotchas](#gotchas)
- [Building](#building)

---

## How it fits together

Four nouns account for nearly everything:

| Term | What it is |
| --- | --- |
| **Style** | One attack in a sequence. Either *native* (indexed automatically from BetterCombat weapons at load time, id `<category>_<n>`) or declared by you. |
| **Sequence** | The ordered list of styles a weapon swings through. Stored on the ItemStack. The slot used is `combo % sequenceSize`. |
| **Affix** | An effect bound to one style — damage, status, mechanics. Rolled as loot or granted by equipment. |
| **Equipment** | A worn item (ring) that modifies the sequence, grants affixes, or fires its own effects. |

### The order things apply in

```
BetterCombat weapon attributes
        ↓
default_styles        (per-item baseline sequence)
        ↓
weapon_rolls          (loot generation, if the item rolled)
        ↓
player edits          (the in-game customizer)
        ↓
equipment             (rings: substitute / augment / grant affixes)
        ↓
what you actually swing
```

Each layer sees the result of the one above it. A ring's `augment` applies on top of whatever the
sequence already is — **including a weapon with no custom sequence at all**, where it modifies the
weapon's own native attacks.

### Where files go

All under `data/<your_namespace>/`:

```
custom_attacks/    declare an attack from scratch (animation, hitbox, timing, trail)
custom_styles/     wrap an existing animation as a named, reusable style
style_affixes/     on-hit / on-swing effects
rings/             worn equipment
default_styles/    give an item a sequence out of the box
weapon_rolls/      loot generation rules
style_books/       the books that teach styles
```

Load order is `custom_attacks` → `custom_styles` → everything else, because a custom attack's id
becomes a style id that custom styles may reference.

---

## Quick start: your first datapack

### 1. A style

The smallest useful thing: wrap an existing BetterCombat animation and give it a tweak.

`data/mypack/custom_styles/heavy_cleave.json`

```json
{
  "id": "mypack:heavy_cleave",
  "category": "sword",
  "rarity": "rare",
  "style": {
    "style": "bettercombat:one_handed_slash_horizontal_left",
    "attack": {
      "damage_multiplier": 1.25,
      "upswing": 0.6
    },
    "name_key": "style.mypack.heavy_cleave"
  }
}
```

Add `"style.mypack.heavy_cleave": "Heavy Cleave"` to your lang file and it shows up in the browser.

### 2. An affix

`data/mypack/style_affixes/searing.json`

```json
{
  "id": "mypack:searing",
  "weight": 8,
  "rarity": "uncommon",
  "cost_modifier": 1.3,
  "trail": { "color": "#FF7A3D", "glows": true },
  "on_hit": {
    "damage": { "mode": "additive", "ratio": 0.2 },
    "ignite": { "seconds": 4, "chance": 1.0 },
    "particles": [
      { "type": "minecraft:flame", "count": 8, "spread": 0.4 }
    ]
  }
}
```

`weight` controls how often it rolls (higher = more common). `cost_modifier` multiplies how much
budget the style costs, so strong affixes crowd out other slots.

### 3. A ring

`data/mypack/rings/searing_band.json`

```json
{
  "id": "mypack:searing_band",
  "rarity": "rare",
  "cooldown": 40,
  "trigger": { "last_slot": true },
  "effect": {
    "grant_affixes": ["mypack:searing"],
    "attributes": {
      "minecraft:generic.attack_damage": { "amount": 1.0, "operation": "add_value" }
    }
  }
}
```

Rings need an item registered in code to attach to. If you only have a datapack, use
`grant_affixes` and `tune_affixes` on an existing ring instead, or see [Addon API](#addon-api) to
register your own item as equipment.

### 4. Give an item a sequence by default

`data/mypack/default_styles/my_sword.json`

```json
{
  "items": ["mypack:flame_sword"],
  "name": "Searing Form",
  "mode": "replace",
  "slots": 4,
  "entries": [
    { "custom_style_id": "mypack:heavy_cleave" },
    { "custom_style_id": "combat_styles:inside_cut" },
    { "custom_style_id": "combat_styles:braced_thrust" }
  ]
}
```

`mode` is the important field:

- `append` — add these on top of the weapon's own attacks
- `replace` — this sequence *is* the weapon's moveset
- `tune` — don't add anything; just modify the weapon's existing attacks via `tunings`

---

## Datapack reference

Every file type Combat Styles loads, with field names, types and defaults taken from the codecs in
source. If a default is listed, that is the value the game falls back to when the field is absent.

All paths are relative to `data/<your_namespace>/`.

### custom_attacks

Declares an attack from scratch. Use this when there is no existing style to wrap — you have an
animation and want to define its hitbox, timing and trail yourself.

The attack's `id` **becomes a style id** directly, so custom styles can reference it.

`custom_attacks/<name>.json`

```json
{
  "id": "mypack:moonfall_cleave",
  "rarity": "epic",
  "category": "sword",
  "attack": {
    "animation": "mypack:moonfall",
    "hitbox": "horizontal_plane",
    "damage_multiplier": 1.4,
    "angle": 120.0,
    "upswing": 0.6,
    "range_multiplier": 1.1,
    "movement_speed_multiplier": 0.9,
    "swing_sound": "minecraft:entity.player.attack.sweep",
    "trail": [
      { "particle_type": "slash180", "y_addition": -0.1 }
    ]
  }
}
```

| Field | Type | Default | Notes |
| --- | --- | --- | --- |
| `id` | string | **required** | Namespaced. Becomes the style id. |
| `attack.animation` | string | **required** | Player Animator id, from `assets/<ns>/player_animations/`. |
| `attack.hitbox` | enum | `horizontal_plane` | `forward_box`, `horizontal_plane`, `vertical_plane`. |
| `attack.damage_multiplier` | double | `1.0` | |
| `attack.angle` | double | `90.0` | Sweep arc for planes; box width for `forward_box`. |
| `attack.upswing` | double | `0.5` | Windup before the hit registers. |
| `attack.range_multiplier` | float | `1.0` | |
| `attack.movement_speed_multiplier` | float | `1.0` | Player movement during the swing. |
| `attack.swing_sound` | string | none | Sound id. |
| `attack.trail` | ParticlePlacement[] | inherited | Omit to inherit the animation's usual trail. |
| `category` | string | `custom` | Weapon category. |
| `rarity` | enum | `common` | `common`, `uncommon`, `rare`, `epic`, `legendary`. |
| `cost` | double | derived | Overrides the computed budget cost. |
| `name_key` | string | derived | Lang key for the display name. |

#### ParticlePlacement

| Field | Type | Default |
| --- | --- | --- |
| `particle_type` | string | `""` |
| `x_addition`, `y_addition`, `z_addition` | float | `0.0` |
| `local_yaw` | float | `0.0` |
| `pitch_addition` | float | `0.0` |
| `roll_set` | float | `0.0` |

> Attacks created through the in-game builder are written to
> `config/combat_styles/custom_attacks/`. Datapack definitions take precedence over config ones, and
> config entries can be deleted from the manager screen while datapack ones cannot.

### Loading from config instead of a datapack

Four of these types also load from the config folder, using the **same schema** as their datapack
equivalent:

| Config folder | Equivalent to |
| --- | --- |
| `config/combat_styles/custom_styles/` | `data/<ns>/custom_styles/` |
| `config/combat_styles/custom_attacks/` | `data/<ns>/custom_attacks/` |
| `config/combat_styles/default_styles/` | `data/<ns>/default_styles/` |
| `config/combat_styles/attacks/` | per-attack default overrides (generated) |

Precedence is **datapack over config** — if both define the same id, the datapack wins.

The practical difference is editability: content authored in config can be edited in-game (the
library's right-click editor, the attack manager's delete), while datapack content is read-only.
Use config for things you want to tweak while playing, and a datapack for things you want to ship.

`/combatstyles reload` picks up changes to any of these without a restart.

---

### custom_styles

Wraps an existing animation as a named, reusable style. This is the common case — most content is a
`custom_style`, not a `custom_attack`.

`custom_styles/<name>.json`

```json
{
  "id": "mypack:inside_cut",
  "group": "foundation",
  "category": "sword",
  "rarity": "common",
  "style": {
    "style": "bettercombat:one_handed_slash_horizontal_left",
    "affixes": ["combat_styles:reading"],
    "attack": { "damage_multiplier": 1.05 },
    "name_key": "style.mypack.inside_cut"
  }
}
```

| Field | Type | Default | Notes |
| --- | --- | --- | --- |
| `id` | string | **required** | Namespaced. Becomes the style id. |
| `style` | StyleEntry | **required** | The style body — see [StyleEntry](#shared-styleentry) for every field it accepts. |
| `category` | string | none | Weapon category, e.g. `sword`. |
| `rarity` | enum | `common` | |
| `cost` | double | derived | Overrides the computed budget cost. |
| `group` | string | `""` | Free-form grouping label for the browser UI. |

The `style` block is a full `StyleEntry`, so the commonly used fields are:

| Field | Type | Notes |
| --- | --- | --- |
| `style.style` | string | Animation id, or another style id to base on. |
| `style.attack` | AttackOverride | Stat changes. |
| `style.affixes` | string[] | Affixes always present on this style. |
| `style.sound` | string | Swing sound override. |
| `style.name_key` | string | Lang key. |

---

### style_affixes

Effects bound to a style. `on_hit` fires when the swing connects; `on_swing` fires on the swing
itself, whether or not it lands.

`style_affixes/<name>.json`

```json
{
  "id": "mypack:kindling",
  "weight": 14,
  "rarity": "common",
  "cost_modifier": 1.15,
  "trail": { "color": "#E87A3D", "glows": true },
  "applies_to": { "hitbox": ["HORIZONTAL_PLANE", "VERTICAL_PLANE"] },
  "on_hit": {
    "damage": { "mode": "additive", "ratio": 0.15 },
    "ignite": { "seconds": 3, "chance": 1.0 }
  }
}
```

| Field | Type | Default | Notes |
| --- | --- | --- | --- |
| `id` | string | **required** | |
| `weight` | int | — | Loot weight. Higher rolls more often. |
| `rarity` | enum | `common` | |
| `cost_modifier` | double | `1.0` | Multiplies the style's budget cost. |
| `trail` | `{color, glows}` | none | Trail tint. Highest-rarity affix on a style wins. |
| `applies_to.hitbox` | string[] | any | Restricts which attack shapes can carry this affix. |
| `on_hit` / `on_swing` | effect block | none | See below. |

#### Effect block

Shared between affixes and equipment — a ring's `on_swing` takes exactly these fields.

| Field | Shape | Notes |
| --- | --- | --- |
| `damage` | `{mode, ratio, school, scaling}` | `mode`: `additive` or `convert`. |
| `damage.scaling` | `{source[], per_point, cap, strongest}` | Scales off external stats, e.g. spell power. |
| `effects` | `[{effect, duration, amplifier, chance}]` | Status effects on the target. |
| `ignite` | `{seconds, chance}` | |
| `lifesteal` | `{ratio, chance}` | |
| `knockback` | `{strength, pull}` | `pull: true` reverses the direction. |
| `splash` | `{radius, ratio, hits_attacker}` | Area damage around the player. |
| `freeze_ticks` | float | |
| `slash_wave` | see below | Travelling wave projectile. |
| `particles` | `[{type, count, spread}]` | |
| `sound` | `{id, volume, pitch}` | |
| `projectiles` | array | |
| `echo` | `{times, delay_ticks, ratio}` | Replays **this block** after a delay at reduced power. |
| `delay_ticks` | int | Delays the whole block. |
| `mechanics` | see below | |

#### mechanics

| Group | Contains |
| --- | --- |
| `offense` | `chain`, `cleave`, `execute`, `momentum` |
| `sustain` | `heal`, `shield` |
| `control` | `attributes`, `swing_attributes` |
| `motion` | `dash`, `delay_ticks` |
| `condition` | slot rules, `on_miss`, `behind_target`, `damaged_within`, `refund_slot`, health gates, `multiplier` |
| `cooldown` | `{ticks}` — per affix, per player |
| `cost` | `{health, cancel_if_short}` |
| `spawn` | `summons`, `commands`, `delay_ticks` |

Selected shapes:

```json
"offense": {
  "chain":   { "max_targets": 3, "range": 7, "falloff": 0.55, "ratio": 0.45 },
  "cleave":  { "radius": 3, "ratio": 0.4, "max_targets": 5 },
  "execute": { "health_threshold": 0.3, "bonus_ratio": 1.0, "repeat_on_kill": true },
  "momentum":{ "per_hit_ratio": 0.1, "max_stacks": 5, "decay_ticks": 60 }
}
```

```json
"sustain": {
  "heal":   { "ratio": 0.15, "chance": 1.0, "overheal": false },
  "shield": { "amount": 6, "duration": 6, "dodge_chance": 0.3, "dodge_duration": 6 }
}
```

`duration` is in **ticks**. `amount` is in absorption half-hearts, so `6` is three hearts.

```json
"motion": {
  "dash": { "forward": 0.8, "upward": 0.15, "toward_target": true, "chance": 1.0 },
  "delay_ticks": 2
}
```

Dash velocity is clamped, so stacking several dash affixes cannot fling the player.

```json
"spawn": {
  "summons": [
    { "entity": "minecraft:vex", "count": 2, "lifespan": 200,
      "radius": 2.0, "inherit_target": true }
  ],
  "commands": [
    { "command": "particle minecraft:flame ~ ~1 ~", "as_server": true }
  ],
  "delay_ticks": 0
}
```

`commands` accepts `command` and `as_server`. With `as_server: true` (the default) the command runs at
permission level 2, so treat it as you would a command block; set it to `false` to run at the
player's own permission level. Output is suppressed either way.

##### Summon damage

`summons` entries can carry damage, which is computed from the hit that spawned them:

```json
"summons": [
  {
    "entity": "combat_styles:wraith",
    "count": 3,
    "lifespan": 160,
    "damage_ratio": 0.4,
    "damage_scaling": { "source": "spell_power:soul", "per_point": 0.02 }
  }
]
```

`damage_ratio` is a fraction of the damage dealt by the triggering hit. `damage_scaling` adds
`per_point` per point of the named stat, linearly, and accepts the same `cap` and `strongest` fields
as damage scaling elsewhere.

`combat_styles:wraith` is the mod's own summon: it takes the summoner as owner, makes a single
charging attack at the target, deals its damage on contact and then fades out over 8 ticks. It has no
collision, cannot be attacked, and despawns if the owner or target is lost.

`summons` caps at 16 entities per effect regardless of `count`.

#### condition

| Field | Type | Notes |
| --- | --- | --- |
| `slot.slot` | int \| int[] | Slot indices, 0-based. Negative counts from the end. |
| `slot.first` / `slot.last` | bool | Only the opening / closing attack. |
| `slot.after_style` / `slot.after_category` | string | Requires the preceding attack to match. |
| `slot.requires.min_attacks` | int | Gate: needs at least N preceding attacks. |
| `slot.requires.per_preceding` | float | Scales damage by this fraction per preceding attack. |
| `slot.requires.per_preceding_cap` | float | Ceiling for the above. |
| `slot.requires.same_target` / `same_style` | bool | |
| `slot.requires.weapon_categories` | string[] | |
| `on_miss` | bool | Fires only when the swing misses. |
| `behind_target` | bool | |
| `damaged_within` | int | Player was hurt within N ticks. |
| `refund_slot` | bool | Next swing replays this slot. |
| `self_health_below` / `self_health_above` / `target_health_below` | float | 0..1 fractions. |
| `sprinting` / `airborne` / `night` | bool | |
| `multiplier` | float | Damage multiplier applied when the condition passes. |

#### slash_wave

| Field | Type | Default | Notes |
| --- | --- | --- | --- |
| `range` | float | `12.0` | Blocks travelled. |
| `radius` | float | `1.2` | |
| `ratio` | float | `0.5` | Damage as a fraction of the hit. |
| `speed` | float | `0.8` | Blocks per tick. |
| `chance` | float | `1.0` | |
| `follows_swing` | bool | `true` | Inherits the attack's orientation. |
| `inherit` | bool | `true` | Inherits the affix trail colour. |
| `pierces` | bool | `true` | |
| `scale` | float | `1.0` | Visual size. |
| `frames` | int | `10` | Animation frames. |
| `texture` | string | derived | |
| `colour` | `{colour, glows}` | inherited | |
| `delay_ticks` | int | `0` | |
| `speed_curve` | see below | none | Optional wind-up. |

```json
"speed_curve": {
  "start": 0.2,          // fraction of full speed at spawn
  "trough": 0.05,        // dips to this
  "end": 1.0,            // recovers to this
  "stall_ticks": 3,      // ticks falling to the trough
  "recover_ticks": 6     // ticks climbing back
}
```

Both legs use smoothstep easing. Wave lifetime is recalculated against the curve, so a stalling wave
still travels its full `range`.

---

### rings

Worn equipment. Loads from `rings/` but functions in armour, offhand, Trinkets and Curios slots.

`rings/<name>.json`

```json
{
  "id": "mypack:searing_band",
  "rarity": "legendary",
  "cooldown": 0,
  "trigger": { "slot_index": 2 },
  "effect": {
    "substitute": {
      "styles": ["mypack:cut_a", "mypack:cut_b"],
      "mode": "replace_all",
      "weapon_categories": ["sword"],
      "item_tags": ["minecraft:swords"]
    },
    "attributes": {
      "minecraft:generic.attack_speed": {
        "amount": 0.20, "operation": "multiply_total", "stacks": false
      }
    }
  }
}
```

| Field | Type | Default | Notes |
| --- | --- | --- | --- |
| `id` | string | **required** | |
| `effect` | block | **required** | |
| `rarity` | enum | `common` | |
| `cooldown` | int | `0` | Ticks. Tracked per worn copy. |
| `trigger` | block | always | |

#### trigger

| Field | Type | Notes |
| --- | --- | --- |
| `slot_index` | int | Only on this slot of the sequence. |
| `last_slot` | bool | Only the final attack. |
| `every_nth_swing` | int | |
| `on_miss` | bool | Miss detection is swing-id scoped, not time-windowed. |
| `on_kill` | bool | |
| `on_summon_hit` | bool | Fires when one of your summons lands its attack, instead of on your own swing. Mutually exclusive with the normal swing phase: a ring without this never fires on a summon hit, and a ring with it never fires on a swing. The effect's `on_hit` block runs with the summon's damage as its base. |
| `idle_ticks` | int | Requires N ticks without combat. |
| `requires_affix` | bool | Attack must carry any affix. |
| `affix_id`, `style_id`, `hitbox` | string | Exact matches. |

#### effect

| Field | Type | Notes |
| --- | --- | --- |
| `substitute` | block | See below. |
| `substitute_style` | string | Legacy single-style form. Collapses the sequence to one attack. |
| `augment` | AttackOverride | Modifies each attack. Applies to native weapon attacks too. |
| `attributes` | map | Modifies the **player** while worn. |
| `grant_affixes` | string[] | Affix ids added to the attack. Deduped. |
| `tune_affixes` | map | Retunes affixes already present. |
| `on_swing` / `on_hit` | effect block | Same shape as affix effects. |
| `reset_combo` | bool | Next swing replays the same attack, animation included. |

#### substitute

| Field | Type | Default | Notes |
| --- | --- | --- | --- |
| `styles` | string \| string[] | **required** | The replacement sequence. |
| `mode` | enum | `replace_all` | `replace_all`, `replace_slot`, `append`, `prepend`. |
| `slot` | int \| int[] | all | For `replace_slot`. Negative counts from the end. |
| `weapon_categories` | string[] | any | |
| `item_tags` | string[] | any | |

A weapon gate passes if it matches **category or tag** — not both.

#### attributes

Values take a bare number (flat) or an object:

```json
"attributes": {
  "minecraft:generic.attack_damage": 2.0,
  "minecraft:generic.attack_speed": {
    "amount": -0.75,
    "operation": "add_value",
    "stacks": false
  }
}
```

`operation` is `add_value` (default), `multiply_base`, or `multiply_total`.
`stacks` defaults to `true`.

#### tune_affixes

```json
"tune_affixes": {
  "combat_styles:kindling": {
    "damage_multiplier": 1.15,
    "cooldown": 20,
    "rarity": "rare",
    "trail": { "color": "#FF0000", "glows": true }
  }
}
```

Accepts `cost_modifier`, `damage_multiplier`, `chance`, `cooldown`, `trail`, `weight`, `rarity`,
`applies_to`, `on_hit`, `on_swing`.

---

### default_styles

Gives an item a sequence out of the box, before any loot roll.

`default_styles/<name>.json`

| Field | Type | Default | Notes |
| --- | --- | --- | --- |
| `items` | string[] | `[]` | Item ids. |
| `tags` | string[] | `[]` | Item tags. |
| `categories` | string[] | `[]` | Weapon categories. |
| `any` | bool | `false` | Matches every supported weapon. |
| `mode` | enum | `append` | `append`, `replace`, `tune`. |
| `entries` | StyleEntry[] | `[]` | The sequence. |
| `tunings` | SlotTuning[] | `[]` | Per-slot edits. |
| `slots` | int | `0` | Slot limit for the item. |
| `priority` | int | `0` | Higher wins when several definitions match. |
| `locked` | bool | `false` | Blocks player editing. |
| `keep_through_rolls` | bool | `false` | Survives a weapon roll. |
| `name` / `name_key` | string | `""` | Display name of the sequence. |

#### SlotTuning

Used with `mode: "tune"` to modify a weapon's own attacks without replacing them.

| Field | Type | Default | Notes |
| --- | --- | --- | --- |
| `slot` | int \| int[] | all | Negative counts from the end (`-1` is the last slot). |
| `affixes` | string \| string[] | `[]` | Affixes to add to that slot. |
| `attack` | AttackOverride | none | Stat changes. |
| `affix_tuning` | map | `{}` | Retune affixes on that slot. |
| `mode` | enum | `add` | `add` sums with existing values; `replace` overwrites. |

---

### weapon_rolls

Generates sequences on loot. Applied after default styles.

`weapon_rolls/<name>.json`

| Field | Type | Notes |
| --- | --- | --- |
| `items`, `tags`, `categories` | string[] | What this roll applies to. |
| `tables` | string[] | Gates by loot table id. |
| `chance` | float | 0..1 probability the roll happens. |
| `min_slots`, `max_slots` | int | Sequence length range. |
| `affix_chance` | float | Per-slot chance of an affix. |
| `max_affixes` | int | Cap per slot. |
| `styles` | string[] | Candidate pool. Empty means all eligible. |
| `names` | string[] | Random sequence names. |
| `slot_limit` | int | |
| `mode` | string | As in default_styles. |

---

### Shared: AttackOverride

Used by `style.attack`, ring `augment`, and slot `tunings`. Every field is optional; absent fields
leave the underlying value alone.

| Field | Type |
| --- | --- |
| `damage_multiplier`, `angle`, `upswing` | double |
| `range_multiplier`, `movement_speed_multiplier` | float |
| `hitbox`, `animation`, `swing_sound`, `rarity` | string |
| `attributes` | map — **swing** attributes, not player attributes |
| `cost` | double |
| `disabled` | bool |
| `ignore_affinity` | bool |

> `augment.attributes` modifies the swing. `effect.attributes` on a ring modifies the player. They
> are different fields with different targets.

---

### Shared: StyleEntry

One slot in a sequence.

| Field | Type | Notes |
| --- | --- | --- |
| `style` | string | Native or declared style id. |
| `custom_style_id` | string | Reference to a `custom_styles` entry. |
| `affixes` | string[] | Affixes on this slot. |
| `attack` | AttackOverride | Per-slot stat changes. |
| `affix_tuning` | map | Per-slot affix retuning. |
| `sound` | string | Swing sound override. |
| `name` / `name_key` | string | Display name for this slot. |

---

## Addon API

Everything public lives under `net.pixeldreamstudios.combat_styles.api`. The static entry point is
`CombatStylesAPI`. Signatures below are taken from source.

### Events

Register listeners at mod init. Most return `boolean` — return `false` to cancel the effect.

```java
CombatStylesAPI.onSwing(event -> {
    if (event.player().isCrouching()) {
        return false;   // suppress combat styles for this swing
    }
    return true;
});
```

| Hook | Payload fields | Returns |
| --- | --- | --- |
| `onSwing` | `player`, `weapon`, `comboCount`, `attack`, `affixes`, `context`, `baseDamage` | `boolean` |
| `onHit` | `player`, `target`, `weapon`, `affix`, `affixes`, `context`, `dealtDamage` | `boolean` |
| `onMiss` | `player`, `weapon`, `comboCount`, `context` | `boolean` |
| `onWave` | `player`, `affix`, `damage`, `texture`, `frames`, `thrusting` | `boolean` |
| `onEquipmentTrigger` | `player`, `equipmentId`, `weapon`, `slotIndex`, `missPhase` | `boolean` |
| `onKill` | `player`, `target`, `weapon`, `affixes`, `comboCount` | `void` — **not cancellable** |
| `onSequenceChanged` | `stack`, `previous`, `current` | `void` — **not cancellable** |

`onSequenceChanged` takes three arguments rather than an event record:

```java
CombatStylesAPI.onSequenceChanged((stack, previous, current) -> {
    // react to a sequence being written
});
```

#### SwingContext

Several events carry a `SwingContext`. It describes where in the sequence the swing sits:

| Method | Returns |
| --- | --- |
| `isKnown()` | Whether the slot could be resolved |
| `slotIndex()` | 0-based position in the sequence |
| `sequenceSize()` | Total slots |
| `precedingStyleIds()` | Style ids swung before this one in the current chain |
| `precedingCategories()` | Their categories |

---

### Extension points

#### Register your own item as equipment

Any item can carry a ring effect. Implement `EquipmentInfo` and return a definition for stacks you
recognise:

```java
CombatStylesAPI.registerEquipment(stack ->
        stack.is(MyItems.AMULET)
                ? CombatStylesAPI.equipment("mypack:amulet_effect")
                : null);
```

The definition itself still comes from a datapack file under `rings/`. This only changes which item
carries it.

#### Adjust the style budget

`BudgetModifier` runs whenever a weapon's budget is computed:

```java
CombatStylesAPI.registerBudgetModifier((holder, stack, weaponCategory, budget) ->
        holder.hasEffect(MyEffects.FOCUS) ? budget * 1.25 : budget);
```

Modifiers compose in registration order.

#### Register a default sequence in code

The code-side equivalent of a `default_styles` file:

```java
CombatStylesAPI.registerDefaultSequence(
        ResourceLocation.parse("mypack:flame_sword"),
        CombatStylesAPI.sequenceOf("Searing Form",
                List.of("mypack:heavy_cleave", "combat_styles:inside_cut")));

CombatStylesAPI.registerSlotLimit(ResourceLocation.parse("mypack:flame_sword"), 4);
```

---

### Reading and writing sequences

| Call | Returns | Notes |
| --- | --- | --- |
| `sequenceOf(stack)` | `StyleSequence` | Current sequence on an item. |
| `sequenceOf(name, styleIds)` | `StyleSequence` | Build one from a list of ids. |
| `hasSequence(stack)` | `boolean` | |
| `applySequence(stack, seq)` | `void` | Write a sequence. |
| `baselineOf(stack)` | `StyleSequence` | What the item has before player edits. |
| `rolledSequenceOf(stack)` | `StyleSequence` | The loot-rolled sequence, if any. |
| `setRolledSequence(stack, seq)` | `void` | |
| `defaultSequenceOf(stack)` | `StyleSequence` | From `default_styles`. |
| `usesDefaultSequence(stack)` | `boolean` | |
| `rollWeapon(stack, random)` | `boolean` | Run the loot roll manually. |
| `hasWeaponRolls()` | `boolean` | Whether any roll data is loaded. |
| `supportsSequences(stack)` | `boolean` | Whether the item can carry one at all. |
| `slotLimitOf(stack)` / `setSlotLimit(stack, n)` | `int` / `void` | |
| `isLocked(stack)` / `setLocked(stack, bool)` | `boolean` / `void` | Locked items block player editing. |

#### Styles

| Call | Returns |
| --- | --- |
| `allStyles()` | `List<StylePage>` |
| `stylesForCategory(category)` | `List<StylePage>` |
| `categories()` | `List<String>` |
| `style(styleId)` | `StylePage` |
| `isKnownStyle(styleId)` | `boolean` |
| `nativeNodesOf(stack)` | `List<StylePage>` — the weapon's own attacks |
| `availableSwingSounds()` | `List<WeaponAttributes.Sound>` |

#### Weapon attributes

| Call | Returns | Notes |
| --- | --- | --- |
| `baseAttributes(stack)` | `WeaponAttributes` | Before Combat Styles composes anything. |
| `composedAttributes(stack)` | `WeaponAttributes` | Final result after every layer. |
| `weaponCategory(stack)` | `String` | |

---

### Budget and cost

Every style costs budget; a weapon has a budget ceiling. This is what stops a player stacking six
legendary styles on a wooden sword.

| Call | Returns |
| --- | --- |
| `costOf(page)` | `double` |
| `costOf(sequence)` | `double` |
| `costOnWeapon(page, weaponCategory)` | `double` — after affinity scaling |
| `budgetOf(stack)` | `double` |
| `budgetFor(holder, stack)` | `double` — includes registered modifiers |
| `fitsBudget(stack, sequence)` | `boolean` |
| `statsOf(sequence)` | `SequenceComposer.SequenceStats` |

#### Affinity

A style built for one weapon category performs worse on another.

| Call | Returns |
| --- | --- |
| `affinityOf(styleCategory, weaponCategory)` | `CategoryAffinity.Tier` |
| `scaledForWeapon(page, weaponCategory)` | `StylePage` — with affinity applied |

---

### Player knowledge

| Call | Returns |
| --- | --- |
| `hasLearned(player, styleId)` | `boolean` |
| `learn(player, styleId)` | `boolean` |
| `learnedStyles(player)` | `Map<String, Integer>` — id to copy count |
| `learnedCopies(player, styleId)` | `int` |
| `learnedCount(player)` | `int` |

---

### Inspecting attacks

`AttackQuery` reads an attack's shape without depending on BetterCombat's internals:

```java
AttackInfo info = AttackQuery.of(attack);
if (info.isKnown() && info.isThrust()) {
    // ...
}
```

| Method | Returns |
| --- | --- |
| `isKnown()` | `boolean` |
| `shape()` | `AttackShape` |
| `isThrust()` | `boolean` |
| `angle()` / `upswing()` | `double` |
| `damageMultiplier()` | `double` |
| `rangeMultiplier()` | `float` |

`AffixInfo` is the equivalent read-only view of an affix — `id()`, `serializedId()`, `rarity()`,
`weight()`, `costModifier()`, `damageMultiplier()`, `canApplyTo(page)`.

---

### Equipment

| Call | Returns |
| --- | --- |
| `allEquipment()` | `List<EquipmentDefinition>` |
| `equipment(id)` | `EquipmentDefinition` |
| `isKnownEquipment(id)` | `boolean` |
| `isEquipmentSlotAvailable()` | `boolean` — whether Trinkets/Curios is present |
| `equippedAccessories(player)` | `List<ItemStack>` |

Accessory support is automatic. `WornItems.of(player)` folds in armour, offhand, and both the
Trinkets (Fabric) and Curios (NeoForge) APIs, so anything reading worn equipment sees all of them
without extra integration.

---

### Combat state

| Call | Returns | Notes |
| --- | --- | --- |
| `lastSwingMissed(player)` | `boolean` | Swing-id scoped, not a time window. |
| `idleTicks(player)` | `long` | Ticks since last combat action. |
| `refundComboSlot(player)` | `void` | Next swing replays the current slot. |
| `verifyData()` | `void` | Logs unresolved references across loaded data. Useful in dev. |

---

### Notes

**Server side.** Effects resolve on the server. Listeners registered on the client will not see swing
or hit events from other players.

**Cancelling.** Returning `false` from a listener suppresses Combat Styles' own handling of that
event. It does not cancel the underlying vanilla or BetterCombat attack.

**Thread safety.** Registries are populated during datapack load and read during combat. Register
listeners and extension points at mod init, not lazily during a swing.


---

## Commands

All under `/combatstyles`, permission level 2. Most act on the item in your main hand.

| Command | Does |
| --- | --- |
| `inspect` | Dump the held weapon's sequence, affixes and budget. |
| `list` · `affixes` · `rings` · `rolls` · `custom_styles` | List loaded content. |
| `learned` · `budget` | Your style knowledge and budget. |
| `give <players> <style> [copies]` · `giveall` | Hand out style pages. |
| `book <rarity>` | Give a style book. |
| `learnall <players> [copies]` · `forgetall <players>` | Bulk progression. |
| `roll` · `roll_clear` | Run or clear a loot roll on the held weapon. |
| `apply_affix <affix>` · `strip_affix` | Edit affixes on the held weapon. |
| `clear` · `unetch` · `improvise` | Sequence editing. |
| `lock` · `unlock` | Block or allow player editing. |
| `slot_limit <n>` | Set the held weapon's slot cap. |
| `ignore_minimum` · `enforce_minimum` | Let the held weapon drop below its minimum attack count, or restore the limit. |
| `item_attack <slot> <field> <value>` · `item_attack_clear` | Per-slot stat overrides. |
| `reload` | Reload **everything** — every datapack and config source, in the same order the server uses at startup. |
| `defaults_reload` · `attacks_reload` · `attacks_regenerate` | Narrower reloads that report per-source counts, useful for checking what a specific folder loaded. |
| `export_default` | Write the held weapon's sequence out as a `default_styles` file. |

Middle-clicking an attack in the customizer's library opens the same editor used for sequence slots,
but edits that attack's **default** — stored in `config/combat_styles/attacks/`, applied everywhere
the attack is used, including custom styles built on it. Attacks defined by a datapack are read-only.

`ignore_minimum` sets a per-item flag, so it survives on that weapon only. The global
`enforceMinimumAttacks` config option turns the limit off everywhere instead.

---

## Gotchas

Things that cost real debugging time.

**`angle` means different things per hitbox.** `HORIZONTAL_PLANE` and `VERTICAL_PLANE` treat it as a
sweep arc; `FORWARD_BOX` treats it as box width. Copying a value between hitbox types will not
behave the way you expect.

**Shield and effect durations are in ticks, not seconds.** `"duration": 120` is six seconds.

**`applies_to.hitbox` is enforced at both roll time and swing time.** An affix granted directly by a
ring still respects it, so a cleave affix restricted to planes will not fire on a thrust.

**Two identical rings stack, mostly.** Player `attributes` sum per attribute *and* operation unless
you set `"stacks": false`. Trigger effects fire once per worn copy, and cooldowns are tracked per
copy. But a ring that substitutes a style applies its augment once — the style package is granted
once, so compounding a multiplier against it would double-count.

**`requires_affix` can never be satisfied on a weapon with no sequence.** There are no affixes to
find. That is intended, but it makes such rings look broken on vanilla gear.

**`echo` lives inside the trigger block**, not beside it:

```json
"on_swing": {
  "slash_wave": { "...": "..." },
  "echo": { "times": 1, "delay_ticks": 8, "ratio": 0.5 }
}
```

That way `on_swing` and `on_hit` echo independently. An `echo` at the `effect` level still parses
for backwards compatibility, but it applies to whichever block happened to fire.

**Summons are capped at 16 per effect** regardless of `count`, and datapack commands run at
permission level 2 unless you set `as_server: false`. Both are guards against a datapack doing
something the server cannot recover from.

**Native style ids are generated at load time.** `sword_0`, `sword_1` and so on are assigned by
scanning whatever weapons are present, so the indices shift between modpacks. Do not hardcode them
in a datapack — declare your own `custom_styles` instead.

**The malfu data set is a separate built-in pack.** `data/combat_styles/` holds the BetterCombat
native content; `resourcepacks/malfu_combat_styles/` mirrors it for Malfu's animations and is not
enabled by default. If you add content that references styles, check whether it needs a mirror on
both sides.

---

## Building

```
./gradlew build
```

Jars land in `fabric/build/libs/` and `neoforge/build/libs/`.

Requires Java 21. Depends on Architectury API and BetterCombat.
