# LightCraft Texture Style Specification

> **Status: DRAFT — no decisions are made yet.**
> Every section below is a question to answer with the project owner.
> Do not fill in a value on your own; record the owner's decision and its source.

How to use this file: when a decision is made, replace `PENDING` with the value
and note where it came from (reference image, existing asset, or owner's words).
Once every required section is decided, change the palette `STATUS` in
`palette.txt` to `LOCKED` and mark this file `Status: LOCKED` with a date.

A fact is only a decision if it is written here. If a rule is missing and you
need it, ask — do not guess.

---

## 1. References & anchors

- [ ] 2–5 reference images that define the target look. `PENDING`
- [ ] Which reference is the "gold standard", and why (mood, detail,
      saturation, outline). `PENDING`
- [ ] Anti-references: what it must **not** look like. `PENDING`

## 2. Palette

Source of truth: `../palette.txt` (machine-readable). Ramp structure is
described here.

- [ ] Ramp list per material, dark → mid → light. `PENDING`
- [ ] Shading steps per ramp (flat / 2-tone / 3-tone / gradient). `PENDING`
- [ ] Hue-shift policy between shades (e.g. warm highlights, cool shadows). `PENDING`
- [ ] Accent colors and their permitted uses. `PENDING`
- [ ] Maximum total colors. `PENDING`

## 3. Rendering & shading rules

- [ ] Outline style (none / dark-colored / pure black / selective) and
      thickness. `PENDING`
- [ ] Global light direction (vanilla/Blockbench default is top-left). `PENDING`
- [ ] Anti-aliasing allowed or forbidden. `PENDING`
- [ ] Dithering allowed or forbidden, and permitted patterns. `PENDING`
- [ ] Noise / surface-grain density per material. `PENDING`
- [ ] Alpha policy: opaque only, or permitted translucency values. `PENDING`
- [ ] Emissive / glow handling. `PENDING`

## 4. Geometry & resolution

- [ ] Base canvas sizes (e.g. 16x16 only, or also 32x32). `PENDING`
- [ ] Mixel policy (no scaling / single resolution). `PENDING`
- [ ] Silhouette and proportion preferences. `PENDING`

## 5. Tiling & continuity

- [ ] Seamless tiling required for block textures. `PENDING`
- [ ] Acceptable tiling-artifact tolerance. `PENDING`
- [ ] How related assets stay consistent (ore → raw block → ingot block →
      item). `PENDING`
- [ ] Overlay/connectivity conventions (grass, leaves, etc.). `PENDING`

## 6. Category specifics

- [ ] Items. `PENDING`
- [ ] Blocks. `PENDING`
- [ ] Entities (box UV, face brightness). `PENDING`
- [ ] GUI icons and slots. `PENDING`
- [ ] Animated textures (frame count, timing). `PENDING`

## 7. Naming & file conventions

- [ ] File naming, directory, namespace, and `.mcmeta` conventions. `PENDING`

## 8. Workflow & tooling

- [ ] Where generator scripts live and whether they are committed. `PENDING`
- [ ] How to run Java tooling (interpreter/command). `PENDING`
- [ ] Determinism policy (seeds and reproducibility). `PENDING`

## 9. Governance

- [ ] Scope: all textures, or new/changed only. `PENDING`
- [ ] Exceptions (vanilla-derived, CC0, third-party assets). `PENDING`
- [ ] Who approves aesthetic changes and how the spec is updated. `PENDING`

---

## Appendix: candidate checklist derived from the Minecraft style

These are the dimensions that commonly drive coherence in Minecraft-style pixel
art. They are listed for reference while filling in the decisions above; none is
a decision until recorded in a section above.

- HSV behavior and hue/saturation/value shifting between shades.
- Color ramps and restricted palettes.
- Shading: banding, pillow shading, pancake shading, unnecessary dithering,
  texture mixels, noise.
- Lines & shapes: jaggies, consistent diagonal steps.
- Item textures: midtone shape, darker outline, top-left light, surface
  properties.
- Block textures: good standalone and when tiled; check before and during
  shading.
- Entity textures: box UV; top and front brighter than bottom and back.
