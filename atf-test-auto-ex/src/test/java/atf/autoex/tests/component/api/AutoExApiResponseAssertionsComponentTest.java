package atf.autoex.tests.component.api;

import atf.reporting.contract.AtfAssertion;
import atf.testkit.AtfComponentTest;
import atf.autoex.api.AutoExApiResponseAssertions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class AutoExApiResponseAssertionsComponentTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void searchAssertionPassesWhenAProductMatchesTheConfiguredTerm() throws Exception {
        var response = JSON.readTree("""
                {"products":[{"name":"Blue Top"},{"name":"Men Tshirt"}]}
                """);

        AtfAssertion assertion = AutoExApiResponseAssertions.searchResultsContain(response, "blue top");

        assertAll(
                () -> assertEquals("blue top", assertion.expected()),
                () -> assertEquals("Blue Top", assertion.actual()),
                () -> assertTrue(assertion.passed()));
    }

    @Test
    void searchAssertionFailsWhenNoReturnedProductMatchesTheConfiguredTerm() throws Exception {
        var response = JSON.readTree("""
                {"products":[{"name":"Men Tshirt"}]}
                """);

        AtfAssertion assertion = AutoExApiResponseAssertions.searchResultsContain(response, "blue top");

        assertAll(
                () -> assertEquals("<no matching product>", assertion.actual()),
                () -> assertFalse(assertion.passed()));
    }

    @Test
    void searchAssertionFailsForMissingResultsOrBlankTerm() throws Exception {
        var response = JSON.readTree("{}");

        AtfAssertion missingResults = AutoExApiResponseAssertions.searchResultsContain(response, "blue top");
        AtfAssertion blankTerm = AutoExApiResponseAssertions.searchResultsContain(response, "  ");

        assertAll(
                () -> assertEquals("<missing>", missingResults.actual()),
                () -> assertFalse(missingResults.passed()),
                () -> assertFalse(blankTerm.passed()));
    }
}
