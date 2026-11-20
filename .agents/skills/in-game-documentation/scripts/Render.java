import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Renders a generated Modonomicon book into a single human-readable output
 * file for manual auditing.
 *
 * <p>Usage:</p>
 * <pre>
 *   java Render --root &lt;project-root&gt; [--book &lt;book-id&gt;] [--out &lt;file&gt;]
 * </pre>
 *
 * <p>The output contains, per book: book metadata, every category in
 * generation order with its entries, and every entry with its pages (type, id,
 * title, resolved text) plus every language key that is referenced. The output
 * file is what the consistency audit reads; plain lines are used rather than
 * JSON for human readability.</p>
 */
public final class Render {

  public static void main(String[] args) {
    java.util.Set<String> allowed = new java.util.HashSet<>(
        java.util.List.of("root", "book", "out"));
    Map<String, String> options = Tool.options("Render", args, allowed);
    Path root = Path.of(options.get("root"));
    String outPath = options.get("out");
    BookTree.Result result = BookTree.load(root, new Report("Render"));
    BookTree.Book book = BookTree.select(result, options, "Render");
    book.load();

    List<String> lines = new ArrayList<>();
    lines.add("book=" + book.fullId);
    lines.add("name=" + resolveText(book, Json.get(book.json, "name"), new ArrayList<>()));
    lines.add("tooltip=" + resolveText(book, Json.get(book.json, "tooltip"), new ArrayList<>()));
    lines.add("description=" + resolveText(book, Json.get(book.json, "description"), new ArrayList<>()));
    lines.add("categories:");
    for (BookTree.Category category : book.orderedCategories()) {
      lines.add("  category " + category.key
          + " sort=" + BookTree.sortNumber(category.json)
          + " name=" + resolveText(book, Json.get(category.json, "name"), new ArrayList<>()));
      List<BookTree.Entry> entries = book.orderedEntries(category);
      lines.add("    entry_count=" + entries.size());
      for (BookTree.Entry entry : entries) {
        lines.add("    entry " + entry.key
            + " sort=" + BookTree.sortNumber(entry.json)
            + " x=" + Json.getInt(entry.json, "x", 0)
            + " y=" + Json.getInt(entry.json, "y", 0)
            + " name=" + resolveText(book, Json.get(entry.json, "name"), new ArrayList<>()));
        List<String> missing = new ArrayList<>();
        lines.add("      description=" + BookTree.resolveText(book, Json.get(entry.json, "description"), missing));
        for (BookTree.Page page : book.orderedPages(entry)) {
          lines.add("        page " + page.key + " type=" + Json.getString(page.json, "type")
              + " sort=" + BookTree.sortNumber(page.json));
          lines.add("          title=" + BookTree.resolveText(book, Json.get(page.json, "title"), missing));
          String pageText = BookTree.resolveText(book, Json.get(page.json, "text"), missing);
          for (String textLine : pageText.split("\n")) {
            lines.add("          text=" + textLine);
          }
          lines.add("          lang=" + Json.getString(page.json, "title"));
          lines.add("          lang=" + Json.getString(page.json, "text"));
          lines.add("          lang=" + Json.getString(entry.json, "name"));
          lines.add("          lang=" + Json.getString(entry.json, "description"));
          lines.add("          lang=" + Json.getString(category.json, "name"));
        }
        for (String missingKey : missing) {
          lines.add("        MISSING=" + missingKey);
        }
      }
    }

    String output = String.join("\n", lines) + "\n";

    if (outPath == null || outPath.isBlank()) {
      System.out.print(output);
    } else {
      try {
        Path out = Path.of(outPath);
        if (out.getParent() != null) {
          Files.createDirectories(out.getParent());
        }
        Files.writeString(out, output);
        System.out.println("rendered " + book.fullId + " to " + outPath);
      } catch (IOException e) {
        Report.fatal("Render", "cannot write " + outPath + ": " + e.getMessage());
      }
    }
  }

  private static String resolveText(BookTree.Book book, Object value, List<String> missing) {
    return BookTree.resolveText(book, value, missing);
  }

  private Render() {
  }
}
