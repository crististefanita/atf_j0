package atf.reporting.governance;

import atf.testkit.AtfIntegrationTest;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** IntelliJ/Maven entry point for mock API fixture contracts consumed by reporting. */
@Suite
@SelectPackages("atf.reporting.api.tests.api")
@IncludeTags("integration")
@ExcludeClassNamePatterns(".*Suite")
@AtfIntegrationTest
public class AtfReportingApiFixtureSuite {
}
