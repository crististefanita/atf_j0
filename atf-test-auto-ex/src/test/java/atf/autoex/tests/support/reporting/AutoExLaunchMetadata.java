package atf.autoex.tests.support.reporting;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.ReportField;
import atf.reporting.model.ReportObservation;
import atf.reporting.ui.UiExtentReportSession;
import org.junit.jupiter.api.extension.ExtensionContext;
import atf.testkit.launch.TestLaunchContext;
import atf.testkit.launch.TestLaunchSnapshot;
import java.util.ArrayList;
import org.junit.platform.engine.UniqueId;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Consumer projection of immutable JUnit source and discovered-plan facts. */
public record AutoExLaunchMetadata(Optional<String> rootSuite, String testClass, String testMethod,
                                  Optional<TestLaunchSnapshot> plan) {
    public AutoExLaunchMetadata {
        java.util.Objects.requireNonNull(rootSuite, "rootSuite");
        java.util.Objects.requireNonNull(plan, "plan");
        java.util.Objects.requireNonNull(testClass, "testClass");
        java.util.Objects.requireNonNull(testMethod, "testMethod");
    }

    public static AutoExLaunchMetadata capture(ExtensionContext context) {
        var suite = UniqueId.parse(context.getUniqueId()).getSegments().stream()
                .filter(segment -> segment.getType().equals("suite"))
                .map(UniqueId.Segment::getValue).findFirst();
        String method = context.getTestMethod().map(value -> value.getName() + "("
                + Arrays.stream(value.getParameterTypes()).map(Class::getTypeName)
                        .collect(Collectors.joining(",")) + ")").orElse("unknown (no test method)");
        return new AutoExLaunchMetadata(suite, context.getRequiredTestClass().getName(), method, TestLaunchContext.capture(context));
    }

    public ReportObservation observation() {
        var fields = new ArrayList<ReportField>(List.of(
                new ReportField("Execution ancestry", rootSuite.isPresent()
                        ? "suite descendant" : "no suite ancestor exposed"),
                new ReportField("Root suite", rootSuite.orElse("unknown (no suite ancestor exposed)")),
                new ReportField("JUnit class", testClass),
                new ReportField("JUnit method", testMethod)));
        if (plan.isPresent()) {
            var discovery = plan.orElseThrow();
            fields.add(new ReportField("Launcher selection", discovery.selectionSource()));
            fields.add(new ReportField("Effective selection count", Long.toString(discovery.discoveredTestCount())));
            fields.add(new ReportField("Count definition", "tests discovered at plan start; includes disabled tests; excludes later dynamic registrations"));
            fields.add(new ReportField("Dynamic registrations at capture", Long.toString(discovery.registeredDynamicTestCount())));
            for (int index = 0; index < discovery.selectors().size(); index++) {
                fields.add(new ReportField("Selector #" + (index + 1), discovery.selectors().get(index)));
            }
            for (int index = 0; index < discovery.discoveredTests().size(); index++) {
                fields.add(new ReportField("Selected test #" + (index + 1), discovery.discoveredTests().get(index).source()));
            }
        } else {
            fields.add(new ReportField("Launcher selection", "unavailable (launcher session listener disabled)"));
            fields.add(new ReportField("Effective selection count", "unavailable (no active TestPlan)"));
        }
        return new ReportObservation("JUnit launch identity", fields, List.of(), List.of());
    }

    public void publish(UiExtentReportSession.UiScenario scenario) {
        try (var phase = scenario.startPhase("Launch context")) {
            phase.observation(observation());
            phase.complete(AtfReportStatus.INFO);
        }
    }
}
