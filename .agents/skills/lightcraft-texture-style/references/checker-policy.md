# Checker Policy: Hard vs Soft Rules

`scripts/TextureCheck.java` separates objective problems from judgement calls.
Hard rules fail the task; soft rules warn and are reviewed by a human. With
`--strict`, warnings also fail (use it in verification).

## Hard failures (objective)

| Rule | Detail |
|------|--------|
| Readable PNG | The file must be a valid PNG that `ImageIO` can decode. |
| Dimensions | Width and height must each be a power of two, and the image must be square **or** an animation multiple (`height % width == 0`). |
| Palette adherence | Every opaque pixel must be a color from `palette.txt` when that file says `STATUS: LOCKED` (or has colors and no `STATUS` line). Skipped while `STATUS` is `UNLOCKED`. |
| Category ramps | With `--category`, a color whose ramp is not in the category's `allow:` list is a failure (same as off-palette). |
| Opaque-only (opt-in) | With `--opaque-only`, any pixel with alpha `1–254` is a failure. |

## Warnings (judgement / review)

| Rule | Detail |
|------|--------|
| Partial transparency | Any pixel with alpha `1–254` is reported for confirmation. |
| Distinct color count | More than `--max-colors` (default 24) distinct colors is reported. |
| Possible seam | Opposite edges differ by more than ~48 average RGB; review tiling in the `*.3x3.png` preview. |

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
