package atf.reporting.governance;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** IntelliJ/Maven entry point for reporting component contracts. */
@Suite
@SelectPackages({"atf.reporting", "atf.ui.tests.extent.component"})
@IncludeTags("component")
@ExcludeClassNamePatterns(".*Suite")
@AtfComponentTest
public class AtfReportingComponentSuite {
}
