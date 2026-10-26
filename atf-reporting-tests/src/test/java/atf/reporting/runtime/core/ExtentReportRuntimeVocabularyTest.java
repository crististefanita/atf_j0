package atf.reporting.runtime.core;
import atf.testkit.AtfUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@AtfUnitTest
class ExtentReportRuntimeVocabularyTest {
    @Test
    void exposesStableSelectorsAttributesAndSafeJavaScriptLiterals() {
        assertAll(
                () -> assertEquals(".test-list-item > .test-item[test-id]",
                        ExtentReportRuntimeVocabulary.Selector.CLASS_ITEMS.value()),
                () -> assertEquals(
                        ".node-info .badge-default,.test-list-item .text-sm span,.test-detail .text-sm span,"
                                + ".detail-head .info > .badge-default:not(.uri-anchor):not(.pointer)",
                        ExtentReportRuntimeVocabulary.Selector.DURATION_MARKERS.value()),
                () -> assertEquals(".pointer",
                        ExtentReportRuntimeVocabulary.HeaderMarker.DISCLOSURE_POINTER.selector()),
                () -> assertEquals(".uri-anchor",
                        ExtentReportRuntimeVocabulary.HeaderMarker.URI_ANCHOR.selector()),
                () -> assertEquals("data-atf-scenario-id",
                        ExtentReportRuntimeVocabulary.Attribute.SCENARIO_ID.value()),
                () -> assertEquals("warning",
                        ExtentReportRuntimeVocabulary.StatusClass.WARNING.value()),
                () -> assertEquals("q",
                        ExtentReportRuntimeVocabulary.function(ExtentReportRuntimeVocabulary.Function.QUERY_ALL)),
                () -> assertEquals("'a\\'b'", ExtentReportRuntimeVocabulary.js("a'b")),
                () -> assertEquals("API tests", ExtentReportRuntimeVocabulary.DEFAULT_TEST_CLASS));
    }
}
