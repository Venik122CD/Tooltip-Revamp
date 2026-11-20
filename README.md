# Tooltip Revamp

Tooltip Revamp overhauls Minecraft item tooltips and adds discoverable item interactions.

## Features
- Pure black tooltip background with a dark gray border.
- Centered text with consistent spacing.
- Item names displayed in a separate framed header.
- Hold **Ctrl** to open the item actions page.
- Undiscovered action text is concealed until the player performs the corresponding action.
- Discoveries are saved to player data and remain unlocked permanently.

## Build
This project targets Minecraft **1.21.1**, NeoForge, and Java 21.
Use Gradle 8.14 or newer: `gradle build`.
The built JAR is generated in `build/libs`.

## Commands
- `/tooltiprevamp reset` — reset all discovered actions.
- `/tooltiprevamp reveal_all` — reveal every action.

## Item descriptions
Use translation keys in the format `tooltiprevamp.description.<namespace>.<item_path>`.

## Datapack-defined item actions
Place JSON files under `data/<namespace>/item_actions/<name>.json`.
Text markup: `[word]` is a visible blue hint, `{word}` is a concealed blue hint block, and `<word>` is visible gray text.
Supported triggers: `bonemeal`, `use_on_block`, `item_ability`, and `disable_shield`.

## License
MIT
