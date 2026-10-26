package atf.autoex.tests.suites;

import atf.testkit.AtfE2eTest;
import atf.autoex.tests.e2e.AutoExCartUiE2eTest;
import atf.autoex.tests.e2e.AutoExAccountUiE2eTest;
import atf.autoex.tests.e2e.AutoExExistingCustomerUiApiE2eTest;
import atf.autoex.tests.e2e.AutoExPurchaseUiE2eTest;
import atf.autoex.tests.e2e.AutoExPurchaseUiApiE2eTest;
import atf.autoex.tests.e2e.AutoExPurchaseUiFinalScreenshotE2eTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/** One UI gate preserves every business story and the executable evidence variants without overlapping suites. */
@Suite
@SelectClasses({
        AutoExPurchaseUiE2eTest.class,
        AutoExCartUiE2eTest.class,
        AutoExAccountUiE2eTest.class,
        AutoExExistingCustomerUiApiE2eTest.class,
        AutoExPurchaseUiApiE2eTest.class,
        AutoExPurchaseUiFinalScreenshotE2eTest.class})
@AtfE2eTest
@Execution(ExecutionMode.SAME_THREAD)
public class AutoExE2eSuite { }
