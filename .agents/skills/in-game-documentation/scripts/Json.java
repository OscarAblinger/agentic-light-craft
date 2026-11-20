import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal dependency-free JSON parser for the documentation scripts.
 *
 * <p>Values map to: {@code Map<String,Object>} (objects), {@code List<Object>}
 * (arrays), {@code String}, {@code Long}/{@code Double} (numbers),
 * {@code Boolean} and {@code null}. Parsing errors carry line/column
 * information so broken generated data can be located quickly.</p>
 */
public final class Json {

  private final String text;
  private int pos;

  private Json(String text) {
    this.text = text;
  }

  /** Parses a complete JSON document, throwing {@link JsonException} on failure. */
  public static Object parse(String text) {
    Json p = new Json(text);
    p.ws();
    Object value = p.value();
    p.ws();
    if (p.pos != p.text.length()) {
      throw p.fail("unexpected trailing content");
    }
    return value;
  }

  // ------------------------------------------------------------------ reads

  /** Returns the value as an object, or {@code null} when it is not an object. */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> asObject(Object value) {
    return value instanceof Map ? (Map<String, Object>) value : null;
  }

  /** Returns the value as a list, or {@code null} when it is not an array. */
  @SuppressWarnings("unchecked")
  public static List<Object> asArray(Object value) {
    return value instanceof List ? (List<Object>) value : null;
  }

  /** Returns the value as a string, or {@code null} when it is not a string. */
  public static String asString(Object value) {
    return value instanceof String ? (String) value : null;
  }

  /** Convenience accessor: object field or {@code null}. */
  public static Object get(Map<String, Object> object, String key) {
    return object == null ? null : object.get(key);
  }

  /** Convenience accessor: string field or {@code null}. */
  public static String getString(Map<String, Object> object, String key) {
    return asString(get(object, key));
  }

  /** Convenience accessor: integer field with default (JSON numbers may be doubles). */
  public static int getInt(Map<String, Object> object, String key, int fallback) {
    Object value = get(object, key);
    if (value instanceof Long l) {
      return l.intValue();
    }
    if (value instanceof Double d) {
      return d.intValue();
    }
    return fallback;
  }

  /** Convenience accessor: boolean field with default. */
  public static boolean getBool(Map<String, Object> object, String key, boolean fallback) {
    Object value = get(object, key);
    return value instanceof Boolean b ? b : fallback;
  }

  /** Type name used in error messages. */
  public static String typeName(Object value) {
    if (value == null) {
      return "null";
    }
    if (value instanceof Map) {
      return "object";
    }
    if (value instanceof List) {
      return "array";
    }
    if (value instanceof String) {
      return "string";
    }
    if (value instanceof Boolean) {
      return "boolean";
    }
    if (value instanceof Number) {
      return "number";
    }
    return value.getClass().getSimpleName();
  }

  // --------------------------------------------------------------- parsing

  private Object value() {
    if (pos >= text.length()) {
      throw fail("unexpected end of input");
    }
    char c = text.charAt(pos);
    if (c == '{') {
      return object();
    }
    if (c == '[') {
      return array();
    }
    if (c == '"') {
      return string();
    }
    if (c == 't') {
      expect("true");
      return Boolean.TRUE;
    }
    if (c == 'f') {
      expect("false");
      return Boolean.FALSE;
    }
    if (c == 'n') {
      expect("null");
      return null;
    }
    return number();
  }

  private Map<String, Object> object() {
    Map<String, Object> map = new LinkedHashMap<>();
    pos++; // consume '{'
    ws();
    if (peek() == '}') {
      pos++;
      return map;
    }
    while (true) {
      ws();
      if (peek() != '"') {
        throw fail("expected a string key");
      }
      String key = string();
      ws();
      if (peek() != ':') {
        throw fail("expected ':' after key \"" + key + "\"");
      }
      pos++;
      ws();
      map.put(key, value());
      ws();
      char c = peek();
      if (c == ',') {
        pos++;
        continue;
      }
      if (c == '}') {
        pos++;
        return map;
      }
      throw fail("expected ',' or '}' in object");
    }
  }

  private List<Object> array() {
    List<Object> list = new ArrayList<>();
    pos++; // consume '['
    ws();
    if (peek() == ']') {
      pos++;
      return list;
    }
    while (true) {
      ws();
      list.add(value());
      ws();
      char c = peek();
      if (c == ',') {
        pos++;
        continue;
      }
      if (c == ']') {
        pos++;
        return list;
      }
      throw fail("expected ',' or ']' in array");
    }
  }

  private String string() {
    pos++; // consume '"'
    StringBuilder out = new StringBuilder();
    while (true) {
      if (pos >= text.length()) {
        throw fail("unterminated string");
      }
      char c = text.charAt(pos++);
      if (c == '"') {
        return out.toString();
      }
      if (c != '\\') {
        out.append(c);
        continue;
      }
      if (pos >= text.length()) {
        throw fail("unterminated escape sequence");
      }
      char esc = text.charAt(pos++);
      switch (esc) {
        case '"' -> out.append('"');
        case '\\' -> out.append('\\');
        case '/' -> out.append('/');
        case 'b' -> out.append('\b');
        case 'f' -> out.append('\f');
        case 'n' -> out.append('\n');
        case 'r' -> out.append('\r');
        case 't' -> out.append('\t');
        case 'u' -> {
          if (pos + 4 > text.length()) {
            throw fail("truncated \\u escape");
          }
          String hex = text.substring(pos, pos + 4);
          pos += 4;
          try {
            out.append((char) Integer.parseInt(hex, 16));
          } catch (NumberFormatException e) {
            throw fail("invalid \\u escape \"" + hex + "\"");
          }
        }
        default -> throw fail("invalid escape character '\\" + esc + "'");
      }
    }
  }

  private Object number() {
    int start = pos;
    if (peek() == '-') {
      pos++;
    }
    boolean integral = true;
    while (pos < text.length() && isDigit(text.charAt(pos))) {
      pos++;
    }
    if (peek() == '.') {
      integral = false;
      pos++;
      while (pos < text.length() && isDigit(text.charAt(pos))) {
        pos++;
      }
    }
    if (peek() == 'e' || peek() == 'E') {
      integral = false;
      pos++;
      if (peek() == '+' || peek() == '-') {
        pos++;
      }
      while (pos < text.length() && isDigit(text.charAt(pos))) {
        pos++;
      }
    }
    String token = text.substring(start, pos);
    try {
      if (integral) {
        return Long.parseLong(token);
      }
      return Double.parseDouble(token);
    } catch (NumberFormatException e) {
      pos = start;
      throw fail("invalid number");
    }
  }

  // -------------------------------------------------------------- helpers

  private void expect(String literal) {
    if (!text.startsWith(literal, pos)) {
      throw fail("expected \"" + literal + "\"");
    }
    pos += literal.length();
  }

  private void ws() {
    while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
      pos++;
    }
  }

  private char peek() {
    return pos < text.length() ? text.charAt(pos) : '\0';
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private JsonException fail(String message) {
    int line = 1;
    int column = 1;
    for (int i = 0; i < pos && i < text.length(); i++) {
      if (text.charAt(i) == '\n') {
        line++;
        column = 1;
      } else {
        column++;
      }
    }
    return new JsonException(message + " (line " + line + ", column " + column + ")");
  }

  /** Thrown when a JSON document cannot be parsed. */
  public static final class JsonException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JsonException(String message) {
      super(message);
    }
  }
}
