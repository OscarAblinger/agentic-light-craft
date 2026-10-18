import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;

/**
 * Generator for the pocket dimension item texture
 * (src/main/resources/assets/lightcraft/textures/item/pocket_dimension.png).
 *
 * The item is a small front-facing marble cube - the pocket room itself - with
 * a dark silhouette outline, a gold lid seam and a recessed lapis window on the
 * front face, the portal you step through. Light comes from the top-left like
 * everywhere else: specular/highlight on the top-left bevel, mid/deep on the
 * bottom-right. Colors are palette names from the locked palette; the pixel map
 * below is the drawing.
 *
 * Committed generator (see generators/README.md). Usage:
 *
 *   javac -d build/texture-gen \
 *     .agents/skills/lightcraft-texture-style/generators/PocketDimensionItem.java \
 *     .agents/skills/lightcraft-texture-style/scripts/TextureLib.java
 *   java -cp build/texture-gen PocketDimensionItem \
 *     .agents/skills/lightcraft-texture-style/palette.txt \
 *     src/main/resources/assets/lightcraft/textures/item/pocket_dimension.png
 *
 * Regenerating must reproduce the committed texture byte-for-byte. Check with
 * --category items.
 */
public final class PocketDimensionItem {

    /** Legend for the pixel map; '.' stays transparent. */
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

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: PocketDimensionItem <palette.txt> <out.png>");
            System.exit(2);
        }
        TextureLib lib = new TextureLib(Path.of(args[0]));

        BufferedImage img = TextureLib.canvas(16, 16);
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

        TextureLib.save(img, Path.of(args[1]));
        System.out.println("wrote " + args[1]);
    }
}
