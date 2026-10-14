import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Palette extractor: report the colors used by one or more PNGs.
 *
 * Dependency-free (JDK only). Useful when building a style palette from
 * existing textures.
 *
 *   java .agents/skills/lightcraft-texture-style/scripts/PaletteExtract.java \
 *     --top 40 --merge 10 src/main/resources/assets/lightcraft/textures
 *
 * Options:
 *   --top <n>        Show at most n clusters (default 40).
 *   --min-alpha <a>  Ignore pixels with alpha below a (default 1).
 *   --merge <d>      Merge colors within Euclidean RGB distance d (default 0).
 *   --min-count <n>  Ignore clusters with fewer than n pixels (default 1).
 */
public final class PaletteExtract {

    private PaletteExtract() {
    }

    public static void main(String[] args) throws IOException {
        List<Path> inputs = new ArrayList<>();
        int top = 40;
        int minAlpha = 1;
        int merge = 0;
        int minCount = 1;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--top" -> top = Integer.parseInt(args[++i]);
                case "--min-alpha" -> minAlpha = Integer.parseInt(args[++i]);
                case "--merge" -> merge = Integer.parseInt(args[++i]);
                case "--min-count" -> minCount = Integer.parseInt(args[++i]);
                case "-h", "--help" -> {
                    System.out.println("Usage: PaletteExtract [--top n] [--min-alpha a]"
                            + " [--merge d] [--min-count n] <file-or-dir>...");
                    return;
                }
                default -> inputs.add(Paths.get(args[i]));
            }
        }
        if (inputs.isEmpty()) {
            System.err.println("No inputs given.");
            System.exit(2);
        }

        List<Path> files = new ArrayList<>();
        for (Path in : inputs) {
            collectPngs(in, files);
        }

        Map<Integer, long[]> counts = new LinkedHashMap<>();
        for (Path f : files) {
            BufferedImage img;
            try {
                img = ImageIO.read(f.toFile());
            } catch (IOException e) {
                System.err.println("skip " + f + ": " + e.getMessage());
                continue;
            }
            if (img == null) {
                continue;
            }
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int argb = img.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    if (a < minAlpha) {
                        continue;
                    }
                    int rgb = argb & 0xFFFFFF;
                    counts.computeIfAbsent(rgb, k -> new long[1])[0]++;
                }
            }
        }

        List<Map.Entry<Integer, long[]>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
        List<long[]> clusters = new ArrayList<>();
        List<Integer> reps = new ArrayList<>();
        List<int[]> sum = new ArrayList<>();
        for (Map.Entry<Integer, long[]> e : sorted) {
            int rgb = e.getKey();
            long n = e.getValue()[0];
            int found = -1;
            for (int i = 0; i < reps.size(); i++) {
                if (dist(rgb, reps.get(i)) <= merge) {
                    found = i;
                    break;
                }
            }
            if (found < 0) {
                reps.add(rgb);
                clusters.add(new long[]{n});
                sum.add(new int[]{rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF});
            } else {
                clusters.get(found)[0] += n;
                int[] s = sum.get(found);
                s[0] += rgb >> 16 & 0xFF;
                s[1] += rgb >> 8 & 0xFF;
                s[2] += rgb & 0xFF;
            }
        }

        long total = 0;
        for (long[] c : clusters) {
            total += c[0];
        }

        System.out.println("Files: " + files.size() + "  distinct colors: " + reps.size()
                + "  pixels: " + total);
        System.out.println();
        System.out.printf("%-9s %10s %7s  %s%n", "hex", "pixels", "pct", "hsv(h,s,v)");
        int shown = 0;
        for (int i = 0; i < reps.size() && shown < top; i++) {
            long n = clusters.get(i)[0];
            if (n < minCount) {
                continue;
            }
            int[] s = sum.get(i);
            long cn = clusters.get(i)[0];
            int r = (int) (s[0] / cn);
            int g = (int) (s[1] / cn);
            int b = (int) (s[2] / cn);
            if (merge == 0) {
                r = reps.get(i) >> 16 & 0xFF;
                g = reps.get(i) >> 8 & 0xFF;
                b = reps.get(i) & 0xFF;
            }
            float[] hsv = java.awt.Color.RGBtoHSB(r, g, b, null);
            System.out.printf("#%02X%02X%02X %10d %6.2f%%  h=%3.0f s=%3.0f v=%3.0f%n",
                    r, g, b, n, 100.0 * n / total,
                    hsv[0] * 360, hsv[1] * 100, hsv[2] * 100);
            shown++;
        }
    }

    private static int dist(int a, int b) {
        int dr = (a >> 16 & 0xFF) - (b >> 16 & 0xFF);
        int dg = (a >> 8 & 0xFF) - (b >> 8 & 0xFF);
        int db = (a & 0xFF) - (b & 0xFF);
        return (int) Math.round(Math.sqrt(dr * dr + dg * dg + db * db));
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
}
