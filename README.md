# Tooltip Revamp (NeoForge 1.21.1)

A complete overhaul of item tooltips, with discoverable item interactions.

## Features
- Pure black background, dark gray border, centered text, and 4 px padding from the border.
- The item name is displayed in a separate frame at the top, followed by the description and dividers.
- Items with available actions show a small indicator at the bottom (two indicators if a regular tooltip is also present).
- **Hold Ctrl** to open the actions page. Undiscovered parts are obscured by gray blocks, while blue words act as hints.
  Once an action is performed in-game, its entry is permanently revealed and stored in the player's data.

## Building
The most reliable approach is to download the official MDK for 1.21.1:
https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle

Copy the `src` directory and `gradle.properties` file from the archive (update `neo_version` if necessary), then run:

```bash
./gradlew build        # JAR output: build/libs
./gradlew runClient    # launch the game for testing
```

You can also build this project directly, but it requires a Gradle wrapper. Generate one with:

```bash
gradle wrapper --gradle-version 8.10
```

## Test Commands (OP)
```text
/tooltiprevamp reset        — reset all discovered entries
/tooltiprevamp reveal_all   — reveal all entries
```

## Item Descriptions
Descriptions are loaded from translation keys in the following format:

`tooltiprevamp.description.<namespace>.<item_path>`

For potions, an additional potion ID is appended:

`tooltiprevamp.description.<namespace>.<item_path>.<potion_id>`

Add these keys to your language files or a resource pack.

## Custom Actions (Datapack / Other Mods)
Create a file at `data/<namespace>/item_actions/<name>.json`:

```json
{
  "items": ["minecraft:bone_meal"],
  "text": "tooltiprevamp.action.bone_meal.plants",
  "order": 0,
  "trigger": {
    "type": "bonemeal",
    "blocks": ["#minecraft:crops"]
  }
}
```

The `items` field accepts item IDs, tag IDs prefixed with `#`, or objects such as `{"item": "...", "potion": "..." }`.

Text formatting:
- `[word]` — visible blue hint.
- `{word}` — hidden blue block.
- `<word>` — visible gray text.
- All other text remains hidden until the entry is discovered.

### Trigger Types
- `bonemeal` — fertilizing a block listed in `blocks`, when that block can actually be fertilized.
- `use_on_block` — right-clicking a block with the item; supports `blocks`.
- `item_ability` — item abilities such as `axe_strip`, `axe_scrape`, `axe_wax_off`, `shovel_flatten`, and `hoe_till`. Triggers only when the block actually changes.
- `disable_shield` — hitting a player or mob that is actively blocking with a shield (for axes).
