package atf.reporting.model.timing;

import atf.reporting.model.ReportExecutionModel;
import java.util.List;
import java.util.OptionalLong;

/** Execution aggregates sum direct phases; incomplete timing is unknown rather than a partial total. */
public final class ReportPhaseDurations {
    private ReportPhaseDurations() { }
    public static OptionalLong scenario(ReportExecutionModel.Scenario scenario) {
        if (scenario.phases().isEmpty() || scenario.phases().stream().anyMatch(p -> p.timing().isEmpty()))
            return OptionalLong.empty();
        long total = 0;
        for (var phase : scenario.phases()) total = Math.addExact(total, phase.timing().orElseThrow().durationMillis());
        return OptionalLong.of(total);
    }
    public static OptionalLong total(List<ReportExecutionModel.Scenario> scenarios) {
        if (scenarios.isEmpty()) return OptionalLong.empty();
        long total = 0;
        for (var scenario : scenarios) {
            var value = scenario(scenario);
            if (value.isEmpty()) return OptionalLong.empty();
            total = Math.addExact(total, value.getAsLong());
        }
        return OptionalLong.of(total);
    }
}
