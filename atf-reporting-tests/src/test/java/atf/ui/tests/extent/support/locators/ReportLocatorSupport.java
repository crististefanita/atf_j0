package atf.ui.tests.extent.support.locators;

import atf.reporting.theme.tokens.AtfReportCssSelectors;
import atf.reporting.ui.contract.ReportUiContract;
import com.microsoft.playwright.Locator;

import java.util.Objects;
import java.util.regex.Pattern;

/** Shared structural selector primitives for the report locator boundary. */
final class ReportLocatorSupport {
    private static final Pattern SAFE_ATTRIBUTE = Pattern.compile("[A-Za-z0-9_-]+");
    static final String TESTS_VIEW = idSelector(AtfReportCssSelectors.Surface.TEST_VIEW);
    static final String VISIBLE_TEST_DETAIL = ReportUiContract.VISIBLE_DETAIL_SELECTOR;

    private ReportLocatorSupport() {
    }

    static String cssAttribute(String value) {
        if (value == null || !SAFE_ATTRIBUTE.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsafe selector attribute value");
        }
        return value;
    }

    static String classSelector(String element, String className) {
        return Objects.requireNonNull(element, "element")
                + "[class~='" + cssAttribute(className) + "']";
    }

    static String classSelector(String className) {
        return "[class~='" + cssAttribute(className) + "']";
    }

    static String idSelector(String id) {
        return "[id='" + cssAttribute(id) + "']";
    }

    static String idSelector(AtfReportCssSelectors.Surface surface) {
        String hash = Objects.requireNonNull(surface, "surface").hash();
        return idSelector(hash.substring(1));
    }

    static int requireIndex(int zeroBasedIndex) {
        if (zeroBasedIndex < 0) throw new IllegalArgumentException("zeroBasedIndex must be >= 0");
        return zeroBasedIndex;
    }

    static Locator cardForHeader(Locator header) {
        return header.locator("xpath=..");
    }

    static String requiredAttribute(Locator locator, String attribute, String description) {
        String value = locator.getAttribute(attribute);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(description + " is missing required " + attribute
                    + " attribute; the report cannot be replayed unambiguously");
        }
        return value;
    }

    static String nodeId(Locator header) {
        Locator node = cardForHeader(header).locator(":scope > " + classSelector("card-header")
                + " > " + classSelector("card-title") + " > " + classSelector("node"));
        return requiredAttribute(node, "id", "structural report node");
    }

    static String nodeHeaderSelector(String nodeId) {
        String safeId = cssAttribute(nodeId);
        return TESTS_VIEW + " " + VISIBLE_TEST_DETAIL
                + " div" + classSelector("card") + ":has(> " + classSelector("card-header")
                + " > " + classSelector("card-title") + " > " + classSelector("node") + "[id='"
                + safeId + "']) > " + classSelector("card-header") + "[role='button']";
    }

    static String nodeScopedSelector(String nodeId, String suffix) {
        String safeId = cssAttribute(nodeId);
        return TESTS_VIEW + " " + VISIBLE_TEST_DETAIL
                + " div" + classSelector("card") + ":has(> " + classSelector("card-header")
                + " > " + classSelector("card-title") + " > " + classSelector("node") + "[id='"
                + safeId + "']) " + suffix;
    }
}

