package atf.autoex.tests.suites;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Offline consumer component contracts; never opens the public Automation Exercise site. */
@Suite
@SelectPackages("atf.autoex.tests.component")
@AtfComponentTest
public class AutoExComponentSuite { }
