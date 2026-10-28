package atf.shop.tests.component.purchase;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.shop.tests.support.ShopComponentTest;
import atf.shop.support.ShopPurchaseCheckpoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ShopComponentTest
class ShopPurchaseCheckpointTest {
    @Test
    void checkpointKeepsTheGherkinAndAssertionsAsAnImmutablePageContract() {
        var checkpoint = new ShopPurchaseCheckpoint(
                AtfGherkinKeyword.GIVEN, "product page is complete",
                List.of(new AtfAssertion("Product", "Combination Pliers", "Combination Pliers", true)));

        assertEquals(AtfGherkinKeyword.GIVEN, checkpoint.keyword());
        assertEquals("product page is complete", checkpoint.title());
        assertEquals(1, checkpoint.assertions().size());
        assertThrows(UnsupportedOperationException.class,
                () -> checkpoint.assertions().clear());
    }
}
