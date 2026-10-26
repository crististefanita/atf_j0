package atf.shop.tests.api.unit.reporting;

import atf.api.reporting.contract.AtfAssertion;
import atf.api.testkit.AtfUnitTest;
import atf.shop.support.api.ShopApiReportValidation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ShopApiReportValidationTest {
    @Test
    void businessValidationAggregatesEveryAssertion() {
        var passing = new ShopApiReportValidation("live business contract", List.of(
                new AtfAssertion("status", "200", "200", true),
                new AtfAssertion("identity", "Pliers", "Pliers", true)));
        var failing = new ShopApiReportValidation("live business contract", List.of(
                new AtfAssertion("status", "200", "401", false)));

        assertTrue(passing.passed());
        assertFalse(failing.passed());
    }
}
