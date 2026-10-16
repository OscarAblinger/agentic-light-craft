import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Restyle generator - maps the pre-restyle lapis lux sources onto the current
 * palette.
 *
 * Reads the pre-restyle source PNGs from a flat source directory (extract
 * them with `git show 85f9094^:<path> > <name>.png`) and writes the restyled
 * files into an output directory, mirroring the resource layout (item/ and
 * block/). Only RGB is remapped; per-pixel structure is preserved.
 * The ore keeps its stone background byte-identical. Full blocks are written
 * opaque (alpha 0 or 255), per the alpha policy.
 *
 * Committed generator (see generators/README.md). Usage:
 *
 *   javac -d build/texture-gen \
 *     .agents/skills/lightcraft-texture-style/generators/RestyleLuxFamily.java \
 *     .agents/skills/lightcraft-texture-style/scripts/TextureLib.java
 *   java -cp build/texture-gen RestyleLuxFamily \
 *     .agents/skills/lightcraft-texture-style/palette.txt <srcDir> <outDir>
 *
 * Regenerating must reproduce the committed textures in
 * src/main/resources/assets/lightcraft/textures/ byte-for-byte. Check the
 * items with --category items, the storage blocks with --category blocks, and
 * the ore with --category ores.
 */
public final class RestyleLuxFamily {

    /** Refined pieces: the bright window. */
    static final String[] BRIGHT = {
            "gold.mid", "gold.highlight", "lux.mid", "lux.light",
            "gold.specular", "lux.highlight", "lux.specular"};
    /** Raw pieces: the mild window. */
    static final String[] MILD = {
            "gold.dark", "gold.shadow", "gold.mid", "gold.highlight",
            "lux.mid", "lux.light", "gold.specular", "lux.highlight", "lux.specular"};
    /** Ore specks: the medium window. */
    static final String[] MEDIUM = {
            "gold.shadow", "gold.mid", "gold.highlight", "lux.mid", "lux.light",
            "gold.specular", "lux.highlight", "lux.specular"};

