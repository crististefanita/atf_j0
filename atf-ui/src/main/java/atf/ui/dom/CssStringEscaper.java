package atf.ui.dom;

import java.util.Objects;

/** Escapes a dynamic value for a single-quoted CSS string literal. */
public final class CssStringEscaper {
    private CssStringEscaper() { }

    public static String escapeQuotedValue(String value) {
        Objects.requireNonNull(value, "value");
        StringBuilder escaped = new StringBuilder(value.length());
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (codePoint == '\\' || codePoint == '\'') {
                escaped.append('\\').appendCodePoint(codePoint);
            } else if (codePoint == 0) {
                escaped.append("\\fffd ");
            } else if (codePoint < 0x20 || codePoint == 0x7f) {
                escaped.append('\\').append(Integer.toHexString(codePoint)).append(' ');
            } else {
                escaped.appendCodePoint(codePoint);
            }
        }
        return escaped.toString();
    }
}
