import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Loads the generated Modonomicon book data plus the generated language file
 * into a small object tree that the documentation scripts can inspect.
 *
 * <p>Expected layout (produced by {@code ./gradlew runData}):</p>
 * <pre>
 * src/generated/resources/data/&lt;ns&gt;/modonomicon/books/&lt;book&gt;/book.json
 * src/generated/resources/data/&lt;ns&gt;/modonomicon/books/&lt;book&gt;/categories/&lt;category&gt;.json
 * src/generated/resources/data/&lt;ns&gt;/modonomicon/books/&lt;book&gt;/entries/&lt;cat&gt;/&lt;entry&gt;.json
 * src/generated/resources/data/&lt;ns&gt;/modonomicon/books/&lt;book&gt;/entries/&lt;cat&gt;/&lt;entry&gt;/pages/&lt;page&gt;.json
 * src/generated/resources/assets/&lt;ns&gt;/lang/en_us.json
 * </pre>
 */
public final class BookTree {

  private BookTree() {
  }

  /** Result of a book scan: project root plus every generated book. */
  public static final class Result {
    public final Path root;
    public final List<Book> books = new ArrayList<>();

    public Result(Path root) {
      this.root = root;
    }
  }

  /** Discovers every generated book under the project root. */
  public static Result load(Path root, Report report) {
    Path dataRoot = root.resolve("src/generated/resources/data");
    if (!Files.isDirectory(dataRoot)) {
      Report.fatal("BookTree",
          "no generated data at " + dataRoot + "; run './gradlew runData' first");
    }

    List<Path> bookDirs = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(dataRoot)) {
      walk.filter(Files::isDirectory)
          .filter(p -> "books".equals(fileName(p.getParent()))
              && "modonomicon".equals(fileName(p.getParent().getParent())))
          .forEach(bookDirs::add);
    } catch (IOException e) {
      Report.fatal("BookTree", "cannot scan " + dataRoot + ": " + e.getMessage());
    }
    Collections.sort(bookDirs);

    Result result = new Result(root);
    for (Path bookDir : bookDirs) {
      String namespace = fileName(bookDir.getParent().getParent().getParent());
      String id = fileName(bookDir);
      Path bookJson = bookDir.resolve("book.json");
      if (!Files.isRegularFile(bookJson)) {
        report.error(rel(root, bookDir), "missing book.json");
        continue;
      }
      Map<String, Object> json = readJson(root, report, bookJson);
      if (json == null) {
        continue;
      }
      result.books.add(new Book(root, report, namespace, id, bookDir, json));
    }

