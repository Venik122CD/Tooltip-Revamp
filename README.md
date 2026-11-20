# Tooltip Revamp

Tooltip Revamp overhauls Minecraft item tooltips and adds discoverable item interactions.

## Features

- Pure black tooltip background with a dark gray border.
- Centered text with consistent spacing.
- Item names displayed in a separate framed header.
- Descriptions and visual separators below the item name.
- Hold **Ctrl** to open the item actions page.
- Undiscovered action text is concealed until the player performs the corresponding action.
- Discoveries are saved to player data and remain unlocked permanently.

## Build

This project targets Minecraft **1.21.1**, NeoForge, and Java 21.

Use Gradle 8.10 or newer:

```bash
gradle build
```

The built JAR is generated in `build/libs`.

## Development commands

The following operator commands are available:

- `/tooltiprevamp reset` — reset all discovered actions.
- `/tooltiprevamp reveal_all` — reveal every action.

## Item descriptions

Descriptions use translation keys in this format:

```text
tooltiprevamp.description.<namespace>.<item_path>
```

Potion descriptions may include the potion ID as an additional suffix. Add translations to the language files or provide them through a resource pack.

## Datapack-defined item actions

Create JSON files under:

```text
data/<namespace>/item_actions/<name>.json
```

Example:

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

The `items` field accepts item IDs, item tags (prefixed with `#`), or objects containing item and potion identifiers.

Text markup:

- `[word]` — visible blue hint.
- `{word}` — concealed blue hint block.
- `<word>` — visible gray text.
- Unmarked text remains concealed until the action is discovered.

Supported trigger types include:

- `bonemeal` — using bone meal on a block that can actually be fertilized.
- `use_on_block` — right-clicking a matching block with the item.
- `item_ability` — item abilities such as `axe_strip`, `axe_scrape`, `axe_wax_off`, `shovel_flatten`, and `hoe_till`; the block must actually change.
- `disable_shield` — hitting a player or entity that is blocking with a shield, for axes.

## License

MIT