    /** { source file (flat), output path relative to outDir }. */
    static final String[][] TEX = {
            {"lapis_lux.png", "item/lapis_lux.png"},
            {"lapis_lux_nugget.png", "item/lapis_lux_nugget.png"},
            {"raw_lapis_lux.png", "item/raw_lapis_lux.png"},
            {"lapis_lux_ingot_block.png", "block/lapis_lux_ingot_block.png"},
            {"raw_lapis_lux_block.png", "block/raw_lapis_lux_block.png"},
            {"lapis_lux_ore.png", "block/lapis_lux_ore.png"},
    };

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: RestyleLuxFamily <palette.txt> <srcDir> <outDir>");
            System.exit(2);
        }
        TextureLib lib = new TextureLib(Path.of(args[0]));
        Path src = Path.of(args[1]);
        Path out = Path.of(args[2]);
        Files.createDirectories(out);
        for (String[] tex : TEX) {
            BufferedImage img = ImageIO.read(src.resolve(tex[0]).toFile());
            BufferedImage dst = render(lib, tex[0], img);
            TextureLib.save(dst, out.resolve(tex[1]));
            System.out.println("wrote " + tex[1]);
        }
    }

    /** @param name flat source file name; "raw*" uses the mild window. */
    static BufferedImage render(TextureLib lib, String name, BufferedImage img) {
        boolean isOre = name.equals("lapis_lux_ore.png");
        boolean isRaw = name.startsWith("raw");
        boolean isBlock = name.endsWith("_block.png") || isOre;
        BufferedImage dst = new BufferedImage(img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        if (isOre) {
            oreMap(img, dst, lib, MEDIUM);
            makeOpaque(dst);
        } else {
            mapWhole(img, dst, ramp(lib, isRaw ? MILD : BRIGHT), range(img));
            if (isBlock) {
                stripBorder(dst);
                makeOpaque(dst);
            }
        }
        return dst;
    }

    // ---- mapping -------------------------------------------------------------

    private static void mapWhole(BufferedImage img, BufferedImage dst, int[] ramp, double[] rng) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 255;
                if (a == 0) {
                    dst.setRGB(x, y, 0);
                    continue;
                }
                dst.setRGB(x, y, (a << 24) | nearest(ramp, rng[0], rng[1], lum(argb & 0xFFFFFF)));
            }
        }
    }

    /** Stone is copied verbatim; only the lux specks are remapped. */
    private static void oreMap(BufferedImage img, BufferedImage dst, TextureLib lib,
                               String[] luxRampName) {
        int[] luxRamp = ramp(lib, luxRampName);
        double[] lRange = range(img, "lux");
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 255;
                if (a == 0) {
                    dst.setRGB(x, y, 0);
                    continue;
                }
                int c = argb & 0xFFFFFF;
                if (isStone(c)) {
                    dst.setRGB(x, y, argb); // stone untouched
                } else {
                    dst.setRGB(x, y, (a << 24) | nearest(luxRamp, lRange[0], lRange[1], lum(c)));
                }
            }
        }
    }

    private static boolean isStone(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        // Ore stone is mid-grey (lum ~104-145); anything bright is a lux highlight.
        return (max - min) < 30 && lum(c) < 160;
    }

    private static double[] range(BufferedImage img) {
        return range(img, null);
    }

    private static double[] range(BufferedImage img, String cls) {
        double lo = 255, hi = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                if (((argb >>> 24) & 255) == 0) {
                    continue;
                }
                int c = argb & 0xFFFFFF;
                if ("stone".equals(cls) && !isStone(c)) {
                    continue;
                }
                if ("lux".equals(cls) && isStone(c)) {
                    continue;
                }
                lo = Math.min(lo, lum(c));
                hi = Math.max(hi, lum(c));
            }
        }
        if (hi < lo) {
            lo = 0;
            hi = 255;
        }
        return new double[]{lo, hi};
    }

    private static int[] ramp(TextureLib lib, String[] names) {
        int[] c = new int[names.length];
        for (int i = 0; i < names.length; i++) {
            c[i] = lib.color(names[i]) & 0xFFFFFF;
        }
        Integer[] order = new Integer[c.length];
        for (int i = 0; i < c.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingDouble(i -> lum(c[i])));
        int[] sorted = new int[c.length];
        for (int i = 0; i < c.length; i++) {
            sorted[i] = c[order[i]];
        }
        return sorted;
    }

    private static int nearest(int[] ramp, double lo, double hi, double l) {
        double t = hi > lo ? (l - lo) / (hi - lo) : 0.5;
        t = Math.max(0, Math.min(1, t));
        double target = lum(ramp[0]) + t * (lum(ramp[ramp.length - 1]) - lum(ramp[0]));
        int best = 0;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < ramp.length; i++) {
            double d = Math.abs(lum(ramp[i]) - target);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return ramp[best];
    }

    private static double lum(int c) {
        return 0.299 * ((c >> 16) & 255) + 0.587 * ((c >> 8) & 255) + 0.114 * (c & 255);
    }

    /** Replace the outermost pixel ring with the adjacent interior colours so the block tiles level. */
    private static void stripBorder(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[][] copy = new int[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                copy[x][y] = img.getRGB(x, y);
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean border = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                if (!border) {
                    continue;
                }
                int sx = x == 0 ? 1 : (x == w - 1 ? w - 2 : x);
                int sy = y == 0 ? 1 : (y == h - 1 ? h - 2 : y);
                img.setRGB(x, y, copy[sx][sy]);
            }
        }
    }

    /** Full blocks are opaque: alpha 0 or 255 only. */
    private static void makeOpaque(BufferedImage img) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 255;
                if (a > 0 && a < 255) {
                    img.setRGB(x, y, (argb & 0xFFFFFF) | 0xFF000000);
                }
            }
        }
    }
}
