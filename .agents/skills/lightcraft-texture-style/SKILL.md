---
name: lightcraft-texture-style
description: "Author and review LightCraft pixel-art textures so every block, item, entity, and GUI asset shares one consistent look. Use when creating, editing, or checking textures, palettes, or procedural texture generators. Not for SD/built-in image generation (use minecraft-imagegen) and not for resource-pack format or packaging (use minecraft-resource-pack)."
---

# LightCraft Texture Style

This skill keeps every pixel-art texture in the LightCraft mod visually
consistent. It defines a single locked style, a machine-readable palette, and a
checker that must pass before a texture change is committed.

## Materials & naming

- **lapis** is Latin for "stone"; alone it names nothing. Two different
  materials share the word:
  - **lapis lazuli** — the familiar blue mineral. The `[lapis]` *accent* ramp.
  - **lapis lux** — a new mineral added by this mod. Golden and self-emissive.
    The `[lux]` ramp. It is **not** a variant of lapis lazuli.
- **marble** is the machine base material (nearest vanilla block: calcite).
- Existing world assets are also allowed other, out-of-palette materials on
  purpose: **stone/ore** (ore is embedded in stone) and **plants** (browns that
  read as a sack, and greens, because they are plants). These are documented as
  their own categories rather than forced into the core palette.

## Status

The style is **not locked yet** — `palette.txt` says `STATUS: UNLOCKED`. The
palette holds draft colors with per-ramp usage notes, and
`references/style-spec.md` is still pending. While `STATUS` is `UNLOCKED`, the
checker skips palette adherence and only reports structural problems.

## Routing Boundaries

- `Use when`: creating, editing, or reviewing any LightCraft texture (block,
  item, entity, GUI, animation), or defining the palette / style rules.
- `Do not use when`: the deliverable is SD/built-in image generation or concept
  art (`minecraft-imagegen`).
- `Do not use when`: the task is resource-pack format, model JSON, blockstates,
  sounds, or packaging (`minecraft-resource-pack`).

## Supporting Files

- `references/style-spec.md` — the locked style decisions. Read it before
  drawing anything. Unknown values are recorded there as `Status: PENDING`.
- `references/categories/*.txt` — per-category rule files (for example `items.txt`,
  `machines.txt`). A category selects which ramps it may use and sets rules such
  as opaque-only and the color budget. It may only restrict the palette, never
  add colors. Pass one to the checker with `--category`.
- `references/checker-policy.md` — which rules are hard failures and which are
  advisory warnings.
- `palette.txt` — the machine-readable source of truth for allowed colors. Every
  ramp carries `use:` / `rule:` notes describing what it is for and how it may be
  used, and individual colors carry short notes. A `STATUS:` line switches
  enforcement on (`LOCKED`) or off (`UNLOCKED`, for drafting).
- `scripts/TextureCheck.java` — dependency-free checker (JDK ImageIO only).
- `scripts/TextureLib.java` — shared palette loader and drawing primitives.
- `examples/` — canonical reference textures used as visual anchors.

## Workflow

1. Read `references/style-spec.md`. If a rule you need is `PENDING`, stop and
   ask the owner instead of inventing one.
2. Read `palette.txt`. Use only those colors. Read the `use:` / `rule:` notes on
   each ramp: they say **what the color is for** and the constraint on how it may
   be used (for example `gold.dark` is shadow-only). If a color you need is not
   there, propose adding it to the right ramp with a note, and ask first. If
   `STATUS` is `UNLOCKED`, the colors are still being drafted.
3. Draw through `scripts/TextureLib.java` (or an equivalent script under
   `scripts/`) so colors and primitives are reused, not hand-picked per pixel.
   Prefer a deterministic seed for anything generated.
4. Run the checker (below) on the changed textures. Fix every `[HARD]` failure.
5. Handle the previews. The checker writes readable previews so the texture can
   be judged at its intended size:
   - If you can view images, inspect them for the soft, aesthetic rules.
   - If you cannot view images (many models cannot), rely on the checker for the
     objective rules and surface the preview paths so the user can review them.
     Do not claim a texture "looks good" when you have not actually seen it.
6. Report the checker result (hard failures and warnings), the preview paths,
   and anything you could not verify visually.

## Checker

Run from the repository root. `java` (JDK 25) is already required by the project,
so this needs no extra dependency:

```bash
java .agents/skills/lightcraft-texture-style/scripts/TextureCheck.java \
  --palette .agents/skills/lightcraft-texture-style/palette.txt \
  --category .agents/skills/lightcraft-texture-style/references/categories/machines.txt \
  --preview build/texture-previews \
  src/main/resources/assets/lightcraft/textures/block/machines
```

Arguments accept files or directories (scanned recursively for `*.png`).

| Flag | Effect |
|------|--------|
| `--palette <file>` | Palette to enforce. Enforcement is on only when the file says `STATUS: LOCKED` (or has colors and no `STATUS` line). |
| `--opaque-only` | Treat partial transparency as a hard failure. |
| `--strict` | Treat warnings as failures (use in verification). |
| `--max-colors <n>` | Soft limit for distinct colors per texture (default 24). |
| `--preview <dir>` | Write 8x and 3x3-tiled previews for visual review. |

Exit code is `1` on any hard failure, or on any warning when `--strict` is set.
See `references/checker-policy.md` for the exact hard/soft split.

Use the preview output for review: the `*.8x.png` file shows the texture at a
readable size and the `*.3x3.png` file shows tiling. These previews are for a
viewer with vision (the user, or an agent that can see images); they are written
next to nothing else, so mention their paths in the change report.

## Verification

A texture change is not complete until the checker passes and the previews have
been reviewed. Follow the project verification policy in the root `AGENTS.md` and
ask for owner feedback before committing.
