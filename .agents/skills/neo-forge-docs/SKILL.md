---
name: neo-forge-docs
description: "Index and reference guide for the official NeoForge documentation at docs.neoforged.net. Provides structured navigation of getting started guides, core concepts, toolchain setup, version migration primers, and user documentation for NeoForge mod development."
---
# NeoForge Documentation Skill

This skill provides a structured index and quick-reference guide for the official NeoForge documentation at **https://docs.neoforged.net/** (also at **https://docs.neoforged.net/docs/**).

## Purpose

Use this skill when you need to:
- Find specific NeoForge API documentation sections
- Navigate version migration guides (primers)
- Locate toolchain/plugin configuration guides
- Reference user installation and troubleshooting guides
- Quickly find blocks, items, entities, or networking docs

## Documentation Structure

The NeoForge documentation is organized into these main sections:

### Getting Started
- Mod files, structuring your mod, versioning
- Concepts: registries, sides, events
- [Full listing](/neo-forge-docs-index.md#getting-started)

### Core Concepts
- Blocks, items, entities, block entities
- Data storage, worldgen, networking, rendering
- [Full listing](/neo-forge-docs-index.md/core-concepts)

### Toolchain
- ModDevGradle and NeoGradle plugins
- Dependencies, Parchment mappings, caching
- [Full listing](/neo-forge-docs-index.md/toolchain)

### Primers (Version Migration)
- Migration guides from 1.20→1.21, 1.21.x→26.1, etc.
- Neo-specific changes per version
- [Full listing](/neo-forge-docs-index.md/primers)

### User Guide
- Client/server installation
- Third-party launchers, troubleshooting & FAQ
- [Full listing](/neo-forge-docs-index.md/user-guide)

### Modpack Development
- Modpack features and tools
- [Full listing](/neo-forge-docs-index.md/modpack-development)

### Reference
- Registries, config, codecs, identifiers
- Events, sides concepts
- [Full listing](/neo-forge-docs-index.md/reference)

## Quick Navigations

| Topic | Documentation Path |
|-------|-------------------|
| **Blocks** | <a href="https://docs.neoforged.net/docs/blocks/" target="_blank">https://docs.neoforged.net/docs/blocks</a> |
| **Items** | <a href="https://docs.neoforged.net/docs/items/" target="_blank">https://docs.neoforged.net/docs/items</a> |
| **Entities** | <a href="https://docs.neoforged.net/docs/entities/" target="_blank">https://docs.neoforged.net/docs/entities</a> |
| **Block Entities** | <a href="https://docs.neoforged.net/docs/blockentities/" target="_blank">https://docs.neoforged.net/docs/blockentities</a> |
| **Networking** | <a href="https://docs.neoforged.net/docs/networking/" target="_blank">https://docs.neoforged.net/docs/networking</a> |
| **Rendering** | <a href="https://docs.neoforged.net/docs/rendering/feature" target="_blank">https://docs.neoforged.net/docs/rendering/feature</a> |
| **Worldgen** | <a href="https://docs.neoforged.net/docs/worldgen/biomemodifier" target="_blank">https://docs.neoforged.net/docs/worldgen/biomemodifier</a> |
| **Data Storage** | <a href="https://docs.neoforged.net/docs/datastorage/nbt" target="_blank">https://docs.neoforged.net/docs/datastorage/nbt</a> |
| **Registries** | <a href="https://docs.neoforged.net/docs/concepts/registries" target="_blank">https://docs.neoforged.net/docs/concepts/registries</a> |
| **ModDevGradle** | <a href="https://docs.neoforged.net/toolchain/docs/plugins/mdg/" target="_blank">https://docs.neoforged.net/toolchain/docs/plugins/mdg</a> |
| **NeoGradle** | <a href="https://docs.neoforged.net/toolchain/docs/plugins/ng/" target="_blank">https://docs.neoforged.net/toolchain/docs/plugins/ng</a> |
| **Primers** | <a href="https://docs.neoforged.net/primer/docs/" target="_blank">https://docs.neoforged.net/primer/docs</a> |
| **User Guide** | <a href="https://docs.neoforged.net/user/docs/" target="_blank">https://docs.neoforged.net/user/docs</a> |

## Usage

When you need NeoForge documentation:

1. **Search the index** at `/neo-forge-docs-index.md` for your topic
2. **Navigate directly** to the documentation path listed
3. **Use the search** functionality within the docs site for specific terms

The index file is located at `.agents/skills/neo-forge-docs/neo-forge-docs-index.md` and contains the complete structured reference.

## Related Skills

- **minecraft-modding**: For actual mod development code and patterns
- **minecraft-neoforge-agentic**: For NeoForge mod implementation workflows
- **elevated-gradle-run**: For running Gradle build/test tasks