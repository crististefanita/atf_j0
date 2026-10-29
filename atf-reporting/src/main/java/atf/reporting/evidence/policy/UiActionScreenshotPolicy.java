package atf.reporting.evidence.policy;

import atf.reporting.model.ReportScreenshot;
import atf.reporting.ui.model.AtfUiActionRecord;
import atf.reporting.ui.model.AtfUiEvidenceSource;
import java.util.List;

/** A request action sequence has one inline capture; observation-only requests may expose a gallery. */
public final class UiActionScreenshotPolicy {
    private UiActionScreenshotPolicy() { }
    public static void validate(List<AtfUiActionRecord> actions, List<ReportScreenshot> screenshots) {
        if (actions.stream().noneMatch(action -> action.source() == AtfUiEvidenceSource.ACTION)) return;
        long count = screenshots.stream().map(ReportScreenshot::base64Png).distinct().count();
        if (count > 1) throw new IllegalStateException("A UI action sequence supports at most one Base64 screenshot; found " + count);
    }
}
