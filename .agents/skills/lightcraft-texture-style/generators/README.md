# Committed texture generators

One Java file per texture family. A generator ships in the same commit as the
textures it produces, so any texture can be regenerated (and reviewed) from code
instead of memory. They draw only palette colours through
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

- `MachineFronts.java` — the three style anchors in `../examples/`
  (`machine_front_01..03.png`). Output is byte-identical to the committed
  examples. Check with `--category machines`.
- `RestyleLuxFamily.java` — the approved restyle of the lapis lux family
  (`lapis_lux`, `lapis_lux_nugget`, `raw_lapis_lux`, `raw_lapis_lux_block`,
  `lapis_lux_ingot_block`, `lapis_lux_ore` — six files). It reads the
  pre-restyle sources from a flat directory and writes the restyled PNGs into
  an output directory (mirroring `item/` and `block/`). The sources are the
  versions before the restyle commit; extract them flat, one per file, e.g.
  `git show 85f9094^:src/main/resources/assets/lightcraft/textures/item/lapis_lux.png > <srcDir>/lapis_lux.png`.
  The six outputs are byte-identical to the committed textures; the three full
  blocks are written opaque as well, per the alpha policy. Check with the
  matching categories (`items`, `blocks`, `ores`).

Note: the restyle remaps colours in place (no pixel moves or changes shape)
and the stone background of the ore stays byte-identical. The only alpha
change is the full-block normalization above: partial pixels become opaque.
