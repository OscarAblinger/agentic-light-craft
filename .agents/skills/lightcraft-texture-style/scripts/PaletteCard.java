import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Render a palette suggestion card as a PNG so it can be reviewed visually.
 *
 * Dependency-free (JDK only).
 *
 *   java .agents/skills/lightcraft-texture-style/scripts/PaletteCard.java \
 *     --in candidate.txt --out candidate.png
 *
 * Spec format:
 *   title: Optional card title
 *   [Ramp name]
 *   RRGGBB  optional label
 *   # comment / blank lines ignored
 */
public final class PaletteCard {

    private static final int PAD = 20;
    private static final int SW = 92;
    private static final int SH = 56;
    private static final int GAP = 6;
    private static final int NAME_H = 16;
    private static final int RAMP_TITLE_H = 26;
    private static final int TITLE_H = 34;

    private PaletteCard() {
    }

    private record Entry(int rgb, String name) {
    }

    private record Ramp(String name, List<Entry> entries) {
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");
        Path in = null;
        Path out = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--in" -> in = Path.of(args[++i]);
                case "--out" -> out = Path.of(args[++i]);
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(2);
                }
            }
        }
        if (in == null || out == null) {
            System.err.println("Usage: PaletteCard --in <spec> --out <png>");
            System.exit(2);
        }

        String title = null;
        List<Ramp> ramps = new ArrayList<>();
        String rampName = null;
        List<Entry> entries = new ArrayList<>();

        for (String raw : Files.readAllLines(in, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            int hash = line.indexOf('#');
            if (hash == 0) {
                continue;
            }
            if (hash > 0) {
                line = line.substring(0, hash).trim();
            }
            if (line.isEmpty()) {
                continue;
            }
            if (line.toLowerCase().startsWith("status:")) {
                continue;
            }
            if (line.toLowerCase().startsWith("title:")) {
                title = line.substring(6).trim();
                continue;
            }
            if (line.startsWith("[")) {
                if (rampName != null) {
                    ramps.add(new Ramp(rampName, entries));
                }
                rampName = line.replaceAll("[\\[\\]]", "").trim();
                entries = new ArrayList<>();
                continue;
            }
            String hex = line.split("\\s+")[0];
            if (hex.length() != 6) {
                System.err.println("Skipping malformed line: " + line);
                continue;
            }
            String name = line.length() > hex.length() ? line.substring(hex.length()).trim() : "";
            entries.add(new Entry(Integer.parseInt(hex, 16), name));
        }
        if (rampName != null) {
            ramps.add(new Ramp(rampName, entries));
        }

        int maxCols = 1;
        for (Ramp r : ramps) {
            maxCols = Math.max(maxCols, r.entries().size());
        }
        int contentW = Math.max(320, maxCols * (SW + GAP) - GAP);
        int width = contentW + PAD * 2;

        int height = PAD * 2 + (title != null ? TITLE_H : 0);
        for (Ramp r : ramps) {
            height += RAMP_TITLE_H + SH + NAME_H + 10;
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x202225));
        g.fillRect(0, 0, width, height);

        int y = PAD;
        if (title != null) {
            g.setColor(new Color(0xF2F2F2));
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.drawString(title, PAD, y + 24);
            y += TITLE_H;
        }

        for (Ramp r : ramps) {
            g.setColor(new Color(0xE0C56A));
            g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.drawString(r.name(), PAD, y + 18);
            y += RAMP_TITLE_H;

            int x = PAD;
            for (Entry e : r.entries()) {
                Color c = new Color(e.rgb());
                g.setColor(c);
                g.fillRect(x, y, SW, SH);
                g.setColor(new Color(0x000000));
                g.drawRect(x, y, SW - 1, SH - 1);

                double lum = (0.2126 * c.getRed() + 0.7152 * c.getGreen()
                        + 0.0722 * c.getBlue()) / 255.0;
                g.setColor(lum > 0.6 ? new Color(0x1A1A1A) : new Color(0xF4F4F4));
                g.setFont(new Font("Monospaced", Font.BOLD, 13));
                String hex = String.format("%06X", e.rgb());
                g.drawString(hex, x + (SW - g.getFontMetrics().stringWidth(hex)) / 2, y + 34);

                if (!e.name().isEmpty()) {
                    g.setColor(new Color(0xC7C7C7));
                    g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                    g.drawString(e.name(), x + 4, y + SH + 12);
                }
                x += SW + GAP;
            }
            y += SH + NAME_H + 10;
        }

        g.dispose();
        Path parent = out.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ImageIO.write(img, "png", out.toFile());
        System.out.println("Wrote " + out + " (" + width + "x" + height + ")");
    }
}
