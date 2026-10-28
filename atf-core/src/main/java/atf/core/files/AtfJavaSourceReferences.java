package atf.core.files;

import java.util.Objects;
import java.util.Arrays;
import java.util.regex.Pattern;

/** Java namespace references in code; documentation and literal examples are not dependencies. */
public final class AtfJavaSourceReferences {
    private AtfJavaSourceReferences() { }

    public static boolean containsNamespace(String source, String namespace) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(namespace, "namespace");
        if (!namespace.matches("[\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*"
                + "(?:\\.[\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*)*")) {
            throw new IllegalArgumentException("Not a Java namespace: " + namespace);
        }
        String qualifiedName = String.join("\\s*\\.\\s*",
                Arrays.stream(namespace.split("\\.")).map(Pattern::quote).toList());
        return Pattern.compile("(?<![\\p{javaJavaIdentifierPart}.])" + qualifiedName
                        + "(?![\\p{javaJavaIdentifierPart}])")
                .matcher(codeOnly(source)
                        .replaceAll("\\s*\\.\\s*", ".")).find();
    }

    /** Lexical code for source guards, with Java Unicode translation before comment/literal removal. */
    public static String codeOnly(String source) {
        return scanCode(translateUnicodeEscapes(Objects.requireNonNull(source, "source")));
    }

    /** Java translates Unicode escapes before recognizing comments and literals. */
    private static String translateUnicodeEscapes(String source) {
        StringBuilder translated = new StringBuilder(source.length());
        int contiguousBackslashes = 0;
        boolean previousTranslated = false;
        for (int cursor = 0; cursor < source.length();) {
            char current = source.charAt(cursor);
            boolean eligible = current == '\\'
                    && (previousTranslated || contiguousBackslashes % 2 == 0);
            if (eligible && cursor + 1 < source.length() && source.charAt(cursor + 1) == 'u') {
                int markerEnd = cursor + 1;
                while (markerEnd < source.length() && source.charAt(markerEnd) == 'u') markerEnd++;
                if (markerEnd + 4 > source.length()) {
                    throw new IllegalArgumentException("Incomplete Java Unicode escape");
                }
                int value = 0;
                for (int index = markerEnd; index < markerEnd + 4; index++) {
                    int digit = asciiHexDigit(source.charAt(index));
                    if (digit < 0) throw new IllegalArgumentException("Invalid Java Unicode escape");
                    value = (value << 4) | digit;
                }
                char decoded = (char) value;
                translated.append(decoded);
                contiguousBackslashes = decoded == '\\' ? contiguousBackslashes + 1 : 0;
                previousTranslated = true;
                cursor = markerEnd + 4;
            } else {
                translated.append(current);
                contiguousBackslashes = current == '\\' ? contiguousBackslashes + 1 : 0;
                previousTranslated = false;
                cursor++;
            }
        }
        return translated.toString();
    }

    private static int asciiHexDigit(char value) {
        if (value >= '0' && value <= '9') return value - '0';
        if (value >= 'a' && value <= 'f') return value - 'a' + 10;
        if (value >= 'A' && value <= 'F') return value - 'A' + 10;
        return -1;
    }

    private static String scanCode(String source) {
        StringBuilder code = new StringBuilder(source.length());
        for (int cursor = 0; cursor < source.length();) {
            char current = source.charAt(cursor);
            if (source.startsWith("//", cursor)) {
                cursor += 2;
                while (cursor < source.length() && source.charAt(cursor) != '\n'
                        && source.charAt(cursor) != '\r') cursor++;
                code.append(' ');
            } else if (source.startsWith("/*", cursor)) {
                int end = source.indexOf("*/", cursor + 2);
                cursor = end < 0 ? source.length() : end + 2;
                code.append(' ');
            } else if (current == '"' || current == '\'') {
                boolean textBlock = source.startsWith("\"\"\"", cursor);
                cursor = skipLiteral(source, cursor + (textBlock ? 3 : 1), current, textBlock);
                code.append(' ');
            } else {
                code.append(current);
                cursor++;
            }
        }
        return code.toString();
    }

    private static int skipLiteral(String source, int cursor, char quote, boolean textBlock) {
        while (cursor < source.length()) {
            if (source.charAt(cursor) == '\\') {
                cursor = Math.min(source.length(), cursor + 2);
            } else if (textBlock && source.startsWith("\"\"\"", cursor)) {
                return cursor + 3;
            } else if (!textBlock && source.charAt(cursor) == quote) {
                return cursor + 1;
            } else {
                cursor++;
            }
        }
        return cursor;
    }
}
