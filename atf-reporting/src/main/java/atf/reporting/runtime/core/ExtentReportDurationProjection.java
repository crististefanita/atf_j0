package atf.reporting.runtime.core;

/** Facade composing lookup, view projection and publication seams. */
final class ExtentReportDurationProjection {
    private ExtentReportDurationProjection() {
    }

    static String script() {
        return String.join("",
                ExtentReportDurationProjectionLookup.script(),
                ExtentReportDurationProjectionViews.script(),
                ExtentReportDurationProjectionPublication.script());
    }
}
