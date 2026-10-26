package atf.reporting.model.projection;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.ReportExecutionModel;
import atf.reporting.model.diagnostic.ReportPhaseDiagnostic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Single-pass collector for the renderer-neutral secondary projections.
 *
 * <p>The individual projectors remain public compatibility seams. The
 * aggregate boundary uses this collector so Overview, Categories and Fails
 * are derived from one traversal of one immutable scenario population.</p>
 */
final class ReportExecutionProjectionAccumulator {
    private final Set<String> tags = new LinkedHashSet<>();
    private final List<ReportExecutionModel.Failure> failures = new ArrayList<>();
    private long passed;
    private long failed;
    private long skipped;

    private ReportExecutionProjectionAccumulator() {
    }

    static ReportExecutionProjections collect(
            List<ReportExecutionModel.Scenario> scenarios) {
        Objects.requireNonNull(scenarios, "scenarios");
        var accumulator = new ReportExecutionProjectionAccumulator();
        scenarios.forEach(accumulator::acceptScenario);
        return accumulator.finish(scenarios.size());
    }

    private void acceptScenario(ReportExecutionModel.Scenario scenario) {
        Objects.requireNonNull(scenario, "scenario");
        tags.addAll(scenario.tags());
        switch (scenario.status()) {
            case PASS -> passed++;
            case FAIL -> failed++;
            case SKIP -> skipped++;
            default -> {
                // INFO and WARNING remain visible in Tests, but are not
                // counted as passed/failed/skipped overview verdicts.
            }
        }
        scenario.phases().forEach(phase -> {
            if (phase.status() == AtfReportStatus.FAIL) {
                ReportPhaseDiagnostic.from(phase.name(), phase.details()).stream()
                        .map(diagnostic -> new ReportExecutionModel.Failure(scenario.id(),
                                diagnostic.kind().label(), diagnostic.message()))
                        .forEach(failures::add);
            }
            collectFailures(scenario, phase.children());
        });
    }

    private void collectFailures(ReportExecutionModel.Scenario scenario,
                                  List<ReportExecutionModel.Node> nodes) {
        for (var node : nodes) {
            if (node instanceof ReportExecutionModel.Request request) {
                collectFailures(scenario, request.children());
                continue;
            }
            var validation = (ReportExecutionModel.Validation) node;
            validation.assertions().stream()
                    .filter(assertion -> assertion.resultStatus() == AtfReportStatus.FAIL
                            || (!assertion.passed()
                            && validation.status() == AtfReportStatus.FAIL))
                    .map(assertion -> new ReportExecutionModel.Failure(
                            scenario.id(), assertion.name(), assertion.actual()))
                    .forEach(failures::add);
            collectFailures(scenario, validation.children());
        }
    }

    private ReportExecutionProjections finish(int scenarioCount) {
        var sortedTags = tags.stream().sorted(Comparator.naturalOrder()).toList();
        return new ReportExecutionProjections(
                new ReportExecutionModel.Overview(scenarioCount, passed, failed, skipped),
                new ReportExecutionModel.Categories(sortedTags),
                new ReportExecutionModel.Failures(failures));
    }
}
