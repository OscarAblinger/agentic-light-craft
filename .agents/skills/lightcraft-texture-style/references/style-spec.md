# LightCraft Texture Style Specification

> **Status: LOCKED (2026-10-17).** Every decision below was made by the project
> owner. Follow it; if a rule you need is missing, ask the owner - do not invent
> one. Color values live in `../palette.txt` (also `STATUS: LOCKED`);
> per-category drawing rules live in `categories/`.

## 1. References & anchors

- **Anchors / gold standard:** `examples/machine_front_01..03.png` (three
  unnamed machine fronts). They define the look: marble body and frame, a 1px
  `outline.deep` recessed edge around the front face, bright gold and lapis
  accents, emissive lux windows, no steel.
- **Mood:** light and powered-up - the set reads bright, not grimy.
- **Anti-references (must NOT look like):** steel or other substitute-metal
  trim; dull, desaturated surfaces; dithering or noise/grain fills; dark
  full-range outlines around a whole asset (the rejected first restyle
  attempts); gold/gold-brown border frames on machines (rejected in favour of
  the marble bevel).

## 2. Palette

- **Source of truth:** `../palette.txt`. One palette for every category.
- **Ramps:** marble (7 colors), lapis (6), gold (6), lux (4), outline (2).
  Each is ordered dark -> light and carries `use:` / `rule:` notes - read them.
- **Shading steps:** use as many steps as the element needs (2-tone bolt,
  4-step gem, 6-step bevel); do not add in-between tones.
- **Hue policy:** the ramps already carry their hue/saturation progression
  (warm gold, cool marble, saturated lapis). Never interpolate, blend or
  hue-shift manually - pick from the steps.
- **Accents:** lapis and gold stay small so they pop; `gold.deepest` and
  `gold.dark` are shadow-only 1px lines; `lux.*` is emissive.
- **Exempt materials (owner decision):** ore stone (byte-identical to the
  source art) and plants (own greens/browns/petal colours). See their
  category files; the checker skips palette adherence there.
- **Color budget (soft):** items 16; machines/blocks/ores 24; plants 64
  (loose guard only). The checker warns above the budget. It counts distinct
  RGBA values, so alpha variants count separately.

## 3. Rendering & shading

- **Outline:** items get a dark outline (`outline.deep`, or `outline.mid` for a
  softer edge). A machine front face may carry the 1px `outline.deep` recessed
  edge ring (as the anchors do); casing and side faces that tile stay
  ring-free. Storage blocks bevel only - no ring. `outline.*` is never a fill,
  a shadow, or a thick border.
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
- **Human-reviewed heuristics** (not checker rules): no banding or pillow
  shading; consistent diagonal steps (no jaggies); silhouette readability at
  in-game size.

## 4. Geometry & resolution

- **Canvas:** 16x16 for blocks and items (everything in use today). No other
  sizes until the owner approves one.
- **Mixels:** none - single resolution; never fake 32x32 detail inside 16x16
  and never scale sub-parts.
- **Silhouette:** items must read at 16px on any background; block faces must
  read level and tile.
- **Not used yet:** entities, GUI icons, animated textures. No rules are
  locked for them; ask the owner when the first one is made.

## 5. Tiling & continuity

- **Tiling:** full block faces (machines, blocks, ores) must tile seamlessly;
  items and crop textures do not (`tiling: none` in their categories).
- **Tolerance:** the checker warns when opposing edges differ by more than
  ~48 average RGB. Treat it as a review trigger: look at the `*.3x3.png`
  preview.
- **Related assets** stay in one family (ore -> raw -> refined). The approved
  lapis lux restyle is the reference: refined pieces use the bright window,
  raw pieces the mild window, the ore keeps its stone byte-identical with
  medium lux specks, and storage blocks replace the outermost pixel ring with
  interior colours so they read level in a wall.
- **Overlay / connectivity textures:** none in use.

## 6. Category specifics

| Category | File | Key rules |
|---|---|---|
| items | `categories/items.txt` | transparent bg, dark outline, budget 16, partial alpha allowed |
| machines | `categories/machines.txt` | opaque, marble body, emissive lux, front edge ring, tiles, budget 24 |
| blocks | `categories/blocks.txt` | storage/plain full blocks: opaque, tiling, no ring |
| plants | `categories/plants.txt` | palette-exempt; luxflower stages and plant items |
| ores | `categories/ores.txt` | palette-exempt stone, opaque, tiling |

- Entities, GUI icons and animated textures: see section 4.
- Advanced tiers add extra ramps to this palette; they never get a second one.

## 7. Naming & file conventions

- Block textures: `src/main/resources/assets/lightcraft/textures/block/<name>.png`
- Item textures: `src/main/resources/assets/lightcraft/textures/item/<name>.png`
- `snake_case` names matching the registry name; namespace `lightcraft`.
- `.mcmeta` files only for animated textures (none exist yet).

## 8. Workflow & tooling

- **Generators are committed** under
  `.agents/skills/lightcraft-texture-style/generators/` - one Java file per
  texture family, committed together with the textures it produces. The two
  committed generators reproduce their outputs byte-for-byte.
- **Run Java tooling** from the repository root (JDK 25):

  ```bash
  javac -d build/texture-gen \
    .agents/skills/lightcraft-texture-style/generators/<Name>.java \
    .agents/skills/lightcraft-texture-style/scripts/TextureLib.java
  java -cp build/texture-gen <Name> \
    .agents/skills/lightcraft-texture-style/palette.txt <outDir>
  ```

  `RestyleLuxFamily` takes an extra `<srcDir>` argument (the pre-restyle
  sources) before `<outDir>`; `generators/README.md` has the exact
  invocations.

- **Checker:** `scripts/TextureCheck.java` with `--palette` and the matching
  `--category`; see `SKILL.md`.
- **Review:** the checker writes `*.8x.png` and `*.3x3.png` previews; they must
  be reviewed by a viewer with vision before commit.
- **Determinism:** fixed seeds; regenerating must reproduce the committed
  files.

## 9. Governance

- **Scope:** every texture in the mod.
- **Current state:** the lapis lux family is on-palette (approved restyle);
  ore stone and plants are exempt by category. Nothing is outstanding.
- **Exceptions:** none for third-party or vanilla-derived textures today. If
  one is added, record it here and mark the file's category exempt.
- **Approval:** the project owner approves aesthetic changes and owns this
  file and `palette.txt`. Propose changes with a `PaletteCard` render and a
  checker dry run, and update both files together so their status stays in
  sync.
