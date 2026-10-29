package atf.reporting.configuration.ui;

import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/** Shared operational UI vocabulary; consumers own configuration resolution and optional API semantics. */
public record ReportUiEnvironment(String environment, URI uiHost, Browser browser, String profile,
                                  String retryPolicy, ReportEvidenceProfile evidence, String sourceBuild) {
    public ReportUiEnvironment {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(browser, "browser");
        Objects.requireNonNull(evidence, "evidence");
    }

    /** Starts a fresh allowlisted projection that the consumer can extend before freezing. */
    public ReportEnvironmentSnapshot.Builder describe() {
        return ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, environment)
                .put(ReportEnvironmentField.UI_HOST, origin(uiHost))
                .put(ReportEnvironmentField.CLIENT, "Playwright browser")
                .put(ReportEnvironmentField.BROWSER, browser.description())
                .put(ReportEnvironmentField.PROFILE, profile)
                .put(ReportEnvironmentField.RETRY_POLICY, retryPolicy)
                .put(ReportEnvironmentField.SCREENSHOT_POLICY, screenshotPolicy(evidence.screenshotPolicy()))
                .put(ReportEnvironmentField.UI_EVIDENCE_LAYOUT, evidence.flowMode() == ReportEvidenceFlowMode.UNIFIED_FLOW
                        ? "One consolidated flow" : "Checkpoint-by-checkpoint")
                .put(ReportEnvironmentField.UI_ACTION_DETAILS, evidence.actionDetailsMode().configValue())
                .put(ReportEnvironmentField.UI_TIMEOUT, duration(browser.actionTimeout()))
                .put(ReportEnvironmentField.NAVIGATION_TIMEOUT, duration(browser.navigationTimeout()))
                .put(ReportEnvironmentField.SOURCE_BUILD, sourceBuild)
                .put(ReportEnvironmentField.REPORTING, "Extent offline HTML · UI/API output roles separated");
    }

    public record Browser(String engine, boolean headless, int width, int height, int zoomPercent,
                          Duration actionTimeout, Duration navigationTimeout) {
        public Browser {
            Objects.requireNonNull(engine, "engine");
            Objects.requireNonNull(actionTimeout, "actionTimeout");
            Objects.requireNonNull(navigationTimeout, "navigationTimeout");
        }
        public String description() {
            return engine + " · " + (headless ? "headless" : "headed") + " · viewport " + width + " × " + height
                    + " · zoom " + zoomPercent + "%";
        }
    }

    /** Origin-only projection deliberately discards user info, path, query and fragment. */
    public static String origin(URI uri) {
        if (uri == null || uri.getScheme() == null || uri.getHost() == null) return "configured";
        String host = uri.getHost();
        if (host.contains(":") && !host.startsWith("[")) host = "[" + host + "]";
        return uri.getScheme() + "://" + host + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
    }

    public static String duration(Duration value) {
        long millis = Objects.requireNonNull(value, "duration").toMillis();
        return millis % 1000 == 0 ? (millis / 1000) + "s" : millis + "ms";
    }

    private static String screenshotPolicy(ReportScreenshotPolicy policy) {
        return switch (policy) {
            case EACH_CHECKPOINT -> "Each checkpoint";
            case FINAL_ONLY -> "Final screenshot only";
            case FAILURE_ONLY -> "Failures only";
            case FINAL_AND_FAILURE -> "Final screenshot + failures";
            case NONE -> "Disabled";
        };
    }
}
