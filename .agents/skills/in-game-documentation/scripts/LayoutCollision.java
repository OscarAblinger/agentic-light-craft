import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks the in-game book layout for collisions: two entries mapped to the
 * same grid cell (x, y) inside one category are a layout collision and will
 * render overlapping in the book UI.
 *
 * <p>Usage:</p>
 * <pre>
 *   java LayoutCollision --root &lt;project-root&gt; --book &lt;book-id&gt; [--out &lt;file&gt;]
 * </pre>
 */
public final class LayoutCollision {

  public static void main(String[] args) {
    Map<String, String> options = Tool.options(
        "LayoutCollision", args, new HashSet<>(List.of("root", "book", "out")));
    Path root = Path.of(options.get("root"));
    BookTree.Result result = BookTree.load(root, new Report("LayoutCollision"));
    BookTree.Book book = BookTree.select(result, options, "LayoutCollision");
    book.load();

    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    for (BookTree.Category category : book.orderedCategories()) {
      Set<String> categoryCells = new HashSet<>();
      for (BookTree.Entry entry : book.orderedEntries(category)) {
        int x = Json.getInt(entry.json, "x", 0);
        int y = Json.getInt(entry.json, "y", 0);
        String cell = category.key + "|" + x + "|" + y;
        if (!categoryCells.add(cell)) {
          errors.add("layout collision: category '" + category.key + "' entry '"
              + entry.key + "' reuses cell x=" + x + ", y=" + y);
        }
      }
    }

    String rendered = render(errors, warnings);
    String outPath = options.get("out");
    if (outPath == null || outPath.isBlank()) {
      System.out.print(rendered);
    } else {
      try {
        Path out = Path.of(outPath);
        if (out.getParent() != null) {
          java.nio.file.Files.createDirectories(out.getParent());
        }
        java.nio.file.Files.writeString(out, rendered);
        System.out.println("wrote " + outPath);
      } catch (java.io.IOException e) {
        Report.fatal("LayoutCollision", "cannot write " + outPath + ": " + e.getMessage());
      }
    }
    if (!errors.isEmpty()) {
      System.exit(1);
    }
  }

  private static String render(List<String> errors, List<String> warnings) {
    List<String> lines = new ArrayList<>();
    for (String error : errors) {
      lines.add("[ERROR] " + error);
    }
    for (String warning : warnings) {
      lines.add("[WARN]  " + warning);
    }
    lines.add("LayoutCollision: " + (errors.isEmpty() ? "PASS" : "FAIL")
        + " (" + errors.size() + " error(s), " + warnings.size() + " warning(s))");
    return String.join("\n", lines) + "\n";
  }
}
