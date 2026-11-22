package atf.shop.tests.component.recovery;

import atf.shop.tests.support.ShopComponentTest;
import atf.shop.tests.support.reporting.ShopRecoveryExtentReportProbe;
import atf.testkit.assertions.RecordedAssertions;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.playwright.PlaywrightUiSession;
import atf.ui.recovery.UiRecoveryTrace;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Opt-in browser acceptance for a suite-generated Shop recovery report. */
@ShopComponentTest
class ShopRecoveryActualReportAcceptanceTest {
    static final String REPORT_PROPERTY = "atf.shop.recovery.actual-report";

    @ParameterizedTest(name = "Actual Shop recovery report · {0}")
    @EnumSource(value = UiRecoveryTrace.Decision.class, names = {"RECOVERED", "EXHAUSTED"})
    @EnabledIfSystemProperty(named = REPORT_PROPERTY, matches = ".+")
    void renderedSuiteReportRetainsTypedRecoveryEvidence(UiRecoveryTrace.Decision outcome,
                                                         RecordedAssertions checks) throws Exception {
        Path report = Path.of(System.getProperty(REPORT_PROPERTY)).toAbsolutePath().normalize();
        checks.isTrue("The supplied actual Shop report exists", Files.isRegularFile(report));
        Path screenshot = Path.of("target", "recovery-actual-report-shop-"
                        + outcome.name().toLowerCase(Locale.ROOT) + ".png")
                .toAbsolutePath();
        try (var viewer = PlaywrightUiSession.open(UiBrowserConfiguration.defaults())) {
            ShopRecoveryExtentReportProbe.verifyActualComponentReport(
                    viewer.page(), report, outcome, screenshot, checks);
        }
    }
}
