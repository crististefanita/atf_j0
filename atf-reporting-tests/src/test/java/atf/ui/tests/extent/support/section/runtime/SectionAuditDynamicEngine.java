package atf.ui.tests.extent.support.section.runtime;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.section.module.SectionAuditRegistry;
import atf.reporting.ui.audit.plan.ExtentSectionGherkin;
import atf.ui.tests.extent.support.section.publish.ExtentSectionAuditPublisher;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import atf.ui.tests.extent.support.audit.screen.OverviewSecondaryContractValidator;
import atf.ui.tests.extent.support.uireport.browser.UiReportStyleValidator;
import atf.ui.tests.extent.support.performance.ReportRuntimePerformanceBrowserProbe;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static atf.reporting.audit.ReportAuditChecks.status;

/**
 * JUnit adapter for section audits; the audit plan and modules remain
 * renderer-neutral production-side components.
 *
 * <p>The class deliberately lives in test support rather than beside the
 * section model. It owns only dynamic-node construction and JUnit assertions;
 * it is not itself an executable test.</p>
 */
public final class SectionAuditDynamicEngine {
    private static final ExtentSectionAuditPublisher PUBLISHER = new ExtentSectionAuditPublisher();

    private SectionAuditDynamicEngine() { }

    public static DynamicNode container(SectionAuditFixture fixture) {
        SectionAuditRegistry registry = new SectionAuditRegistry();
        fixture.spec().validatePlan();
        List<DynamicNode> nodes = new ArrayList<>();
        nodes.add(renderHealth(fixture));
        nodes.addAll(fixture.spec().sections().stream()
                        .map(section -> DynamicTest.dynamicTest(ExtentSectionGherkin.proposition(section),
                                () -> {
                                    var result = PUBLISHER.publish(fixture, fixture.spec(),
                                            registry.module(section));
                                    assertTrue(result.valid(), result.validation()::failureSummary);
                                }))
                        .toList());
        nodes.add(overviewSecondaryExpansion(fixture));
        return DynamicContainer.dynamicContainer(fixture.spec().id(), nodes.stream());
    }

    private static DynamicTest overviewSecondaryExpansion(SectionAuditFixture fixture) {
        return DynamicTest.dynamicTest(ExtentSectionGherkin.overviewSecondaryExpansionProposition(),
                () -> fixture.audit().auditResult("ATF metadata sections expansion", audit -> {
                    Page overview = fixture.audit().openTab(ReportUiContract.Tab.OVERVIEW.hash());
                    var expansion = new OverviewSecondaryContractValidator()
                            .validateExpansionWithEvidence(overview, fixture.audit().actions(),
                                    () -> fixture.audit().capture(overview,
                                            "ATF metadata sections expanded"));
                    audit.screen("ATF metadata sections expansion", "Overview / secondary metadata",
                            expansion.expandedEvidence(), expansion.assertions(), status(expansion.valid()));
                    assertTrue(expansion.valid(), expansion::failureSummary);
                    return null;
                }));
    }

    /**
     * Every generated API report gets the same browser boundary as a static
     * report.  Section modules validate semantics; this gate validates the
     * actual rendered surface and lifecycle diagnostics once, without making
     * every section duplicate the same CSS/console checks.
     */
    private static DynamicTest renderHealth(SectionAuditFixture fixture) {
        return DynamicTest.dynamicTest("GIVEN generated API report WHEN every tab is opened "
                        + "THEN render health and browser diagnostics are valid",
                () -> fixture.audit().auditResult("Generated report render health", audit -> {
                    ReportUiLocators locators = new ReportUiLocators();
                    UiReportStyleValidator validator = new UiReportStyleValidator(locators);
                    List<AtfAssertion> assertions = new ArrayList<>();
                    List<String> violations = new ArrayList<>();
                    Page lastPage = null;
                    for (ReportUiContract.Tab tab : ReportUiContract.Tab.values()) {
                        lastPage = fixture.audit().openTab(tab.hash());
                        UiReportStyleValidator.Result result = validator.inspect(lastPage, tab);
                        violations.addAll(result.violations());
                        assertions.add(ReportAuditChecks.check(tab.name() + " render health",
                                "zero render violations", result.violations().isEmpty()
                                        ? "valid" : result.violations().toString(), result.valid()));
                    }
                    List<String> diagnostics = fixture.context().browserDiagnostics().violations();
                    violations.addAll(diagnostics);
                    assertions.add(ReportAuditChecks.check("Browser diagnostics",
                            "zero console/page/request errors", diagnostics.isEmpty()
                                    ? "valid" : diagnostics.toString(), diagnostics.isEmpty()));
                    ReportRuntimePerformanceBrowserProbe.Result performance =
                            new ReportRuntimePerformanceBrowserProbe().inspect(fixture);
                    violations.addAll(performance.violations());
                    assertions.add(ReportAuditChecks.check("Runtime performance readiness",
                            "fresh browser publishes a ready snapshot",
                            performance.snapshot().readinessAvailable()
                                    ? performance.metricSummary() : "unavailable",
                            performance.snapshot().readinessAvailable()));
                    assertions.add(ReportAuditChecks.check("Runtime performance budget",
                            "load, readiness, disclosure and mutation counters stay within budget",
                            performance.metricSummary()
                                    + (performance.violations().isEmpty()
                                    ? " / valid" : " / " + performance.violations()),
                            performance.violations().isEmpty()));
                    assertions.add(ReportAuditChecks.check("Request signature coverage",
                            "at least six unique rendered request signatures are expandable",
                            performance.requestSignatures().size() + " unique signatures; expanded="
                                    + performance.expandedRequestCount(),
                            performance.requestSignatures().size() >= 6));
                    boolean valid = violations.isEmpty();
                    audit.screen("Generated API report render health", "all report tabs + runtime budget",
                            fixture.audit().capture(performance.page(), "generated report render health"),
                            assertions, status(valid));
                    assertTrue(valid, () -> "Generated report render violations: " + violations);
                    return null;
                }));
    }
}
