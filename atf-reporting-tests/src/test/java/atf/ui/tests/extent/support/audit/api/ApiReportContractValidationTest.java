package atf.ui.tests.extent.support.audit.api;

import atf.testkit.AtfUnitTest;
import atf.ui.tests.extent.support.data.UiTestData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ApiReportContractValidationTest {

    @Test
    void lifecycleResultPublishesAnExplicitSkipAssertion() {
        var result = new ApiReportContractValidation.Result(
                UiTestData.LIFECYCLE_OWNER, 3, List.of(), List.of(), 1);

        var validation = result.validation();

        assertTrue(validation.valid());
        assertEquals("API lifecycle skip outcome", validation.assertions().get(1).name());
        assertEquals("TEST BODY status SKIP (1 rendered branch)",
                validation.assertions().get(1).actual());
    }

    @Test
    void lifecycleResultFailsWhenTheExpectedSkipBranchIsMissingOrDuplicated() {
        var missing = new ApiReportContractValidation.Result(
                UiTestData.LIFECYCLE_OWNER, 3, List.of(), List.of(), 0);
        var duplicated = new ApiReportContractValidation.Result(
                UiTestData.LIFECYCLE_OWNER, 3, List.of(), List.of(), 2);

        assertFalse(missing.validation().valid());
        assertFalse(duplicated.validation().valid());
    }
}
