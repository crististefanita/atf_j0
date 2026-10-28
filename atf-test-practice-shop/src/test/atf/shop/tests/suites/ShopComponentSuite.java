package atf.shop.tests.suites;

import atf.shop.tests.support.ShopComponentTest;
import atf.testkit.scope.TestRunScope;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Fast Shop business/component and consumer-adapter contracts; no live journeys. */
@Suite
@ConfigurationParameter(key = TestRunScope.CONFIGURATION_PARAMETER, value = "suite")
@ConfigurationParameter(key = "junit.jupiter.execution.parallel.enabled", value = "false")
@SelectPackages({
        "atf.shop.tests.component",
        "atf.shop.tests.api.component",
        "atf.shop.tests.api.unit",
        "atf.shop.tests.api.reporting",
        "atf.shop.tests.api.network",
        "atf.shop.tests.support",
        "atf.shop.flow"
})
@ShopComponentTest
public class ShopComponentSuite { }
