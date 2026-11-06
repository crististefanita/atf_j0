package atf.reporting.artifact.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jsoup.nodes.Element;

/** URI classification and CSS/srcset token extraction, after HTML entity decoding by Jsoup. */
final class ResourceReferences {
    private static final Pattern EVIDENCE = Pattern.compile("(?i).*\\.(?:xml|json|jsonl|properties|log|txt|csv|tsv|zip|gz|tar|png|jpe?g|webp|gif|svg|pdf|mp4|webm|wav|mp3)(?:[?#].*)?$");
    private static final Pattern CSS_ESCAPE = Pattern.compile("\\\\(?:([0-9a-fA-F]{1,6})\\s?|([^\\r\\n]))");
    private static final Pattern CSS_TOKEN = Pattern.compile(
            "/\\*.*?\\*/|url\\(\\s*(?:\"([^\"]*)\"|'([^']*)'|([^)]*))\\s*\\)|@import\\s+(?:\"([^\"]*)\"|'([^']*)')|\"[^\"]*\"|'[^']*'",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private ResourceReferences() { }

    static boolean embedded(String reference) { return reference.regionMatches(true, 0, "data:", 0, 5); }

    static boolean externalEvidence(Element anchor, String reference) {
        if (reference.isEmpty() || reference.startsWith("#") || embedded(reference)) return false;
        String normalized = reference.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("javascript:")) return false;
        boolean web = normalized.startsWith("https://") || normalized.startsWith("http://");
        return !web || anchor.closest("[data-atf-node-role=observation]") != null
                || anchor.hasAttr("download") || anchor.hasAttr("data-atf-screenshot")
                || anchor.hasAttr("data-atf-screenshot-ref") || EVIDENCE.matcher(reference).matches();
    }

    static List<String> css(String source) {
        var escapes = CSS_ESCAPE.matcher(source);
        String decoded = escapes.replaceAll(m -> java.util.regex.Matcher.quoteReplacement(m.group(1) == null ? m.group(2)
                : Character.toString(Integer.parseInt(m.group(1), 16) == 0 ? 0xfffd : safeCodePoint(m.group(1)))));
        var result = new ArrayList<String>();
        var tokens = CSS_TOKEN.matcher(decoded);
        while (tokens.find()) for (int group = 1; group <= 5; group++)
            if (tokens.group(group) != null) { result.add(tokens.group(group).strip()); break; }
        return result;
    }

    private static int safeCodePoint(String hex) {
        int point = Integer.parseInt(hex, 16);
        return Character.isValidCodePoint(point) ? point : 0xfffd;
    }

    static List<String> srcset(String value) {
        var result = new ArrayList<String>();
        int index = 0;
        while (index < value.length()) {
            while (index < value.length() && (Character.isWhitespace(value.charAt(index)) || value.charAt(index) == ',')) index++;
            int start = index;
            boolean data = value.regionMatches(true, index, "data:", 0, 5);
            while (index < value.length() && !Character.isWhitespace(value.charAt(index)) && (data || value.charAt(index) != ',')) index++;
            if (start < index) result.add(value.substring(start, index).replaceFirst(",+$", ""));
            while (index < value.length() && value.charAt(index) != ',') index++;
        }
        return result;
    }
}
