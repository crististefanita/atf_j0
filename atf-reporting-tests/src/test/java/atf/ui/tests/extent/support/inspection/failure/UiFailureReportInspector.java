package atf.ui.tests.extent.support.inspection.failure;

import atf.ui.tests.extent.support.failure.UiFailureReportFixture;
import atf.reporting.ui.failure.UiFailureSurface;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.tests.extent.support.locators.failurecontext.ReportFailureContextLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.io.IOException;
import java.nio.file.Files;

/** Browser adapter that validates one failure channel without hard-coded test selectors. */
public final class UiFailureReportInspector {
    private static final ReportFailureContextLocators EVIDENCE = new ReportFailureContextLocators();
    private final UiFailureReportFixture fixture;

    public UiFailureReportInspector(UiFailureReportFixture fixture) {
        this.fixture = Objects.requireNonNull(fixture, "fixture");
    }

    public Observation inspect(UiFailureSurface surface) {
        Objects.requireNonNull(surface, "surface");
        Page page = fixture.openTests();
        List<String> violations = new ArrayList<>();
        Locator tests = fixture.locators().testsView(page);
        List<String> owners = fixture.flow().ownerNames(page);
        if (owners.isEmpty()) {
            return Observation.invalid(surface, "Tests tab has no rendered owner");
        }
        var owner = fixture.flow().openOwner(page, owners.get(0));
        int expandedOnArrival = fixture.locators().expandedScenarioHeaders(tests).count()
                + fixture.locators().expandedPhaseHeaders(tests).count()
                + fixture.locators().expandedRequestOrValidationHeaders(tests).count()
                + fixture.locators().openEvidenceDisclosures(tests).count();
        if (expandedOnArrival != 0) violations.add("failure input is not collapsed on arrival");
        fixture.flow().expandAll(owner.detail());
        int visibleClosedEvidenceBodies = fixture.locators()
                .visibleClosedEvidenceBodies(owner.detail()).count();
        if (visibleClosedEvidenceBodies > 0) {
            violations.add("closed evidence disclosure leaks " + visibleClosedEvidenceBodies
                    + " visible body(ies); expected summary-only rendering");
        }
        int fabricatedHttpDisclosures = fixture.locators()
                .httpEvidenceDisclosures(owner.detail()).count();
        if (fabricatedHttpDisclosures > 0) {
            violations.add("UI failure evidence must not invent HTTP Request/Response disclosures; found "
                    + fabricatedHttpDisclosures);
        }

        int matching = switch (surface) {
            case SETUP -> failedSetupPhases(owner.detail());
            case PLAYWRIGHT_ACTION -> failedActionRows(owner.detail());
            case AUDIT_OBSERVATION -> failedObservationRows(owner.detail());
            case ASSERTION -> failedAssertionRows(owner.detail());
        };
        if (matching == 0) violations.add("no rendered failure found for " + surface);
        boolean siblingIsolationClean = surface != UiFailureSurface.PLAYWRIGHT_ACTION
                || validatePlaywrightFailureIsolation(owner.detail(), violations);
        if (surface == UiFailureSurface.PLAYWRIGHT_ACTION) {
            boolean exactAction = fixture.locators().actionSequences(owner.detail()).all().stream()
                    .flatMap(sequence -> fixture.locators().failedActionRows(sequence).all().stream())
                    .anyMatch(UiFailureReportInspector::matchesTimeoutAction);
            if (!exactAction) {
                violations.add("Tests must retain the failed wait action, locator, measured time and expectation");
            }
            validateTimeoutDiagnostics(owner.detail(), violations);
        }
        validateConcreteFailureStatus(surface, owner.detail(), violations);

        Page fails = fixture.audit().openTab(ReportUiContract.Tab.FAILS.hash());
        Locator failsView = fixture.locators().failsView(fails);
        int failRows = fixture.locators().failureItems(failsView).count();
        if (failRows == 0) violations.add("Fails tab does not expose the fixture failures");
        validateFailsProjection(surface, fails, failsView, violations);
        return new Observation(surface, matching, failRows, expandedOnArrival,
                visibleClosedEvidenceBodies, fabricatedHttpDisclosures, siblingIsolationClean,
                violations.isEmpty(), List.copyOf(violations));
    }

