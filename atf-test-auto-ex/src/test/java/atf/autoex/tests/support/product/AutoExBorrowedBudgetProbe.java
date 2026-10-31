package atf.autoex.tests.support.product;

import atf.autoex.runtime.navigation.AutoExReadOnlyNavigation;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryPolicy;
import atf.ui.playwright.PlaywrightUiSession;
import atf.ui.playwright.AtfLocator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Actual HTTP recovery with a transparent public Page decorator; no fabricated document or timing result. */
public final class AutoExBorrowedBudgetProbe {
    private AutoExBorrowedBudgetProbe() { }

    public static Snapshot run() throws Exception {
        var configuration = new UiBrowserConfiguration(URI.create("about:blank"), "chromium", true,
                Duration.ofMillis(1100), Duration.ofSeconds(3), 1440, 1000, 70, true);
        try (var browser = PlaywrightUiSession.open(configuration);
             var site = new AutoExProductTransitionSite(browser.page(), "repeat-520")) {
            var sampled = new AtomicBoolean();
            var timeouts = new ArrayList<Double>();
            Page observed = (Page) Proxy.newProxyInstance(Page.class.getClassLoader(), new Class<?>[]{Page.class},
                    (proxy, method, arguments) -> {
                        if (sampled.get() && method.getName().equals("navigate")) {
                            var options = (Page.NavigateOptions) arguments[1];
                            timeouts.add(options.timeout);
                        }
                        try { return method.invoke(browser.page(), arguments); }
                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
            var actions = browser.newActionSequence();
            var navigation = new AutoExReadOnlyNavigation(observed, actions,
                    site.origin(), UiRetryPolicy.readOnly(3, Duration.ZERO));
            site.openReady(observed);
            var allowance = new AtomicReference<>(Double.NaN);
            RuntimeException failure = null;
            var target = new AtfLocator(observed.locator("a[href^='/product_details/']"), "a[href^='/product_details/']");
            try {
                actions.clickAndAwait(target, "the owned document is confirmed within the original action budget", budget -> {
                    observed.waitForURL(site.origin() + "/product_details/1", new Page.WaitForURLOptions()
                            .setWaitUntil(WaitUntilState.COMMIT).setTimeout(budget.remainingMilliseconds()));
                    allowance.set(budget.remainingMilliseconds());
                    sampled.set(true);
                    navigation.within(budget).recoverIfBusy();
                });
            }
            catch (RuntimeException refusal) { failure = refusal; }
            return new Snapshot(allowance.get(), timeouts, site.requests(), site.activations(), failure,
                    actions.snapshot().stream().filter(action -> action.operation().equals("Click")).count(),
                    actions.snapshot().getLast().status().name());
        }
    }

    public record Snapshot(double allowanceBeforeRecovery, List<Double> nativeTimeouts,
                           List<String> requests, int pointerActivations, RuntimeException refusal,
                           long timedClicks, String finalActionStatus) {
        public Snapshot {
            nativeTimeouts = List.copyOf(nativeTimeouts);
            requests = List.copyOf(requests);
        }
    }
}
