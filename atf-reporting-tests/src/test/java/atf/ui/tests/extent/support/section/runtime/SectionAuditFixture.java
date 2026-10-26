package atf.ui.tests.extent.support.section.runtime;
import atf.ui.tests.extent.support.audit.api.ApiReportContractValidation;
import atf.ui.tests.extent.support.audit.mock.MockReportContractAuditor;
import atf.ui.tests.extent.support.api.ApiReportExplorationFlow;
import atf.ui.tests.extent.support.api.ApiReportUiFlow;
import atf.ui.tests.extent.support.audit.ReportAuditContext;
import atf.ui.tests.extent.support.data.UiTestData;
import atf.ui.tests.extent.support.lifecycle.UiAuditFlow;
import atf.reporting.audit.ValidationResult;

import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.ui.tests.extent.support.api.ApiReportAuditSetup;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.reporting.ui.audit.plan.UiExtentReportSource;
import atf.ui.tests.extent.support.section.source.ReportSourceResolver;
import atf.reporting.ui.audit.source.ResolvedReportSource;
import atf.reporting.ui.audit.plan.ReportProjection;
import atf.reporting.ui.audit.vocabulary.UiAuditGherkin;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Composition root shared by static component audits and generated API audits. */
public final class SectionAuditFixture implements AutoCloseable {
    private final SectionAuditSpec spec;
    private final ApiReportAuditSetup.Result setup;
    private final ResolvedReportSource source;
    private final ReportAuditContext context;
    private final UiAuditFlow audit;
    private final ApiReportUiFlow flow;
    private final ApiReportExplorationFlow exploration;

    private SectionAuditFixture(SectionAuditSpec spec, ApiReportAuditSetup.Result setup,
                                ResolvedReportSource source,
                                ReportAuditContext context, UiAuditFlow audit,
                                ApiReportUiFlow flow, ApiReportExplorationFlow exploration) {
        this.spec = spec;
        this.setup = setup;
        this.source = source;
        this.context = context;
        this.audit = audit;
        this.flow = flow;
        this.exploration = exploration;
    }

    public static SectionAuditFixture open(Class<?> owner, UiConfiguration configuration) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(configuration, "configuration");
        SectionAuditSpec spec = SectionAuditPreconditions.resolve(owner);
        ApiReportAuditSetup.Result setup = null;
        ResolvedReportSource source;
        if (spec.source() == UiExtentReportSource.GENERATED_API) {
            setup = new ApiReportAuditSetup().generate(owner, configuration);
            source = ReportSourceResolver.fingerprintGenerated(setup.report());
        } else {
            source = ReportSourceResolver.resolveStaticSource(owner, configuration, spec);
        }
        ReportAuditContext context = ReportAuditContext.open(configuration, source.input(), spec.id());
        List<String> tags = new ArrayList<>(UiTestData.defaultTags());
        tags.add("section");
        tags.addAll(spec.sections().stream().map(Enum::name).toList());
        UiAuditFlow audit = new UiAuditFlow(context, configuration, owner.getSimpleName(),
                owner.getPackageName(), tags, UiAuditGherkin.reportReady(spec.source()),
                "Extent section audit failed", spec.sections().toString());
        var locators = new atf.ui.tests.extent.support.locators.ReportUiLocators();
        return new SectionAuditFixture(spec, setup, source, context, audit,
                new ApiReportUiFlow(audit, locators),
                new ApiReportExplorationFlow(audit, locators));
    }

    public SectionAuditSpec spec() { return spec; }

    public ApiReportAuditSetup.Result setup() { return setup; }

    public ResolvedReportSource source() { return source; }

    public List<String> expectedOwnerNames() {
        return setup == null ? List.of() : setup.manifest().expectedOwnerNames();
    }

    /**
     * Builds the immutable expectation before browser inspection. Static
     * reports intentionally expose an empty projection; generated reports
     * derive it from the API selection manifest.
     */
    public ReportProjection expectedProjection() {
        return setup == null
                ? ReportProjection.empty()
                : setup.manifest().expectedProjection();
    }

    /**
     * Reconciles every rendered request for the generated API source against
     * the same mock/YAML catalog. This is the executable owner of the
     * MOCK_RECONCILIATION section check; callers receive only a typed result.
     */
    public ValidationResult reconcileMockContracts(Page testsPage) {
        Objects.requireNonNull(testsPage, "testsPage");
        if (setup == null) return ValidationResult.empty();
        ApiReportContractValidation validator = new ApiReportContractValidation(
                flow, new MockReportContractAuditor(setup.mock()));
        ValidationResult combined = ValidationResult.empty();
        for (String ownerName : expectedOwnerNames()) {
            flow.confirmTestsTabVisible(testsPage, ownerName);
            ApiReportUiFlow.Owner owner = flow.openOwner(testsPage, ownerName);
            int expanded = flow.expandOwnerRequests(owner);
            ApiReportContractValidation.Result result = validator.inspect(owner);
            flow.recordOwnerContractAudit(owner, expanded, result);
            combined = combined.merge(result.validation());
        }
        return combined;
    }

    public ReportAuditContext context() { return context; }

    public UiAuditFlow audit() { return audit; }

    public ApiReportUiFlow flow() { return flow; }

    public ApiReportExplorationFlow exploration() { return exploration; }

    @Override
    public void close() {
        context.close();
    }
}

