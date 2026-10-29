package atf.ui.tests.extent.component.layout;

import atf.reporting.asset.ReportAssetComposer;
import atf.reporting.runtime.evidence.ExtentReportRuntimeEvidenceTableLayout;
import atf.reporting.theme.evidence.AtfReportEvidenceTableCss;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Asset boundary only; measured column allocation is proved by EvidenceColumnSizingComponentTest. */
@AtfComponentTest
class ValidationTableLayoutComponentTest {
    @Test
    void bothRenderersComposeTheSameContentAwareAllocatorAndIncrementalLifecycle() {
        String script = ExtentReportRuntimeEvidenceTableLayout.script();
        assertAll(
                () -> assertTrue(ReportAssetComposer.fallbackNavigationScript().endsWith(script)),
                () -> assertTrue(script.contains("measureText")),
                () -> assertTrue(script.contains("data-atf-content-column-max-px")),
                () -> assertTrue(script.contains("data-atf-content-column-widths")),
                () -> assertTrue(script.contains("data-atf-validation-column-widths")),
                () -> assertTrue(script.contains("data-atf-key-column-width")),
                () -> assertTrue(script.contains("cache.fingerprint")),
                () -> assertTrue(script.contains("ResizeObserver")),
                () -> assertTrue(script.contains("MutationObserver")),
                () -> assertFalse(script.contains("new MutationObserver(schedule)")),
                () -> assertFalse(script.contains("return q(")));
    }

    @Test
    void cssLeavesColumnAllocationToTheSharedRuntime() {
        String css = AtfReportEvidenceTableCss.css();
        assertAll(
                () -> assertTrue(css.contains("table-layout:fixed!important")),
                () -> assertTrue(css.contains("overflow-wrap:anywhere!important;word-break:break-word!important")),
                () -> assertFalse(css.contains("width:72px!important;min-width:72px!important;max-width:72px!important")),
                () -> assertFalse(css.contains("width:62px!important;min-width:62px!important;max-width:62px!important")));
    }
}
