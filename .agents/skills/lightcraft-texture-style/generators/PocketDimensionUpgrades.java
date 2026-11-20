import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;

/**
 * Generator for the four pocket dimension upgrade item textures
 * (src/main/resources/assets/lightcraft/textures/item/pocket_dimension_upgrade_*.png).
 *
 * Each texture is the same front-facing marble cube as the pocket dimension
 * item itself (see PocketDimensionItem.java - the base drawing is kept in sync
 * with it) with one difference: the recessed lapis window carries a nested gold
 * ring whose frame grows with the upgrade size. Bigger ring = bigger room:
 * 7x7x7 a tiny 2x2 pip, 9x9x9 a 3x3 frame, 11x11x11 a 4x4 frame and 15x15x15
 * fills the whole window with a 6x5 frame. Light comes from the top-left, so
 * the top/left of every ring is gold.mid and the bottom/right gold.shadow.
 * Colors are palette names from the locked palette; the pixel map below is the
 * drawing.
 *
 * Committed generator (see generators/README.md). Usage:
 *
 *   javac -d build/texture-gen \
 *     .agents/skills/lightcraft-texture-style/generators/PocketDimensionUpgrades.java \
 *     .agents/skills/lightcraft-texture-style/scripts/TextureLib.java
 *   java -cp build/texture-gen PocketDimensionUpgrades \
 *     .agents/skills/lightcraft-texture-style/palette.txt \
 *     src/main/resources/assets/lightcraft/textures/item
 *
 * Regenerating must reproduce the committed textures byte-for-byte. Check with
 * --category items.
 */
public final class PocketDimensionUpgrades {

    /** Legend for the pixel map; '.' stays transparent. Same as PocketDimensionItem. */
    static final Map<Character, String> LEGEND = Map.ofEntries(
            Map.entry('O', "outline.deep"),
            Map.entry('S', "marble.specular"),
            Map.entry('H', "marble.highlight"),
            Map.entry('L', "marble.light"),
            Map.entry('M', "marble.mid"),
            Map.entry('D', "marble.deep"),
            Map.entry('d', "lapis.deepest"),
            Map.entry('k', "lapis.dark"),
            Map.entry('B', "lapis.light"),
            Map.entry('c', "lapis.highlight"),
            Map.entry('W', "lapis.specular"),
            Map.entry('g', "gold.mid"),
            Map.entry('G', "gold.shadow"));

    /** 16x16 pixel map, top row first. The cube spans x3..x12, y2..y14. */
    static final String[] MAP = {
            "................",
            "................",
            "...OOOOOOOOOO...",
            "...OSSHHHHHMO...",
            "...OHLLLLLLMO...",
            "...OggggggGGO...",
            "...OLddddddMO...",
            "...OLdWcBBdMO...",
            "...OLdcBBkdMO...",
            "...OLdBBkkdMO...",
            "...OLddddddMO...",
            "...OLLLLLLLMO...",
            "...OMMMMMMMDO...",
            "...ODDDDDDDDO...",
            "...OOOOOOOOOO...",
            "................",
    };

    /** Upgrade sizes in draw order; matches PocketDimensions.UPGRADE_SIZES. */
    static final int[] SIZES = {7, 9, 11, 15};

    /** size -> {ringX, ringY, width, height} of the gold ring in the window. */
    static final Map<Integer, int[]> RINGS = Map.of(
            7, new int[] {7, 7, 2, 2},
            9, new int[] {6, 7, 3, 3},
            11, new int[] {6, 6, 4, 4},
            15, new int[] {5, 6, 6, 5});

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: PocketDimensionUpgrades <palette.txt> <outDir>");
            System.exit(2);
        }
        TextureLib lib = new TextureLib(Path.of(args[0]));
        Path outDir = Path.of(args[1]);

        for (int size : SIZES) {
            BufferedImage img = TextureLib.canvas(16, 16);
            drawBase(lib, img);
            int[] ring = RINGS.get(size);
            drawRing(lib, img, ring[0], ring[1], ring[2], ring[3]);

            Path out = outDir.resolve("pocket_dimension_upgrade_" + size + "x" + size + "x" + size + ".png");
            TextureLib.save(img, out);
            System.out.println("wrote " + out);
        }
    }

    /** Paints the shared marble cube with its lapis window (the base drawing). */
    private static void drawBase(TextureLib lib, BufferedImage img) {
        for (int y = 0; y < MAP.length; y++) {
            String row = MAP[y];
            if (row.length() != 16) {
                throw new IllegalStateException("Row " + y + " is " + row.length() + " pixels wide, expected 16");
            }
            for (int x = 0; x < row.length(); x++) {
                char pixel = row.charAt(x);
                if (pixel == '.') {
                    continue; // transparent background
                }
                String color = LEGEND.get(pixel);
                if (color == null) {
                    throw new IllegalStateException("Unknown pixel '" + pixel + "' at " + x + "," + y);
                }
                lib.set(img, x, y, color);
            }
        }
    }

    /** Overlays a hollow gold ring; top/left lit, bottom/right in shadow. */
    private static void drawRing(TextureLib lib, BufferedImage img, int x, int y, int w, int h) {
        for (int i = 0; i < w; i++) {
            lib.set(img, x + i, y, "gold.mid");            // top edge catches the light
            lib.set(img, x + i, y + h - 1, "gold.shadow"); // bottom edge falls away
        }
        for (int j = 1; j < h - 1; j++) {
            lib.set(img, x, y + j, "gold.mid");            // left edge stays lit
            lib.set(img, x + w - 1, y + j, "gold.shadow"); // right edge turns away
        }
    }
}
