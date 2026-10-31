package atf.reporting.evidence;

import atf.reporting.markup.AtfReportEvidenceKeys;
import atf.reporting.model.ReportField;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class UnifiedFlowProvenanceFieldsTest {
    @Test
    void phaseContextIsSeparatedWithoutSwallowingRealHttpAndCustomFields() {
        var context = List.of(field("evidence.type", "unified-ui-flow"), field("flow.mode", "unified-flow"),
                field("flow.scenario", "purchase"), field("flow.phase", "TEST BODY"));
        var http = List.of(field("request.method", "GET"), field("status", "200"), field("custom.diagnostic", "retained"));
        var fields = java.util.stream.Stream.concat(context.stream(), http.stream()).toList();
        assertEquals(context, EvidenceTableFormatter.uiFlowProvenanceFields(fields));
        assertEquals(List.of(http.get(1), http.get(2)), EvidenceTableFormatter.technicalTraceFields(fields));
        assertEquals(List.of(http.getFirst()), EvidenceTableFormatter.requestFields(fields));
        assertFalse(EvidenceTableFormatter.isCompactUiFlow(fields));
    }

    @Test
    void matchingFieldNamesCannotReclassifyAnOrdinaryHttpRequest() {
        var fields = List.of(field("flow.phase", "TEST BODY"), field("status", "500"));
        assertEquals(List.of(), EvidenceTableFormatter.uiFlowProvenanceFields(fields));
        assertEquals(fields, EvidenceTableFormatter.technicalTraceFields(fields));
    }

    @Test
    void aConflictingEvidenceOwnerCannotSwallowAnApiTrace() {
        var fields = List.of(field("evidence.type", "api-trace"), field("evidence.type", "unified-ui-flow"),
                field("flow.presentation", "compact-ui-only"));
        assertTrue(EvidenceTableFormatter.uiFlowProvenanceFields(fields).isEmpty());
        assertEquals(fields, EvidenceTableFormatter.technicalTraceFields(fields));
        assertFalse(EvidenceTableFormatter.isCompactUiFlow(fields));
    }

    @Test
    void compactFlowKeepsItsHistoricalProvenanceAndPresentationRemainsInternal() {
        var fields = List.of(field("evidence.type", "unified-ui-flow"),
                field("flow.presentation", "compact-ui-only"), field("flow.checkpoint.1.phase", "TEST BODY"));
        assertTrue(EvidenceTableFormatter.isCompactUiFlow(fields));
        assertEquals(List.of(fields.getFirst(), fields.getLast()), EvidenceTableFormatter.uiFlowProvenanceFields(fields));
        assertEquals(List.of(), EvidenceTableFormatter.technicalTraceFields(fields));
    }

    @Test
    void normalizedFieldNamesPreserveTheOriginalFieldObjectsAndOrder() {
        var fields = List.of(field(AtfReportEvidenceKeys.EVIDENCE_TYPE.toUpperCase(), "UNIFIED-UI-FLOW"),
                field("FLOW.MODE", "unified-flow"), field("FLOW.PHASE", "TEST BODY"));
        assertEquals(fields, EvidenceTableFormatter.uiFlowProvenanceFields(fields));
        assertSame(fields.getFirst(), EvidenceTableFormatter.uiFlowProvenanceFields(fields).getFirst());
        assertTrue(EvidenceTableFormatter.technicalTraceFields(fields).isEmpty());
    }

    private static ReportField field(String name, String value) { return new ReportField(name, value); }
}
