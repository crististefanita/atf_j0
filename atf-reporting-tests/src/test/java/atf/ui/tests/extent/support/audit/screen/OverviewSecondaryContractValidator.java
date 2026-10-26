package atf.ui.tests.extent.support.audit.screen;

import atf.reporting.audit.ReportAuditChecks;
import atf.reporting.audit.ValidationResult;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.ui.contract.ReportUiContract;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.UiScreenValidation;
import atf.ui.tests.extent.support.locators.ReportUiLocators;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Reconciles the two secondary Overview disclosures and the package projection
 * with the package labels rendered under the Tests-tab owner names.
 *
 * <p>The browser adapter owns only DOM extraction. The equality rules are
 * exposed as small pure methods so component tests can protect the contract
 * without starting a browser or adding another E2E test.</p>
 */
public final class OverviewSecondaryContractValidator {
    private static final int REQUIRED_SECONDARY_ZONE_COUNT = 2;
    private static final List<String> REQUIRED_EXECUTION_METRICS = List.of(
            "Test classes in this run", "Scenarios in this run", "Scenario pass",
            "Scenario fail", "Scenario skipped", "Scenario warnings");
    private static final List<String> REQUIRED_TAG_AUDIT_TABLES = List.of(
            "atf-audit-scenarios", "atf-audit-packages", "atf-audit-tags");
    private static final List<ReportUiContract.OverviewSection> SECONDARY_ZONES = List.of(
            ReportUiContract.OverviewSection.EXECUTION_METADATA,
            ReportUiContract.OverviewSection.TAG_AUDIT);
    private final ReportUiLocators locators;

    public OverviewSecondaryContractValidator() {
        this(new ReportUiLocators());
    }

    public OverviewSecondaryContractValidator(ReportUiLocators locators) {
        this.locators = Objects.requireNonNull(locators, "locators");
    }

    public ValidationResult validate(Page testsPage, Page overviewPage) {
        Objects.requireNonNull(testsPage, "testsPage");
        Objects.requireNonNull(overviewPage, "overviewPage");
        Locator overview = locators.overviewView(overviewPage);
        int zones = locators.overviewSecondaryZones(overview).count();
        int openZones = locators.overviewOpenSecondaryZones(overview).count();
        int visibleClosedBodies = locators.overviewVisibleClosedSecondaryBodies(overview).count();
        Set<String> testPackages = textValues(locators.testPackageNodes(testsPage));
        Set<String> overviewPackages = packageAuditNames(overview);

        List<AtfAssertion> assertions = List.of(
                ReportAuditChecks.check("ATF secondary Overview zones", "2 collapsed disclosures",
                        zones + " found / " + openZones + " open / " + visibleClosedBodies
                                + " visible closed bodies",
                        collapsedSecondaryZones(zones, openZones, visibleClosedBodies)),
                ReportAuditChecks.check("Test packages", testPackages.toString(),
                        overviewPackages.toString(),
                        packageProjectionMatches(testPackages, overviewPackages)));
        return ValidationResult.of(assertions);
    }

    /**
     * Verifies the interaction contract without creating another E2E class:
     * both secondary disclosures start closed, each exposes its body when
     * opened, and the flow restores the default closed state afterwards.
     */
    public ValidationResult validateExpansion(Page overviewPage, PlaywrightActionSequence actions) {
        return validateExpansionWithEvidence(overviewPage, actions, () -> null).validation();
    }

