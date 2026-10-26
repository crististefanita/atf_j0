package atf.shop.tests.support.reporting;

import atf.shop.config.ShopConfiguration;
import atf.shop.reporting.ShopScreenshotPolicy;
import atf.core.config.ApiTransportMode;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/** Builds the safe, semantic configuration snapshot shown in Overview/System. */
public final class ShopReportSystemMetadata {
    private static final String ENVIRONMENT_PROPERTY = "atf.environment";

    private ShopReportSystemMetadata() {
    }

    public static Map<String, String> from(ShopConfiguration configuration) {
        return snapshot(configuration).systemDisplayValues();
    }

    /** Builds the allowlisted form consumed by the reporting composition root. */
    public static ReportEnvironmentSnapshot snapshot(ShopConfiguration configuration) {
        if (configuration == null) return ReportEnvironmentSnapshot.empty();
        return snapshotValues(configuration, configuredEvidenceProfile(configuration)).build();
    }

    /**
     * Compatibility overload for composition roots that also own generic
     * reporting settings. The System preview is deliberately read only from
     * the consumer policy, never from the Request/Response JSON limit.
     */
    public static ReportEnvironmentSnapshot snapshot(ShopConfiguration configuration,
                                                     ReportingSettings reportingSettings) {
        return snapshot(configuration, reportingSettings,
                configuration == null ? ReportEvidenceProfile.defaults()
                        : configuredEvidenceProfile(configuration));
    }

    /** Uses the resolved profile actually passed to the reporting session. */
    public static ReportEnvironmentSnapshot snapshot(ShopConfiguration configuration,
                                                     ReportingSettings reportingSettings,
                                                     ReportEvidenceProfile evidenceProfile) {
        if (reportingSettings == null) throw new NullPointerException("reportingSettings");
        if (configuration == null) return ReportEnvironmentSnapshot.empty();
        if (evidenceProfile == null) throw new NullPointerException("evidenceProfile");
        return snapshotValues(configuration, evidenceProfile).build();
    }

    private static ReportEnvironmentSnapshot.Builder snapshotValues(
            ShopConfiguration configuration,
            ReportEvidenceProfile evidenceProfile) {
        var snapshot = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT,
                        System.getProperty(ENVIRONMENT_PROPERTY, "LOCAL"))
                .put(ReportEnvironmentField.UI_HOST, origin(configuration.baseUrl()))
                .put(ReportEnvironmentField.CLIENT, "Playwright browser")
                .put(ReportEnvironmentField.BROWSER, browser(configuration))
                .put(ReportEnvironmentField.PROFILE,
                        profile(configuration))
                .put(ReportEnvironmentField.UI_JOURNEY_PRECONDITION,
                        journeyPrecondition(configuration))
                .put(ReportEnvironmentField.RETRY_POLICY,
                        configuration.uiRetry().summary())
                .put(ReportEnvironmentField.SCREENSHOT_POLICY,
                        screenshotPolicy(evidenceProfile.screenshotPolicy()))
                .put(ReportEnvironmentField.UI_EVIDENCE_LAYOUT,
                        evidenceLayout(evidenceProfile.flowMode()))
                .put(ReportEnvironmentField.UI_TIMEOUT,
                        humanDuration(configuration.browser().timeout()))
                .put(ReportEnvironmentField.NAVIGATION_TIMEOUT,
                        humanDuration(configuration.browser().navigationTimeout()))
                .put(ReportEnvironmentField.SOURCE_BUILD,
                        configuration.validationProfile().apiEvidenceRequired()
                                && configuration.api().mode() == ApiTransportMode.LIVE
                                ? "Live HTTP evidence when observed"
                                : configuration.validationProfile().apiEvidenceRequired()
                                ? "Fixture transport boundary"
                                : "External live website")
                .put(ReportEnvironmentField.REPORTING,
                        "Extent offline HTML · UI/API output roles separated");

        if (configuration.validationProfile().apiEvidenceRequired()) {
            snapshot
                    .put(ReportEnvironmentField.API_ENDPOINT, origin(configuration.api().endpoint()))
                    .put(ReportEnvironmentField.TRANSPORT_MODE,
                            configuration.api().mode() == ApiTransportMode.LIVE ? "LIVE HTTP" : "FIXTURE")
                    .put(ReportEnvironmentField.PRECONDITION, precondition(configuration))
                    .put(ReportEnvironmentField.API_EVIDENCE,
                            "enabled at checkpoints: "
                                    + configuration.api().evidenceBinding().displayValue())
                    .put(ReportEnvironmentField.NETWORK_INSPECTOR,
                            "Enabled · collapsed summary · checkpoints: "
                                    + configuration.api().evidenceBinding().displayValue()
                                    + " · JSON in checkpoint evidence")
                    .put(ReportEnvironmentField.BODY_PREVIEW,
                            String.format("%,d characters · full JSON remains in checkpoint evidence",
                                    configuration.reporting().systemBodyPreviewCharacters()))
                    .put(ReportEnvironmentField.API_TIMEOUT,
                            humanDuration(configuration.api().timeout()));
        }
        return snapshot;
    }

    private static ReportEvidenceProfile configuredEvidenceProfile(
            ShopConfiguration configuration) {
        return ShopScreenshotPolicy.profile(configuration.reporting().screenshotPolicy());
    }

    private static String evidenceLayout(ReportEvidenceFlowMode flowMode) {
        return flowMode == ReportEvidenceFlowMode.UNIFIED_FLOW
                ? "One consolidated flow" : "Checkpoint-by-checkpoint";
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

    /**
     * Builds the consumer snapshot with the System preview policy bound.
     * The overload keeps the pure component contract convenient while generic
     * Request/Response JSON settings remain owned by {@code atf-reporting}.
     */
    public static Map<String, String> from(ShopConfiguration configuration,
                                           ReportingSettings reportingSettings) {
        return snapshot(configuration, reportingSettings).systemDisplayValues();
    }

    private static String browser(ShopConfiguration configuration) {
        var browser = configuration.browser();
        return browser.browser() + " · " + (browser.headless() ? "headless" : "headed")
                + " · viewport " + browser.viewportWidth() + " × " + browser.viewportHeight()
                + " · zoom " + browser.zoomPercent() + "%";
    }

    private static String profile(ShopConfiguration configuration) {
        return configuration.validationProfile().apiEvidenceRequired()
                ? "UI + API evidence" : "UI evidence";
    }

    private static String precondition(ShopConfiguration configuration) {
        return "API request precondition: "
                + configuration.api().precondition().name().toLowerCase();
    }

    private static String journeyPrecondition(ShopConfiguration configuration) {
        return configuration.authenticatedE2eEnabled()
                ? "authenticated account lifecycle selected"
                : "guest checkout configuration ready";
    }

    private static String humanDuration(Duration duration) {
        long millis = duration.toMillis();
        if (millis % 1_000 == 0) return (millis / 1_000) + "s";
        return millis + "ms";
    }

    private static String origin(URI uri) {
        if (uri == null || uri.getScheme() == null || uri.getHost() == null) return "configured";
        String authority = uri.getRawAuthority();
        return uri.getScheme() + "://" + (authority == null ? uri.getHost() : authority);
    }
}
