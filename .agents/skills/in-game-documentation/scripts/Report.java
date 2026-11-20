import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects findings from a documentation check and prints them
 * deterministically (sorted) with a stable summary line.
 *
 * <p>Exit codes used by all scripts: {@code 0} = no errors (warnings allowed),
 * {@code 1} = at least one error, {@code 2} = the tool could not run
 * (bad arguments, missing inputs).</p>
 */
public final class Report {

  private final String tool;
  private final List<String> errors = new ArrayList<>();
  private final List<String> warnings = new ArrayList<>();

  public Report(String tool) {
    this.tool = tool;
  }

  /** Records a hard failure: the check does not pass. */
  public void error(String where, String message) {
    errors.add(where + ": " + message);
  }

  /** Records a soft finding: worth a human look, but not a failure. */
  public void warn(String where, String message) {
    warnings.add(where + ": " + message);
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public int errorCount() {
    return errors.size();
  }

  public int warningCount() {
    return warnings.size();
  }

  /** Prints every finding plus the summary and returns the process exit code. */
  public int printAndExitCode() {
    List<String> sortedErrors = new ArrayList<>(errors);
    List<String> sortedWarnings = new ArrayList<>(warnings);
    Collections.sort(sortedErrors);
    Collections.sort(sortedWarnings);

    for (String error : sortedErrors) {
      System.out.println("[ERROR] " + error);
    }
    for (String warning : sortedWarnings) {
      System.out.println("[WARN]  " + warning);
    }

    if (errors.isEmpty()) {
      System.out.println(tool + ": PASS (" + warnings.size() + " warning(s))");
      return 0;
    }
    System.out.println(tool + ": FAIL ("
        + errors.size() + " error(s), " + warnings.size() + " warning(s))");
    return 1;
  }

  /** Terminates the process for unrecoverable tool problems (exit code 2). */
  public static void fatal(String tool, String message) {
    System.out.println("[FATAL] " + tool + ": " + message);
    System.exit(2);
  }
}
