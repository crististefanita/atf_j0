package atf.ui.tests.extent.support.audit;

import atf.reporting.api.ReportModelAdapters;
import atf.reporting.ui.model.AtfUiActionRecord;
import java.util.Collection;
import java.util.List;

/** Test-composition adapter: Playwright/core action records become renderer evidence records. */
public final class ReportUiActionAdapters {
    private ReportUiActionAdapters() { }

    public static List<AtfUiActionRecord> from(Collection<? extends atf.core.ui.AtfUiActionRecord> actions) {
        return ReportModelAdapters.uiActions(actions);
    }
}
