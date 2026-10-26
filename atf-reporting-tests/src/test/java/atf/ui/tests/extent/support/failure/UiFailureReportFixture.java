package atf.ui.tests.extent.support.failure;

import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.tests.extent.support.api.ApiReportUiFlow;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.audit.ReportAuditContext;
import atf.reporting.ui.audit.input.ReportAuditInput;
import atf.reporting.ui.contract.ReportUiContract;
import atf.reporting.ui.failure.UiFailureReportInput;
import atf.reporting.ui.failure.UiFailureReportSpec;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;
import com.microsoft.playwright.Page;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Browser composition root for one annotated UI failure audit. */
public final class UiFailureReportFixture implements AutoCloseable {
    private final UiFailureReportSpec spec;
    private final Path inputReport;
    private final ReportAuditContext context;
    private final UiAuditFlow audit;
    private final ReportUiLocators locators;
    private final ApiReportUiFlow flow;

    private UiFailureReportFixture(UiFailureReportSpec spec, Path inputReport,
                                   ReportAuditContext context, UiAuditFlow audit,
                                   ReportUiLocators locators, ApiReportUiFlow flow) {
        this.spec = spec;
        this.inputReport = inputReport;
        this.context = context;
        this.audit = audit;
        this.locators = locators;
        this.flow = flow;
    }

    public static UiFailureReportFixture open(Class<?> owner, UiConfiguration configuration) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(configuration, "configuration");
        UiFailureReportSpec spec = UiFailureReportSpec.resolve(owner);
        Path input = resolveInput(spec, configuration);
        ReportAuditContext context = ReportAuditContext.open(configuration,
                ReportAuditInput.existing(input), spec.id());
        ReportUiLocators locators = new ReportUiLocators();
        UiAuditFlow audit = new UiAuditFlow(context, configuration, owner.getSimpleName(),
                owner.getPackageName(), List.of("ui", "extent", "failure-contract"),
                setupDescription(spec.input()),
                "UI failure contract failed", spec.id());
        return new UiFailureReportFixture(spec, input, context, audit, locators,
                new ApiReportUiFlow(audit, locators));
    }

    private static Path resolveInput(UiFailureReportSpec spec, UiConfiguration configuration) {
        if (spec.input() == UiFailureReportInput.GENERATED_FAILURE_FIXTURE) {
            return UiFailureReportFactory.create(configuration);
        }
        String configured = System.getProperty(spec.reportProperty());
        if ((configured == null || configured.isBlank()) && configuration.auditInputReport().isPresent()) {
            configured = configuration.auditInputReport().orElseThrow().toString();
        }
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Missing report input -D" + spec.reportProperty());
        }
        Path input = Path.of(configured).toAbsolutePath().normalize();
        if (!Files.isRegularFile(input)) {
            throw new IllegalArgumentException("Configured report input does not exist: " + input);
        }
        return input;
    }

    private static String setupDescription(UiFailureReportInput input) {
        return switch (input) {
            case GENERATED_FAILURE_FIXTURE -> "the generated Extent failure fixture is available for inspection";
            case STATIC_API_REPORT -> "the configured API Extent report is available for failure inspection";
            case STATIC_UI_REPORT -> "the configured UI Extent report is available for failure inspection";
        };
    }

    public UiFailureReportSpec spec() { return spec; }
    public Path inputReport() { return inputReport; }
    public UiAuditFlow audit() { return audit; }
    public ReportUiLocators locators() { return locators; }
    public ApiReportUiFlow flow() { return flow; }

    public Page openTests() {
        return audit.openTab(ReportUiContract.Tab.TESTS.hash());
    }

    @Override
    public void close() {
        context.close();
    }
}
