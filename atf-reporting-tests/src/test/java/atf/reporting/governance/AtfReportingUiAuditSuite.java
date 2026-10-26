package atf.reporting.governance;

import atf.testkit.AtfE2eTest;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** IntelliJ/Maven entry point for browser-backed UI audit contracts. */
@Suite
@SelectPackages("atf.ui.tests.extent.e2e")
@IncludeTags("e2e")
@ExcludeClassNamePatterns(".*Suite")
@AtfE2eTest
public class AtfReportingUiAuditSuite {
}
