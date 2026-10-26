package atf.reporting.runtime.tests;

import atf.reporting.asset.ReportAssetComposer;
import atf.reporting.runtime.core.ExtentReportDurationVocabulary;
import atf.reporting.runtime.core.ExtentReportRuntimeVocabulary;
import atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.Template;

import java.util.Map;

import static atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.Attribute;
import static atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.CssClass;
import static atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.Event;
import static atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.Function;
import static atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.Selector;
import static atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.StylePriority;
import static atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.StyleProperty;
import static atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.StyleValue;
import static atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.Template.*;
import static atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.Value;
import static atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.function;
import static atf.reporting.runtime.tests.ExtentReportRuntimeTestsVocabulary.js;
import static java.util.Map.entry;

/** Java-governed facade for the native Tests-tab filter asset. */
public final class ExtentReportRuntimeTestFilters {
    private ExtentReportRuntimeTestFilters() {
    }

    public static String script() {
        return ReportAssetComposer.testFiltersScript(templateVariables());
    }

    private static Map<String, String> templateVariables() {
        return Map.ofEntries(
                template(FILTER_STATE_STATUS, Value.STATUS_KEY.value()),
                template(FILTER_STATE_TAG, Value.TAG_KEY.value()),
                template(FILTER_MATCHES, function(Function.FILTER_MATCHES)),
                template(REFRESH_FILTER_SELECTION, function(Function.REFRESH_FILTER_SELECTION)),
                template(APPLY_TEST_FILTERS, function(Function.APPLY_TEST_FILTERS)),
                template(SET_TEST_FILTER, function(Function.SET_TEST_FILTER)),
                template(BIND_TEST_FILTERS, function(Function.BIND_TEST_FILTERS)),
                template(NORMALIZE_TAG_FILTER, function(Function.NORMALIZE_TAG_FILTER)),
                template(FILTER_ITEMS, js(Selector.FILTER_ITEMS)),
                template(STATUS_MENU, js(Selector.STATUS_MENU)),
                template(TAG_FILTER_ITEMS, js(Selector.TAG_FILTER_ITEMS)),
                template(ACTIVE_TEST, js(Selector.ACTIVE_TEST)),
                template(TEST_DETAIL, js(Selector.TEST_DETAIL)),
                template(CLASS_ITEMS, ExtentReportRuntimeVocabulary.function(
                        ExtentReportRuntimeVocabulary.Function.CLASS_ITEMS)),
                template(IS_CLASS_ROOT, ExtentReportRuntimeVocabulary.function(
                        ExtentReportRuntimeVocabulary.Function.IS_CLASS_ROOT)),
                template(DIRECT_SCENARIO_CARDS, ExtentReportRuntimeVocabulary.function(
                        ExtentReportRuntimeVocabulary.Function.DIRECT_SCENARIO_CARDS)),
                template(STATUS_OF, ExtentReportRuntimeVocabulary.function(
                        ExtentReportRuntimeVocabulary.Function.STATUS_OF)),
                template(TEST_TAGS, ExtentReportRuntimeVocabulary.function(
                        ExtentReportRuntimeVocabulary.Function.TEST_TAGS)),
                template(QUERY_ALL, ExtentReportRuntimeVocabulary.function(
                        ExtentReportRuntimeVocabulary.Function.QUERY_ALL)),
                template(TEXT, ExtentReportRuntimeVocabulary.function(
                        ExtentReportRuntimeVocabulary.Function.TEXT)),
                template(AGGREGATE_CLASS_DURATIONS,
                        ExtentReportDurationVocabulary.function(
                                ExtentReportDurationVocabulary.Function.AGGREGATE_CLASS_DURATIONS)),
                template(CAPTURE_DOM_CONTRACT, ExtentReportRuntimeVocabulary.externalFunction(
                        ExtentReportRuntimeVocabulary.ExternalFunction.CAPTURE_DOM_CONTRACT)),
                template(STATUS_ATTRIBUTE, js(Attribute.STATUS)),
                template(FILTER_BOUND_ATTRIBUTE, js(Attribute.FILTER_BOUND)),
                template(ARIA_PRESSED_ATTRIBUTE, js(Attribute.ARIA_PRESSED)),
                template(FILTER_HIDDEN_ATTRIBUTE, js(Attribute.FILTER_HIDDEN)),
                template(TRUE_VALUE, js(Value.TRUE)),
                template(ACTIVE_CLASS, js(CssClass.ACTIVE)),
                template(CLICK_EVENT, js(Event.CLICK)),
                template(CLEAR_VALUE, js(Value.CLEAR)),
                template(DISPLAY_PROPERTY, js(StyleProperty.DISPLAY)),
                template(EMPTY_VALUE, js(StyleValue.EMPTY)),
                template(NONE_VALUE, js(StyleValue.NONE)),
                template(IMPORTANT_PRIORITY, js(StylePriority.IMPORTANT)),
                template(EXCLUDED_TAGS, ExtentReportRuntimeTestsVocabulary.excludedTags()));
    }

    private static Map.Entry<String, String> template(Template key, String value) {
        return entry(key.value(), value);
    }
}







