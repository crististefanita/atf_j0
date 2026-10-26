package atf.shop.config;

import atf.reporting.evidence.ReportScreenshotPolicy;

import java.util.Objects;

/** Report-only policy owned by the Practice Shop consumer. */
public record ShopReportConfiguration(int systemBodyPreviewCharacters,
                                              ReportScreenshotPolicy screenshotPolicy) {
    public static final int DEFAULT_SYSTEM_BODY_PREVIEW_CHARACTERS = 100;

    public ShopReportConfiguration(int systemBodyPreviewCharacters) {
        this(systemBodyPreviewCharacters, ReportScreenshotPolicy.EACH_CHECKPOINT);
    }

    public ShopReportConfiguration {
        if (systemBodyPreviewCharacters <= 0) {
            throw new IllegalArgumentException(
                    "reporting.systemBodyPreviewCharacters must be positive");
        }
        screenshotPolicy = Objects.requireNonNull(screenshotPolicy, "screenshotPolicy");
    }

    public static ShopReportConfiguration defaults() {
        return new ShopReportConfiguration(DEFAULT_SYSTEM_BODY_PREVIEW_CHARACTERS,
                ReportScreenshotPolicy.EACH_CHECKPOINT);
    }
}
