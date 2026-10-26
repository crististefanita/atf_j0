package atf.reporting.security;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Redacts captured form-data parts without changing safe fields or MIME boundaries. */
final class MultipartEvidenceRedactor {
    private static final Pattern DISPOSITION = Pattern.compile(
            "(?im)^Content-Disposition[ \\t]*:[ \\t]*form-data[ \\t]*;([^\\r\\n]*(?:\\r?\\n[ \\t]+[^\\r\\n]*)*)");
    private static final Pattern PARAMETER = Pattern.compile(
            "(?:^|;)[ \\t]*([^\\s=;]+)[ \\t]*=[ \\t]*(?:\"((?:\\\\.|[^\"\\\\])*)\"|([^;\\s]+))");
    private static final Pattern BOUNDARY = Pattern.compile("(?m)^--[^\\r\\n]+\\r?$");
    private static final Pattern HEADER_END = Pattern.compile("\\r?\\n\\r?\\n");

    private MultipartEvidenceRedactor() { }

    static String redact(String value, String replacement, Predicate<String> sensitive) {
        Matcher parts = DISPOSITION.matcher(value);
        if (!parts.find()) return value;
        Matcher boundaries = BOUNDARY.matcher(value);
        boolean pendingBoundary = boundaries.find();
        String lastBoundary = null;
        StringBuilder safe = new StringBuilder(value.length());
        int copied = 0;
        do {
            while (pendingBoundary && boundaries.start() < parts.start()) {
                lastBoundary = boundaries.group().stripTrailing();
                pendingBoundary = boundaries.find();
            }
            // A curl display is not a raw MIME document: its first boundary is shell-quoted inline.
            // Replay redacts the raw body before quoting it; never consume its shell delimiters here.
            if (lastBoundary == null) return value;
            if (parts.start() < copied) continue;
            Matcher parameters = PARAMETER.matcher(parts.group(1).replaceAll("\\r?\\n[ \\t]+", " "));
            boolean sensitivePart = false;
            while (parameters.find()) {
                if (!"name".equalsIgnoreCase(parameters.group(1))) continue;
                String field = parameters.group(2) == null ? parameters.group(3)
                        : parameters.group(2).replaceAll("\\\\(.)", "$1");
                sensitivePart |= sensitive.test(field);
            }
            if (!sensitivePart) continue;

            Pattern boundary = Pattern.compile(
                    "(?m)^" + Pattern.quote(lastBoundary) + "(?:--)?[ \\t]*(?:\\r?$)");
            int limit = nextBoundary(value, boundary, parts.end());
            Matcher headers = HEADER_END.matcher(value);
            int start = headers.find(parts.end()) && headers.start() < limit
                    ? headers.end() : parts.end();
            int end = nextBoundary(value, boundary, start);
            // The CRLF introducing the next boundary is framing, not the field value.
            if (end < value.length() && end > start && value.charAt(end - 1) == '\n') {
                end--;
                if (end > start && value.charAt(end - 1) == '\r') end--;
            }
            safe.append(value, copied, start).append(replacement);
            copied = end;
        } while (parts.find());
        return copied == 0 ? value : safe.append(value, copied, value.length()).toString();
    }

    private static int nextBoundary(String value, Pattern boundary, int start) {
        Matcher next = boundary.matcher(value);
        return next.find(start) ? next.start() : value.length();
    }
}
