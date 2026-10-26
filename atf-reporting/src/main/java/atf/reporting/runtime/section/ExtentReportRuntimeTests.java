package atf.reporting.runtime.section;

import atf.reporting.runtime.tests.ExtentReportRuntimeCategories;
import atf.reporting.runtime.tests.ExtentReportRuntimeScenarios;
import atf.reporting.runtime.tests.ExtentReportRuntimeTestFilters;

/** Runtime controller for Tests navigation data, filters and Categories projection. */
public final class ExtentReportRuntimeTests {
    private ExtentReportRuntimeTests() { }

    public static String script() {
        return String.join("",
                ExtentReportRuntimeTestFilters.script(),
                ExtentReportRuntimeCategories.script(),
                ExtentReportRuntimeScenarios.script());
    }
}
