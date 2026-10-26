package atf.reporting.governance;

import atf.testkit.AtfE2eTest;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Compatibility launcher; use ExtentReportingE2eSuite as the canonical UI gate. */
@Suite
@SelectPackages("atf.ui.tests.extent.e2e")
@IncludeTags("e2e")
@ExcludeClassNamePatterns(".*Suite")
@AtfE2eTest
@Deprecated(since = "1.0", forRemoval = false)
public class AtfReportingUiAuditSuite {
}
