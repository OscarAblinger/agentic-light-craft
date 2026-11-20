import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares the generated book order against {@code knowledge/doc-structure.md}
 * headings and flags drift (categories that are missing, unknown or documented
 * out of order).
 *
 * <p>Usage:</p>
 * <pre>
 *   java DocStructureSync --root &lt;project-root&gt; --book &lt;book-id&gt; [--doc &lt;path&gt;] [--out &lt;file&gt;]
 * </pre>
 *
 * <p>The document is expected to use markdown headings; each heading belongs to
 * a category. The check verifies that the order of the categories in the book
 * matches the order of their sections in the document, and that every category
 * is covered.</p>
 */
public final class DocStructureSync {

  private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*#*\\s*$");

  public static void main(String[] args) throws java.io.IOException {
    Map<String, String> options = Tool.options(
        "DocStructureSync", args, new HashSet<>(List.of("root", "book", "doc", "out")));
    Path root = Path.of(options.get("root"));
    String docPath = options.get("doc");
    if (docPath == null || docPath.isBlank()) {
      docPath = "knowledge/doc-structure.md";
    }
    Path docFile = root.resolve(docPath);

    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    List<String> headings = new ArrayList<>();
    if (!Files.isRegularFile(docFile)) {
      errors.add("missing documentation structure file " + docFile);
    } else {
      headings = headings(docFile);
    }

    BookTree.Result result = BookTree.load(root, new Report("DocStructureSync"));
    BookTree.Book book = BookTree.select(result, options, "DocStructureSync");
    book.load();

    List<String> foundCategories = new ArrayList<>();
    if (errors.isEmpty()) {
      for (String heading : headings) {
        String normalized = normalize(heading);
        for (BookTree.Category category : book.orderedCategories()) {
          if (normalized.contains(normalize(category.key))) {
            foundCategories.add(category.key);
            break;
          }
        }
      }
      List<String> expected = new ArrayList<>();
      for (BookTree.Category category : book.orderedCategories()) {
        expected.add(category.key);
      }
      List<String> uniqueFound = new ArrayList<>();
      for (String key : foundCategories) {
        if (!uniqueFound.contains(key)) {
          uniqueFound.add(key);
        }
      }
      if (!uniqueFound.equals(expected)) {
        for (String key : expected) {
          if (!uniqueFound.contains(key)) {
            errors.add("doc-structure.md is missing a section for category '" + key + "'");
          }
        }
        for (String key : uniqueFound) {
          if (!expected.contains(key)) {
            errors.add("doc-structure.md has an unknown category section '" + key + "'");
          }
        }
      }
    }

    List<String> output = new ArrayList<>();
    for (String error : errors) {
      output.add("[ERROR] " + error);
    }
    for (String warning : warnings) {
      output.add("[WARN]  " + warning);
    }
    output.add("DocStructureSync: " + (errors.isEmpty() ? "PASS" : "FAIL")
        + " (" + errors.size() + " error(s), " + warnings.size() + " warning(s))");
    String rendered = String.join("\n", output) + "\n";
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
        Report.fatal("DocStructureSync", "cannot write " + outPath + ": " + e.getMessage());
      }
    }
    if (!errors.isEmpty()) {
      System.exit(1);
    }
  }

  private static List<String> headings(Path docFile) throws java.io.IOException {
    List<String> out = new ArrayList<>();
    for (String line : Files.readAllLines(docFile)) {
      Matcher matcher = HEADING.matcher(line);
      if (matcher.matches()) {
        out.add(matcher.group(2).trim());
      }
    }
    return out;
  }

  private static String normalize(String heading) {
    String lower = heading.toLowerCase();
    StringBuilder out = new StringBuilder();
    for (char c : lower.toCharArray()) {
      if (Character.isLetterOrDigit(c) || c == '-') {
        out.append(c);
      }
    }
    return out.toString();
  }

  private DocStructureSync() {
  }
}
