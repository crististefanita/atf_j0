package atf.reporting.governance;

import atf.testkit.AtfIntegrationTest;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** IntelliJ/Maven entry point for reporting integration contracts. */
@Suite
@SelectPackages("atf.reporting")
@IncludeTags("integration")
@ExcludeClassNamePatterns(".*Suite")
@AtfIntegrationTest
public class AtfReportingIntegrationSuite {
}