    if (result.books.isEmpty()) {
      Report.fatal("BookTree", "no generated books found under " + dataRoot);
    }
    return result;
  }

  /** Picks the book to operate on: {@code --book} when given, otherwise the only book. */
  public static Book select(Result result, Map<String, String> options, String tool) {
    List<Book> books = result.books;
    String want = options.get("book");
    if (want != null) {
      for (Book book : books) {
        if (book.fullId.equals(want) || book.id.equals(want)) {
          return book;
        }
      }
      Report.fatal(tool, "book '" + want + "' not found; available: " + ids(books));
    }
    if (books.size() == 1) {
      return books.get(0);
    }
    Report.fatal(tool, "multiple books found, pass --book <id>; available: " + ids(books));
    return null; // unreachable
  }

  private static String ids(List<Book> books) {
    List<String> out = new ArrayList<>();
    for (Book book : books) {
      out.add(book.fullId);
    }
    return String.join(", ", out);
  }

  // ------------------------------------------------------------------ book

  /** One generated book with its categories, entries, pages and language. */
  public static final class Book {
    public final Path root;
    public final String namespace;
    public final String id;
    public final String fullId;
    public final Path dir;
    public final Map<String, Object> json;
    public final Map<String, String> lang = new LinkedHashMap<>();
    private final Report report;

    final Map<String, Category> categoriesByKey = new LinkedHashMap<>();
    final Map<String, Entry> entriesByKey = new LinkedHashMap<>();
    final Map<String, Page> pagesByKey = new LinkedHashMap<>();

    Book(Path root, Report report, String namespace, String id, Path dir, Map<String, Object> json) {
      this.root = root;
      this.report = report;
      this.namespace = namespace;
      this.id = id;
      this.fullId = namespace + ":" + id;
      this.dir = dir;
      this.json = json;
    }

    private void loadLanguage() {
      Path langFile = root.resolve("src/generated/resources/assets/" + namespace + "/lang/en_us.json");
      if (!Files.isRegularFile(langFile)) {
        report.error(rel(root, langFile), "missing language file for book " + fullId);
        return;
      }
      Map<String, Object> parsed = readJson(root, report, langFile);
      if (parsed == null) {
        return;
      }
      java.util.Map<String, Object> langMap = parsed;
      for (java.util.Map.Entry<String, Object> langEntry : langMap.entrySet()) {
        String value = Json.asString(langEntry.getValue());
        if (value != null) {
          lang.put(langEntry.getKey(), value);
        }
      }
    }

    private void loadCategories() {
      Path categoriesDir = dir.resolve("categories");
      if (!Files.isDirectory(categoriesDir)) {
        report.error(rel(root, dir), "book " + fullId + " has no categories directory");
        return;
      }
      for (Path file : jsonFiles(categoriesDir)) {
        String key = stripJson(fileName(file));
        Map<String, Object> json = readJson(root, report, file);
        if (json == null) {
          continue;
        }
        if (categoriesByKey.containsKey(key)) {
          report.error(rel(root, file), "duplicate category '" + key + "'");
          continue;
        }
        categoriesByKey.put(key, new Category(this, key, file, json));
      }
    }

    private void loadEntries() {
      Path entriesDir = dir.resolve("entries");
      if (!Files.isDirectory(entriesDir)) {
        report.error(rel(root, dir), "book " + fullId + " has no entries directory");
        return;
      }
      List<Path> entryFiles = new ArrayList<>();
      List<Path> pageFiles = new ArrayList<>();
      for (Path file : jsonFiles(entriesDir)) {
        if (relativeParts(entriesDir, file).contains("pages")) {
          pageFiles.add(file);
        } else {
          entryFiles.add(file);
        }
      }
      // Entries first so every page can resolve its parent entry.
      for (Path file : entryFiles) {
        List<String> parts = relativeParts(entriesDir, file);
        Map<String, Object> json = readJson(root, report, file);
        if (json == null) {
          continue;
        }
        String key = String.join("/", parts.stream().map(BookTree::stripJson).toList());
        if (entriesByKey.containsKey(key)) {
          report.error(rel(root, file), "duplicate entry '" + key + "'");
          continue;
        }
        entriesByKey.put(key, new Entry(this, key, file, json));
      }
      for (Path file : pageFiles) {
        List<String> parts = relativeParts(entriesDir, file);
        int pagesIndex = parts.indexOf("pages");
        Map<String, Object> json = readJson(root, report, file);
        if (json == null) {
          continue;
        }
        if (pagesIndex == 0) {
          report.error(rel(root, file), "page file outside any entry directory");
          continue;
        }
        String entryKey = String.join("/", parts.subList(0, pagesIndex));
        Entry entry = entriesByKey.get(entryKey);
        if (entry == null) {
          report.error(rel(root, file),
              "page belongs to unknown entry '" + entryKey + "' (entry file missing?)");
          continue;
        }
        String pageId = stripJson(parts.get(pagesIndex + 1));
        String pageKey = entryKey + "/" + pageId;
        if (pagesByKey.containsKey(pageKey)) {
          report.error(rel(root, file), "duplicate page '" + pageKey + "'");
          continue;
        }
        Page page = new Page(entry, pageId, pageKey, file, json);
        entry.pages.add(page);
        pagesByKey.put(pageKey, page);
      }
      for (Entry entry : entriesByKey.values()) {
        entry.pages.sort(Comparator
            .comparingInt((Page p) -> sortNumber(p.json))
            .thenComparing(p -> p.id));
      }
    }

    public void load() {
      loadLanguage();
      loadCategories();
      loadEntries();
    }

    public List<Category> orderedCategories() {
      List<Category> out = new ArrayList<>(categoriesByKey.values());
      out.sort(Comparator
          .comparingInt((Category c) -> sortNumber(c.json))
          .thenComparing(c -> c.key));
      return out;
    }

    public List<Entry> orderedEntries(Category category) {
      List<Entry> out = new ArrayList<>();
      for (Entry entry : entriesByKey.values()) {
        if (entry.categoryKey().equals(category.key)) {
          out.add(entry);
        }
      }
      return out.stream()
          .sorted(Comparator
              .comparingInt((Entry e) -> sortNumber(e.json))
              .thenComparingInt(e -> Json.getInt(e.json, "y", 0))
              .thenComparingInt(e -> Json.getInt(e.json, "x", 0))
              .thenComparing(e -> e.key))
          .toList();
    }

    public List<Entry> orderedEntries() {
      return entriesByKey.values().stream()
          .sorted(Comparator
              .comparingInt((Entry e) -> sortNumber(e.json))
              .thenComparingInt(e -> Json.getInt(e.json, "y", 0))
              .thenComparingInt(e -> Json.getInt(e.json, "x", 0))
              .thenComparing(e -> e.key))
          .toList();
    }

    public List<Page> orderedPages(Entry entry) {
      return new ArrayList<>(entry.pages);
    }

    public Category category(String key) {
      return categoriesByKey.get(key);
    }

    public Entry entry(String key) {
      return entriesByKey.get(key);
    }

    public Page page(String key) {
      return pagesByKey.get(key);
    }

    public int pageCount(Entry entry) {
      return entry.pages.size();
    }
  }

  // ------------------------------------------------------------- entities

  /** One category file. */
  public static final class Category {
    public final Book book;
    public final String key;
    public final Path file;
    public final Map<String, Object> json;

    Category(Book book, String key, Path file, Map<String, Object> json) {
      this.book = book;
      this.key = key;
      this.file = file;
      this.json = json;
    }
  }

  /** One entry file. */
  public static final class Entry {
    public final Book book;
    /** Full path under {@code entries/}, e.g. {@code getting_started/welcome}. */
    public final String key;
    public final Path file;
    public final Map<String, Object> json;
    final List<Page> pages = new ArrayList<>();

    Entry(Book book, String key, Path file, Map<String, Object> json) {
      this.book = book;
      this.key = key;
      this.file = file;
      this.json = json;
    }

    /** Category segment of the key; empty for top-level entries. */
    public String categoryKey() {
      int slash = key.indexOf('/');
      return slash < 0 ? "" : key.substring(0, slash);
    }

    public List<Page> pages() {
      return pages;
    }
  }

  /** One page file inside an entry. */
  public static final class Page {
    public final Entry entry;
    /** Page id as used by {@code @page-id} links, e.g. {@code intro}. */
    public final String id;
    /** Full key: {@code <entry-key>/<page-id>}. */
    public final String key;
    public final Path file;
    public final Map<String, Object> json;

    Page(Entry entry, String id, String key, Path file, Map<String, Object> json) {
      this.entry = entry;
      this.id = id;
      this.key = key;
      this.file = file;
      this.json = json;
    }
  }

  // ----------------------------------------------------------- resolution

  private static final Pattern INLINE_TAG = Pattern.compile("<t>([^<]+)</t>");

  /**
   * Resolves a text holder (plain string or {@code {"translate": ...}} object)
   * to the text a player would see. Missing {@code book.*} keys are recorded
   * in the given list when unresolved.
   */
  public static String resolveText(Book book, Object value, List<String> missing) {
    String raw;
    if (value instanceof String s) {
      raw = s;
    } else if (value instanceof Map<?, ?> map) {
      Object translate = map.get("translate");
      if (translate instanceof String s) {
        raw = s;
      } else {
        Object text = map.get("text");
        raw = text instanceof String s ? s : "";
      }
    } else if (value == null) {
      raw = "";
    } else {
      raw = String.valueOf(value);
    }
    if (raw.startsWith("book.")) {
      String resolved = book.lang.get(raw);
      if (resolved == null) {
        missing.add(raw);
        return "<<" + raw + " (missing)>>";
      }
      raw = resolved;
    }
    return expandInlineTags(book, raw, missing);
  }

  /** Expands {@code <t>key</t>} placeholders when the key is known. */
  public static String expandInlineTags(Book book, String text, List<String> missing) {
    Matcher matcher = INLINE_TAG.matcher(text);
    StringBuilder out = new StringBuilder();
    while (matcher.find()) {
      String key = matcher.group(1);
      String replacement = book.lang.get(key);
      if (replacement == null) {
        if (key.startsWith("book.")) {
          missing.add(key);
        }
        replacement = matcher.group();
      }
      matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(out);
    return out.toString();
  }

  // ------------------------------------------------------------- plumbing

  static int sortNumber(Map<String, Object> json) {
    return Json.getInt(json, "sort_number", 0);
  }

  static List<Path> jsonFiles(Path dir) {
    List<Path> out = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(dir)) {
      walk.filter(Files::isRegularFile)
          .filter(p -> fileName(p).endsWith(".json"))
          .forEach(out::add);
    } catch (IOException e) {
      Report.fatal("BookTree", "cannot scan " + dir + ": " + e.getMessage());
    }
    Collections.sort(out);
    return out;
  }

  /** Splits a file's path relative to {@code dir} into plain name segments. */
  static List<String> relativeParts(Path dir, Path file) {
    Path rel = dir.relativize(file);
    List<String> parts = new ArrayList<>();
    for (int i = 0; i < rel.getNameCount(); i++) {
      parts.add(rel.getName(i).toString());
    }
    return parts;
  }

  static Map<String, Object> readJson(Path root, Report report, Path file) {
    String text;
    try {
      text = Files.readString(file);
    } catch (IOException e) {
      report.error(rel(root, file), "cannot read: " + e.getMessage());
      return null;
    }
    try {
      Map<String, Object> object = Json.asObject(Json.parse(text));
      if (object == null) {
        report.error(rel(root, file), "expected a JSON object at the top level");
        return null;
      }
      return object;
    } catch (Json.JsonException e) {
      report.error(rel(root, file), "invalid JSON: " + e.getMessage());
      return null;
    }
  }

  static String rel(Path root, Path file) {
    Path absoluteRoot = root.toAbsolutePath().normalize();
    Path absoluteFile = file.toAbsolutePath().normalize();
    return absoluteFile.startsWith(absoluteRoot)
        ? absoluteRoot.relativize(absoluteFile).toString().replace('\\', '/')
        : absoluteFile.toString().replace('\\', '/');
  }

  private static String fileName(Path path) {
    return path == null || path.getFileName() == null ? "" : path.getFileName().toString();
  }

  private static String stripJson(String name) {
    return name.endsWith(".json") ? name.substring(0, name.length() - ".json".length()) : name;
  }
}
