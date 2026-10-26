package atf.ui.tests.extent.component.section.contract;
import atf.reporting.ui.audit.plan.ExtentJourney;
import atf.testkit.AtfComponentTest;
import atf.reporting.ui.audit.input.ReportAuditInputKind;

import atf.ui.config.UiBrowserConfiguration;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.config.UiScreenValidationConfiguration;
import atf.reporting.ui.audit.plan.ExtentSection;
import atf.reporting.ui.audit.plan.ExtentSectionCheck;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.reporting.ui.audit.plan.UiExtentReportSource;
import atf.ui.tests.extent.support.section.fixtures.MissingApiSetupDeclaration;
import atf.ui.tests.extent.support.section.source.ReportSourceResolver;
import atf.reporting.ui.audit.source.ResolvedReportSource;
import atf.ui.tests.extent.support.section.runtime.SectionAuditPreconditions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class SectionSourceResolverTest {
    @AfterEach
    void clearProperty() {
        System.clearProperty("atf.section.report");
    }

    @Test
    void staticPathIsConfiguredFromJvmOrUiConfigurationAndFingerprinted() throws Exception {
        Path report = Files.createTempFile("section-source-", ".html");
        Files.writeString(report, "<!doctype html><html><body>report</body></html>");
        UiConfiguration configuration = new UiConfiguration(
                UiBrowserConfiguration.defaults(), Files.createTempDirectory("section-output-"),
                "ui-", List.of("lifecycle"), UiScreenValidationConfiguration.defaults(),
                Optional.of(report), Map.of());
        SectionAuditSpec spec = spec(UiExtentReportSource.STATIC_PATH, "atf.ui.report");

        ResolvedReportSource resolved = ReportSourceResolver.resolveStaticSource(
                getClass(), configuration, spec);

        assertEquals(report.toAbsolutePath().normalize(), resolved.input().report());
        assertEquals(ReportAuditInputKind.EXISTING, resolved.input().kind());
        assertEquals(64, resolved.identity().sha256().length());
        assertTrue(resolved.readOnly());
    }

    @Test
    void malformedStaticReportFailsBeforeDebugUiStarts() throws Exception {
        Path report = Files.createTempFile("section-source-invalid-", ".html");
        Files.writeString(report, "<html><body>not a doctype report</body></html>");
        System.setProperty("atf.section.report", report.toString());

        assertThrows(IllegalArgumentException.class, () ->
                ReportSourceResolver.resolveStaticSource(getClass(), null,
                        spec(UiExtentReportSource.STATIC_PATH, "atf.section.report")));
    }

    @Test
    void generatedAnnotationRequiresExplicitApiSetup() {
        assertThrows(IllegalArgumentException.class,
                () -> SectionAuditPreconditions.resolve(MissingApiSetupDeclaration.class));
    }

    private static SectionAuditSpec spec(UiExtentReportSource source, String property) {
        return new SectionAuditSpec("source-test", source, "", property,
                java.util.Set.of(ExtentSection.TESTS),
                java.util.Set.of(ExtentSectionCheck.STRUCTURE),
                ExtentJourney.NONE);
    }
}
