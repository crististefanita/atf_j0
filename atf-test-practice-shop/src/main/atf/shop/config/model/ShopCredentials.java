package atf.shop.config.model;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/** External account material. Password is intentionally never exposed by toString. */
public record ShopCredentials(String email, String password,
                                      String firstName, String lastName) {
    private static final Set<String> RESERVED_EMAIL_DOMAINS = Set.of(
            "example.com", "example.net", "example.org", "test", "invalid", "localhost", "example");

    public ShopCredentials {
        email = required(email, "email");
        password = required(password, "password");
        firstName = required(firstName, "firstName");
        lastName = required(lastName, "lastName");
    }

    public static ShopCredentials load(Path file) {
        Objects.requireNonNull(file, "file");
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read Practice Shop account file: " + file, error);
        }
        return new ShopCredentials(properties.getProperty("email"),
                properties.getProperty("password"), properties.getProperty("firstName", "ATF"),
                properties.getProperty("lastName", "E2E"));
    }

    public String displayName() { return firstName + " " + lastName; }

    /**
     * Rejects obvious documentation/test addresses before an opt-in can send
     * credentials to the external Practice Shop service. This is only a local
     * safety check; a successful live login remains the proof of validity.
     */
    public boolean hasLiveAccountCandidate() {
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        int separator = normalizedEmail.lastIndexOf('@');
        if (separator <= 0 || separator == normalizedEmail.length() - 1) return false;

        String domain = normalizedEmail.substring(separator + 1);
        return RESERVED_EMAIL_DOMAINS.stream().noneMatch(reservedDomain ->
                domain.equals(reservedDomain) || domain.endsWith("." + reservedDomain));
    }

    @Override
    public String toString() {
        return "ShopCredentials[email=<redacted>, displayName=<redacted>]";
    }

    private static String required(String value, String name) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }
}


