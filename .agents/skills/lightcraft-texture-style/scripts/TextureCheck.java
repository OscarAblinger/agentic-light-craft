import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LightCraft texture checker.
 *
 * Dependency-free: uses only the JDK (javax.imageio). Run it with a single-file
 * source launch, for example:
 *
 *   java .agents/skills/lightcraft-texture-style/scripts/TextureCheck.java \
 *     --palette .agents/skills/lightcraft-texture-style/palette.txt \
 *     --preview build/texture-previews \
 *     src/main/resources/assets/lightcraft/textures
 *
 * Hard checks (exit code 1):
 *   - the file cannot be read as a PNG
 *   - dimensions are not a power of two, or are neither square nor an
 *     animation multiple (height % width == 0)
 *   - the texture uses a color outside the locked palette
 *   - --opaque-only is set and the texture uses partial transparency
 *
 * Soft checks (warnings; fail only with --strict):
 *   - the texture uses partial transparency
 *   - the texture uses more than --max-colors distinct colors
 *   - opposing edges differ strongly (possible visible tiling seam)
 *
 * See references/checker-policy.md for the policy behind this split.
 */
public final class TextureCheck {

    private static final Pattern HEX =
            Pattern.compile("(?i)(?<![0-9a-f])[0-9a-f]{6}(?![0-9a-f])");
    private static final int SEAM_THRESHOLD = 48;

    private TextureCheck() {
    }

