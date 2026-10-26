package atf.reporting.html;
import atf.testkit.AtfUnitTest;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.support.ApiReportTestUrls;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class HtmlReportSemanticDocumentTest {
    @Test
    void ownsDeterministicScenarioIdentityAndInsertionOrder() {
        var document = new HtmlReportSemanticDocument();

        var first = document.startScenario("UsersTests", "first", List.of(" API ", "api", "CONTRACT"));
        var second = document.startScenario("PostsTests", "second", List.of("posts"));

        assertAll(
                () -> assertEquals("test-id=1", first.id),
                () -> assertEquals("test-id=2", second.id),
                () -> assertEquals(List.of("api", "contract"), first.tags),
                () -> assertEquals(List.of(first, second), document.scenarios()));
    }

    @Test
    void exposesSnapshotWithoutAllowingCollectionMutation() {
        var document = new HtmlReportSemanticDocument();
        document.startScenario("Tests", "scenario", List.of());

        var scenarios = document.scenarios();

        assertThrows(UnsupportedOperationException.class,
                scenarios::clear);
        assertEquals(1, document.scenarios().size(), "the attempted external mutation must not alter ownership");
    }

    @Test
    void keepsPhaseAndNodeOrderAsSemanticSiblingContract() {
        var document = new HtmlReportSemanticDocument();
        var scenario = document.startScenario("Tests", "request then validation", List.of());
        var phase = new HtmlReportSemanticDocument.PhaseModel("TEST BODY");
        var request = new HtmlReportSemanticDocument.RequestModel("item", "GET", ApiReportTestUrls.GENERIC_USER_ONE);
        var validation = new HtmlReportSemanticDocument.ValidationModel("Validate item");

        phase.children.add(request);
        phase.children.add(validation);
        scenario.phases.add(phase);
        phase.status = AtfReportStatus.PASS;

        assertAll(
                () -> assertSame(request, phase.children.get(0)),
                () -> assertSame(validation, phase.children.get(1)),
                () -> assertEquals("TEST BODY", scenario.phases.get(0).name));
    }
}
