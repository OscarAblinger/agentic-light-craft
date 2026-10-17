# LightCraft Texture Style Specification

> Follow this spec for every texture in the mod. Color values live in
> `../palette.txt`; per-category drawing rules live in `categories/`. If a rule
> you need is missing, ask the owner - do not invent one.

## 1. References & anchors

- **Anchors / reference fronts:** `examples/machine_front_01..03.png`. They
  define the look: marble body and frame, a 1px `outline.deep` recessed edge
  around the front face, bright gold and lapis accents, emissive lux windows,
  no steel.
- **Mood:** light and powered-up - the set reads bright, not grimy.
- **Anti-references (must NOT look like):** steel or other substitute-metal
  trim; dull, desaturated surfaces; dithering or noise/grain fills; dark
  full-range outlines around a whole asset; gold/gold-brown border frames on
  machines.

## 2. Palette

- **Source of truth:** `../palette.txt`. One palette for every category; an
  "advanced" tier adds extra ramps to it, never a second palette.
- **Ramps:** marble (7 colors), lapis (6), gold (6), lux (4), outline (2).
  Each is ordered dark -> light and carries `use:` / `rule:` notes - read them.
- **Shading steps:** use as many steps as the element needs (2-tone bolt,
  4-step gem, 6-step bevel); do not add in-between tones.
- **Hue policy:** the ramps already carry their hue/saturation progression
  (warm gold, cool marble, saturated lapis). Never interpolate, blend or
  hue-shift manually - pick from the steps.
- **Accents:** lapis and gold stay small so they pop; `gold.deepest` and
  `gold.dark` are shadow-only 1px lines; `lux.*` is emissive.
- **Exempt materials:** ore stone (keeps its source-art colours byte-identical)
  and plants (own greens/browns/petal colours). See their category files; the
  checker skips palette adherence there.
- **Color budget (soft):** items 16; machines/blocks/ores 24; plants 64
  (loose guard only). The checker warns above the budget. It counts distinct
  RGBA values, so alpha variants count separately.

## 3. Rendering & shading

- **Outline:** items get a dark outline (`outline.deep`, or `outline.mid` for a
  softer edge). A machine front face may carry the 1px `outline.deep` recessed
  edge ring; casing and side faces that tile stay ring-free. Storage blocks
  bevel only - no ring. `outline.*` is never a fill, a shadow, or a thick
  border.
- **Light:** top-left everywhere.
- **Anti-aliasing / alpha:**
  - Full blocks (machines, blocks, ores): alpha 0 or 255 only - partial
    transparency is a hard failure (`opaque-only` in the category).
  - Items and plants: partial transparency is allowed (soft glow, soft
    edges); the checker reports it so it is confirmed as intended.
- **Dithering:** forbidden.
- **Noise / grain:** none. Surface detail comes from ramp steps and deliberate
  features (for example `marble.vein`).
- **Emissive:** `lux.*` is self-lit; ramp outward from `lux.specular` at the
  centre. Never shade a lux color with a non-lux color.
- **Human-reviewed heuristics** (not checker rules): banding, pillow or
  pancake shading; unnecessary dithering; jaggy line steps and curve quality;
  silhouette readability at in-game size.

## 4. Geometry & resolution

- **Canvas:** 16x16 for blocks and items. No other sizes until the owner
  approves one.
- **Mixels:** none - single resolution; never fake 32x32 detail inside 16x16
  and never scale sub-parts.
- **Silhouette:** items must read at 16px on any background; block faces must
  read level and tile.
- **Entities, GUI icons, animated textures:** no rules yet - ask the owner
  before making the first one.

## 5. Tiling & continuity

- **Tiling:** full block faces (machines, blocks, ores) must tile seamlessly;
  items and crop textures do not (`tiling: none` in their categories).
- **Tolerance:** the checker warns when opposing edges differ by more than
  ~48 average RGB. Treat it as a review trigger: look at the `*.3x3.png`
  preview.
- **Related assets** stay in one family (ore -> raw -> refined): refined pieces
  use the bright window, raw pieces the mild window, the ore keeps its stone
  pixels untouched with medium lux specks, and storage blocks replace the
  outermost pixel ring with interior colours so they read level in a wall.
- **Overlay / connectivity textures:** no rules - ask the owner before
  making one.

## 6. Category specifics

| Category | File | Key rules |
|---|---|---|
| items | `categories/items.txt` | transparent bg, dark outline, budget 16, partial alpha allowed |
| machines | `categories/machines.txt` | opaque, marble body, emissive lux, front edge ring, tiles, budget 24 |
| blocks | `categories/blocks.txt` | storage/plain full blocks: opaque, tiling, no ring |
| plants | `categories/plants.txt` | palette-exempt; luxflower stages and plant items |
| ores | `categories/ores.txt` | palette-exempt stone, opaque, tiling |

## 7. Naming & file conventions

- Block textures: `src/main/resources/assets/lightcraft/textures/block/<name>.png`
- Item textures: `src/main/resources/assets/lightcraft/textures/item/<name>.png`
- `snake_case` names matching the registry name; namespace `lightcraft`.
- `.mcmeta` files only for animated textures.

## 8. Workflow & tooling

- **Generators** live under
  `.agents/skills/lightcraft-texture-style/generators/` - one Java file per
  texture family, committed together with the textures it produces.
  Regenerating must reproduce the committed files byte-for-byte; run them from
  the repository root (JDK 25) as shown in `generators/README.md`.
- **Checker:** `scripts/TextureCheck.java` with `--palette` and the matching
  `--category`; see `SKILL.md`.
- **Review:** the checker writes `*.8x.png` and `*.3x3.png` previews; they must
  be reviewed by a viewer with vision before commit.

## 9. Exceptions & approval

- **Exceptions:** if a third-party or vanilla-derived texture can never follow
  this style, record it here and mark its category exempt.
- **Approval:** the project owner approves aesthetic changes and owns this
  file and `../palette.txt`. Propose changes with a `PaletteCard` render and a
  checker dry run, and update both files together.
