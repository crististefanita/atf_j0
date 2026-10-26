package atf.shop.tests.support.lifecycle;

import atf.api.reporting.contract.AtfAssertion;
import atf.api.testkit.AtfComponentTest;
import atf.shop.config.ShopConfiguration;
import atf.shop.config.ShopConfigurationLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ShopE2eSetupAssertionsTest {
    @Test
    void guestSetupDoesNotSurfaceAnAccountFixtureThatTheJourneyDoesNotUse() {
        ShopConfiguration configuration = ShopConfigurationLoader.load();

        List<AtfAssertion> assertions = ShopE2eContext.setupAssertions(configuration);
        AtfAssertion fixturePolicy = assertion(assertions, "Account fixture policy");
        AtfAssertion accountSetup = assertion(assertions, "Account setup");

        assertTrue(assertions.stream().allMatch(AtfAssertion::passed));
        assertEquals("not required for guest journey", fixturePolicy.expected());
        assertEquals(fixturePolicy.expected(), fixturePolicy.actual());
        assertEquals("not required for guest journey", accountSetup.actual());
        assertFalse(assertion(assertions, "Checkout mode").actual().contains("accountFile"));
    }

    @Test
    void missingOptionalFixtureDoesNotFailGuestSetupOrItsOwnershipPolicy() {
        ShopConfiguration base = ShopConfigurationLoader.load();
        Path missingFixture = Path.of("atf-test-practice-shop", "src", "test", "resources",
                "missing-optional-account.properties").toAbsolutePath().normalize();
        ShopConfiguration guest = withAccountFile(base, missingFixture);

        List<AtfAssertion> assertions = ShopE2eContext.setupAssertions(guest);

        assertTrue(assertions.stream().allMatch(AtfAssertion::passed));
        assertEquals("not required for guest journey", assertion(assertions, "Account setup").actual());
        assertTrue(assertion(assertions, "Account fixture policy").passed());
    }

    @Test
    void unusedFixtureOutsideOwnedTestResourcesDoesNotBlockGuestJourney(@TempDir Path temp) {
        ShopConfiguration base = ShopConfigurationLoader.load();
        ShopConfiguration guest = withAccountFile(base, temp.resolve("account.properties"));

        List<AtfAssertion> assertions = ShopE2eContext.setupAssertions(guest);

        assertTrue(assertions.stream().allMatch(AtfAssertion::passed));
        assertEquals("not required for guest journey", assertion(assertions,
                "Account fixture policy").actual());
        assertEquals("guest checkout; account fixture not required",
                assertion(assertions, "Checkout mode").actual());
    }

    private static AtfAssertion assertion(List<AtfAssertion> assertions, String name) {
        return assertions.stream().filter(candidate -> candidate.name().equals(name))
                .findFirst().orElseThrow();
    }

    private static ShopConfiguration withAccountFile(
            ShopConfiguration base, Path accountFile) {
        return new ShopConfiguration(base.baseUrl(), accountFile, base.productName(),
                base.productPath(), base.checkout(), base.browser(), base.uiRetry(),
                base.validationProfile(), base.api(), base.uiReportDirectory(),
                base.apiReportDirectory(), false, base.reporting());
    }
}
