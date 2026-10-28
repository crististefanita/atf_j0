package atf.autoex.tests.component.api;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.support.catalog.AutoExCatalogSnapshot;
import atf.autoex.support.journey.AutoExCatalogApiPrecondition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards API setup evidence ordering and its separation from causal browser traffic. */
@AutoExComponentTest
class AutoExCatalogApiPreconditionComponentTest {
    private static final String PRODUCT = "Configured Product";
    private static final String PRICE = "25.99";

    @Test
    void publishesTheApiPreconditionBeforeTakingTheCausalTrafficBaseline() {
        List<String> events = new ArrayList<>();
        List<AtfAssertion> publishedAssertions = new ArrayList<>();

        AutoExCatalogSnapshot result = AutoExCatalogApiPrecondition.verifyAndMarkBaseline(
                PRODUCT, PRICE,
                productName -> {
                    events.add("API request");
                    assertEquals(PRODUCT, productName);
                    return new AutoExCatalogSnapshot(200, 3, PRICE);
                },
                assertions -> {
                    events.add("precondition checkpoint");
                    publishedAssertions.addAll(assertions);
                },
                () -> events.add("causal baseline"));

        assertAll(
                () -> assertEquals(List.of("API request", "precondition checkpoint", "causal baseline"), events),
                () -> assertEquals(3, publishedAssertions.size()),
                () -> assertTrue(publishedAssertions.stream().allMatch(AtfAssertion::passed)),
                () -> assertEquals(PRICE, result.configuredProductPrice()));
    }

    @Test
    void failedPreconditionDoesNotAdvanceTheCausalBaseline() {
        List<String> events = new ArrayList<>();

        assertThrows(AssertionError.class, () -> AutoExCatalogApiPrecondition.verifyAndMarkBaseline(
                        PRODUCT, PRICE,
                        ignored -> new AutoExCatalogSnapshot(503, 0, "unavailable"),
                        assertions -> {
                            events.add("precondition checkpoint");
                            assertEquals(3, assertions.size());
                            assertTrue(assertions.stream().anyMatch(assertion -> !assertion.passed()));
                            throw new AssertionError("checkpoint failed as the reporting adapter would");
                        },
                        () -> events.add("causal baseline")));

        assertEquals(List.of("precondition checkpoint"), events,
                "failed setup evidence cannot be reclassified as post-setup browser traffic");
    }
}
