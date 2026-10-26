package atf.shop.flow;

import atf.testkit.AtfComponentTest;
import atf.shop.config.ShopCheckoutConfiguration;
import atf.shop.config.ShopConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ShopPurchaseStageInputsComponentTest {
    @Test
    void catalogInputOwnsItsHomeValidationCapability() {
        Set<String> fields = componentNames(ShopPurchaseJourneyContext.CatalogInputs.class);

        assertEquals(Set.of("page", "actions", "selection", "checkpointPublisher", "apiNetwork",
                "locators", "pageValidator", "homeScreenValidator", "purchaseScreenValidator"), fields);
        assertTrue(componentTypes(ShopPurchaseJourneyContext.CatalogInputs.class)
                .contains(ShopConfiguration.CatalogSelection.class));
    }

    @Test
    void checkoutInputCannotReachCatalogHomeValidation() {
        Set<String> fields = componentNames(ShopPurchaseJourneyContext.CheckoutInputs.class);

        assertFalse(fields.contains("homeScreenValidator"));
        assertEquals(Set.of("page", "actions", "cartUrl", "productName", "checkout", "checkpointPublisher",
                "apiNetwork", "locators", "pageValidator", "purchaseScreenValidator"), fields);
        Set<Class<?>> types = componentTypes(ShopPurchaseJourneyContext.CheckoutInputs.class);
        assertTrue(types.contains(ShopCheckoutConfiguration.class));
        assertFalse(types.contains(ShopConfiguration.class));
    }

    @Test
    void reusableCatalogFlowAcceptsOnlyTheCatalogProjection() {
        var method = Arrays.stream(ShopCatalogFlow.class.getMethods())
                .filter(candidate -> candidate.getName().equals("searchAndOpenConfiguredProduct"))
                .findFirst().orElseThrow();

        assertEquals(ShopConfiguration.CatalogSelection.class, method.getParameterTypes()[2]);
        assertFalse(Arrays.asList(method.getParameterTypes()).contains(ShopConfiguration.class));
    }

    private static Set<String> componentNames(Class<?> contract) {
        return Arrays.stream(contract.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<Class<?>> componentTypes(Class<?> contract) {
        return Arrays.stream(contract.getRecordComponents())
                .map(component -> component.getType())
                .collect(Collectors.toUnmodifiableSet());
    }
}
