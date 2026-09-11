# LightCraft

A Minecraft mod for NeoForge 26.1.x.

## Lapis Lux Metal

This mod adds a new iron-tier metal called "Lapis Lux" (`lightcraft:lapis_lux`) with the following variants:

### Blocks
- **`lightcraft:lapis_lux_ore`** - Main ore block, drops 3-5 Raw Lapis Lux when mined
- **`lightcraft:raw_lapis_lux_block`** - Smeltable raw material block
- **`lightcraft:lapis_lux_ingot_block`** - Ingot form for storage

### Items
- **`lightcraft:raw_lapis_lux`** - Raw material dropped from ore (stacks to 64)
- **`lightcraft:lapis_lux`** - Smelted ingot (stacks to 64)

### Properties
- **Mining level:** Iron tier (requires iron pickaxe or better)
- **XP reward:** 0.7 XP per ore block smelted
- **Smelting ratio:** 4 Raw Lapis Lux → 1 Lapis Lux ingot

> **Note:** Textures currently use gold textures as placeholders and will be replaced later.


## Project stack

- **Minecraft:** 26.1.2
- **Mod loader:** NeoForge 26.1.2.107
- **Java:** JDK 25
- **Build system:** Gradle 9.2.1

## Getting started

Import the project into IntelliJ IDEA with the MinecraftDev plugin.

Common Gradle tasks:

```bash
./gradlew build      # Build the mod jar
./gradlew runClient  # Run the Minecraft client
./gradlew runServer  # Run a dedicated server
./gradlew runData    # Generate data-driven resources
```

## Project decisions

See [research/0_basic_setup.md](research/0_basic_setup.md) for the source-of-truth project configuration.

## License

[Add your license here]
