package atf.shop.tests.component.configuration;

import atf.shop.tests.support.ShopComponentTest;
import atf.shop.config.ShopRegistrationProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ShopComponentTest
class ShopRegistrationProfileTest {
    @Test
    void createsUniqueRunScopedIdentityWithoutExposingProfileValues() {
        var profile = profile("atf+{runId}@owned-domain.net");

        var first = profile.newRegistrationData("run0123456789ab");
        var second = profile.newRegistrationData("run0123456789ac");
        var credentials = profile.credentials(first);

        assertAll(
                () -> assertNotEquals(first.email(), second.email()),
                () -> assertTrue(credentials.hasLiveAccountCandidate()),
                () -> assertFalse(profile.toString().contains("owned-domain.net")),
                () -> assertFalse(profile.toString().contains("Secret!123")),
                () -> assertFalse(first.toString().contains(first.email())),
                () -> assertFalse(first.toString().contains("Secret Street")),
                () -> assertFalse(credentials.toString().contains(first.email())));
    }

    @Test
    void rejectsReservedDomainsAndMalformedRunIdsBeforeTheBrowserStarts() {
        var reserved = profile("atf+{runId}@example.test");

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> reserved.newRegistrationData("run0123456789ab")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> profile("atf@owned-domain.net").newRegistrationData("run0123456789ab")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> profile("atf+{runId}}@owned-domain.net")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> profile("atf+{runId}@owned-domain.net").newRegistrationData("bad")));
    }

    @Test
    void builtInSyntheticProfileCreatesUniqueNonDeliverableDemoIdentities() {
        var profile = ShopRegistrationProfile.synthetic();
        var first = profile.newRegistrationData("run0123456789ab");
        var second = profile.newRegistrationData("run0123456789ac");

        assertAll(
                () -> assertTrue(first.email().endsWith("@atf.invalid")),
                () -> assertNotEquals(first.email(), second.email()),
                () -> assertTrue(first.password().length() >= 12),
                () -> assertFalse(profile.toString().contains(first.email())),
                () -> assertFalse(profile.toString().contains(first.password())));
    }

    @Test
    void loadsLocalPropertiesWithoutIncludingAnyValueInItsStringRepresentation(@TempDir Path temp)
            throws Exception {
        Path file = temp.resolve("registration.local.properties");
        Files.writeString(file, "emailTemplate=atf+{runId}@owned-domain.net\n"
                + "password=Secret!123\nfirstName=Ada\nlastName=Lovelace\n"
                + "dateOfBirth=1990-01-01\ncountry=Romania\npostalCode=010101\n"
                + "houseNumber=10\nstreet=Secret Street\ncity=Bucharest\n"
                + "state=Bucharest\nphone=0700000000\n");

        var profile = ShopRegistrationProfile.load(file);

        assertTrue(profile.newRegistrationData("run0123456789ab").email()
                .startsWith("atf+run0123456789ab@"));
        assertFalse(profile.toString().contains("Ada"));
        assertFalse(profile.toString().contains("Secret Street"));
    }

    private static ShopRegistrationProfile profile(String emailTemplate) {
        return new ShopRegistrationProfile(emailTemplate, "Secret!123", "Ada", "Lovelace",
                "1990-01-01", "Romania", "010101", "10", "Secret Street", "Bucharest",
                "Bucharest", "0700000000");
    }
}
