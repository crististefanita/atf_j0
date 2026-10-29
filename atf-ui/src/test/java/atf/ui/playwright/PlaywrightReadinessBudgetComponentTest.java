package atf.ui.playwright;

import atf.testkit.AtfComponentTest;
import atf.ui.config.UiRetryOperation;
import atf.ui.config.UiRetryPolicy;
import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaywrightReadinessBudgetComponentTest {
    private Playwright playwright;
    private Browser browser;
    private Page page;
    @BeforeAll void open() {
        playwright = Playwright.create();browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        page = browser.newPage();
    }
    @AfterAll void close() { try {if (browser != null) browser.close();} finally {if (playwright != null) playwright.close();} }
    @BeforeEach void document() {page.setContent("<button id='target'>Owned target</button>");}

    @Test void readinessAndNativeVisibilityProduceOneMeasuredSuccess() {
        var actions = actions();var calls = new AtomicInteger();
        actions.awaitVisible(target(), budget -> {calls.incrementAndGet();assertTrue(budget.remainingMilliseconds() <= 150);});
        assertEquals(1, calls.get());assertEquals(1, actions.snapshot().size());assertEquals("PASS", actions.snapshot().getFirst().status().name());
    }
    @Test void originalFailureIsRetainedAndReadinessIsNeverAutomaticallyReplayed() {
        var actions = actions();var calls = new AtomicInteger();var original = new IllegalStateException("owned readiness failure");
        assertSame(original, assertThrows(IllegalStateException.class, () -> actions.awaitVisible(target(), budget -> {
            calls.incrementAndGet();throw original;
        })));
        assertEquals(1, calls.get());assertEquals("FAIL", actions.snapshot().getFirst().status().name());
        assertFalse(actions.snapshot().stream().anyMatch(a -> a.operation().contains("Retry")));
    }
    @Test void aLateVisibleResultCannotResetTheOriginalDeadline() {
        var actions = actions();
        assertThrows(IllegalStateException.class, () -> actions.awaitVisible(target(), budget -> page.waitForTimeout(250)));
        assertEquals("FAIL", actions.snapshot().getFirst().status().name());
    }
    @Test void aSuccessfulCallbackCannotReplaceTheNativeVisibilityCheck() {
        var actions = actions();page.locator("#target").evaluate("e=>e.remove()");
        assertThrows(TimeoutError.class, () -> actions.awaitVisible(target(), budget -> { }));
        assertEquals("FAIL", actions.snapshot().getFirst().status().name());
    }
    private AtfLocator target() {return new AtfLocator(page.locator("#target"), "#target");}
    private PlaywrightActionSequence actions() {
        return new PlaywrightActionSequence(UiActionTrace.noOp(), Duration.ofMillis(150),
                new UiRetryPolicy(3, Duration.ZERO, Set.of(UiRetryOperation.WAIT_FOR_VISIBLE), "owned negative control"));
    }
}
