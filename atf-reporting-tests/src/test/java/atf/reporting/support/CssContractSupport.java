package atf.reporting.support;

import java.util.Objects;

/** Shared structural matcher for compact CSS publication contracts. */
public final class CssContractSupport {
    private static final String PUNCTUATION = "{}:;,>~()[]";
    private final String css;

    private CssContractSupport() {
        this.css = null;
    }

    private CssContractSupport(String css) {
        this.css = Objects.requireNonNull(css, "css");
    }

    public static CssContractSupport of(String css) {
        return new CssContractSupport(css);
    }

    public boolean contains(String fragment) {
        return contains(css, fragment);
    }

    public static boolean contains(String css, String fragment) {
        return normalize(css).contains(normalize(fragment));
    }

    private static String normalize(String css) {
        Objects.requireNonNull(css, "css");
        StringBuilder normalized = new StringBuilder(css.length());
        char quote = 0;
        boolean escaped = false;
        boolean whitespace = false;

        for (int index = 0; index < css.length(); index++) {
            char current = css.charAt(index);
            if (quote != 0) {
                normalized.append(current == quote ? '\'' : current);
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    quote = 0;
                }
                continue;
            }
            if (current == '\'' || current == '"') {
                appendWhitespace(normalized, whitespace, current);
                whitespace = false;
                quote = current;
                normalized.append('\'');
                continue;
            }
            if (Character.isWhitespace(current)) {
                whitespace = true;
                continue;
            }
            if (PUNCTUATION.indexOf(current) >= 0) {
                removeTrailingWhitespace(normalized);
                normalized.append(current);
                whitespace = false;
                continue;
            }
            appendWhitespace(normalized, whitespace, current);
            whitespace = false;
            normalized.append(current);
        }
        return normalized.toString().trim();
    }

    private static void appendWhitespace(StringBuilder target, boolean pending, char next) {
        if (!pending || target.isEmpty()) {
            return;
        }
        char previous = target.charAt(target.length() - 1);
        if (Character.isLetterOrDigit(previous) && Character.isLetterOrDigit(next)) {
            target.append(' ');
        } else if (next == '(' && Character.isLetterOrDigit(previous)) {
            target.append(' ');
        } else if (previous != '>' && previous != '~') {
            target.append(' ');
        }
    }

    private static void removeTrailingWhitespace(StringBuilder target) {
        while (!target.isEmpty() && Character.isWhitespace(target.charAt(target.length() - 1))) {
            target.deleteCharAt(target.length() - 1);
        }
    }
}
