package atf.autoex.runtime.recovery;

import atf.autoex.config.AutoExConfiguration;
import atf.autoex.locator.AutoExRoutes;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.recovery.UiBackendRecovery;
import atf.ui.recovery.UiRecoveryOperation;
import com.microsoft.playwright.Page;
import java.util.*;

/** The explicit catalog read excludes queue recovery, account, cart, payment and cleanup. */
public final class AutoExBackendRecovery {
    public static final String CATALOG_READINESS = atf.autoex.config.recovery.AutoExRecoveryOperation.CATALOG_READINESS.key();
    private AutoExBackendRecovery() { }
    public static UiBackendRecovery open(Page page, AutoExConfiguration configuration, PlaywrightActionSequence actions) {
        var policy = configuration.browser().recovery();
        var operation = new UiRecoveryOperation(CATALOG_READINESS, UiRecoveryOperation.Safety.READ_ONLY,
                configuration.baseUrl(), AutoExRoutes.PRODUCTS_API.resolve(configuration.baseUrl()), "GET");
        return new UiBackendRecovery(page, policy, List.of(operation), trace -> actions.information(
                "BE observation #" + trace.attempt(), trace.operation(),
                "profile=" + policy.profile() + "; maxAttempts=" + policy.maxAttempts() + "; totalTimeout="
                        + policy.totalTimeout() + "; source=" + policy.source(), trace.description()));
    }
}
