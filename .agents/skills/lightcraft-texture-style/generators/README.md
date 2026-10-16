# Committed texture generators

One Java file per texture family. A generator ships in the same commit as the
textures it produces, so any texture can be regenerated (and reviewed) from
code instead of memory. They draw only palette colours through
`../scripts/TextureLib.java` and are deterministic (no RNG).

Run from the repository root with the project JDK 25:

```bash
javac -d build/texture-gen \
  .agents/skills/lightcraft-texture-style/generators/MachineFronts.java \
  .agents/skills/lightcraft-texture-style/scripts/TextureLib.java
java -cp build/texture-gen MachineFronts \
  .agents/skills/lightcraft-texture-style/palette.txt \
  .agents/skills/lightcraft-texture-style/examples
```

- `MachineFronts.java` — draws the three style anchors in `../examples/`
  (`machine_front_01..03.png`). Regenerating must reproduce them byte-for-byte.
  Check with `--category machines`.
- `RestyleLuxFamily.java` — restyles the lapis lux family (`lapis_lux`,
  `lapis_lux_nugget`, `raw_lapis_lux`, `raw_lapis_lux_block`,
  `lapis_lux_ingot_block`, `lapis_lux_ore` — six files). It reads the
  pre-restyle sources from a flat directory and writes the restyled PNGs into
  an output directory (mirroring `item/` and `block/`); the three full blocks
  are written opaque, per the alpha policy. Extract the sources from git
  history, one per file, e.g.
  `git show 85f9094^:src/main/resources/assets/lightcraft/textures/item/lapis_lux.png > <srcDir>/lapis_lux.png`.
  Regenerating must reproduce the committed textures byte-for-byte. Check with
  the matching categories (`items`, `blocks`, `ores`).

Note: the restyle remaps colours in place (no pixel moves or changes shape)
and the stone background of the ore stays byte-identical. The only alpha
change is the full-block normalization above: partial pixels become opaque.
