package atf.autoex.runtime.reporting;

import atf.autoex.config.AutoExConfiguration;
import atf.autoex.locator.AutoExRoutes;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.ui.config.geometry.UiViewportScale;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.configuration.ui.ReportUiEnvironment;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryPolicy;
import java.util.Locale;
import java.util.Objects;

/** Pure AutoEx projection of resolved per-scenario settings; no JVM reads or report-resource ownership. */
public final class AutoExReportSystemMetadata {
    public static final String DEFAULT_ENVIRONMENT = "LOCAL";
    private AutoExReportSystemMetadata() { }

    public static ReportEnvironmentSnapshot snapshot(AutoExConfiguration configuration, ReportEvidenceProfile evidence,
                                                      boolean apiEnabled, String environment) {
        Objects.requireNonNull(configuration, "configuration");
        var browser = configuration.browser();
        var snapshot = new ReportUiEnvironment(environment, configuration.baseUrl(), browser(browser),
                apiEnabled ? "UI + API evidence" : "UI evidence",
                "UI: " + retry(browser.retryPolicy()) + "; store navigation: " + retry(configuration.storeAvailabilityPolicy()),
                evidence, apiEnabled ? "Browser HTTP evidence when observed" : "External live website").describe()
                .put(ReportEnvironmentField.VIEWPORT, new UiViewportScale(browser).description());
        if (apiEnabled) {
            var network = browser.networkInspection();
            snapshot.put(ReportEnvironmentField.API_ENDPOINT, ReportUiEnvironment.origin(AutoExRoutes.PRODUCTS_API.resolve(configuration.baseUrl())))
                    .put(ReportEnvironmentField.TRANSPORT_MODE, "LIVE HTTP")
                    .put(ReportEnvironmentField.PRECONDITION, "Browser observation; API-only preparation remains explicit")
                    .put(ReportEnvironmentField.API_EVIDENCE, "enabled at UI_API checkpoints")
                    .put(ReportEnvironmentField.NETWORK_INSPECTOR, network.enabled()
                            ? "Enabled · collapsed summary · " + network.capturePolicy().description() : "Disabled")
                    .put(ReportEnvironmentField.BODY_PREVIEW, String.format(Locale.ROOT,
                            "%,d characters captured · JSON in checkpoint evidence", network.maxBodyPreviewCharacters()))
                    .put(ReportEnvironmentField.API_TIMEOUT, network.settlementPolicy().enabled()
                            ? "Browser-managed; observation wait " + ReportUiEnvironment.duration(network.settlementPolicy().timeout())
                            : "Browser-managed; observation wait disabled");
        }
        return snapshot.build();
    }

    /** Low-level legacy callers have a browser contract but no declared UI/API channel. */
    public static ReportEnvironmentSnapshot browserOnly(UiBrowserConfiguration browser, ReportEvidenceProfile evidence) {
        return new ReportUiEnvironment(DEFAULT_ENVIRONMENT, browser.baseUrl(), browser(browser),
                "Channel declared by scenario", retry(browser.retryPolicy()), evidence, "Configured browser target").describe()
                .put(ReportEnvironmentField.VIEWPORT, new UiViewportScale(browser).description()).build();
    }

    private static ReportUiEnvironment.Browser browser(UiBrowserConfiguration browser) {
        return new ReportUiEnvironment.Browser(browser.browser(), browser.headless(), browser.viewportWidth(), browser.viewportHeight(),
                browser.zoomPercent(), browser.timeout(), browser.navigationTimeout());
    }

    private static String retry(UiRetryPolicy policy) {
        return policy.enabled() ? policy.maxAttempts() + " attempts · " + ReportUiEnvironment.duration(policy.backoff())
                + " backoff · " + policy.eligibleOperations().stream().map(Enum::name).sorted().toList() : "disabled";
    }
}
