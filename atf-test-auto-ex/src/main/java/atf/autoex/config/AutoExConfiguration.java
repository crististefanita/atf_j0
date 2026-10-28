package atf.autoex.config;

import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryPolicy;
import atf.ui.config.UiRetryOperation;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/** Immutable consumer settings; site data stays outside reusable ATF libraries. */
public record AutoExConfiguration(URI baseUrl, boolean e2eEnabled,
                                  UiBrowserConfiguration browser,
                                  Path uiReportBaseDirectory,
                                  Catalog catalog, Registration registration,
                                  Payment payment, UiRetryPolicy storeAvailabilityPolicy,
                                  AutoExEvidenceSettings evidence) {
    public static final UiRetryPolicy DEFAULT_STORE_AVAILABILITY_POLICY = new UiRetryPolicy(
            1, Duration.ZERO, Set.of(UiRetryOperation.NAVIGATE), "store queue full");

    public AutoExConfiguration(URI baseUrl, boolean e2eEnabled, UiBrowserConfiguration browser,
                               Path uiReportBaseDirectory, Catalog catalog, Registration registration,
                               Payment payment) {
        this(baseUrl, e2eEnabled, browser, uiReportBaseDirectory, catalog, registration, payment,
                DEFAULT_STORE_AVAILABILITY_POLICY);
    }

    public AutoExConfiguration(URI baseUrl, boolean e2eEnabled, UiBrowserConfiguration browser,
                               Path uiReportBaseDirectory, Catalog catalog, Registration registration,
                               Payment payment, UiRetryPolicy storeAvailabilityPolicy) {
        this(baseUrl, e2eEnabled, browser, uiReportBaseDirectory, catalog, registration, payment,
                storeAvailabilityPolicy, AutoExEvidenceSettings.defaults());
    }

    public AutoExConfiguration {
        baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        if (baseUrl.getHost() == null || !("https".equalsIgnoreCase(baseUrl.getScheme())
                || "http".equalsIgnoreCase(baseUrl.getScheme()))) {
            throw new IllegalArgumentException("baseUrl must be an absolute HTTP(S) URI");
        }
        browser = Objects.requireNonNull(browser, "browser");
        uiReportBaseDirectory = safeRelative(uiReportBaseDirectory);
        catalog = Objects.requireNonNull(catalog, "catalog");
        registration = Objects.requireNonNull(registration, "registration");
        payment = Objects.requireNonNull(payment, "payment");
        evidence = Objects.requireNonNull(evidence, "evidence");
        storeAvailabilityPolicy = Objects.requireNonNull(storeAvailabilityPolicy, "storeAvailabilityPolicy");
        if (storeAvailabilityPolicy.eligibleOperations().stream()
                .anyMatch(operation -> operation != UiRetryOperation.NAVIGATE)) {
            throw new IllegalArgumentException("Store availability recovery only supports navigation");
        }
    }

    private static Path safeRelative(Path path) {
        Path normalized = Objects.requireNonNull(path, "uiReportBaseDirectory").normalize();
        if (normalized.isAbsolute() || normalized.toString().isBlank()
                || normalized.startsWith("..")) {
            throw new IllegalArgumentException("reports.uiBaseDirectory must stay project-relative");
        }
        return normalized;
    }

    public record Catalog(String searchTerm, String productId, String productName,
                          String unitPrice, int quantity) {
        public Catalog {
            searchTerm = required(searchTerm, "catalog.searchTerm");
            productId = required(productId, "catalog.productId");
            productName = required(productName, "catalog.productName");
            unitPrice = required(unitPrice, "catalog.unitPrice");
            if (quantity < 1) throw new IllegalArgumentException("catalog.quantity must be positive");
        }

        public Catalog withQuantity(int value) {
            return new Catalog(searchTerm, productId, productName, unitPrice, value);
        }

        public String expectedTotal() {
            String digits = unitPrice.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) throw new IllegalStateException("Configured unit price has no numeric amount");
            return "Rs. " + Math.multiplyExact(Integer.parseInt(digits), quantity);
        }
    }

    public record Registration(String birthDay, String birthMonth, String birthYear,
                               String firstName, String lastName, String company,
                               String address1, String address2, String country,
                               String state, String city, String postalCode,
                               String mobileNumber) {
        public Registration {
            birthDay = required(birthDay, "registration.birthDay");
            birthMonth = required(birthMonth, "registration.birthMonth");
            birthYear = required(birthYear, "registration.birthYear");
            firstName = required(firstName, "registration.firstName");
            lastName = required(lastName, "registration.lastName");
            company = required(company, "registration.company");
            address1 = required(address1, "registration.address1");
            address2 = required(address2, "registration.address2");
            country = required(country, "registration.country");
            state = required(state, "registration.state");
            city = required(city, "registration.city");
            postalCode = required(postalCode, "registration.postalCode");
            mobileNumber = required(mobileNumber, "registration.mobileNumber");
        }

        public String displayName() {
            return firstName + " " + lastName;
        }

        @Override
        public String toString() {
            return "Registration[configured=true]";
        }
    }

    public String expectedCartTotal() {
        return catalog.expectedTotal();
    }

    public AutoExConfiguration withRecovery(atf.ui.config.recovery.UiRecoveryConfiguration policy) {
        return new AutoExConfiguration(baseUrl, e2eEnabled, browser.withRecovery(policy), uiReportBaseDirectory,
                catalog, registration, payment, storeAvailabilityPolicy, evidence);
    }

    /** Avoid exposing configured form data if the aggregate is ever logged for diagnostics. */
    @Override
    public String toString() {
        return "AutoExConfiguration[baseUrl=" + baseUrl + ", e2eEnabled=" + e2eEnabled
                + ", browser=" + browser + ", uiReportBaseDirectory=" + uiReportBaseDirectory
                + ", catalog=" + catalog + ", registration=configured, payment=configured]";
    }

    /** Test-only card details accepted by the site's demo payment form; never a real payment method. */
    public record Payment(String nameOnCard, String cardNumber, String cvc,
                          String expiryMonth, String expiryYear) {
        public Payment {
            nameOnCard = required(nameOnCard, "payment.nameOnCard");
            cardNumber = required(cardNumber, "payment.cardNumber");
            cvc = required(cvc, "payment.cvc");
            expiryMonth = required(expiryMonth, "payment.expiryMonth");
            expiryYear = required(expiryYear, "payment.expiryYear");
        }

        @Override
        public String toString() {
            return "Payment[configured=true]";
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
