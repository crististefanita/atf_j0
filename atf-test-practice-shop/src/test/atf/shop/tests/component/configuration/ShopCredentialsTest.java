package atf.shop.tests.component.configuration;

import atf.shop.tests.support.ShopComponentTest;
import atf.shop.config.model.ShopCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertThrows;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@ShopComponentTest
class ShopCredentialsTest {
    @Test
    void loadsCredentialsAndNeverRedactsPasswordIntoToString(@TempDir Path temp) throws Exception {
        Path file = temp.resolve("account.properties");
        Files.writeString(file, "email=customer@example.test\npassword=Secret!123\nfirstName=ATF\nlastName=E2E\n");

        ShopCredentials credentials = ShopCredentials.load(file);

        assertTrue(credentials.email().contains("@"));
        assertTrue(credentials.displayName().equals("ATF E2E"));
        assertFalse(credentials.toString().contains("Secret!123"));
        assertFalse(credentials.toString().contains(credentials.email()));
        assertFalse(credentials.toString().contains(credentials.displayName()));
    }

    @Test
    void rejectsMissingPasswordInsteadOfStartingAnAmbiguousExternalRun(@TempDir Path temp) throws Exception {
        Path file = temp.resolve("account.properties");
        Files.writeString(file, "email=customer@example.test\nfirstName=ATF\nlastName=E2E\n");

        assertThrows(IllegalArgumentException.class, () -> ShopCredentials.load(file));
    }

    @Test
    void distinguishesReservedExampleAddressesFromLiveAccountCandidates(@TempDir Path temp) throws Exception {
        Path externalCandidate = temp.resolve("external.properties");
        Files.writeString(externalCandidate,
                "email=customer@practicesoftwaretesting.com\npassword=secret\n");

        for (String email : List.of("customer@example.com", "customer@nested.example.org",
                "customer@example.test", "customer@example.invalid", "customer@localhost")) {
            Path example = temp.resolve(email.replaceAll("[^a-zA-Z0-9]", "_") + ".properties");
            Files.writeString(example, "email=" + email + "\npassword=secret\n");
            assertFalse(ShopCredentials.load(example).hasLiveAccountCandidate(), email);
        }
        assertTrue(ShopCredentials.load(externalCandidate).hasLiveAccountCandidate());
    }
}


