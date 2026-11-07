package atf.reporting.governance;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.*;

/** All registered Reporting browser entries and their isolation controls; original layer tags remain unchanged. */
@Suite
@SelectPackages({"atf.reporting", "atf.ui.tests.extent"})
@IncludeTags("report-browser")
@ExcludeClassNamePatterns(".*Suite")
@AtfComponentTest
public class AtfReportingBrowserSuite { }
