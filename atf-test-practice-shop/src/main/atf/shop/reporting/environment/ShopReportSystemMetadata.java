package atf.shop.reporting.environment;

import atf.shop.config.ShopConfiguration;
import atf.shop.reporting.ShopScreenshotPolicy;
import atf.core.config.ApiTransportMode;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.ui.config.geometry.UiViewportScale;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.configuration.ui.ReportUiEnvironment;
import atf.reporting.evidence.profile.ReportEvidenceProfile;

import java.util.Map;
import java.util.Locale;

/** Builds the safe, semantic configuration snapshot shown in Overview/System. */
public final class ShopReportSystemMetadata {
    public static final String DEFAULT_ENVIRONMENT = "LOCAL";

    private ShopReportSystemMetadata() {
    }

    public static Map<String, String> from(ShopConfiguration configuration) {
        return snapshot(configuration).systemDisplayValues();
    }

    /** Builds the allowlisted form consumed by the reporting composition root. */
    public static ReportEnvironmentSnapshot snapshot(ShopConfiguration configuration) {
        if (configuration == null) return ReportEnvironmentSnapshot.empty();
        return snapshotValues(configuration, configuredEvidenceProfile(configuration), DEFAULT_ENVIRONMENT).build();
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
        return snapshot(configuration, evidenceProfile, DEFAULT_ENVIRONMENT);
    }

    /** Composition roots pass the environment captured for this execution; this projection reads no JVM state. */
    public static ReportEnvironmentSnapshot snapshot(ShopConfiguration configuration,
                                                     ReportEvidenceProfile evidenceProfile,
                                                     String environment) {
        if (configuration == null) return ReportEnvironmentSnapshot.empty();
        return snapshotValues(configuration, java.util.Objects.requireNonNull(evidenceProfile, "evidenceProfile"),
                java.util.Objects.requireNonNull(environment, "environment")).build();
    }

    private static ReportEnvironmentSnapshot.Builder snapshotValues(
            ShopConfiguration configuration,
            ReportEvidenceProfile evidenceProfile, String environment) {
        var browser = configuration.browser();
        var snapshot = new ReportUiEnvironment(environment, configuration.baseUrl(),
                new ReportUiEnvironment.Browser(browser.browser(), browser.headless(), browser.viewportWidth(), browser.viewportHeight(),
                        browser.zoomPercent(), browser.timeout(), browser.navigationTimeout()),
                profile(configuration), configuration.uiRetry().summary(), evidenceProfile,
                configuration.validationProfile().apiEvidenceRequired()
                                && configuration.api().mode() == ApiTransportMode.LIVE
                                ? "Live HTTP evidence when observed"
                                : configuration.validationProfile().apiEvidenceRequired()
                                ? "Fixture transport boundary"
                                : "External live website").describe()
                .put(ReportEnvironmentField.VIEWPORT, new UiViewportScale(browser).description())
                .put(ReportEnvironmentField.UI_JOURNEY_PRECONDITION, journeyPrecondition(configuration));

        if (configuration.validationProfile().apiEvidenceRequired()) {
            snapshot
                    .put(ReportEnvironmentField.API_ENDPOINT, ReportUiEnvironment.origin(configuration.api().endpoint()))
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
                            String.format(Locale.ROOT, "%,d characters · full JSON remains in checkpoint evidence",
                                    configuration.reporting().systemBodyPreviewCharacters()))
                    .put(ReportEnvironmentField.API_TIMEOUT,
                            ReportUiEnvironment.duration(configuration.api().timeout()));
        }
        return snapshot;
    }

    private static ReportEvidenceProfile configuredEvidenceProfile(
            ShopConfiguration configuration) {
        return ShopScreenshotPolicy.profile(configuration.reporting());
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

    private static String profile(ShopConfiguration configuration) {
        return configuration.validationProfile().apiEvidenceRequired()
                ? "UI + API evidence" : "UI evidence";
    }

    private static String precondition(ShopConfiguration configuration) {
        return "API request precondition: "
                + configuration.api().precondition().name().toLowerCase(Locale.ROOT);
    }

    private static String journeyPrecondition(ShopConfiguration configuration) {
        return configuration.authenticatedE2eEnabled()
                ? "authenticated account lifecycle selected"
                : "guest checkout configuration ready";
    }

}
