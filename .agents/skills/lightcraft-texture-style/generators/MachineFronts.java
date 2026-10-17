import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * Machine front generator - draws the three style anchors in examples/.
 *
 * Committed generator (see generators/README.md). Draws only palette colours
 * through TextureLib, deterministically (no RNG). Usage:
 *
 *   javac -d build/texture-gen \
 *     .agents/skills/lightcraft-texture-style/generators/MachineFronts.java \
 *     .agents/skills/lightcraft-texture-style/scripts/TextureLib.java
 *   java -cp build/texture-gen MachineFronts \
 *     .agents/skills/lightcraft-texture-style/palette.txt \
 *     .agents/skills/lightcraft-texture-style/examples
 *
 * Outputs machine_front_01..03.png; regenerating must reproduce the committed
 * examples byte-for-byte. Check with --category machines.
 */
public final class MachineFronts {

    private static final String[] MARBLE = {
            "marble.specular", "marble.highlight", "marble.light",
            "marble.mid", "marble.shadow", "marble.deep"
    };

    // The machine front look: marble base, no steel.
    private static final String FRAME_LIGHT = "marble.specular";
    private static final String FRAME_DARK = "marble.deep";
    private static final String SHINE = "marble.specular";
    private static final String RING = "outline.deep";   // 1px recessed front edge
    private static final String DEEP = "outline.deep";   // recesses / firebox

    private static TextureLib lib;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: MachineFronts <palette.txt> <outDir>");
            System.exit(2);
        }
        lib = new TextureLib(Path.of(args[0]));
        Path out = Path.of(args[1]);
        TextureLib.save(furnace(), out.resolve("machine_front_01.png"));
        TextureLib.save(collector(), out.resolve("machine_front_02.png"));
        TextureLib.save(press(), out.resolve("machine_front_03.png"));
        System.out.println("Wrote machine_front_01..03.png to " + out);
    }

    // ---- drawing helpers -----------------------------------------------------

    private static void p(BufferedImage img, int x, int y, String color) {
        lib.set(img, x, y, color);
    }

    private static String marbleShade(int x, int y) {
        double t = ((x - 2) + (y - 2)) / 24.0;
        int i = (int) Math.round(t * (MARBLE.length - 1));
        i = Math.max(0, Math.min(MARBLE.length - 1, i));
        return MARBLE[i];
    }

    /**
     * A machine front: 1px outline.deep recessed edge ring, a marble frame with
     * a top-left shine, and a diagonal marble body to draw the machine-specific
     * features on.
     */
    private static BufferedImage panel() {
        BufferedImage img = TextureLib.canvas(16, 16);
        for (int i = 0; i < 16; i++) {
            p(img, i, 0, RING);
            p(img, i, 15, RING);
            p(img, 0, i, RING);
            p(img, 15, i, RING);
        }
        for (int i = 1; i <= 14; i++) {
            p(img, i, 1, FRAME_LIGHT);   // top
            p(img, 1, i, FRAME_LIGHT);   // left
            p(img, i, 14, FRAME_DARK);   // bottom
            p(img, 14, i, FRAME_DARK);   // right
        }
        p(img, 1, 1, SHINE);
        p(img, 2, 1, SHINE);
        p(img, 1, 2, SHINE);
        for (int y = 2; y <= 13; y++) {
            for (int x = 2; x <= 13; x++) {
                p(img, x, y, marbleShade(x, y));
            }
        }
        return img;
    }

    private static void bolts(BufferedImage img) {
        for (int[] b : new int[][]{{3, 3}, {12, 3}, {3, 12}, {12, 12}}) {
            p(img, b[0], b[1], "gold.highlight");
            p(img, b[0], b[1] + 1, "gold.dark");
        }
    }

    private static void lapisGem(BufferedImage img, int x, int y) {
        p(img, x, y, "lapis.highlight");
        p(img, x + 1, y, "lapis.mid");
        p(img, x, y + 1, "lapis.dark");
        p(img, x + 1, y + 1, "lapis.deepest");
    }

    // ---- the three fronts ----------------------------------------------------

    private static BufferedImage furnace() {
        BufferedImage img = panel();
        bolts(img);
        lapisGem(img, 7, 3);
        lapisGem(img, 7, 5);
        for (int x = 4; x <= 11; x++) {
            p(img, x, 9, DEEP);
            p(img, x, 13, DEEP);
        }
        for (int y = 9; y <= 13; y++) {
            p(img, 4, y, DEEP);
            p(img, 11, y, DEEP);
        }
        for (int y = 10; y <= 12; y++) {
            for (int x = 5; x <= 10; x++) {
                double dx = x - 7.5;
                double dy = y - 11.0;
                double d = Math.sqrt(dx * dx + dy * dy);
                String col = d < 1.2 ? "lux.specular"
                        : d < 2.2 ? "lux.highlight"
                        : d < 3.2 ? "lux.light"
                        : "lux.mid";
                p(img, x, y, col);
            }
        }
        return img;
    }

    private static BufferedImage collector() {
        BufferedImage img = panel();
        bolts(img);
        for (int y = 2; y <= 13; y++) {
            for (int x = 2; x <= 13; x++) {
                double dx = x - 7.5;
                double dy = y - 7.5;
                double d = Math.sqrt(dx * dx + dy * dy);
                String col = null;
                if (d <= 1.6) {
                    col = "lux.specular";
                } else if (d <= 2.6) {
                    col = "lux.highlight";
                } else if (d <= 3.6) {
                    col = "lapis.light";
                } else if (d <= 4.6) {
                    col = "lapis.mid";
                } else if (d <= 5.4) {
                    col = "gold.mid";
                } else if (d <= 6.2) {
                    col = "gold.shadow";
                }
                if (col != null) {
                    p(img, x, y, col);
                }
            }
        }
        return img;
    }

    private static BufferedImage press() {
        BufferedImage img = panel();
        bolts(img);
        for (int y = 3; y <= 12; y++) {
            p(img, 5, y, "gold.highlight");
            p(img, 6, y, "gold.mid");
            p(img, 9, y, "gold.mid");
            p(img, 10, y, "gold.highlight");
            p(img, 7, y, "gold.dark");
        }
        p(img, 5, 2, "gold.specular");
        p(img, 10, 2, "gold.specular");
        p(img, 5, 13, "gold.dark");
        p(img, 10, 13, "gold.dark");
        for (int y = 6; y <= 9; y++) {
            for (int x = 6; x <= 9; x++) {
                p(img, x, y, "lapis.mid");
            }
        }
        for (int y = 7; y <= 8; y++) {
            for (int x = 7; x <= 8; x++) {
                p(img, x, y, "lux.highlight");
            }
        }
        for (int x = 6; x <= 9; x++) {
            p(img, x, 12, DEEP);
        }
        return img;
    }
}
