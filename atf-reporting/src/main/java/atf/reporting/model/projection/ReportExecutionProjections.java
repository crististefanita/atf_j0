package atf.reporting.model.projection;

import atf.reporting.model.ReportExecutionModel;

import java.util.List;
import java.util.Objects;

/**
 * Immutable boundary for the renderer-neutral report projections.
 *
 * <p>The three views are derived from the same scenario population in one
 * collector pass. Keeping them together makes that relationship explicit and
 * gives composition roots one typed value to pass to renderers and validators.</p>
 */
public record ReportExecutionProjections(
        ReportExecutionModel.Overview overview,
        ReportExecutionModel.Categories categories,
        ReportExecutionModel.Failures failures) {

    public ReportExecutionProjections {
        Objects.requireNonNull(overview, "overview");
        Objects.requireNonNull(categories, "categories");
        Objects.requireNonNull(failures, "failures");
    }

    public static ReportExecutionProjections project(
            List<ReportExecutionModel.Scenario> scenarios) {
        return ReportExecutionProjectionAccumulator.collect(scenarios);
    }
}
