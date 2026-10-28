package atf.autoex.flow.purchase;

import atf.autoex.config.AutoExConfiguration.Catalog;
import atf.autoex.config.AutoExConfiguration.Registration;
import atf.autoex.config.AutoExConfiguration.Payment;

import java.net.URI;
import java.util.Objects;

/** Independent configured expectations captured once at the journey composition root. */
public record AutoExPurchaseInputs(Catalog product, Registration registration,
                                   Payment payment, URI cleanupLanding) {
    public AutoExPurchaseInputs {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(registration, "registration");
        Objects.requireNonNull(payment, "payment");
        Objects.requireNonNull(cleanupLanding, "cleanupLanding");
    }
}
