import java.io.IOException;
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
 * Checks that every book-level reference used by the generated documentation
 * resolves to something that actually exists: entry links, command links, item
 * links, patchouli links and inline {@code <t>key</t>} translations.
 *
 * <p>Usage:</p>
 * <pre>
 *   java ReferenceIntegrity --root &lt;project-root&gt; --book &lt;book-id&gt; [--out &lt;file&gt;]
 * </pre>
 *
 * <p>A reference is a markdown-style link target in page text or a
 * {@code command://...} / {@code item://...} / {@code patchouli://...} /
 * {@code book://...} / {@code category://...} URL used by Modonomicon.</p>
 */
public final class ReferenceIntegrity {

  private static final Pattern LINK = Pattern.compile("\\]\\(([^)\\s]+)\\)");
  private static final Pattern INLINE_TAG = Pattern.compile("<t>([^<]+)</t>");
  private static final Pattern ID = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9_./-]+$");

  public static void main(String[] args) {
    java.util.Map<String, String> options = Tool.options(
        "ReferenceIntegrity", args, new java.util.HashSet<>(java.util.List.of("root", "book", "out")));
    Path root = Path.of(options.get("root"));
    BookTree.Result result = BookTree.load(root, new Report("ReferenceIntegrity"));
    BookTree.Book book = BookTree.select(result, options, "ReferenceIntegrity");
    book.load();

    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    Set<String> missingKeys = new HashSet<>();

    for (BookTree.Entry entry : book.orderedEntries()) {
      for (BookTree.Page page : book.orderedPages(entry)) {
        scanText(book, page.json, page.key, errors, warnings, missingKeys);
      }
    }
    for (Map<String, Object> command : commands(book)) {
      String key = "book." + commandKey(book, command);
      if (!book.lang.containsKey(key)) {
        errors.add("command " + Json.getString(command, "id") + " has no language entry " + key);
      }
    }
    for (String missing : missingKeys) {
      errors.add("unresolved inline translation <t>" + missing + "</t> in " + missing);
    }

    List<String> output = new ArrayList<>();
    for (String error : errors) {
      output.add("[ERROR] " + error);
    }
    for (String warning : warnings) {
      output.add("[WARN]  " + warning);
    }
    output.add("ReferenceIntegrity: " + (errors.isEmpty() ? "PASS" : "FAIL")
        + " (" + errors.size() + " error(s), " + warnings.size() + " warning(s))");

    String rendered = String.join("\n", output) + "\n";
    String outPath = options.get("out");
    if (outPath == null || outPath.isBlank()) {
      System.out.print(rendered);
    } else {
      try {
        Path out = Path.of(outPath);
        if (out.getParent() != null) {
          Files.createDirectories(out.getParent());
        }
        Files.writeString(out, rendered);
        System.out.println("wrote " + outPath);
      } catch (IOException e) {
        Report.fatal("ReferenceIntegrity", "cannot write " + outPath + ": " + e.getMessage());
      }
    }
    if (!errors.isEmpty()) {
      System.exit(1);
    }
  }

  private static String commandKey(BookTree.Book book, Map<String, Object> command) {
    return "book." + Json.getString(command, "id");
  }

  private static List<Map<String, Object>> commands(BookTree.Book book) {
    List<Map<String, Object>> commands = new ArrayList<>();
    Object commandsField = Json.get(book.json, "commands");
    if (commandsField instanceof List<?> list) {
      for (Object o : list) {
        if (o instanceof Map) {
          commands.add((Map<String, Object>) o);
        }
      }
    }
    return commands;
  }

  private static void scanText(
      BookTree.Book book,
      Map<String, Object> pageJson,
      String pageKey,
      List<String> errors,
      List<String> warnings,
      Set<String> missingKeys) {
    String text = Json.getString(pageJson, "text");
    String title = Json.getString(pageJson, "title");
    scanField(book, text, pageKey, "text", errors, warnings, missingKeys);
    scanField(book, title, pageKey, "title", errors, warnings, missingKeys);
  }

  private static void scanField(
      BookTree.Book book,
      String text,
      String pageKey,
      String field,
      List<String> errors,
      List<String> warnings,
      Set<String> missingKeys) {
    if (text == null) {
      return;
    }
    Matcher matcher = LINK.matcher(text);
    while (matcher.find()) {
      String target = matcher.group(1);
      String kind = linkKind(target);
      if (kind == null) {
        errors.add(pageKey + ": unparsable link in " + field + ": " + target);
        continue;
      }
      switch (kind) {
        case "entry" -> checkEntry(book, target, pageKey, errors);
        case "command" -> checkCommand(book, target, pageKey, errors, warnings);
        case "item" -> checkItem(target, pageKey, errors, warnings);
        case "patchouli" -> checkPatchouli(target, pageKey, errors, warnings);
        case "book" -> checkBook(book, target, pageKey, errors, warnings);
        case "category" -> checkCategory(book, target, pageKey, errors, warnings);
        default -> errors.add(pageKey + ": unknown link kind for " + target);
      }
    }
    Matcher inline = INLINE_TAG.matcher(text);
    while (inline.find()) {
      String key = inline.group(1);
      if (!book.lang.containsKey(key)) {
        missingKeys.add(key);
      }
    }
  }

  private static String linkKind(String target) {
    if (target.startsWith("book://")) {
      return "book";
    }
    if (target.startsWith("category://")) {
      return "category";
    }
    if (target.startsWith("entry://")) {
      return "entry";
    }
    if (target.startsWith("command://")) {
      return "command";
    }
    if (target.startsWith("item://")) {
      return "item";
    }
    if (target.startsWith("patchouli://")) {
      return "patchouli";
    }
    return null;
  }

  private static void checkEntry(BookTree.Book book, String target, String pageKey, List<String> errors) {
    String entryId = target.substring("entry://".length());
    if (!book.entriesByKey.containsKey(entryId)) {
      errors.add("page " + pageKey + " links to unknown entry '" + entryId + "'");
    }
  }

  private static void checkCommand(
      BookTree.Book book, String target, String pageKey, List<String> errors, List<String> warnings) {
    String commandId = target.substring("command://".length());
    boolean found = false;
    for (Map<String, Object> command : commands(book)) {
      if (Json.getString(command, "id").equals(commandId)) {
        found = true;
        break;
      }
    }
    if (!found) {
      errors.add("page " + pageKey + " links to unknown command '" + commandId + "'");
    }
  }

  private static void checkItem(String target, String pageKey, List<String> errors, List<String> warnings) {
    if (!ID.matcher(target.substring("item://".length())).matches()) {
      errors.add("page " + pageKey + " has malformed item link '" + target + "'");
    }
  }

  private static void checkPatchouli(String target, String pageKey, List<String> errors, List<String> warnings) {
    if (!target.contains("://") || !target.substring("patchouli://".length()).contains(":")) {
      errors.add("page " + pageKey + " has malformed patchouli link '" + target + "'");
    }
  }

  private static void checkBook(BookTree.Book book, String target, String pageKey, List<String> errors, List<String> warnings) {
    String bookId = target.substring("book://".length());
    if (!book.fullId.equals(bookId) && !book.id.equals(bookId)) {
      errors.add("page " + pageKey + " links to unknown book '" + bookId + "'");
    }
  }

  private static void checkCategory(BookTree.Book book, String target, String pageKey, List<String> errors, List<String> warnings) {
    String ref = target.startsWith("category://") ? target.substring("category://".length()) : target;
    String categoryId = ref.split("/", 2)[0];
    if (!book.categoriesByKey.containsKey(categoryId)) {
      errors.add("page " + pageKey + " links to unknown category '" + categoryId + "'");
    }
  }
}