    /**
     * Runs the expansion contract and captures the visual evidence while both
     * secondary disclosures are genuinely open.  The callback is deliberately
     * injected so the validator remains independent from the report lifecycle.
     * Cleanup clicks are executed directly after the capture and are therefore
     * not misrepresented as part of the user-facing action sequence.
     */
    public ExpansionAudit validateExpansionWithEvidence(
            Page overviewPage,
            PlaywrightActionSequence actions,
            Supplier<UiScreenValidation.Evidence> expandedEvidenceCapture) {
        Objects.requireNonNull(overviewPage, "overviewPage");
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(expandedEvidenceCapture, "expandedEvidenceCapture");
        Locator overview = locators.overviewView(overviewPage);
        int discovered = locators.overviewSecondaryZones(overview).count();
        int initialOpen = locators.overviewOpenSecondaryZones(overview).count();
        int initialVisibleClosedBodies = locators.overviewVisibleClosedSecondaryBodies(overview).count();
        int expanded = 0;
        int visibleBodies = 0;

        for (ReportUiContract.OverviewSection zone : SECONDARY_ZONES) {
            Locator disclosure = locators.overviewSecondaryZone(overview, zone);
            if (disclosure.count() == 0) continue;
            AtfLocator action = locators.overviewSecondaryZoneAction(overviewPage, zone);
            if (disclosure.getAttribute("open") == null) {
                actions.click(action, "expand " + zone.name().toLowerCase() + " metadata");
            }
            boolean isOpen = disclosure.getAttribute("open") != null;
            Locator body = locators.overviewSecondaryZoneBody(overview, zone);
            boolean hasVisibleBody = body.count() > 0 && body.isVisible();
            if (isOpen) expanded++;
            if (hasVisibleBody) visibleBodies++;
            actions.observation("ATF disclosure content", zone.name(),
                    "expanded disclosure exposes visible metadata", "open=" + isOpen
                            + ", bodyVisible=" + hasVisibleBody);
        }

        List<String> metricLabels = metricLabels(overview);
        List<String> metricValues = metricValues(overview);
        Map<String, Integer> tableHeaders = new LinkedHashMap<>();
        Map<String, Integer> tableRows = new LinkedHashMap<>();
        Map<String, Integer> populatedRows = new LinkedHashMap<>();
        Locator tagTables = locators.overviewTagAuditTables(overview);
        for (int index = 0; index < tagTables.count(); index++) {
            Locator table = tagTables.nth(index);
            String id = table.getAttribute("id");
            if (id == null || id.isBlank()) continue;
            tableHeaders.put(id, nonBlankTextCount(locators.tableHeaders(table)));
            Locator rows = locators.tableRows(table);
            tableRows.put(id, rows.count());
            int populated = 0;
            for (int rowIndex = 0; rowIndex < rows.count(); rowIndex++) {
                Locator cells = locators.tableCells(rows.nth(rowIndex));
                if (cells.count() > 0 && nonBlankTextCount(cells) == cells.count()) populated++;
            }
            populatedRows.put(id, populated);
        }
        actions.observation("ATF execution metadata", "execution metrics",
                "expanded metadata exposes all required labels and values",
                metricLabels + " / values=" + metricValues);
        actions.observation("ATF tag audit", "audit tables",
                "expanded audit exposes scenario, package and tag tables with populated rows",
                "headers=" + tableHeaders + ", rows=" + tableRows
                        + ", populatedRows=" + populatedRows);

        UiScreenValidation.Evidence expandedEvidence = expandedEvidenceCapture.get();

        for (ReportUiContract.OverviewSection zone : SECONDARY_ZONES) {
            Locator disclosure = locators.overviewSecondaryZone(overview, zone);
            if (disclosure.count() > 0 && disclosure.getAttribute("open") != null) {
                // Restore the default state without appending cleanup actions
                // after the screenshot. The final assertion below still proves
                // the browser was restored correctly.
                locators.overviewSecondaryZoneAction(overviewPage, zone).locator().click();
            }
        }
        int finalOpen = locators.overviewOpenSecondaryZones(overview).count();
        int finalVisibleClosedBodies = locators.overviewVisibleClosedSecondaryBodies(overview).count();

        List<AtfAssertion> assertions = List.of(
                ReportAuditChecks.check("ATF zones initial state", "2 collapsed disclosures",
                        discovered + " found / " + initialOpen + " open / " + initialVisibleClosedBodies
                                + " visible closed bodies",
                        collapsedSecondaryZones(discovered, initialOpen, initialVisibleClosedBodies)),
                ReportAuditChecks.check("Execution metadata labels", REQUIRED_EXECUTION_METRICS.toString(),
                        metricLabels.toString(), executionMetadataLabelsComplete(metricLabels)),
                ReportAuditChecks.check("Execution metadata values", "6 non-empty values",
                        metricValues.size() + " values / " + metricValues,
                        executionMetadataValuesComplete(metricValues)),
                ReportAuditChecks.check("ATF tag audit tables", REQUIRED_TAG_AUDIT_TABLES.toString(),
                        tableHeaders.keySet().toString(),
                        tagAuditTableIdsComplete(tableHeaders.keySet())),
                ReportAuditChecks.check("ATF tag audit table structure", "non-empty headers and rows for every table",
                        "headers=" + tableHeaders + ", rows=" + tableRows
                                + ", populatedRows=" + populatedRows,
                        tagAuditTableContentComplete(tableHeaders, tableRows, populatedRows)),
                ReportAuditChecks.check("ATF zones expansion", "2 expanded disclosures with visible content",
                        expanded + " expanded / " + visibleBodies + " visible bodies",
                        expandableSecondaryZones(discovered, expanded, visibleBodies)),
                ReportAuditChecks.check("ATF zones restored state", "0 open after validation",
                        finalOpen + " open / " + finalVisibleClosedBodies + " visible closed bodies",
                        restoredSecondaryZones(finalOpen, finalVisibleClosedBodies)));
        return new ExpansionAudit(ValidationResult.of(assertions), expandedEvidence);
    }