    public static void main(String[] args) throws IOException {
        List<Path> inputs = new ArrayList<>();
        Path paletteFile = null;
        Path previewDir = null;
        boolean opaqueOnly = false;
        boolean strict = false;
        int maxColors = 24;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--palette" -> paletteFile = Paths.get(args[++i]);
                case "--preview" -> previewDir = Paths.get(args[++i]);
                case "--opaque-only" -> opaqueOnly = true;
                case "--strict" -> strict = true;
                case "--max-colors" -> maxColors = Integer.parseInt(args[++i]);
                case "-h", "--help" -> {
                    printUsage();
                    return;
                }
                default -> {
                    if (a.startsWith("--")) {
                        System.err.println("Unknown option: " + a);
                        System.exit(2);
                    }
                    inputs.add(Paths.get(a));
                }
            }
        }

        if (inputs.isEmpty()) {
            printUsage();
            System.exit(2);
        }

        Map<String, Integer> palette =
                paletteFile != null ? loadPalette(paletteFile) : new LinkedHashMap<>();
        boolean paletteLocked = !palette.isEmpty();

        List<Path> files = new ArrayList<>();
        for (Path in : inputs) {
            collectPngs(in, files);
        }
        if (files.isEmpty()) {
            System.err.println("No PNG files found in: " + inputs);
            System.exit(2);
        }

        int hardFailures = 0;
        int filesWithWarnings = 0;

        System.out.println("TextureCheck: " + files.size() + " file(s)");
        System.out.println("Palette: " + (paletteLocked
                ? paletteFile + " (" + palette.size() + " colors)"
                : "unlocked - palette checks skipped"));
        System.out.println();

        for (Path f : files) {
            List<String> hard = new ArrayList<>();
            List<String> soft = new ArrayList<>();

            BufferedImage img;
            try {
                img = ImageIO.read(f.toFile());
            } catch (IOException e) {
                img = null;
                hard.add("cannot read PNG: " + e.getMessage());
            }
            if (img == null && hard.isEmpty()) {
                hard.add("not a readable PNG");
            }

            if (img != null) {
                checkDimensions(img, hard);
                analyzePixels(img, paletteLocked, palette, opaqueOnly, maxColors, hard, soft);
                if (previewDir != null) {
                    writePreviews(img, f, previewDir);
                }
            }

            if (!hard.isEmpty()) {
                hardFailures++;
            }
            if (!soft.isEmpty()) {
                filesWithWarnings++;
            }
            if (!hard.isEmpty() || !soft.isEmpty()) {
                System.out.println(f);
                for (String e : hard) {
                    System.out.println("  [HARD] " + e);
                }
                for (String w : soft) {
                    System.out.println("  [warn] " + w);
                }
            }
        }

        System.out.println();
        System.out.println("Result: " + hardFailures + " hard failure(s), "
                + filesWithWarnings + " file(s) with warnings");
        boolean failed = hardFailures > 0 || (strict && filesWithWarnings > 0);
        System.exit(failed ? 1 : 0);
    }

    private static void checkDimensions(BufferedImage img, List<String> hard) {
        int w = img.getWidth();
        int h = img.getHeight();
        if (!isPowerOfTwo(w) || !isPowerOfTwo(h)) {
            hard.add("dimensions " + w + "x" + h + " are not powers of two");
            return;
        }
        if (w != h && h % w != 0) {
            hard.add("dimensions " + w + "x" + h
                    + " are neither square nor an animation multiple (height % width)");
        }
    }

    private static void analyzePixels(BufferedImage img, boolean paletteLocked,
                                      Map<String, Integer> palette, boolean opaqueOnly,
                                      int maxColors, List<String> hard, List<String> soft) {
        int w = img.getWidth();
        int h = img.getHeight();
        Set<Integer> colors = new LinkedHashSet<>();
        Map<Integer, Integer> offPalette = new LinkedHashMap<>();
        int partial = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) {
                    continue;
                }
                if (a < 255) {
                    partial++;
                }
                colors.add(argb);
                int rgb = argb & 0xFFFFFF;
                if (paletteLocked && !palette.containsKey(rgbHex(rgb))) {
                    offPalette.merge(rgb, 1, Integer::sum);
                }
            }
        }

        if (paletteLocked && !offPalette.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (Map.Entry<Integer, Integer> e : offPalette.entrySet()) {
                if (shown == 8) {
                    sb.append(", ...");
                    break;
                }
                if (shown > 0) {
                    sb.append(", ");
                }
                sb.append('#').append(rgbHex(e.getKey())).append(" x").append(e.getValue());
                shown++;
            }
            hard.add("uses " + offPalette.size()
                    + " color(s) outside the locked palette: " + sb);
        }

        if (partial > 0) {
            String msg = partial + " pixel(s) use partial transparency (alpha 1-254)";
            if (opaqueOnly) {
                hard.add(msg + " but --opaque-only is set");
            } else {
                soft.add(msg + " - confirm this is intended");
            }
        }

        if (colors.size() > maxColors) {
            soft.add("uses " + colors.size() + " distinct colors (soft limit " + maxColors + ")");
        }

        int edge = edgeContrast(img);
        if (edge > SEAM_THRESHOLD) {
            soft.add("opposing edges differ by ~" + edge
                    + " (avg RGB) - review tiling in the 3x3 preview");
        }
    }

    private static int edgeContrast(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        long sum = 0;
        int n = 0;
        for (int y = 0; y < h; y++) {
            sum += rgbDiff(img.getRGB(0, y), img.getRGB(w - 1, y));
            n++;
        }
        for (int x = 0; x < w; x++) {
            sum += rgbDiff(img.getRGB(x, 0), img.getRGB(x, h - 1));
            n++;
        }
        return n == 0 ? 0 : (int) (sum / n);
    }

    private static int rgbDiff(int a, int b) {
        int dr = Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
        int dg = Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF));
        int db = Math.abs((a & 0xFF) - (b & 0xFF));
        return (dr + dg + db) / 3;
    }

    private static void writePreviews(BufferedImage img, Path src, Path dir) throws IOException {
        Files.createDirectories(dir);
        String base = src.getFileName().toString().replaceAll("(?i)\\.png$", "");
        saveScaled(img, dir.resolve(base + ".8x.png"), 8, 1);
        saveScaled(img, dir.resolve(base + ".3x3.png"), 4, 3);
    }

    private static void saveScaled(BufferedImage img, Path out, int scale, int tiles)
            throws IOException {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dst = new BufferedImage(w * scale * tiles, h * scale * tiles,
                BufferedImage.TYPE_INT_ARGB);
        for (int ty = 0; ty < tiles; ty++) {
            for (int tx = 0; tx < tiles; tx++) {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int argb = img.getRGB(x, y);
                        int px0 = (tx * w + x) * scale;
                        int py0 = (ty * h + y) * scale;
                        for (int sy = 0; sy < scale; sy++) {
                            for (int sx = 0; sx < scale; sx++) {
                                dst.setRGB(px0 + sx, py0 + sy, argb);
                            }
                        }
                    }
                }
            }
        }
        ImageIO.write(dst, "png", out.toFile());
    }

    private static void collectPngs(Path in, List<Path> out) throws IOException {
        if (Files.isDirectory(in)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(in)) {
                for (Path p : ds) {
                    collectPngs(p, out);
                }
            }
        } else if (Files.isRegularFile(in)
                && in.getFileName().toString().toLowerCase().endsWith(".png")) {
            out.add(in);
        }
    }

    private static Map<String, Integer> loadPalette(Path file) throws IOException {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) {
                continue;
            }
            Matcher m = HEX.matcher(line);
            if (m.find()) {
                map.putIfAbsent(m.group().toUpperCase(), map.size());
            }
        }
        return map;
    }

    private static String rgbHex(int rgb) {
        return String.format("%06X", rgb & 0xFFFFFF);
    }

    private static boolean isPowerOfTwo(int v) {
        return v > 0 && (v & (v - 1)) == 0;
    }

    private static void printUsage() {
        System.out.println("Usage: java TextureCheck.java [options] <file-or-dir>...");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --palette <file>   Palette file (RRGGBB tokens, # comments, [ramp] groups).");
        System.out.println("                     Empty/unlocked palette skips palette checks.");
        System.out.println("  --opaque-only      Treat partial transparency as a hard failure.");
        System.out.println("  --strict           Treat warnings as failures.");
        System.out.println("  --max-colors <n>   Soft limit for distinct colors per texture (default 24).");
        System.out.println("  --preview <dir>    Write 8x and 3x3-tiled previews for review.");
    }
}
