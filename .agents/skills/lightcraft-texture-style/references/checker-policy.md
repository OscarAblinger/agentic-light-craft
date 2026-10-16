# Checker Policy: Hard vs Soft Rules

`scripts/TextureCheck.java` separates objective problems from judgement calls.
Hard rules fail the task; soft rules warn and are reviewed by a human. With
`--strict`, warnings also fail (use it when no warning is expected).

## Hard failures (objective)

| Rule | Detail |
|------|--------|
| Readable PNG | The file must be a valid PNG that `ImageIO` can decode. |
| Dimensions | Width and height must each be a power of two, and the image must be square **or** an animation multiple (`height % width == 0`). |
| Palette adherence | Every non-transparent pixel (alpha > 0) must be a color from `palette.txt` when that file says `STATUS: LOCKED` (or has colors and no `STATUS` line). Skipped while `STATUS` is `UNLOCKED` and for categories that declare `palette: exempt`. |
| Category ramps | With `--category`, a color whose ramp is not in the category's `allow:` list is a failure (same as off-palette). Not applied when the category is `palette: exempt`. |
| Opaque-only (opt-in) | With `--opaque-only`, or when the category says `opaque-only: true`, any pixel with alpha `1–254` is a failure. |

## Warnings (judgement / review)

| Rule | Detail |
|------|--------|
| Partial transparency | Any pixel with alpha `1–254` is reported for confirmation (unless opaque-only applies, where it is a hard failure instead). |
| Distinct color count | More than the budget distinct RGBA values are reported; alpha variants count separately. Default 24; override with `--max-colors` or the category's `max-colors:`. |
| Possible seam | Opposite edges differ by more than ~48 average RGB; review tiling in the `*.3x3.png` preview. Suppressed when the category says `tiling: none`. |

Partial transparency in `items` and `plants` is intentional (soft glow / soft
edges); the warning only asks for confirmation - it is not a failure.

## Category directives that change enforcement

- `palette: exempt` - skip palette adherence and `allow:` filtering (used by
  `plants` and `ores`; structural checks still run).
- `opaque-only: true` - partial transparency becomes a hard failure (full
  block categories).
- `tiling: none` - suppress the seam warning (items, plants).
- `max-colors: n` - per-category soft budget (items 16, machines/blocks/ores
  24, plants 64).

## Heuristics that remain human-reviewed

These cannot be checked reliably from pixels alone and stay in the style spec as
review guidance, not as checker rules:

- Banding, pillow shading, pancake shading.
- Necessary vs unnecessary dithering.
- Jaggy line steps and curve quality.
- Whether a silhouette reads at in-game size.

## Extending the checker

Add a new **hard** rule only when it is objective and unambiguous (for example a
duplicate-file-name or missing-`mcmeta` check). Anything aesthetic belongs in
`style-spec.md` and, at most, as a warning.