    private static boolean matchesTimeoutAction(Locator row) {
        if (!EVIDENCE.operation(row).textContent().equals("Wait for visible")) return false;
        var cells = EVIDENCE.cells(row);
        var observed = cells.nth(3);
        var full = EVIDENCE.fullValue(observed);
        String context = full.count() == 0 ? observed.textContent() : full.textContent();
        return cells.nth(1).textContent().equals("button[data-test='login-submit']")
                && cells.nth(2).textContent().equals("login submit becomes visible")
                && cells.nth(4).textContent().equals("20000 ms")
                && context.contains("elapsed=20000 ms")
                && context.contains("expected=login submit becomes visible");
    }

    private boolean validatePlaywrightFailureIsolation(Locator ownerDetail,
                                                         List<String> violations) {
        Locator scenarios = fixture.locators().scenarios(ownerDetail);
        int failureOwners = 0;
        int siblingLeaks = 0;
        for (int index = 0; index < scenarios.count(); index++) {
            String text = Objects.requireNonNullElse(scenarios.nth(index).textContent(), "");
            if (text.contains("Timeout 20000ms exceeded")) {
                failureOwners++;
                if (!text.contains("button[data-test='login-submit']")) {
                    violations.add("the timeout cause is not colocated with its actionable Playwright locator");
                }
            } else if (text.contains("raw-ui-password") || text.contains("raw-ui-token")) {
                siblingLeaks++;
            }
        }
        if (scenarios.count() < 2) {
            violations.add("the Extent fixture must contain a sibling scenario to prove failure isolation");
        }
        if (failureOwners != 1) {
            violations.add("the Playwright timeout cause must belong to exactly one Extent scenario; found "
                    + failureOwners);
        }
        if (siblingLeaks > 0) {
            violations.add("the Playwright timeout cause or raw credentials leaked into "
                    + siblingLeaks + " sibling scenario(s)");
        }
        return scenarios.count() >= 2 && failureOwners == 1 && siblingLeaks == 0;
    }

    private void validateFailsProjection(UiFailureSurface surface, Page page, Locator fails,
                                         List<String> violations) {
        Locator item = fixture.locators().failureItemByName(fails, surface.scenarioTitle());
        if (item.count() == 0) {
            violations.add("Fails tab does not expose the selected " + surface + " scenario");
            return;
        }
        fixture.audit().actions().click(
                fixture.locators().failureItemActionByName(page, surface.scenarioTitle()),
                "select the " + surface + " failure in Fails");
        fixture.audit().actions().waitForVisible(fixture.locators().failureDetailAction(fails));
        String detail = fixture.locators().failureDetail(fails).innerText();
        surface.failsDetailMarkers().stream()
                .filter(marker -> !detail.contains(marker))
                .forEach(marker -> violations.add(
                        "Fails detail for " + surface + " is missing concrete marker: " + marker));
        if (surface == UiFailureSurface.PLAYWRIGHT_ACTION) {
            if (detail.contains("raw-ui-password") || detail.contains("raw-ui-token")) {
                violations.add("Fails detail exposes a raw credential from the timeout cause");
            }
        }
        validateUiFailureVocabulary(fails, surface, violations);
        validateNarrowFailsLayout(page, surface, violations);
    }

    private void validateTimeoutDiagnostics(Locator detail, List<String> violations) {
        String diagnostics = String.join(" ", fixture.locators()
                .uiValidationObservations(detail).allTextContents());
        if (!diagnostics.contains("Primary exception")
                || !diagnostics.contains("TimeoutError: Timeout 20000ms exceeded")
                || !diagnostics.contains("password=***REDACTED***")
                || !diagnostics.contains("Authorization: Bearer ***REDACTED***")
                || !diagnostics.contains("Recorded action #1")) {
            violations.add("Tests Audit observations must retain the native timeout and redacted credentials");
        }
        try {
            String html = Files.readString(fixture.inputReport());
            if (html.contains("raw-ui-password") || html.contains("raw-ui-token")) {
                violations.add("the published HTML exposes a raw timeout credential");
            }
        } catch (IOException error) {
            throw new IllegalStateException("Cannot inspect the published failure artifact", error);
        }
    }

