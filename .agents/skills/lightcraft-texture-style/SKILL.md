---
name: lightcraft-texture-style
description: "Author and review LightCraft pixel-art textures so every block, item, entity, and GUI asset shares one consistent look. Use when creating, editing, or checking textures, palettes, or procedural texture generators. Not for SD/built-in image generation (use minecraft-imagegen) and not for resource-pack format or packaging (use minecraft-resource-pack)."
---

# LightCraft Texture Style

This skill keeps every pixel-art texture in the LightCraft mod visually
consistent. It defines a single locked style, a machine-readable palette, and a
checker that must pass before a texture change is committed.

## Status

The style is **not locked yet**. The palette (`palette.txt`) and the rules
(`references/style-spec.md`) are placeholders to be designed with the project
owner. Until `palette.txt` contains colors, the checker only reports structural
problems and skips palette checks.

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
- `references/checker-policy.md` — which rules are hard failures and which are
  advisory warnings.
- `palette.txt` — the machine-readable source of truth for allowed colors.
- `scripts/TextureCheck.java` — dependency-free checker (JDK ImageIO only).
- `scripts/TextureLib.java` — shared palette loader and drawing primitives.
- `examples/` — canonical reference textures used as visual anchors.

## Workflow

1. Read `references/style-spec.md`. If a rule you need is `PENDING`, stop and
   ask the owner instead of inventing one.
2. Read `palette.txt`. Use only those colors. If it is unlocked, propose the
   colors you intend to use and ask before proceeding.
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
  --preview build/texture-previews \
  src/main/resources/assets/lightcraft/textures
```

Arguments accept files or directories (scanned recursively for `*.png`).

| Flag | Effect |
|------|--------|
| `--palette <file>` | Palette to enforce. Unlocked/empty skips palette checks. |
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
