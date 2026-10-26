package atf.ui.tests.extent.component.audit;

import atf.testkit.AtfComponentTest;
import atf.ui.tests.extent.support.audit.screen.OverviewSecondaryContractValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class OverviewSecondaryContractValidatorTest {
    @Test
    void requiresExactlyTwoSecondaryZonesAndKeepsThemCollapsed() {
        assertTrue(OverviewSecondaryContractValidator.collapsedSecondaryZones(2, 0));
        assertFalse(OverviewSecondaryContractValidator.collapsedSecondaryZones(2, 1));
        assertFalse(OverviewSecondaryContractValidator.collapsedSecondaryZones(1, 0));
        assertTrue(OverviewSecondaryContractValidator.collapsedSecondaryZones(2, 0, 0));
        assertFalse(OverviewSecondaryContractValidator.collapsedSecondaryZones(2, 0, 1));
    }

    @Test
    void requiresBothSecondaryZonesToExpandWithVisibleContentAndRestoreClosedState() {
        assertTrue(OverviewSecondaryContractValidator.expandableSecondaryZones(2, 2, 2));
        assertFalse(OverviewSecondaryContractValidator.expandableSecondaryZones(2, 2, 1));
        assertFalse(OverviewSecondaryContractValidator.expandableSecondaryZones(2, 1, 2));
        assertTrue(OverviewSecondaryContractValidator.restoredSecondaryZones(0));
        assertFalse(OverviewSecondaryContractValidator.restoredSecondaryZones(1));
        assertTrue(OverviewSecondaryContractValidator.restoredSecondaryZones(0, 0));
        assertFalse(OverviewSecondaryContractValidator.restoredSecondaryZones(0, 1));
    }

    @Test
    void reconcilesOverviewPackagesWithTheTestsTabProjection() {
        assertTrue(OverviewSecondaryContractValidator.packageProjectionMatches(
                List.of("atf.ui.tests.extent.section"),
                List.of("atf.ui.tests.extent.section")));
        assertFalse(OverviewSecondaryContractValidator.packageProjectionMatches(
                List.of("atf.ui.tests.extent.section"), List.of("Unspecified")));
    }

    @Test
    void requiresEveryExecutionMetricLabelAndValue() {
        assertTrue(OverviewSecondaryContractValidator.executionMetadataLabelsComplete(List.of(
                "Test classes in this run", "Scenarios in this run", "Scenario pass",
                "Scenario fail", "Scenario skipped", "Scenario warnings")));
        assertFalse(OverviewSecondaryContractValidator.executionMetadataLabelsComplete(List.of(
                "Test classes in this run", "Scenarios in this run")));
        assertTrue(OverviewSecondaryContractValidator.executionMetadataValuesComplete(
                List.of("1", "7", "7", "0", "0", "0")));
        assertFalse(OverviewSecondaryContractValidator.executionMetadataValuesComplete(
                List.of("1", "7", "", "0", "0", "0")));
    }

    @Test
    void requiresAllTagAuditTablesToHaveHeadersAndPopulatedRows() {
        Map<String, Integer> headers = Map.of(
                "atf-audit-scenarios", 3, "atf-audit-packages", 3, "atf-audit-tags", 2);
        Map<String, Integer> rows = Map.of(
                "atf-audit-scenarios", 7, "atf-audit-packages", 1, "atf-audit-tags", 9);
        assertTrue(OverviewSecondaryContractValidator.tagAuditTableContentComplete(
                headers, rows, rows));
        assertFalse(OverviewSecondaryContractValidator.tagAuditTableContentComplete(
                headers, rows, Map.of("atf-audit-scenarios", 7,
                        "atf-audit-packages", 0, "atf-audit-tags", 9)));
        assertFalse(OverviewSecondaryContractValidator.tagAuditTableIdsComplete(
                List.of("atf-audit-scenarios", "atf-audit-packages")));
    }
}
