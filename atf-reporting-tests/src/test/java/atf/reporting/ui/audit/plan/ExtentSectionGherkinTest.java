package atf.reporting.ui.audit.plan;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ExtentSectionGherkinTest {
    @Test
    void everySectionHasOneCompleteSelfExplanatoryProposition() {
        for (ExtentSection section : ExtentSection.values()) {
            String proposition = ExtentSectionGherkin.proposition(section);
            assertFalse(proposition.contains("GIVEN"), proposition);
            assertFalse(proposition.contains("WHEN"), proposition);
            assertFalse(proposition.contains("THEN"), proposition);
            assertFalse(proposition.endsWith(section.displayName()), proposition);
            assertTrue(proposition.length() <= 110, proposition);
        }
    }

    @Test
    void testsPropositionNamesTheRenderedTestsEvidence() {
        String proposition = ExtentSectionGherkin.proposition(ExtentSection.TESTS);
        assertTrue(proposition.equals("Tests show owners, scenarios, phases and request evidence"));
    }

    @Test
    void secondaryOverviewPropositionDescribesInteractionWithoutGherkinNoise() {
        String proposition = ExtentSectionGherkin.overviewSecondaryExpansionProposition();
        assertTrue(proposition.equals(
                "ATF metadata sections expand from their collapsed default with visible content"));
        assertFalse(proposition.contains("GIVEN"));
        assertFalse(proposition.contains("WHEN"));
        assertFalse(proposition.contains("THEN"));
    }

    @Test
    void testBodyUsesOnlyThenOutcomeWhileOverviewStaysFreeOfGherkinKeywords() {
        for (ExtentSection section : ExtentSection.values()) {
            String proposition = ExtentSectionGherkin.proposition(section);
            String statement = ExtentSectionGherkin.thenStatement(section);
            assertTrue(statement.startsWith("the ") || statement.startsWith("request evidence")
                    || statement.startsWith("UI validation"), statement);
            assertFalse(statement.startsWith("GIVEN "), statement);
            assertFalse(statement.contains(" WHEN "), statement);
            assertFalse(statement.contains(" THEN "), statement);
            assertTrue(ExtentSectionGherkin.thenStep(section).startsWith("THEN "), statement);
            assertFalse(proposition.contains("GIVEN"), proposition);
            assertFalse(proposition.contains("WHEN"), proposition);
            assertFalse(proposition.contains("THEN"), proposition);
        }
    }
}
