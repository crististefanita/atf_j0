package atf.reporting.asset;

import java.util.Objects;

/**
 * Applies the publication-only whitespace policy for native browser assets.
 *
 * <p>The class deliberately does not rewrite the classpath source. Native
 * {@code .js}/{@code .css} files remain readable for maintenance and
 * component tests; only the copy embedded in an HTML artifact is compacted.
 * Strings and quoted CSS values are copied byte-for-byte, while comments and
 * insignificant whitespace outside them are removed conservatively.</p>
 */
public final class ReportAssetCompactor {
    public String compact(ReportAsset asset, String source) {
        ReportAsset.require(asset);
        Objects.requireNonNull(source, "source");
        return switch (asset.format()) {
            case CSS -> compactCss(source);
            case JAVASCRIPT -> compactJavaScript(source);
        };
    }

    private String compactJavaScript(String source) {
        return compact(source, true);
    }

    private String compactCss(String source) {
        return compact(source, false);
    }

    private String compact(String source, boolean javascript) {
        StringBuilder result = new StringBuilder(source.length());
        char quote = 0;
        boolean escaped = false;
        boolean regexLiteral = false;
        boolean regexCharacterClass = false;
        boolean blockComment = false;
        boolean lineComment = false;
        boolean whitespace = false;

        for (int index = 0; index < source.length(); index++) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : 0;

            if (regexLiteral) {
                result.append(current);
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '[') {
                    regexCharacterClass = true;
                } else if (current == ']') {
                    regexCharacterClass = false;
                } else if (current == '/' && !regexCharacterClass) {
                    regexLiteral = false;
                }
                continue;
            }
            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    index++;
                }
                continue;
            }
            if (lineComment) {
                if (current == '\n' || current == '\r') {
                    lineComment = false;
                    whitespace = true;
                }
                continue;
            }
            if (quote != 0) {
                result.append(current);
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    quote = 0;
                }
                continue;
            }
            if (current == '\'' || current == '"' || (javascript && current == (char) 96)) {
                appendPendingWhitespace(result, whitespace, current, javascript);
                whitespace = false;
                quote = current;
                result.append(current);
                continue;
            }
            if (current == '/' && next == '*') {
                blockComment = true;
                index++;
                whitespace = true;
                continue;
            }
            if (javascript && current == '/' && next == '/') {
                lineComment = true;
                index++;
                whitespace = true;
                continue;
            }
            if (javascript && current == '/' && canStartRegex(result)) {
                appendPendingWhitespace(result, whitespace, current, true);
                whitespace = false;
                result.append(current);
                regexLiteral = true;
                regexCharacterClass = false;
                escaped = false;
                continue;
            }
            if (Character.isWhitespace(current)) {
                whitespace = true;
                continue;
            }

            if (javascript && (current == '}' || current == ']')
                    && result.length() > 0 && result.charAt(result.length() - 1) == ',') {
                result.deleteCharAt(result.length() - 1);
                whitespace = false;
            }
            if (!javascript && current == '}'
                    && result.length() > 0 && result.charAt(result.length() - 1) == ';') {
                result.deleteCharAt(result.length() - 1);
            }
            appendPendingWhitespace(result, whitespace, current, javascript);
            whitespace = false;
            result.append(current);
        }
        return result.toString().trim();
    }

    private void appendPendingWhitespace(StringBuilder result, boolean pending,
                                         char next, boolean javascript) {
        if (!pending || result.isEmpty()) {
            return;
        }
        char previous = result.charAt(result.length() - 1);
        if (requiresSeparator(previous, next, javascript)) {
            result.append(' ');
        }
    }

    private boolean requiresSeparator(char previous, char next, boolean javascript) {
        if (isWord(previous) && isWord(next)) {
            return true;
        }
        if (!javascript) {
            return !isCssWhitespaceRemovable(previous, next);
        }
        return (previous == '+' && next == '+')
                || (previous == '-' && next == '-')
                || (previous == '/' && next == '/')
                || (previous == '*' && next == '/');
    }

    private boolean isCssWhitespaceRemovable(char previous, char next) {
        // `card :is(...)` is a descendant selector; joining it to `card:is(...)`
        // changes which element must match and can silently disable a CSS rule.
        if (next == ':' && !isCssCombinator(previous) && !isCssBoundary(previous)) {
            return false;
        }
        if (next == '!' || previous == '!') {
            return true;
        }
        if (isCssBoundary(previous) || isCssBoundary(next)) {
            return true;
        }
        if (isCssCombinator(previous) || isCssCombinator(next)) {
            return true;
        }
        return previous == '(' || next == ')' || previous == '[' || next == ']';
    }

    private boolean isCssBoundary(char value) {
        return "{}:;,".indexOf(value) >= 0;
    }

    private boolean isCssCombinator(char value) {
        return ">~+".indexOf(value) >= 0;
    }

    private boolean isWord(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$'
                || value == '-' || value == '%';
    }

    private boolean canStartRegex(StringBuilder result) {
        for (int index = result.length() - 1; index >= 0; index--) {
            char previous = result.charAt(index);
            if (Character.isWhitespace(previous)) {
                continue;
            }
            return "=([{,:;!&|?+-*%~<>".indexOf(previous) >= 0;
        }
        return true;
    }
}
