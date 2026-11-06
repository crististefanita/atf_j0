package atf.testkit.suites;

import atf.testkit.AtfUnitTest;
import atf.testkit.scope.TestRunScope;
import org.junit.platform.suite.api.*;

/** Primary unit gate; focused suites are declared separately in the suite catalogue. */
@Suite
@SelectPackages("atf.testkit")
@IncludeClassNamePatterns("^[^$]+Test$")
@IncludeTags("unit")
@IncludeEngines("junit-jupiter")
@ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@AtfUnitTest
public class KitUnitSuite { }
