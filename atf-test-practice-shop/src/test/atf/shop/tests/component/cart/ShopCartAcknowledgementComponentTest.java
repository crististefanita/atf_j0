package atf.shop.tests.component.cart;

import atf.shop.tests.support.ShopComponentTest;
import atf.shop.tests.support.cart.ShopCartAcknowledgementFixture;
import atf.core.reporting.contract.AtfReportStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static atf.testkit.assertions.AuditedAssertions.*;

@ShopComponentTest
class ShopCartAcknowledgementComponentTest {
    @ParameterizedTest @ValueSource(strings = {"toast", "short-toast", "no-toast"})
    void theOwnedResponseAcknowledgesOneAdditionEvenWithoutAToast(String mode) {
        try (var fixture = new ShopCartAcknowledgementFixture(mode)) {
            fixture.add();
            assertEquals(1, fixture.clicks(), "Exactly one add-to-cart activation is dispatched");
            assertEquals(1, fixture.starts(), "One matching request is observed without replay");
            assertEquals(2, fixture.persisted(), "The fixture's independent cart state contains the requested quantity");
            assertTrue(fixture.records().stream().anyMatch(r -> r.operation().equals("Cart addition acknowledgement")),
                    "The accepted mutation has an auditable acknowledgement distinct from a toast");
        }
    }
    @ParameterizedTest @ValueSource(strings = {"rejected", "absent", "foreign", "wrong-product", "wrong-quantity", "old-response", "transport"})
    void staleForeignUnrelatedOrFailedResponsesNeverAcknowledgeTheCurrentAddition(String mode) {
        try (var fixture = new ShopCartAcknowledgementFixture(mode)) {
            assertThrows(RuntimeException.class, fixture::add, "An unowned or unsuccessful response cannot authorize cart review");
            assertEquals(1, fixture.clicks(), "Failure never replays the product activation");
            assertTrue(fixture.starts() <= 1, "The adapter never repeats the mutation request");
            assertEquals(AtfReportStatus.FAIL, fixture.records().getLast().status(), "The original action's unmet postcondition remains FAIL");
        }
    }
}
