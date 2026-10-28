package atf.shop.support.reporting.component;

import atf.shop.config.ShopConfigurationLoader;
import atf.reporting.audit.AssertionAuditRun;
import atf.reporting.configuration.ReportEnvironmentField;
import atf.reporting.configuration.ReportEnvironmentSnapshot;
import atf.reporting.console.AtfConsoleSink;

/** Consumer configuration and metadata; shared session mechanics belong to Reporting. */
public final class ShopComponentReportFactory {
    private ShopComponentReportFactory() { }

    public static AssertionAuditRun open(AtfConsoleSink console) {
        var directory = ShopConfigurationLoader.load().apiReportDirectory().resolve("component");
        var environment = ReportEnvironmentSnapshot.builder()
                .put(ReportEnvironmentField.ENVIRONMENT, "COMPONENT")
                .put(ReportEnvironmentField.SUITE, "Practice Shop component")
                .put(ReportEnvironmentField.REPORTING, "Extent HTML")
                .put(ReportEnvironmentField.REPORT_ARTIFACT, "Scoped component audit").build();
        return AssertionAuditRun.extent(directory, "component-atf-practice-shop", environment, console);
    }
}
