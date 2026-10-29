package atf.shop.runtime;

import atf.shop.config.ShopConfiguration;
import atf.shop.reporting.environment.ShopReportSystemMetadata;
import atf.reporting.console.AtfConsoleSink;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.ui.UiExtentReportSession;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Report acquisition consumes captured environment/profile; JUnit chooses ownership and disposal. */
public final class ShopRunReportFactory {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");
    private ShopRunReportFactory() { }
    public static UiExtentReportSession open(ShopConfiguration configuration, ReportEvidenceProfile profile,
                                              AtfConsoleSink console, String environment) {
        Objects.requireNonNull(configuration); Objects.requireNonNull(profile); Objects.requireNonNull(console); Objects.requireNonNull(environment);
        var clock = Clock.systemDefaultZone();
        String file = "ui-atf-practice-shop-e2e_" + STAMP.withZone(clock.getZone()).format(clock.instant()) + ".html";
        return UiExtentReportSession.openConfiguredResolved(configuration.uiReportDirectory(), file, console, profile,
                ShopReportSystemMetadata.snapshot(configuration, profile, environment));
    }
}
