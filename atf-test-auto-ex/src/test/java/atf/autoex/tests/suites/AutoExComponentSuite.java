package atf.autoex.tests.suites;

import atf.testkit.AtfComponentTest;
import atf.testkit.scope.TestRunScope;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Offline consumer component contracts; never opens the public Automation Exercise site. */
@Suite
@ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@SelectPackages("atf.autoex.tests.component")
@AtfComponentTest
public class AutoExComponentSuite { }
