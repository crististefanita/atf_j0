package atf.reporting.support.failurecorrelation;

import atf.reporting.contract.*;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.support.TestReportPaths;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Repeated labels and identical bounded values prevent text-based routing from passing this fixture. */
public final class FailureNavigationFixture {
    private FailureNavigationFixture() { }
    public enum Owner { UI, API, COMPONENT }
    public static final List<String> PHASES = List.of("SETUP", "TEST BODY", "TEARDOWN");

    public static Path publish(Owner owner) {
        Path output = TestReportPaths.report("failure-navigation-" + owner.name().toLowerCase(java.util.Locale.ROOT)
                + "-" + UUID.randomUUID() + ".html");
        try (var session = new ExtentReportSession(output)) {
            var sibling = session.startScenario("FailureNavigation", "atf.reporting.component", "PASS neighbour", List.of("component"));
            sibling.startPhase("TEST BODY").complete(AtfReportStatus.PASS);
            sibling.complete(AtfReportStatus.PASS);
            var scenario = session.startScenario("FailureNavigation", "atf.reporting.component", owner + " failures", List.of("component"));
            for (String name : PHASES) {
                var phase = scenario.startPhase(name);
                for (int checkpoint = 1; checkpoint <= 2; checkpoint++) {
                    var assertions = List.of(new AtfAssertion("Passing comparison", "ready", "ready", true),
                            new AtfAssertion("Repeated comparison", "expected", "x".repeat(260) + name + checkpoint, false));
                    if (owner == Owner.COMPONENT) {
                        var step = phase.startAssertionStep("Checkpoint " + checkpoint);
                        step.assertions(assertions); step.complete(AtfReportStatus.FAIL); step.close();
                    } else {
                        var request = phase.startRequest("Checkpoint " + checkpoint, owner == Owner.API ? "GET" : "",
                                owner == Owner.API ? "/observed/" + checkpoint : "");
                        var validation = request.startValidation(owner + " validation");
                        validation.evidenceOwner(owner == Owner.UI ? ReportEvidenceOwner.UI : ReportEvidenceOwner.API);
                        validation.assertions(assertions); validation.complete(AtfReportStatus.FAIL); validation.close();
                        request.complete(AtfReportStatus.FAIL); request.close();
                    }
                }
                phase.complete(AtfReportStatus.FAIL); phase.close();
            }
            scenario.complete(AtfReportStatus.FAIL); scenario.close();
        }
        return output;
    }
}
