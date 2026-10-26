package atf.reporting.ui.support;

import atf.reporting.support.ReportTestScreenshots;

public final class ExtentReportUiEvidenceTestSupport {
    public static final String EXTENT_UI_TEST_PACKAGE = "atf.ui.tests.extent";

    private ExtentReportUiEvidenceTestSupport() {
    }

    public static int count(String text, String fragment) {
        int occurrences = 0;
        int from = 0;
        while ((from = text.indexOf(fragment, from)) >= 0) {
            occurrences++;
            from += fragment.length();
        }
        return occurrences;
    }

    public static boolean screenshotAnchorContainsDirectDataUri(String html) {
        int anchorStart = html.indexOf("<a class=\"atf-ui-action-screenshot-link\"");
        int anchorEnd = anchorStart < 0 ? -1 : html.indexOf("</a>", anchorStart);
        if (anchorStart < 0 || anchorEnd < 0) {
            return false;
        }
        String anchor = html.substring(anchorStart, anchorEnd);
        return anchor.contains("data-atf-screenshot=\"base64\"")
                && anchor.contains("href=\"data:image/png;base64," + ReportTestScreenshots.ONE_PIXEL_PNG + "\"");
    }
}
