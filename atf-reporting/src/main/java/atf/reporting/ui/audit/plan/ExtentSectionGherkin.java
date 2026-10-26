package atf.reporting.ui.audit.plan;

import java.util.Objects;

/**
 * Canonical, self-explanatory report-section overviews for section audits.
 *
 * <p>The same overview is used by the JUnit dynamic node and the Extent
 * scenario. It is intentionally a normal, concise sentence; Gherkin keywords
 * belong to the validation steps below it, while section-specific evidence and
 * assertions carry the detailed proof.
 */
public final class ExtentSectionGherkin {
    private ExtentSectionGherkin() { }

    /**
     * Returns one concise, self-explanatory overview sentence. The
     * section-specific assertions and evidence rows provide the detailed
     * validation steps below the overview.
     */
    public static String proposition(ExtentSection section) {
        Objects.requireNonNull(section, "section");
        return switch (section) {
            case OVERVIEW -> "Overview shows reconciled totals, package projections and collapsed ATF metadata";
            case TESTS -> "Tests show owners, scenarios, phases and request evidence";
            case CATEGORIES -> "Categories show counts and working filters";
            case FAILS -> "Fails show failure rows, causes and actionable links";
            case EVIDENCE -> "Request evidence includes request JSON, response JSON and execution evidence";
            case UI_VALIDATION -> "UI validation covers actions, observations, assertions and screenshot evidence";
        };
    }

    /** Concise overview for the interactive secondary-Overview contract. */
    public static String overviewSecondaryExpansionProposition() {
        return "ATF metadata sections expand from their collapsed default with visible content";
    }

    /** Returns the outcome only; the publisher adds exactly one THEN keyword. */
    public static String thenStatement(ExtentSection section) {
        Objects.requireNonNull(section, "section");
        return switch (section) {
            case OVERVIEW -> "the Overview section shows reconciled totals, package projections and collapsed ATF metadata";
            case TESTS -> "the Tests section shows owners, scenarios, phases and request evidence";
            case CATEGORIES -> "the Categories section shows counts and working filters";
            case FAILS -> "the Fails section shows failure rows, causes and actionable links";
            case EVIDENCE -> "request evidence shows request JSON, response JSON and execution evidence";
            case UI_VALIDATION -> "UI validation shows Playwright actions, audit observations, assertions and screenshot evidence";
        };
    }

    /** Returns the complete nested step for callers that do not use a publisher. */
    public static String thenStep(ExtentSection section) {
        return "THEN " + thenStatement(section);
    }

}
