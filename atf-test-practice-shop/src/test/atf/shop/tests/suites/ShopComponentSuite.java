package atf.shop.tests.suites;

import atf.testkit.AtfComponentTest;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/** Fast Shop business/component and consumer-adapter contracts; no live journeys. */
@Suite
@SelectPackages({
        "atf.shop.tests.component",
        "atf.shop.tests.api.component",
        "atf.shop.tests.api.unit",
        "atf.shop.tests.api.reporting",
        "atf.shop.tests.api.network",
        "atf.shop.tests.support"
})
@AtfComponentTest
public class ShopComponentSuite { }
