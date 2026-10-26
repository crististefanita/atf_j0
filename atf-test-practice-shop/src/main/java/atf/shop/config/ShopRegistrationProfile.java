package atf.shop.config;

import atf.shop.config.model.ShopCredentials;
import atf.shop.config.model.ShopRegistrationData;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

/** Local or built-in synthetic registration values; generated email identity is unique per invocation. */
public record ShopRegistrationProfile(
        String emailTemplate, String password, String firstName, String lastName,
        String dateOfBirth, String country, String postalCode, String houseNumber,
        String street, String city, String state, String phone) {
    public static final String RUN_ID_TOKEN = "{runId}";
    private static final String SYNTHETIC_EMAIL_TEMPLATE = "atf-practice-shop+{runId}@atf.invalid";

    /** Non-deliverable identity for explicit demo-app registration tests; no mailbox or secrets are needed. */
    public static ShopRegistrationProfile synthetic() {
        return new ShopRegistrationProfile(SYNTHETIC_EMAIL_TEMPLATE,
                "Atf!" + UUID.randomUUID().toString().replace("-", "") + "9x",
                "ATF", "Shop", "1990-01-01", "Romania", "010101", "10",
                "ATF Test Street", "Bucharest", "Bucharest", "0700000000");
    }

    public ShopRegistrationProfile {
        emailTemplate = required(emailTemplate, "emailTemplate");
        password = required(password, "password");
        firstName = required(firstName, "firstName");
        lastName = required(lastName, "lastName");
        dateOfBirth = required(dateOfBirth, "dateOfBirth");
        country = required(country, "country");
        postalCode = required(postalCode, "postalCode");
        houseNumber = required(houseNumber, "houseNumber");
        street = required(street, "street");
        city = required(city, "city");
        state = required(state, "state");
        phone = required(phone, "phone");
        if (occurrences(emailTemplate, RUN_ID_TOKEN) != 1) {
            throw new IllegalArgumentException("emailTemplate must contain {runId} exactly once");
        }
        String withoutRunId = emailTemplate.replace(RUN_ID_TOKEN, "");
        if (withoutRunId.contains("{") || withoutRunId.contains("}")) {
            throw new IllegalArgumentException("emailTemplate contains an unsupported placeholder");
        }
    }

    public static ShopRegistrationProfile load(Path file) {
        Objects.requireNonNull(file, "file");
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        } catch (IOException | IllegalArgumentException failure) {
            throw new IllegalStateException("Cannot read the local Practice Shop registration profile", failure);
        }
        return new ShopRegistrationProfile(
                properties.getProperty("emailTemplate"), properties.getProperty("password"),
                properties.getProperty("firstName"), properties.getProperty("lastName"),
                properties.getProperty("dateOfBirth"), properties.getProperty("country"),
                properties.getProperty("postalCode"), properties.getProperty("houseNumber"),
                properties.getProperty("street"), properties.getProperty("city"),
                properties.getProperty("state"), properties.getProperty("phone"));
    }

    public ShopRegistrationData newRegistrationData() {
        return newRegistrationData(UUID.randomUUID().toString().replace("-", ""));
    }

    /** Deterministic seam for unit tests; runId is restricted to safe email-local-part characters. */
    public ShopRegistrationData newRegistrationData(String runId) {
        String id = required(runId, "runId");
        if (!id.matches("[A-Za-z0-9]{12,64}")) {
            throw new IllegalArgumentException("runId must contain 12–64 alphanumeric characters");
        }
        String email = emailTemplate.replace(RUN_ID_TOKEN, id);
        ShopCredentials candidate = new ShopCredentials(email, password, firstName, lastName);
        boolean explicitlySynthetic = SYNTHETIC_EMAIL_TEMPLATE.equals(emailTemplate)
                && email.endsWith("@atf.invalid");
        if (!candidate.hasLiveAccountCandidate() && !explicitlySynthetic) {
            throw new IllegalArgumentException(
                    "emailTemplate must use a non-reserved domain controlled by the test owner");
        }
        return new ShopRegistrationData(firstName, lastName, dateOfBirth, country,
                postalCode, houseNumber, street, city, state, phone, email, password);
    }

    public ShopCredentials credentials(ShopRegistrationData data) {
        Objects.requireNonNull(data, "data");
        return new ShopCredentials(data.email(), data.password(), data.firstName(), data.lastName());
    }

    @Override
    public String toString() {
        return "ShopRegistrationProfile[emailTemplate=<redacted>, identity=<redacted>, "
                + "address=<redacted>, contact=<redacted>, password=<redacted>]";
    }

    private static int occurrences(String value, String token) {
        int count = 0;
        for (int index = 0; (index = value.indexOf(token, index)) >= 0; index += token.length()) count++;
        return count;
    }

    private static String required(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
