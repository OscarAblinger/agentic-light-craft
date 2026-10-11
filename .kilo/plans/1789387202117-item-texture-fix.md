# Item Texture Fix Plan

## Goal
Ensure all five LightCraft items display correct textures using existing vanilla block textures as placeholders. No PNG files are created — all changes are JSON-based.

## Final Implementation

### Block items (`BlockItem` subclasses)
These are registered in `src/main/java/dev/ablinger/lightcraft/LightCraft.java:57-69`:
- `lapis_lux_ore`
- `raw_lapis_lux_block`
- `lapis_lux_ingot_block`

For `BlockItem`s, NeoForge automatically uses the block model. The item definition JSONs at `src/main/resources/assets/lightcraft/items/` point directly to the block models:

- `lapis_lux_ore.json` → `lightcraft:block/lapis_lux_ore`
- `raw_lapis_lux_block.json` → `lightcraft:block/raw_lapis_lux_block`
- `lapis_lux_ingot_block.json` → `lightcraft:block/lapis_lux_ingot_block`

No separate item model JSONs are needed for these items.

### Standalone items
These are registered in `src/main/java/dev/ablinger/lightcraft/LightCraft.java:72-84`:
- `raw_lapis_lux`
- `lapis_lux`

Each has an item definition JSON at `src/main/resources/assets/lightcraft/items/` pointing to its item model:

- `raw_lapis_lux.json` → `lightcraft:item/raw_lapis_lux`
- `lapis_lux.json` → `lightcraft:item/lapis_lux`

The item models at `src/main/resources/assets/lightcraft/models/item/` use `minecraft:item/generated` with the corresponding block textures:

- `raw_lapis_lux.json` → `textures.layer0: "minecraft:block/raw_gold_block"`
- `lapis_lux.json` → `textures.layer0: "minecraft:block/gold_block"`

## Validation

- `./gradlew build` — successful
- Items should now display block textures in the inventory and hotbar

## Out of Scope
- Creating custom PNG texture images
- Removing the duplicate root `assets/` directory
