package atf.reporting.html;

import atf.reporting.model.ReportExecutionModel;
import atf.testkit.AtfUnitTest;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.support.ApiReportTestUrls;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@AtfUnitTest
class HtmlReportSemanticSnapshotMapperTest {

    @Test
    void mapsLifecycleModelsToAnImmutableRendererTreeWithoutRendererKnowledge() {
        var document = new HtmlReportSemanticDocument();
        var scenario = document.startScenario("UsersTests", "read user", List.of("api"));
        var phase = scenario.addPhase("TEST BODY");
        var request = phase.addRequest("GET user", "GET", ApiReportTestUrls.GENERIC_USER_ONE);
        request.addDetail("response.status", "200");
        var validation = request.addValidation("Validate response");
        validation.complete(AtfReportStatus.PASS);
        request.complete(AtfReportStatus.PASS);
        phase.complete(AtfReportStatus.PASS);
        scenario.complete(AtfReportStatus.PASS);

        var snapshot = HtmlReportSemanticSnapshotMapper.map(document.scenarios());
        var mappedRequest = assertInstanceOf(
                ReportExecutionModel.Request.class,
                snapshot.scenarios().get(0).phases().get(0).children().get(0));

        assertAll(
                () -> assertEquals("UsersTests", snapshot.scenarios().get(0).testClass()),
                () -> assertEquals("response.status", mappedRequest.details().get(0).name()),
                () -> assertEquals("Validate response",
                        ((ReportExecutionModel.Validation)
                                mappedRequest.children().get(0)).title()),
                () -> assertEquals(AtfReportStatus.PASS, snapshot.scenarios().get(0).status())
        );
    }
}
