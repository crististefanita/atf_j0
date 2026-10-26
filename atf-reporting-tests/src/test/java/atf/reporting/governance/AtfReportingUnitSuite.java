package atf.reporting.governance;

import atf.testkit.AtfUnitTest;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** IntelliJ/Maven entry point for reporting-only unit contracts. */
@Suite
@SelectPackages("atf.reporting")
@IncludeTags("unit")
@ExcludeClassNamePatterns(".*Suite")
@AtfUnitTest
public class AtfReportingUnitSuite {
}
