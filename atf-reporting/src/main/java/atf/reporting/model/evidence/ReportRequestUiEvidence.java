package atf.reporting.model.evidence;

import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.model.ReportScreenshot;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import java.util.List;
import java.util.Objects;

/** Actual UI evidence owned by a request, independently of transport or a nested validation. */
public record ReportRequestUiEvidence(List<AtfUiActionRecord> actions, List<ReportScreenshot> screenshots,
                                      ReportUiActionDetailsMode detailsMode) {
    public ReportRequestUiEvidence {
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        screenshots = List.copyOf(Objects.requireNonNull(screenshots, "screenshots"));
        Objects.requireNonNull(detailsMode, "detailsMode");
    }
    public static ReportRequestUiEvidence empty() {
        return new ReportRequestUiEvidence(List.of(), List.of(), ReportUiActionDetailsMode.INCLUDED_COLLAPSED);
    }
}
