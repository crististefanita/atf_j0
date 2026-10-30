package atf.ui.playwright;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Proxy;
import com.microsoft.playwright.Locator;
import atf.ui.config.UiRetryPolicy;
import static org.junit.jupiter.api.Assertions.*;

/** Deterministic arithmetic, separate from scheduling and native browser integration. */
@AtfComponentTest
class ObservationBudgetComponentTest {
    @Test void everyConfirmationConsumesTheSameDeadlineWithoutRenewal() {
        var ticks = new AtomicLong(10);
        var budget = new PlaywrightActionSequence.ObservationBudget(Duration.ofMillis(850), ticks::get);
        assertEquals(850d, budget.remainingMilliseconds());
        ticks.addAndGet(Duration.ofMillis(400).toNanos());
        assertEquals(450d, budget.remainingMilliseconds());
        ticks.addAndGet(Duration.ofMillis(449).toNanos());
        assertEquals(1d, budget.remainingMilliseconds());
        ticks.addAndGet(Duration.ofMillis(1).toNanos());
        assertThrows(IllegalStateException.class, budget::remainingMilliseconds);
        assertThrows(IllegalStateException.class, budget::remainingMilliseconds);
    }

    @Test void subMillisecondNativeTimeoutNeverBecomesAnUnboundedZero() {
        var ticks = new AtomicLong();
        var budget = new PlaywrightActionSequence.ObservationBudget(Duration.ofNanos(500_000), ticks::get);
        assertEquals(1d, budget.remainingMilliseconds());
        ticks.set(500_000);
        assertThrows(IllegalStateException.class, budget::remainingMilliseconds);
    }

    @Test void elapsedSubtractionPreservesTheDeadlineAcrossMonotonicCounterWrap() {
        var ticks = new AtomicLong(Long.MAX_VALUE - Duration.ofMillis(100).toNanos());
        var budget = new PlaywrightActionSequence.ObservationBudget(Duration.ofMillis(850), ticks::get);
        ticks.addAndGet(Duration.ofMillis(600).toNanos());
        assertEquals(250d, budget.remainingMilliseconds());
        ticks.addAndGet(Duration.ofMillis(250).toNanos());
        assertThrows(IllegalStateException.class, budget::remainingMilliseconds);
    }
    @Test void pointerAndSuccessiveReadOnlyConfirmationsNeverRenewThe1100msDeadline() {
        var probe=new Probe(400);
        var failure=assertThrows(IllegalStateException.class,()->probe.actions.clickAndAwait(probe.target,"confirmed",budget->{
            assertEquals(700d,budget.remainingMilliseconds());
            probe.advance(500); // first GET and document confirmation
            assertEquals(200d,budget.remainingMilliseconds());
            probe.advance(200); // a subsequent GET exhausts the original deadline
            assertThrows(IllegalStateException.class,budget::remainingMilliseconds);
        }));
        assertTrue(failure.getCause().getMessage().contains("budget exhausted"));
        probe.verify("FAIL");
    }
    @Test void exhaustionDuringPointerCannotStartTheConfirmation() {
        var probe=new Probe(1100);var confirmations=new AtomicInteger();
        assertThrows(IllegalStateException.class,()->probe.actions.clickAndAwait(probe.target,"confirmed",budget->confirmations.incrementAndGet()));
        assertEquals(0,confirmations.get());probe.verify("FAIL");
    }
    @Test void aConfirmationThatReturnsAtTheDeadlineCannotPublishPass() {
        var probe=new Probe(100);
        assertThrows(IllegalStateException.class,()->probe.actions.clickAndAwait(probe.target,"confirmed",budget->probe.advance(1000)));
        probe.verify("FAIL");
    }
    @Test void successfulConfirmationAndOriginalRefusalBothKeepOneActivation() {
        var success=new Probe(400);
        success.actions.clickAndAwait(success.target,"confirmed",budget->{success.advance(600);assertEquals(100d,budget.remainingMilliseconds());});
        success.verify("PASS");
        var refusal=new Probe(400);var primary=new IllegalStateException("owned document refused");
        var failure=assertThrows(IllegalStateException.class,()->refusal.actions.clickAndAwait(refusal.target,"confirmed",budget->{throw primary;}));
        assertSame(primary,failure.getCause());refusal.verify("FAIL");
    }
    private static final class Probe {
        final AtomicLong ticks=new AtomicLong();final AtomicInteger activations=new AtomicInteger();
        final PlaywrightActionSequence actions=new PlaywrightActionSequence(UiActionTrace.noOp(),Duration.ofMillis(1100),UiRetryPolicy.none(),ticks::get);
        final AtfLocator target;
        Probe(long pointerMillis) {
            Locator locator=(Locator)Proxy.newProxyInstance(Locator.class.getClassLoader(),new Class<?>[]{Locator.class},(proxy,method,args)->{
                if(!method.getName().equals("click"))throw new AssertionError("Unexpected native operation: "+method.getName());
                assertEquals(1100d,((Locator.ClickOptions)args[0]).timeout);
                activations.incrementAndGet();advance(pointerMillis);return null;
            });
            target=new AtfLocator(locator,"#product");
        }
        void advance(long millis){ticks.addAndGet(Duration.ofMillis(millis).toNanos());}
        void verify(String status){assertEquals(1,activations.get());assertEquals(1,actions.snapshot().size());assertEquals(status,actions.snapshot().getFirst().status().name());}
    }
}
