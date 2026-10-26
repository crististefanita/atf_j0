package atf.ui.tests.extent.component.layout;

import atf.reporting.runtime.evidence.ExtentReportRuntimeEvidenceTableLayout;
import atf.reporting.theme.evidence.AtfReportEvidenceTableCss;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Component contract for content-aware sizing of all UI/API evidence tables. */
@AtfComponentTest
class ValidationTableLayoutComponentTest {
    @Test
    void runtimeAllocatesColumnsFromTheLongestVisibleValue() {
        String script = ExtentReportRuntimeEvidenceTableLayout.script();

        assertAll(
                () -> assertTrue(script.contains("validationCellLength")),
                () -> assertTrue(script.contains("normalizeEvidenceTableColumns")),
                () -> assertTrue(script.contains("evidenceTableIntrinsicWidth")),
                () -> assertTrue(script.contains("evidenceTableMeasureContext")),
                () -> assertTrue(script.contains("measureText")),
                () -> assertTrue(script.contains("evidenceTableContentMinimumWidth")),
                () -> assertTrue(script.contains("isKeyValue")),
                () -> assertTrue(script.contains("data-atf-key-column-width")),
                () -> assertTrue(script.contains("tableWidth?intrinsic[0]")),
                () -> assertTrue(script.contains("Math.max(20,available - 62)")),
                () -> assertTrue(script.contains("semanticMinimum")),
                () -> assertTrue(script.contains("headerPercent")),
                () -> assertTrue(script.contains("minimums")),
                () -> assertTrue(script.contains("activeIndexes")),
                () -> assertTrue(script.contains("ATF_EVIDENCE_TABLE_SELECTOR")),
                () -> assertTrue(script.contains("data-atf-content-column-max-px")),
                () -> assertTrue(script.contains("table.atf-ui-action-table")),
                () -> assertTrue(script.contains("table.atf-ui-observation-table")),
                () -> assertTrue(script.contains("table.assertions")),
                () -> assertTrue(script.contains("table.atf-setup-evidence-table")),
                () -> assertTrue(script.contains("table.helm-evidence-table")),
                () -> assertTrue(script.contains("table.helm-validation-table")),
                () -> assertFalse(script.contains("index === count - 1 && count <= 2")),
                () -> assertTrue(script.contains("evidenceTableColumnKind")),
                () -> assertTrue(script.contains("EVIDENCE_STATUS_COLUMN")),
                () -> assertTrue(script.contains("evidenceTableReservedWidth")),
                () -> assertTrue(script.contains("table.clientWidth")),
                () -> assertTrue(script.contains("measuredPercent")),
                () -> assertTrue(script.contains("Math.max(10.5")),
                () -> assertTrue(script.contains("24")),
                () -> assertTrue(script.contains("contentIndexes.length")),
                () -> assertTrue(script.contains("evidenceTableReservedMaximumWidth")),
                () -> assertTrue(script.contains("evidenceTableDistributeReservedSlack")),
                () -> assertTrue(script.contains("reservedScale")),
                () -> assertTrue(script.contains("reservedIndexes.forEach")),
                () -> assertTrue(script.contains("remaining")),
                () -> assertTrue(script.contains("100 - total")),
                () -> assertTrue(script.contains("evidenceTableAllocateWidths")),
                () -> assertTrue(script.contains("longest[index]")),
                () -> assertTrue(script.contains("data-atf-content-layout")),
                () -> assertTrue(script.contains("data-atf-content-column-widths")),
                () -> assertTrue(script.contains("data-atf-validation-layout")),
                () -> assertTrue(script.contains("data-atf-validation-column-widths")),
                () -> assertTrue(script.contains("ATF_EVIDENCE_TABLE_LAYOUT_CACHE")),
                () -> assertTrue(script.contains("evidenceTableFingerprint")),
                () -> assertTrue(script.contains("cache.fingerprint")),
                () -> assertTrue(script.contains("cache.widthSignature")),
                () -> assertTrue(script.contains("evidenceTableCandidates(root)")),
                () -> assertTrue(script.contains("evidenceTablesFromMutation")),
                () -> assertTrue(script.contains("ResizeObserver")),
                () -> assertTrue(script.contains("requestAnimationFrame")),
                () -> assertTrue(script.contains("pendingTables")),
                () -> assertTrue(script.contains("window.addEventListener('resize'")),
                () -> assertTrue(script.contains("MutationObserver")),
                () -> assertFalse(script.contains("new MutationObserver(schedule)")),
                () -> assertFalse(script.contains("setTimeout(normalizeValidationTableColumns")),
                () -> assertTrue(script.contains("normalizeValidationTableColumns(root)")));
    }

    @Test
    void cssLeavesAssertionColumnWidthsToTheRuntimeNormalizer() {
        String css = AtfReportEvidenceTableCss.css();

        assertAll(
                () -> assertTrue(css.contains("table.helm-validation-table{width:100%!important;max-width:100%!important;min-width:0!important;table-layout:fixed!important}")),
                () -> assertTrue(css.contains("overflow-wrap:anywhere!important;word-break:break-word!important")),
                () -> assertTrue(css.contains("min-width:0!important;max-width:none!important;white-space:nowrap!important")),
                () -> assertFalse(css.contains("width:72px!important;min-width:72px!important;max-width:72px!important")),
                () -> assertFalse(css.contains("width:62px!important;min-width:62px!important;max-width:62px!important")));
    }
}
