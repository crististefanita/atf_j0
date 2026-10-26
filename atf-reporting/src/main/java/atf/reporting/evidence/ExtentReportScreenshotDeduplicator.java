package atf.reporting.evidence;

import atf.reporting.markup.AtfReportLabels;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes repeated embedded screenshot payloads while keeping every visual state
 * actionable through a visible inline reference to the first PNG anchor.
 */
public final class ExtentReportScreenshotDeduplicator {
    private static final Pattern SCREENSHOT_LINK = Pattern.compile(
            "<" + AtfReportHtmlVocabulary.Tag.A.value() + "\\b(?=[^>]*"
                    + AtfReportHtmlVocabulary.Attribute.SCREENSHOT.value()
                    + "=\\\"base64\\\")"
                    + "(?=[^>]*href=\\\"data:image/png;base64,([^\\\"]+)\\\")[^>]*>.*?</"
                    + AtfReportHtmlVocabulary.Tag.A.value() + ">",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private ExtentReportScreenshotDeduplicator() {
    }

    public static String deduplicate(String html) {
        if (html == null || html.isEmpty()) return html;
        Map<String, String> references = new LinkedHashMap<>();
        Matcher matcher = SCREENSHOT_LINK.matcher(html);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String payload = matcher.group(1);
            String fingerprint = fingerprint(payload);
            String reference = references.get(fingerprint);
            boolean canonical = reference == null;
            if (canonical) {
                reference = "screenshot-" + (references.size() + 1);
                references.put(fingerprint, reference);
            }
            String replacement = canonical
                    ? withCanonicalReference(matcher.group(), reference)
                    : duplicateReference(reference);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String duplicateReference(String reference) {
        String marker = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN,
                AtfReportLabels.BASE64_IMAGE_MARKER,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.BADGE_GRADIENT_PRIMARY));
        String note = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.SPAN,
                AtfReportLabels.DUPLICATE_SCREENSHOT_NOTE,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.ATF_UI_SCREENSHOT_DUPLICATE_TEXT));
        return AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.A,
                marker + note,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_SCREENSHOT_LINK,
                        AtfReportHtmlVocabulary.CssClass.ATF_UI_SCREENSHOT_DUPLICATE),
                AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.SCREENSHOT_REF, reference),
                AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.HREF,
                        "#atf-screenshot-" + reference),
                AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.ARIA_LABEL,
                        AtfReportLabels.OPEN_DUPLICATE_SCREENSHOT),
                AtfReportHtmlVocabulary.attribute(
                        AtfReportHtmlVocabulary.Attribute.TITLE,
                        AtfReportLabels.OPEN_DUPLICATE_SCREENSHOT));
    }

    private static String withCanonicalReference(String anchor, String reference) {
        String openingTag = anchor.substring(0, anchor.indexOf('>') + 1);
        String referenceAttribute = AtfReportHtmlVocabulary.Attribute.SCREENSHOT_REF.value()
                + "=\"";
        if (openingTag.contains(referenceAttribute)) return anchor;
        String referenceAttributes = " "
                + AtfReportHtmlVocabulary.idAttribute("atf-screenshot-" + reference)
                + " "
                + AtfReportHtmlVocabulary.attribute(
                AtfReportHtmlVocabulary.Attribute.SCREENSHOT_REF, reference);
        String enrichedOpeningTag = openingTag.substring(0, openingTag.length() - 1)
                + referenceAttributes + ">";
        return enrichedOpeningTag + anchor.substring(openingTag.length());
    }

    private static String fingerprint(String payload) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.US_ASCII));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is required for screenshot deduplication", error);
        }
    }
}
