package atf.shop.support.recovery;

import atf.shop.config.ShopConfiguration;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.recovery.UiBackendRecovery;
import atf.ui.recovery.UiRecoveryOperation;
import com.microsoft.playwright.Page;
import java.util.*;

/** Shop's catalog list is an explicit read; login, cart, checkout, order and cleanup remain excluded. */
public final class ShopBackendRecovery {
    public static final String CATALOG_READINESS = atf.shop.config.recovery.ShopRecoveryOperation.CATALOG_READINESS.key();
    private ShopBackendRecovery() { }
    public static UiBackendRecovery open(Page page, ShopConfiguration configuration, PlaywrightActionSequence actions) {
        var policy = configuration.browser().recovery();
        var operation = new UiRecoveryOperation(CATALOG_READINESS, UiRecoveryOperation.Safety.READ_ONLY,
                configuration.baseUrl(), configuration.api().endpoint().resolve("/products"), "GET");
        return new UiBackendRecovery(page, policy, List.of(operation), trace -> actions.information(
                "BE observation #" + trace.attempt(), trace.operation(),
                "profile=" + policy.profile() + "; maxAttempts=" + policy.maxAttempts() + "; totalTimeout="
                        + policy.totalTimeout() + "; source=" + policy.source(), trace.description()));
    }
}
