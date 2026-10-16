---
name: lightcraft-texture-style
description: "Author and review LightCraft pixel-art textures so all textures in the mod share one consistent look. Use when creating, editing, or checking textures, palettes, or procedural texture generators. Not for SD/built-in image generation (use minecraft-imagegen) and not for resource-pack format or packaging (use minecraft-resource-pack)."
---

# LightCraft Texture Style

This skill defines the one style that every pixel-art texture in the LightCraft
mod follows. It is made of a machine-readable palette (`palette.txt`), a style
spec (`references/style-spec.md`), per-category rules
(`references/categories/`) and a checker (`scripts/TextureCheck.java`) that
must pass before a texture change is committed.

Treat the palette and the spec as fixed: if a color or rule you need is missing,
propose the change (with a `PaletteCard` render and a checker dry run) and ask
the owner - do not invent colors or rules.

## Materials & naming

- **lapis** is Latin for "stone"; alone it names nothing. Two different
  materials share the word:
  - **lapis lazuli** — the familiar blue mineral. The `[lapis]` *accent* ramp.
  - **lapis lux** — a mineral added by this mod. Golden and self-emissive.
    The `[lux]` ramp. It is **not** a variant of lapis lazuli.
- **marble** is the machine base material (nearest vanilla block: calcite).
- Two material families are intentionally outside the palette: **stone/ore**
  (ore is embedded in stone) and **plants** (their own greens, browns and
  petal colors). Their category files declare them palette-exempt.

## Supporting files

- `references/style-spec.md` — the style decisions (look, shading, geometry,
  tiling, conventions). Read it before drawing anything. If a rule you need is
  missing, ask the owner.
- `references/categories/*.txt` — per-category rule files (`items.txt`,
  `machines.txt`, `blocks.txt`, `plants.txt`, `ores.txt`). A category selects
  which ramps it may use and sets rules such as opaque-only, tiling and the
  color budget; `plants` and `ores` declare `palette: exempt`. A category may
  only restrict the palette, never add colors. Pass one to the checker with
  `--category`.
- `references/checker-policy.md` — which rules are hard failures and which are
  advisory warnings.
- `palette.txt` — the source of truth for allowed colors, with per-ramp
  `use:` / `rule:` notes. A `STATUS:` line switches enforcement on (`LOCKED`)
  or off (`UNLOCKED`, for drafting a new palette).
- `scripts/TextureCheck.java` — dependency-free checker (JDK ImageIO only).
- `scripts/TextureLib.java` — shared palette loader and drawing primitives.
- `scripts/PaletteCard.java` — renders a palette as a review image; use it
  when proposing a palette change.
- `scripts/PaletteExtract.java` — lists the colors used by existing art; use
  it when proposing a new ramp.
- `generators/` — committed texture generators, one per texture family (see
  `generators/README.md`). A generator ships in the same commit as the
  textures it produces.
- `examples/` — reference textures (machine fronts) used as anchors;
  regenerate with `generators/MachineFronts.java`.

## Workflow

1. Read `references/style-spec.md`. If a rule you need is missing, stop and
   ask the owner instead of inventing one.
2. Read `palette.txt` and the category file for the asset. Use only those
   colors (except in the palette-exempt categories, which follow the style
   spec instead). Read the `use:` / `rule:` notes: they say **what each color
   is for** and the constraint on how it may be used (for example `gold.dark`
   is shadow-only). If a color you need is not there, propose adding it to the
   right ramp with a note, and ask first.
3. Draw through `scripts/TextureLib.java` in a generator under `generators/`
   so colors and primitives are reused, not hand-picked per pixel. Use a
   deterministic seed. Commit the generator together with the textures it
   produces.
4. Run the checker (below) on the changed textures with the matching category.
   Fix every `[HARD]` failure.
5. Handle the previews. The checker writes readable previews so the texture can
   be judged at its intended size:
   - If you can view images, inspect them for the soft, aesthetic rules.
   - If you cannot view images (many models cannot), rely on the checker for the
     objective rules and surface the preview paths so the user can review them.
     Do not claim a texture "looks good" when you have not actually seen it.
6. Report the checker result (hard failures and warnings), the preview paths,
   and anything you could not verify visually.

## Checker

Run from the repository root with the project JDK 25; the checker needs no
extra dependency:

```bash
java .agents/skills/lightcraft-texture-style/scripts/TextureCheck.java \
  --palette .agents/skills/lightcraft-texture-style/palette.txt \
  --category .agents/skills/lightcraft-texture-style/references/categories/machines.txt \
  --preview build/texture-previews \
  .agents/skills/lightcraft-texture-style/examples
```

Arguments accept files or directories (scanned recursively for `*.png`). Pass
the category file that matches the asset; for a palette-exempt category the
palette line in the output will say so.

| Flag | Effect |
|------|--------|
| `--palette <file>` | Palette to enforce. Enforcement is on only when the file says `STATUS: LOCKED` (or has colors and no `STATUS` line). |
| `--category <file>` | Category rules: restricts allowed ramps and may set `opaque-only`, `tiling: none` and the color budget; `palette: exempt` skips palette checks for that category. |
| `--opaque-only` | Treat partial transparency as a hard failure. |
| `--strict` | Treat warnings as failures (use when warnings are not expected). |
| `--max-colors <n>` | Soft limit for distinct colors per texture (default 24). |
| `--preview <dir>` | Write 8x and 3x3-tiled previews for visual review. |

Exit code is `1` on any hard failure, or on any warning when `--strict` is set.
See `references/checker-policy.md` for the exact hard/soft split.

Use the preview output for review: the `*.8x.png` file shows the texture at a
readable size and the `*.3x3.png` file shows tiling. These previews are for a
viewer with vision (the user, or an agent that can see images) and are build
artifacts only - mention their paths in the change report so they can be
reviewed.

## Verification

A texture change is not complete until the checker passes and the previews
have been reviewed (by the owner, or by another viewer with vision). Ask for
owner feedback before committing, and follow the verification policy in the
root `AGENTS.md`.
