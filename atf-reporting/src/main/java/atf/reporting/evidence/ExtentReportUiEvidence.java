package atf.reporting.evidence;
import atf.reporting.markup.ExtentReportAssets;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.theme.evidence.AtfUiEvidenceCss;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.closeTag;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.idAttribute;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.openTag;

/**
 * Publishes temporary Playwright captures as self-contained, collapsible
 * evidence in the final Extent HTML artifact.
 */
public final class ExtentReportUiEvidence {
    private static final AtfReportHtmlVocabulary.ElementId SECTION_ID =
            AtfReportHtmlVocabulary.ElementId.UI_EVIDENCE;
    private static final AtfReportHtmlVocabulary.ElementId STYLE_ID =
            AtfReportHtmlVocabulary.ElementId.UI_EVIDENCE_STYLE;
    private static final AtfReportHtmlVocabulary.CssClass SCREENSHOT_LINK_CLASS =
            AtfReportHtmlVocabulary.CssClass.ATF_UI_EVIDENCE_SCREENSHOT_LINK;

    private ExtentReportUiEvidence() {
    }

    public record Capture(Path png, String title, List<String> steps) {
        public Capture {
            Objects.requireNonNull(png, "png");
            Objects.requireNonNull(title, "title");
            steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        }
    }

    /**
     * Embeds PNG captures and their audit steps into the report Overview.
     * The source files are not copied beside the report and are never
     * referenced by a relative URL.
     */
    public static void embedInlinePngs(Path report, List<Capture> captures) throws IOException {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(captures, "captures");
        if (captures.isEmpty()) {
            return;
        }
        String html = Files.readString(report, StandardCharsets.UTF_8);
        String evidence = render(captures);
        html = html.replaceAll("(?s)<section id=\"" + SECTION_ID.value() + "\".*?</section>", "");
        int dashboard = html.indexOf("<div id=\""
                + AtfReportHtmlVocabulary.ViewId.OVERVIEW.value() + "\"");
        int openingEnd = dashboard < 0 ? -1 : html.indexOf('>', dashboard);
        if (openingEnd < 0) {
            throw new IOException("Extent report does not expose dashboard-view: " + report);
        }
        html = html.substring(0, openingEnd + 1) + evidence + html.substring(openingEnd + 1);
        writeAtomically(report, html);
    }

    private static String render(List<Capture> captures) throws IOException {
        Map<String, RenderedCapture> uniqueCaptures = new LinkedHashMap<>();
        for (Capture capture : captures) {
            Path png = capture.png().toAbsolutePath().normalize();
            if (!Files.isRegularFile(png)) {
                throw new IOException("Playwright capture does not exist: " + png);
            }
            String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(png));
            uniqueCaptures.putIfAbsent(base64, new RenderedCapture(capture, base64));
        }
        StringBuilder html = new StringBuilder();
        html.append(openTag(AtfReportHtmlVocabulary.Tag.STYLE, idAttribute(STYLE_ID.value())))
                .append(AtfUiEvidenceCss.stylesheet(SECTION_ID, SCREENSHOT_LINK_CLASS))
                .append(closeTag(AtfReportHtmlVocabulary.Tag.STYLE))
                .append(openTag(AtfReportHtmlVocabulary.Tag.SECTION, idAttribute(SECTION_ID.value())))
                .append(openTag(AtfReportHtmlVocabulary.Tag.H2))
                .append("Playwright exploratory evidence · ").append(uniqueCaptures.size())
                .append(" captures").append(closeTag(AtfReportHtmlVocabulary.Tag.H2));
        for (RenderedCapture rendered : uniqueCaptures.values()) {
            Capture capture = rendered.capture();
            html.append(openTag(AtfReportHtmlVocabulary.Tag.DETAILS))
                    .append(openTag(AtfReportHtmlVocabulary.Tag.SUMMARY))
                    .append(AtfReportHtmlVocabulary.escapeHtml(capture.title()))
                    .append(closeTag(AtfReportHtmlVocabulary.Tag.SUMMARY))
                    .append(openTag(AtfReportHtmlVocabulary.Tag.OL));
            for (String step : capture.steps()) {
                html.append(openTag(AtfReportHtmlVocabulary.Tag.LI))
                        .append(AtfReportHtmlVocabulary.escapeHtml(step))
                        .append(closeTag(AtfReportHtmlVocabulary.Tag.LI));
            }
            html.append(closeTag(AtfReportHtmlVocabulary.Tag.OL))
                    .append(ExtentReportAssets.base64ScreenshotLink(
                            SCREENSHOT_LINK_CLASS.value(), capture.title(), rendered.base64()))
                    .append(closeTag(AtfReportHtmlVocabulary.Tag.DETAILS));
        }
        return html.append(closeTag(AtfReportHtmlVocabulary.Tag.SECTION)).toString();
    }

    private record RenderedCapture(Capture capture, String base64) {
    }

    private static void writeAtomically(Path report, String html) throws IOException {
        Path absolute = report.toAbsolutePath().normalize();
        Path temporary = Files.createTempFile(absolute.getParent(), absolute.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, html, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