    /** Result plus the screenshot captured while the disclosures were open. */
    public record ExpansionAudit(ValidationResult validation,
                                 UiScreenValidation.Evidence expandedEvidence) {
        public ExpansionAudit {
            Objects.requireNonNull(validation, "validation");
        }

        public List<AtfAssertion> assertions() {
            return validation.assertions();
        }

        public boolean valid() {
            return validation.valid();
        }

        public String failureSummary() {
            return validation.failureSummary();
        }
    }

    public static boolean collapsedSecondaryZones(int discovered, int open) {
        return discovered == REQUIRED_SECONDARY_ZONE_COUNT && open == 0;
    }

    public static boolean collapsedSecondaryZones(int discovered, int open, int visibleClosedBodies) {
        return collapsedSecondaryZones(discovered, open) && visibleClosedBodies == 0;
    }

    public static boolean expandableSecondaryZones(int discovered, int expanded, int visibleBodies) {
        return discovered == REQUIRED_SECONDARY_ZONE_COUNT
                && expanded == REQUIRED_SECONDARY_ZONE_COUNT
                && visibleBodies == REQUIRED_SECONDARY_ZONE_COUNT;
    }

    public static boolean restoredSecondaryZones(int open) {
        return open == 0;
    }

    public static boolean restoredSecondaryZones(int open, int visibleClosedBodies) {
        return restoredSecondaryZones(open) && visibleClosedBodies == 0;
    }

    public static boolean executionMetadataLabelsComplete(Collection<String> labels) {
        return normalizedValues(labels).equals(normalizedValues(REQUIRED_EXECUTION_METRICS));
    }

    public static boolean executionMetadataValuesComplete(Collection<String> values) {
        return values != null && values.size() == REQUIRED_EXECUTION_METRICS.size()
                && values.stream().allMatch(value -> value != null && !value.isBlank());
    }

    public static boolean tagAuditTableIdsComplete(Collection<String> tableIds) {
        return normalizedValues(tableIds).equals(normalizedValues(REQUIRED_TAG_AUDIT_TABLES));
    }

    public static boolean tagAuditTableContentComplete(Map<String, Integer> headers,
                                                       Map<String, Integer> rows,
                                                       Map<String, Integer> populatedRows) {
        if (!tagAuditTableIdsComplete(headers == null ? Set.of() : headers.keySet())
                || !tagAuditTableIdsComplete(rows == null ? Set.of() : rows.keySet())
                || !tagAuditTableIdsComplete(populatedRows == null ? Set.of() : populatedRows.keySet())) {
            return false;
        }
        return headers.values().stream().allMatch(value -> value != null && value > 0)
                && rows.values().stream().allMatch(value -> value != null && value > 0)
                && populatedRows.values().stream().allMatch(value -> value != null && value > 0);
    }

    public static boolean packageProjectionMatches(Collection<String> tests,
                                                   Collection<String> overview) {
        return normalizedValues(tests).equals(normalizedValues(overview));
    }

    private Set<String> packageAuditNames(Locator overview) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        Locator rows = locators.overviewPackageRows(overview);
        for (int index = 0; index < rows.count(); index++) {
            Locator cells = locators.tableCells(rows.nth(index));
            if (cells.count() > 0) addValue(names, cells.nth(0).textContent());
        }
        return names;
    }

    private List<String> metricLabels(Locator overview) {
        List<String> labels = new ArrayList<>();
        Locator metrics = locators.overviewMetrics(overview);
        for (int index = 0; index < metrics.count(); index++) {
            addValue(labels, locators.metricLabel(metrics.nth(index)).textContent());
        }
        return labels;
    }

    private List<String> metricValues(Locator overview) {
        List<String> values = new ArrayList<>();
        Locator metrics = locators.overviewMetrics(overview);
        for (int index = 0; index < metrics.count(); index++) {
            addValue(values, locators.metricValue(metrics.nth(index)).textContent());
        }
        return values;
    }

    private static int nonBlankTextCount(Locator nodes) {
        int count = 0;
        for (int index = 0; index < nodes.count(); index++) {
            String text = nodes.nth(index).textContent();
            if (text != null && !text.isBlank()) count++;
        }
        return count;
    }

    private static Set<String> textValues(Locator nodes) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (int index = 0; index < nodes.count(); index++) {
            addValue(values, nodes.nth(index).textContent());
        }
        return values;
    }

    private static void addValue(Collection<String> values, String raw) {
        if (raw != null && !raw.isBlank()) values.add(raw.trim());
    }

    private static Set<String> normalizedValues(Collection<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values != null) {
            values.stream().filter(Objects::nonNull).map(String::trim)
                    .filter(value -> !value.isBlank()).forEach(normalized::add);
        }
        return normalized;
    }
}
