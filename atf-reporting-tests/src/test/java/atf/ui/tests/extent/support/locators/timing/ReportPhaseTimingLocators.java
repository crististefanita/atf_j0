package atf.ui.tests.extent.support.locators.timing;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.Attribute.*;

/** Timing-specific DOM boundary; the tests retain their independent numeric expectations. */
public final class ReportPhaseTimingLocators {
    public Locator metadata(Locator phase) { return phase.locator("[" + EXECUTION_DURATION.value() + "]"); }
    public Locator badge(Locator phase, boolean fallback) {
        return phase.locator(fallback ? ":scope > h3 [data-duration-ms]" : ":scope > [class~='card-header'] [data-duration-ms]");
    }
    public Locator stepBadge(Locator step) { return step.locator(":scope > [class~='card-header'] [data-duration-ms]"); }
    public Locator classes(Page page) {
        return page.locator("[id='test-view'] [class~='test-list-item'] > [class~='test-item'][test-id]");
    }
    public Locator fallbackOverviewDuration(Page page) {
        return page.locator("[id='overview'] [data-duration-ms]");
    }
    public Locator overviewTab(Page page) { return page.locator("button[data-tab='overview']"); }
    public Locator timeline(Page page) { return page.locator("[id='timeline']"); }
}
