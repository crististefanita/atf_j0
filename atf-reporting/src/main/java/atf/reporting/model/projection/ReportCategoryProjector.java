package atf.reporting.model.projection;

import atf.reporting.model.ReportExecutionModel;

import java.util.List;
import java.util.Objects;

/** Compatibility facade for the aggregate category projection. */
public final class ReportCategoryProjector {
    private ReportCategoryProjector() {
    }

    public static ReportExecutionModel.Categories project(
            List<ReportExecutionModel.Scenario> scenarios) {
        return ReportExecutionProjections.project(
                Objects.requireNonNull(scenarios, "scenarios")).categories();
    }
}
