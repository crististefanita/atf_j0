package atf.shop.tests.api.component.lifecycle;

import atf.testkit.AtfComponentTest;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.contract.ShopApiJsonField;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.tests.api.integration.support.ShopApiLiveCartScope;
import atf.shop.tests.api.support.ShopApiComponentFixture;
import atf.shop.tests.api.support.ShopApiScenarioFixture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ShopApiLiveCartScopeComponentTest {
    @Test
    void closesAStatefulCartEmptyWithoutRepeatingTheDelete() {
        var client = ShopApiComponentFixture.statefulCartClient();
        var cart = new ShopApiLiveCartScope(client, ShopApiScenarioFixture.MUTATION_PRODUCT_ID);
        int updatedQuantity = ShopApiScenarioFixture.UPDATED_QUANTITY;

        try (cart) {
            assertTrue(ShopApiHttpStatus.CREATED.matches(cart.create().response().status()));
            assertTrue(ShopApiHttpStatus.OK.matches(
                    cart.add(ShopApiScenarioFixture.QUANTITY).response().status()));
            cart.read();
            assertTrue(ShopApiHttpStatus.OK.matches(cart.updateQuantity(updatedQuantity)
                    .response().status()));
            assertEquals(updatedQuantity, quantity(cart.read().response().body()));
            assertTrue(ShopApiHttpStatus.NO_CONTENT.matches(cart.remove().response().status()));
            assertTrue(ShopApiHttpStatus.OK.matches(cart.read().response().status()));
        }

        long deleteCount = cart.executions().stream()
                .filter(execution -> "DELETE".equals(execution.request().method()))
                .count();
        assertEquals(1, deleteCount);
        assertTrue(ShopApiHttpStatus.OK.matches(
                cart.executions().get(cart.executions().size() - 1).response().status()));
        var finalBody = ShopJsonEvidence.parse(
                cart.executions().get(cart.executions().size() - 1).response().body());
        assertTrue(finalBody.path(ShopApiJsonField.CART_ITEMS.wireValue()).isEmpty());
    }

    @Test
    void cleansUpTheRemoteItemWhenTheBusinessFlowAborts() {
        var client = ShopApiComponentFixture.statefulCartClient();
        var cart = new ShopApiLiveCartScope(client, ShopApiScenarioFixture.MUTATION_PRODUCT_ID);

        assertThrows(IllegalStateException.class, () -> {
            try (cart) {
                cart.create();
                cart.add(ShopApiScenarioFixture.QUANTITY);
                throw new IllegalStateException("simulated business-flow failure");
            }
        });

        cart.close();
        var executions = cart.executions();
        long deleteCount = executions.stream()
                .filter(execution -> "DELETE".equals(execution.request().method()))
                .count();
        assertEquals(1, deleteCount, "failure cleanup must issue one delete and never retry it");

        var finalRead = executions.get(executions.size() - 1);
        assertTrue(ShopApiHttpStatus.OK.matches(finalRead.response().status()));
        var finalBody = ShopJsonEvidence.parse(finalRead.response().body());
        assertTrue(finalBody.path(ShopApiJsonField.CART_ITEMS.wireValue()).isEmpty());
    }

    private static int quantity(String body) {
        var root = ShopJsonEvidence.parse(body);
        return root.path(ShopApiJsonField.CART_ITEMS.wireValue()).get(0)
                .path(ShopApiJsonField.QUANTITY.wireValue()).asInt();
    }
}
