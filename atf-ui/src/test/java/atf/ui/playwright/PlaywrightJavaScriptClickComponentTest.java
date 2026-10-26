package atf.ui.playwright;

import atf.core.reporting.contract.AtfReportStatus;
import atf.core.ui.AtfUiEvidenceSource;
import atf.testkit.AtfComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryPolicy;
import com.microsoft.playwright.PlaywrightException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Neutral platform fixtures: no advertisement, consumer URL or AutoEx business. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class PlaywrightJavaScriptClickComponentTest {
    private PlaywrightUiSession session;
    private String fixtureUrl;

    @BeforeAll
    void openBrowser() throws IOException {
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream(
                "/atf/ui/playwright/javascript-click.html"), "component HTML fixture")) {
            fixtureUrl = "data:text/html;base64," + Base64.getEncoder().encodeToString(input.readAllBytes());
        }
        var configuration = new UiBrowserConfiguration(URI.create("about:blank"), "chromium", true,
                Duration.ofMillis(500), Duration.ofSeconds(10), 1440, 1000, 70, true);
        session = PlaywrightUiSession.open(configuration);
    }

    @BeforeEach
    void loadIsolatedDocument() {
        session.page().navigate(fixtureUrl);
    }

    @AfterAll
    void closeOwnedBrowser() {
        if (session != null) session.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"#embedded", "#isolated"})
    void explicitActivationWorksInFramesAt70AndRecordsItsNonPointerNature(String frameSelector) {
        var control = session.page().frameLocator(frameSelector).locator("button");
        var actions = session.newActionSequence();

        actions.clickWithJavaScript(new AtfLocator(control, frameSelector + " >> button"),
                "embedded control changes state", () -> assertEquals("activated:false", control.innerText()));

        var action = actions.snapshot().getFirst();
        assertEquals("0.7", session.page().evaluate("getComputedStyle(document.documentElement).zoom"));
        if (frameSelector.equals("#isolated")) {
            assertNull(session.page().evaluate("document.querySelector('#isolated').contentDocument"),
                    "the sandboxed frame has an opaque origin, inaccessible to the parent document");
        }
        assertEquals(1, actions.snapshot().size());
        assertEquals("JavaScript click", action.operation());
        assertEquals(AtfUiEvidenceSource.ACTION, action.source());
        assertEquals(AtfReportStatus.PASS, action.status());
        assertTrue(action.observed().contains("not a trusted pointer click"));
        assertTrue(action.durationMillis() > 0);
    }

    @Test
    void ordinaryClickStillUsesTrustedPointerInput() {
        var actions = session.newActionSequence();
        actions.click(element("#visible"), "normal pointer activation");

        assertEquals("true", session.page().locator("#result").innerText());
        assertEquals("Click", actions.snapshot().getFirst().operation());
    }

    @Test
    void ordinaryClickNeverFallsBackToJavaScriptWhenThePointerIsIntercepted() {
        session.page().locator("#overlay").evaluate("element => element.hidden = false");
        var actions = session.newActionSequence();

        assertThrows(PlaywrightException.class, () -> actions.click(element("#visible"), "blocked pointer"));

        assertEquals("0", session.page().locator("#count").innerText());
        assertEquals(1, actions.snapshot().size());
        assertEquals("Click", actions.snapshot().getFirst().operation());
        assertEquals(AtfReportStatus.FAIL, actions.snapshot().getFirst().status());
    }

    @ParameterizedTest
    @MethodSource("invalidTargets")
    void invalidTargetsFailWithoutDispatchOrFalsePass(String selector, Class<? extends Throwable> failureType) {
        var actions = session.newActionSequence();

        assertThrows(failureType, () -> actions.clickWithJavaScript(element(selector),
                "activation must be possible", () -> { }));

        assertEquals("0", session.page().locator("#count").innerText());
        assertEquals(1, actions.snapshot().size());
        assertEquals(AtfReportStatus.FAIL, actions.snapshot().getFirst().status());
    }

    @Test
    void failedPostconditionFailsTheActionAndNeverReplaysItsSideEffect() {
        var actions = new PlaywrightActionSequence(UiActionTrace.noOp(), Duration.ofMillis(500),
                UiRetryPolicy.readOnly(3, Duration.ZERO));
        var failure = new AssertionError("business outcome is absent");

        var actual = assertThrows(AssertionError.class, () -> actions.clickWithJavaScript(element("#visible"),
                "the required outcome is observed", () -> { throw failure; }));

        assertSame(failure, actual);
        assertEquals("1", session.page().locator("#count").innerText());
        assertEquals(1, actions.snapshot().size(), "a mutating DOM activation must not be retried");
        assertEquals(AtfReportStatus.FAIL, actions.snapshot().getFirst().status());
        assertTrue(actions.snapshot().getFirst().observed().contains("business outcome is absent"));
    }

    @Test
    void callerMustSupplyAPostconditionBeforeAnySideEffect() {
        var actions = session.newActionSequence();

        assertThrows(NullPointerException.class,
                () -> actions.clickWithJavaScript(element("#visible"), "must validate", null));

        assertEquals("0", session.page().locator("#count").innerText());
        assertTrue(actions.snapshot().isEmpty());
    }

    @Test
    void sensitiveScriptErrorsAreRedactedFromTheRecordAndPropagatedFailure() {
        var actions = session.newActionSequence();
        var failure = assertThrows(IllegalStateException.class,
                () -> actions.clickWithJavaScript(element("#password-close"), "sensitive activation", () -> { }));

        assertFalse(failure.toString().contains("synthetic-private-activation-value"));
        assertFalse(actions.snapshot().toString().contains("synthetic-private-activation-value"));
        assertEquals(AtfReportStatus.FAIL, actions.snapshot().getFirst().status());
    }

    private AtfLocator element(String selector) {
        return new AtfLocator(session.page().locator(selector), selector);
    }

    private static Stream<Arguments> invalidTargets() {
        return Stream.of(
                Arguments.of("#missing", AssertionError.class),
                Arguments.of(".ambiguous", AssertionError.class),
                Arguments.of("#hidden", PlaywrightException.class),
                Arguments.of("#disabled", IllegalStateException.class),
                Arguments.of("#script-error", PlaywrightException.class));
    }
}
