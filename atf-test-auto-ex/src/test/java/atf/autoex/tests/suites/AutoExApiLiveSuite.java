package atf.autoex.tests.suites;

import atf.testkit.AtfIntegrationTest;
import atf.autoex.tests.api.AutoExApiLiveContractTest;
import atf.autoex.tests.api.AutoExApiReadOnlyLiveContractTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.ConfigurationParameter;
import atf.testkit.scope.TestRunScope;

/** Separate live API gate; the selected test remains disabled unless explicitly opted in. */
@Suite
@SelectClasses({AutoExApiReadOnlyLiveContractTest.class, AutoExApiLiveContractTest.class})
@AtfIntegrationTest
@ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
public class AutoExApiLiveSuite { }
