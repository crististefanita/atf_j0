package atf.shop.tests.suites;

import atf.testkit.AtfIntegrationTest;
import atf.shop.tests.api.integration.ShopStandaloneApiReportTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/** Real HTTP contract gate; the selected class retains its explicit live opt-in. */
@Suite
@SelectClasses(ShopStandaloneApiReportTest.class)
@AtfIntegrationTest
public class ShopApiLiveSuite { }
