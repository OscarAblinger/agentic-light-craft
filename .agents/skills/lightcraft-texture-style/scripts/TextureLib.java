import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal shared drawing helpers for LightCraft textures.
 *
 * Dependency-free (JDK only). Use this from a generator script so colors and
 * primitives are reused instead of hand-picked per pixel, which is the main
 * lever for a consistent look. Pair generated output with TextureCheck.
 *
 * Example:
 *
 *   var lib = new TextureLib(Paths.get(palettePath));
 *   var img = TextureLib.canvas(16, 16);
 *   lib.fill(img, "stone.mid");
 *   lib.set(img, 3, 4, "stone.light");
 *   TextureLib.clear(img, 0, 0);
 *   TextureLib.save(img, Paths.get("src/main/resources/assets/lightcraft/textures/block/my_block.png"));
 */
public final class TextureLib {

    private final Map<String, Integer> byHex = new LinkedHashMap<>();
    private final Map<String, Integer> byName = new LinkedHashMap<>();

    /** Load a palette file. Colors outside the palette cannot be produced by name. */
    public TextureLib(Path paletteFile) throws IOException {
        for (String raw : Files.readAllLines(paletteFile, StandardCharsets.UTF_8)) {
            String line = stripComment(raw);
            if (line.isEmpty() || line.startsWith("[")) {
                continue;
            }
            String lower = line.toLowerCase();
            if (lower.startsWith("status:") || lower.startsWith("title:")) {
                continue; // file directives, not colors
            }
            Matcher m = Pattern.compile("(?i)(?<![0-9a-f])[0-9a-f]{6}(?![0-9a-f])").matcher(line);
            if (!m.find()) {
                continue;
            }
            int rgb = Integer.parseInt(m.group(), 16) & 0xFFFFFF;
            byHex.putIfAbsent(m.group().toUpperCase(), rgb);
            String rest = line.substring(m.end()).trim();
            if (!rest.isEmpty()) {
                byName.putIfAbsent(rest, rgb);
                for (String name : rest.split("[\\s,]+")) {
                    byName.putIfAbsent(name, rgb);
                }
            }
        }
    }

    /**
     * Trim a palette line and cut any trailing {@code #} note, so a line like
     * {@code AAAAA6  marble.deep   # under-edges only} keeps only the color name.
     * A line that starts with {@code #} becomes empty (a full-line comment).
     */
    private static String stripComment(String raw) {
        String line = raw.trim();
        int hash = line.indexOf('#');
        if (hash == 0) {
            return "";
        }
        if (hash > 0) {
            line = line.substring(0, hash).trim();
        }
        return line;
    }

    /** Resolve a {@code #RRGGBB} string or a palette name to an opaque ARGB color. */
    public int color(String hexOrName) {
        String key = hexOrName.startsWith("#") ? hexOrName.substring(1) : hexOrName;
        Integer rgb = byHex.get(key.toUpperCase());
        if (rgb == null) {
            rgb = byName.get(hexOrName);
        }
        if (rgb == null) {
            throw new IllegalArgumentException("Not in palette: " + hexOrName);
        }
        return 0xFF000000 | rgb;
    }

    /** All hex colors known to the palette (without leading '#'). */
    public Set<String> hexColors() {
        return Collections.unmodifiableSet(byHex.keySet());
    }

    /** A transparent ARGB canvas. */
    public static BufferedImage canvas(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    public void set(BufferedImage img, int x, int y, String color) {
        img.setRGB(x, y, color(color));
    }

    /** Clear a pixel to fully transparent. */
    public static void clear(BufferedImage img, int x, int y) {
        img.setRGB(x, y, 0);
    }

    public void fill(BufferedImage img, String color) {
        int argb = color(color);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.setRGB(x, y, argb);
            }
        }
    }

    /**
     * Draw a texture from an ASCII template, one character per pixel.
     * {@code '.'} and space are transparent. Every other character must map to a
     * palette color in {@code legend}. This keeps pixel art readable and
     * deterministic while staying strictly palette-bound.
     */
    public BufferedImage fromTemplate(String[] rows, Map<Character, String> legend) {
        int h = rows.length;
        int w = rows[0].length();
        BufferedImage img = canvas(w, h);
        for (int y = 0; y < h; y++) {
            String row = rows[y];
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c == '.' || c == ' ') {
                    continue;
                }
                String name = legend.get(c);
                if (name == null) {
                    throw new IllegalArgumentException("No legend entry for '" + c + "'");
                }
                img.setRGB(x, y, color(name));
            }
        }
        return img;
    }

    public static void save(BufferedImage img, Path out) throws IOException {
        Path parent = out.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ImageIO.write(img, "png", out.toFile());
    }

    /** Deterministic RNG for reproducible generation. */
    public static Random rng(long seed) {
        return new Random(seed);
    }
}
