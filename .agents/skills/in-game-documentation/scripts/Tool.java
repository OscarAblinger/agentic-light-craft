import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Shared command line helpers for the documentation scripts. */
public final class Tool {

  private Tool() {
  }

  /**
   * Finds the project root by walking up from the working directory until a
   * Gradle marker is found, so the scripts work from any subdirectory.
   */
  public static Path projectRoot() {
    Path dir = Paths.get("").toAbsolutePath();
    while (dir != null) {
      if (Files.exists(dir.resolve("gradlew"))
          || Files.exists(dir.resolve("settings.gradle"))
          || Files.exists(dir.resolve("settings.gradle.kts"))) {
        return dir;
      }
      dir = dir.getParent();
    }
    return Paths.get("").toAbsolutePath();
  }

  /**
   * Parses {@code --key value} (or {@code --key=value}) arguments.
   * Unknown keys abort with exit code 2.
   *
   * @param tool    script name for error messages
   * @param argv    raw arguments
   * @param allowed keys the script understands (without the leading dashes)
   */
  public static Map<String, String> options(String tool, String[] argv, Set<String> allowed) {
    Map<String, String> out = new HashMap<>();
    for (int i = 0; i < argv.length; i++) {
      String arg = argv[i];
      if (arg.equals("--help") || arg.equals("-h")) {
        System.out.println("Usage: java <Script> --root <project-root> [options]");
        System.out.println("Options: --root <path> (required)");
        System.out.println("         --book <book-id> (optional)");
        System.out.println("         --out <path>   (optional output file)");
        System.exit(0);
      }
      if (!arg.startsWith("--")) {
        Report.fatal(tool, "unexpected argument '" + arg + "' (expected --key value)");
      }
      String body = arg.substring(2);
      String key;
      String value;
      int eq = body.indexOf('=');
      if (eq >= 0) {
        key = body.substring(0, eq);
        value = body.substring(eq + 1);
      } else {
        key = body;
        if (i + 1 >= argv.length || argv[i + 1].startsWith("--")) {
          Report.fatal(tool, "missing value for --" + key);
        }
        value = argv[++i];
      }
      if (!allowed.contains(key)) {
        Report.fatal(tool, "unknown option --" + key + "; allowed: " + allowed);
      }
      if (out.put(key, value) != null) {
        Report.fatal(tool, "option --" + key + " given twice");
      }
    }
    return out;
  }

  /** Option keys commonly shared by the scripts. */
  public static Set<String> commonOptions() {
    return new HashSet<>(java.util.List.of("root", "book"));
  }
}
