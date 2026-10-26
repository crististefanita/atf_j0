package atf.reporting.extent.session;
import atf.reporting.evidence.ExtentReportScreenshotDeduplicator;
import atf.reporting.markup.ExtentReportAssets;
import atf.reporting.markup.ExtentReportCategoryMarkup;
import atf.reporting.markup.ExtentReportNavigationView;
import atf.reporting.markup.ExtentReportOverviewMarkup;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.runtime.core.ExtentReportRuntimeBehaviour;
import atf.reporting.theme.AtfReportTheme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Converts the vendor Spark output into the self-contained ATF report contract.
 * The vendor remains responsible for the report model; this class owns only
 * deterministic publication concerns such as assets, navigation and defaults.
 */
public final class ExtentReportPublicationPipeline {
    private static final String VENDOR_BODY_OPEN = AtfReportHtmlVocabulary.openTag(
            AtfReportHtmlVocabulary.Tag.BODY,
            AtfReportHtmlVocabulary.classAttribute(
                    AtfReportHtmlVocabulary.CssClass.SPA,
                    AtfReportHtmlVocabulary.CssClass.REPORT,
                    AtfReportHtmlVocabulary.CssClass.DARK));
    private static final String ATF_BODY_OPEN = AtfReportHtmlVocabulary.openTag(
            AtfReportHtmlVocabulary.Tag.BODY,
            AtfReportHtmlVocabulary.classAttribute(
                    AtfReportHtmlVocabulary.CssClass.SPA,
                    AtfReportHtmlVocabulary.CssClass.REPORT,
                    AtfReportHtmlVocabulary.CssClass.DARK,
                    AtfReportHtmlVocabulary.CssClass.ATF_OVERVIEW_DEFAULT));
    private static final String VENDOR_TEST_VIEW = AtfReportHtmlVocabulary.classAttribute(
            AtfReportHtmlVocabulary.CssClass.TEST_WRAPPER,
            AtfReportHtmlVocabulary.CssClass.ROW,
            AtfReportHtmlVocabulary.CssClass.VIEW,
            AtfReportHtmlVocabulary.CssClass.TEST_VIEW);
    private static final String VENDOR_CATEGORY_VIEW = AtfReportHtmlVocabulary.classAttribute(
            AtfReportHtmlVocabulary.CssClass.TEST_WRAPPER,
            AtfReportHtmlVocabulary.CssClass.ROW,
            AtfReportHtmlVocabulary.CssClass.VIEW,
            AtfReportHtmlVocabulary.CssClass.CATEGORY_VIEW,
            AtfReportHtmlVocabulary.CssClass.ATTRIBUTES_VIEW);

    private ExtentReportPublicationPipeline() {
    }

    public static void publish(Path reportPath) throws IOException {
        publish(reportPath, ExtentReportAssets::removeGeneratedAssets);
    }

    static void publish(Path reportPath, SparkAssetCleanup cleanup) throws IOException {
        Objects.requireNonNull(reportPath, "reportPath");
        Objects.requireNonNull(cleanup, "cleanup");
        if (!Files.isRegularFile(reportPath)) {
            throw new IOException("Extent report does not exist: " + reportPath);
        }

        Path assetDirectory = reportPath.toAbsolutePath().normalize().getParent()
                .resolve(ExtentReportAssets.SPARK_DIRECTORY);
        String html = Files.readString(reportPath, StandardCharsets.UTF_8);
        html = ExtentReportAssets.inlineSparkAssets(html, assetDirectory);
        html = injectAtfTheme(html);
        html = injectNavigationAndFailureView(html);
        html = ensureViewIdentifiersAndOverviewDefault(html);
        html = ExtentReportCategoryMarkup.ensureView(html);
        html = injectOverviewMetadata(html);
        html = injectRuntimeBehaviour(html);
        html = removeLegacyFailureTreeHook(html);
        html = ExtentReportScreenshotDeduplicator.deduplicate(html);
        writeAtomically(reportPath, html);
        cleanup.remove(assetDirectory);
    }

    @FunctionalInterface
    interface SparkAssetCleanup {
        void remove(Path assetDirectory) throws IOException;
    }

    private static String injectAtfTheme(String html) {
        String css = AtfReportTheme.css();
        String style = AtfReportHtmlVocabulary.element(AtfReportHtmlVocabulary.Tag.STYLE, css,
                AtfReportHtmlVocabulary.idAttribute(
                        AtfReportHtmlVocabulary.ElementId.THEME_STYLE));
        String headClose = AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.HEAD);
        return html.replace(headClose, style + headClose)
                .replace(VENDOR_BODY_OPEN, ATF_BODY_OPEN);
    }

    /** Fails keeps only its diagnostic index; the full tree remains in Tests. */
    private static String removeLegacyFailureTreeHook(String html) {
        return html.replace("atf-failure-test-detail", "atf-failure-detail");
    }

    private static String injectNavigationAndFailureView(String html) {
        return ExtentReportNavigationView.inject(html);
    }

    private static String ensureViewIdentifiersAndOverviewDefault(String html) {
        String testView = AtfReportHtmlVocabulary.idAttribute(
                AtfReportHtmlVocabulary.ViewId.TESTS) + " "
                + AtfReportHtmlVocabulary.classAttribute(
                AtfReportHtmlVocabulary.CssClass.TEST_WRAPPER,
                AtfReportHtmlVocabulary.CssClass.ROW,
                AtfReportHtmlVocabulary.CssClass.VIEW,
                AtfReportHtmlVocabulary.CssClass.TEST_VIEW,
                AtfReportHtmlVocabulary.CssClass.HIDE);
        String categoryView = AtfReportHtmlVocabulary.idAttribute(
                AtfReportHtmlVocabulary.ViewId.CATEGORIES) + " "
                + AtfReportHtmlVocabulary.classAttribute(
                AtfReportHtmlVocabulary.CssClass.TEST_WRAPPER,
                AtfReportHtmlVocabulary.CssClass.ROW,
                AtfReportHtmlVocabulary.CssClass.VIEW,
                AtfReportHtmlVocabulary.CssClass.CATEGORY_VIEW,
                AtfReportHtmlVocabulary.CssClass.ATTRIBUTES_VIEW,
                AtfReportHtmlVocabulary.CssClass.HIDE);
        html = replaceFirstLiteral(html, VENDOR_TEST_VIEW, testView);
        html = replaceFirstLiteral(html, VENDOR_CATEGORY_VIEW, categoryView);
        return html;
    }

    private static String replaceFirstLiteral(String source, String target, String replacement) {
        int start = source.indexOf(target);
        if (start < 0) return source;

        int end = start + target.length();
        return new StringBuilder(source.length() - target.length() + replacement.length())
                .append(source, 0, start)
                .append(replacement)
                .append(source, end, source.length())
                .toString();
    }

    private static String injectOverviewMetadata(String html) {
        return ExtentReportOverviewMarkup.inject(html);
    }

    private static String injectRuntimeBehaviour(String html) {
        return ExtentReportRuntimeBehaviour.inject(html);
    }

    private static void writeAtomically(Path reportPath, String html) throws IOException {
        Path parent = reportPath.toAbsolutePath().normalize().getParent();
        Path temp = Files.createTempFile(parent, reportPath.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temp, html, StandardCharsets.UTF_8);
            try {
                Files.move(temp, reportPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temp, reportPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

}


