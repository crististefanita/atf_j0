package atf.reporting.markup;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.markup.vocabulary.AtfReportAsset;
import atf.reporting.model.ReportScreenshot;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Owns only the Spark asset boundary of the Extent publication pipeline.
 * Keeping this concern separate makes asset inlining and cleanup testable
 * without constructing a report session or exercising the HTML runtime.
 */
public final class ExtentReportAssets {
    public static final String SPARK_DIRECTORY = "spark";
    public static final String BASE64_SCREENSHOT_LABEL = AtfReportLabels.BASE64_IMAGE_MARKER;
    private static final String PNG_DATA_URI_PREFIX = "data:image/png;base64,";
    private static final int CLEANUP_ATTEMPTS = 4;
    private static final long CLEANUP_RETRY_DELAY_MILLIS = 50;

    private ExtentReportAssets() {
    }

    /** Shared clickable Base64 screenshot: one text-only anchor, one payload and one safe opener. */
    public static String base64ScreenshotLink(String cssClass, String title, String base64Png) {
        String normalizedClass = cssClass == null || cssClass.isBlank()
                ? AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_SCREENSHOT_LINK.value() : cssClass.trim();
        String normalizedTitle = title == null || title.isBlank() ? AtfReportLabels.UI_SCREENSHOT : title.trim();
        String dataUri = PNG_DATA_URI_PREFIX + AtfReportHtmlVocabulary.escapeHtml(base64Png.trim());
        String anchor = AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.A,
                AtfReportHtmlVocabulary.classAttribute(normalizedClass),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.SCREENSHOT,
                        AtfReportHtmlVocabulary.AttributeValue.BASE64.value()),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.HREF, dataUri),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.FEATHERLIGHT,
                        AtfReportHtmlVocabulary.AttributeValue.IMAGE.value()),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.TARGET,
                        AtfReportHtmlVocabulary.AttributeValue.BLANK.value()),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.REL,
                        AtfReportHtmlVocabulary.AttributeValue.NOOPENER.value()),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.ARIA_LABEL,
                        "Open " + normalizedTitle));
        String badge = AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.SPAN,
                        AtfReportHtmlVocabulary.classAttribute(
                                AtfReportHtmlVocabulary.CssClass.BADGE,
                                AtfReportHtmlVocabulary.CssClass.BADGE_GRADIENT_PRIMARY))
                + BASE64_SCREENSHOT_LABEL
                + AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.SPAN);
        return String.format("%s%s%s", anchor, badge,
                AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.A));
    }

    /** Keeps the first title for each payload while preserving the original order. */
    public static List<String[]> distinctScreenshots(List<String[]> screenshots) {
        if (screenshots == null || screenshots.isEmpty()) return List.of();
        Map<String, String[]> unique = new LinkedHashMap<>();
        for (String[] screenshot : screenshots) {
            if (screenshot == null || screenshot.length < 2
                    || screenshot[1] == null || screenshot[1].isBlank()) continue;
            String payload = screenshot[1].trim();
            String title = screenshot[0] == null || screenshot[0].isBlank()
                    ? AtfReportLabels.UI_SCREENSHOT : screenshot[0];
            unique.putIfAbsent(payload, new String[]{title, payload});
        }
        return List.copyOf(unique.values());
    }

    /** Canonical typed screenshot deduplication used by report lifecycle renderers. */
    public static List<ReportScreenshot> distinctScreenshotModels(List<ReportScreenshot> screenshots) {
        if (screenshots == null || screenshots.isEmpty()) return List.of();
        Map<String, ReportScreenshot> unique = new LinkedHashMap<>();
        for (ReportScreenshot screenshot : screenshots) {
            if (screenshot != null) unique.putIfAbsent(screenshot.base64Png(), screenshot);
        }
        return List.copyOf(unique.values());
    }

    public static String inlineSparkAssets(String html, Path assetDirectory) throws IOException {
        String result = html;
        for (var asset : AtfReportAsset.textAssets()) {
            result = inlineTextAsset(result, assetDirectory, asset);
        }
        for (var asset : AtfReportAsset.fontAssets()) {
            result = inlineFontAsset(result, assetDirectory, asset);
        }
        return inlineImageAsset(result, assetDirectory, AtfReportAsset.LOGO);
    }

    public static List<AtfReportAsset> sparkAssetManifest() {
        return List.of(AtfReportAsset.values());
    }

    public static void removeGeneratedAssets(Path assetDirectory) throws IOException {
        removeGeneratedAssets(assetDirectory, Files::deleteIfExists, Thread::sleep);
    }

    static void removeGeneratedAssets(Path assetDirectory, AssetDelete delete, CleanupPause pause)
            throws IOException {
        if (!Files.isDirectory(assetDirectory)
                || !SPARK_DIRECTORY.equals(assetDirectory.getFileName().toString())) {
            return;
        }
        List<Path> orderedPaths;
        try (Stream<Path> paths = Files.walk(assetDirectory)) {
            orderedPaths = paths.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path path : orderedPaths) {
            for (int attempt = 1; attempt <= CLEANUP_ATTEMPTS; attempt++) {
                try {
                    delete.deleteIfExists(path);
                    break;
                } catch (FileSystemException locked) {
                    if (attempt == CLEANUP_ATTEMPTS) {
                        throw new IOException("Unable to remove generated Extent asset after "
                                + CLEANUP_ATTEMPTS + " attempts: " + path, locked);
                    }
                    try {
                        pause.sleep(CLEANUP_RETRY_DELAY_MILLIS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while retrying generated Extent asset cleanup: "
                                + path, interrupted);
                    }
                }
            }
        }
    }

    @FunctionalInterface
    interface AssetDelete {
        boolean deleteIfExists(Path path) throws IOException;
    }

    @FunctionalInterface
    interface CleanupPause {
        void sleep(long millis) throws InterruptedException;
    }

    private static String inlineTextAsset(String html, Path assetDirectory,
                                          AtfReportAsset descriptor) throws IOException {
        String fileName = descriptor.fileName();
        Path asset = assetDirectory.resolve(fileName);
        if (!Files.isRegularFile(asset)) {
            return html;
        }
        String content = Files.readString(asset, StandardCharsets.UTF_8);
        String replacement = AtfReportAsset.Element.STYLE == descriptor.elementType()
                ? AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.STYLE,
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.INLINE, fileName))
                + content + AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.STYLE)
                : AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.SCRIPT,
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.INLINE, fileName))
                + content + AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.SCRIPT);
        String escapedName = "spark/" + fileName;
        String source = AtfReportAsset.Element.STYLE == descriptor.elementType()
                ? AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.LINK,
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.REL, "stylesheet"),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.HREF, escapedName))
                : AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.SCRIPT,
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.SRC, escapedName))
                + AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.SCRIPT);
        return html.replace(source, replacement);
    }

    private static String inlineImageAsset(String html, Path assetDirectory,
                                           AtfReportAsset descriptor) throws IOException {
        Path logo = assetDirectory.resolve(descriptor.fileName());
        if (!Files.isRegularFile(logo)) {
            return html;
        }
        String dataUri = dataUri(descriptor.mimeType(), Files.readAllBytes(logo));
        return html.replace("spark/" + descriptor.fileName(), dataUri);
    }

    private static String inlineFontAsset(String html, Path assetDirectory, AtfReportAsset descriptor)
            throws IOException {
        String fileName = descriptor.fileName();
        Path asset = findAsset(assetDirectory, fileName);
        if (asset == null) {
            return html;
        }
        String dataUri = dataUri(descriptor.mimeType(), Files.readAllBytes(asset));
        String version = AtfReportAsset.FONT_AWESOME_VERSION;
        return html.replace(fileName + "?v=" + version, dataUri)
                .replace(fileName + "?#iefix&v=" + version, dataUri)
                .replace(fileName + "?v=" + version + "#fontawesomeregular", dataUri);
    }

    private static Path findAsset(Path assetDirectory, String fileName) throws IOException {
        if (!Files.isDirectory(assetDirectory)) {
            return null;
        }
        Path direct = assetDirectory.resolve(fileName);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        try (Stream<Path> paths = Files.walk(assetDirectory)) {
            return paths.filter(path -> path.getFileName().toString().equals(fileName))
                    .filter(Files::isRegularFile)
                    .findFirst()
                    .orElse(null);
        }
    }

    private static String dataUri(String mimeType, byte[] content) {
        return String.format("data:%s;base64,%s", mimeType,
                Base64.getEncoder().encodeToString(content));
    }

}
