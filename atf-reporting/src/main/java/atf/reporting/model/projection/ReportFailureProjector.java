package atf.reporting.model.projection;

import atf.reporting.model.ReportExecutionModel;

import java.util.List;
import java.util.Objects;

/** Compatibility facade for the aggregate failure projection. */
public final class ReportFailureProjector {
    private ReportFailureProjector() {
    }

    public static ReportExecutionModel.Failures project(
            List<ReportExecutionModel.Scenario> scenarios) {
        return ReportExecutionProjections.project(
                Objects.requireNonNull(scenarios, "scenarios")).failures();
    }
}
