package atf.reporting.model.projection;

import atf.reporting.model.ReportExecutionModel;

import java.util.List;
import java.util.Objects;

/** Compatibility facade for the aggregate overview projection. */
public final class ReportOverviewProjector {
    private ReportOverviewProjector() {
    }

    public static ReportExecutionModel.Overview project(
            List<ReportExecutionModel.Scenario> scenarios) {
        return ReportExecutionProjections.project(
                Objects.requireNonNull(scenarios, "scenarios")).overview();
    }
}
