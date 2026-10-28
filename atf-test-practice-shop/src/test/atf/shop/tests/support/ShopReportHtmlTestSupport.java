package atf.shop.tests.support;

import atf.testkit.assertions.AuditedAssertions;

import java.util.List;

/** Semantic assertions for generated report markup; tests do not own HTML parsing details. */
public final class ShopReportHtmlTestSupport {
    private ShopReportHtmlTestSupport() { }

    public static void requireText(String html, String expected, String message) {
        AuditedAssertions.assertTrue(html.contains(expected), message + ": " + expected);
    }

    public static void requireInOrder(String html, List<String> expected, String message) {
        int previous = -1;
        for (String value : expected) {
            int current = html.indexOf(value);
            AuditedAssertions.assertTrue(current >= 0, message + " is missing: " + value);
            AuditedAssertions.assertTrue(current > previous,
                    message + " is out of order at: " + value);
            previous = current;
        }
    }

    public static void requireAbsent(String html, String forbidden, String message) {
        AuditedAssertions.assertFalse(html.contains(forbidden), message + ": " + forbidden);
    }
}