    private void validateUiFailureVocabulary(Locator fails, UiFailureSurface surface,
                                             List<String> violations) {
        String labels = String.join(" ", fixture.locators().failureDetailLabels(fails).allInnerTexts());
        if (!labels.contains("STEP") && !labels.contains("ACTION")) {
            violations.add("Fails detail for " + surface + " must expose STEP or ACTION, not an API REQUEST field");
        }
        if (labels.contains("REQUEST")) {
            violations.add("Fails detail for " + surface + " exposes API REQUEST terminology");
        }
        if (labels.contains("DURATION")
                || fixture.locators().failureDetail(fails).innerText().contains("No response duration recorded")) {
            violations.add("Fails detail for " + surface + " exposes a transport duration placeholder");
        }
        if (!fixture.locators().failureDetailTitle(fails).isVisible()
                || !fixture.locators().failureDetailOpenLink(fails).isVisible()) {
            violations.add("Fails detail for " + surface + " hides its title or Open in Tests link");
        }
    }

    private void validateNarrowFailsLayout(Page page, UiFailureSurface surface,
                                           List<String> violations) {
        Map<String, Object> viewport = viewport(page);
        int width = ((Number) viewport.get("width")).intValue();
        int height = ((Number) viewport.get("height")).intValue();
        try {
            // The Fails tab is already active at the normal viewport. Keep its hash
            // while resizing: the narrow contract validates the Fails layout, not
            // whether a horizontally clipped navigation bar can still be clicked.
            page.setViewportSize(700, Math.max(600, height));
            fixture.audit().actions().waitForVisible(
                    fixture.locators().failsView(page), ReportUiContract.Tab.FAILS.hash());
            Page narrow = page;
            Locator narrowFails = fixture.locators().failsView(narrow);
            Map<String, Object> layout = fixture.locators().failureLayout(narrowFails);
            if (!"column".equals(layout.get("flexDirection"))) {
                violations.add("Fails must stack list and detail vertically below the narrow breakpoint");
            }
            if (number(layout, "scrollWidth") > number(layout, "clientWidth") + 1) {
                violations.add("Fails has horizontal overflow at the narrow breakpoint");
            }
            if (number(layout, "documentScrollWidth") > number(layout, "viewportWidth") + 1) {
                violations.add("Fails page has horizontal overflow at the narrow breakpoint");
            }
            if (!fixture.locators().failureDetailTitle(narrowFails).isVisible()
                    || !fixture.locators().failureDetailOpenLink(narrowFails).isVisible()) {
                violations.add("Fails narrow layout hides the failure title or Open in Tests link");
            }
        } finally {
            page.setViewportSize(width, height);
            fixture.audit().actions().waitForVisible(
                    fixture.locators().failsView(page), ReportUiContract.Tab.FAILS.hash());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> viewport(Page page) {
        return (Map<String, Object>) page.evaluate("() => ({width: window.innerWidth, height: window.innerHeight})");
    }

    private static double number(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.doubleValue() : 0d;
    }

    private int failedSetupPhases(Locator detail) {
        int count = 0;
        Locator phases = fixture.locators().phases(detail);
        for (int index = 0; index < phases.count(); index++) {
            Locator phase = phases.nth(index);
            if (!fixture.locators().nodeTitle(phase).contains("SETUP")
                    || fixture.locators().failedPhaseBadge(phase).count() == 0) continue;
            Locator evidence = fixture.locators().setupEvidence(phase);
            if (fixture.locators().setupEvidenceRows(evidence)
                    .filter(new Locator.FilterOptions().setHasText("❌")).count() > 0) count++;
        }
        return count;
    }

    private int failedActionRows(Locator detail) {
        int count = 0;
        Locator sequences = fixture.locators().actionSequences(detail);
        for (int index = 0; index < sequences.count(); index++) {
            count += fixture.locators().failedActionRows(sequences.nth(index)).count();
        }
        return count;
    }

    private void validateConcreteFailureStatus(UiFailureSurface surface, Locator detail,
                                               List<String> violations) {
        switch (surface) {
            case SETUP -> {
                Locator phases = fixture.locators().phases(detail);
                for (int index = 0; index < phases.count(); index++) {
                    Locator phase = phases.nth(index);
                    Locator evidence = fixture.locators().setupEvidence(phase);
                    if (fixture.locators().setupEvidenceRows(evidence)
                            .filter(new Locator.FilterOptions().setHasText("❌")).count() > 0
                            && fixture.locators().failedEvidenceEventStatuses(evidence).count() == 0) {
                        violations.add("setup failure evidence is rendered as Info instead of Fail");
                    }
                }
            }
            case PLAYWRIGHT_ACTION -> validateActionZones(
                    fixture.locators().actionSequences(detail), violations,
                    "Playwright action failure evidence is rendered as Info instead of Fail");
            case AUDIT_OBSERVATION -> validateActionZones(
                    fixture.locators().uiValidationObservations(detail), violations,
                    "audit observation failure evidence is rendered as Info instead of Fail");
            case ASSERTION -> {
                Locator tables = fixture.locators().uiValidationAssertionTables(detail);
                for (int index = 0; index < tables.count(); index++) {
                    Locator table = tables.nth(index);
                    if (fixture.locators().failedAssertionRows(table).count() > 0
                            && fixture.locators().failedEvidenceEventStatuses(table).count() == 0) {
                        violations.add("assertion failure evidence is rendered as Info instead of Fail");
                    }
                    if (fixture.locators().failedAssertionRows(table).count() > 0
                            && fixture.locators().uiFailureEvidenceFor(table).count() == 0) {
                        violations.add("assertion failure must expose one explicit Fail evidence leaf");
                    }
                }
            }
        }
    }

    private void validateActionZones(Locator zones, List<String> violations, String message) {
        for (int index = 0; index < zones.count(); index++) {
            Locator zone = zones.nth(index);
            Locator failedRows = fixture.locators().failedAssertionRows(zone);
            if (failedRows.count() > 0
                    && fixture.locators().failedEvidenceEventStatuses(zone).count() == 0) {
                violations.add(message);
            }
            if (failedRows.count() > 0
                    && fixture.locators().uiFailureEvidenceFor(zone).count() == 0) {
                violations.add("failed UI evidence must expose one explicit Fail leaf with its concrete cause");
            }
        }
    }

    private int failedObservationRows(Locator detail) {
        int count = 0;
        Locator observations = fixture.locators().uiValidationObservations(detail);
        for (int index = 0; index < observations.count(); index++) {
            count += fixture.locators().failedObservationRows(observations.nth(index)).count();
        }
        return count;
    }

    private int failedAssertionRows(Locator detail) {
        int count = 0;
        Locator tables = fixture.locators().uiValidationAssertionTables(detail);
        for (int index = 0; index < tables.count(); index++) {
            count += fixture.locators().failedAssertionRows(tables.nth(index)).count();
        }
        return count;
    }

    public record Observation(UiFailureSurface surface, int matchingFailures,
                              int failRows, int expandedOnArrival,
                              int visibleClosedEvidenceBodies,
                              int fabricatedHttpDisclosures,
                              boolean siblingIsolationClean, boolean valid,
                              List<String> violations) {
        public Observation {
            surface = Objects.requireNonNull(surface, "surface");
            violations = List.copyOf(Objects.requireNonNull(violations, "violations"));
        }

        static Observation invalid(UiFailureSurface surface, String violation) {
            return new Observation(surface, 0, 0, 0, 0, 0, false, false, List.of(violation));
        }
    }
}
