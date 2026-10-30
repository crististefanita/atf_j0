package atf.ui.playwright;

import atf.core.reporting.contract.AtfReportStatus;
import atf.testkit.AtfComponentTest;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiRetryPolicy;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/** Native strict actions own one timeout and re-resolve asynchronous DOM targets before activation. */
@AtfComponentTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
class PlaywrightLocatorReadinessComponentTest {
    enum Action { CLICK, FILL, SELECT }
    private HttpServer server;
    private PlaywrightUiSession session;
    private String origin;

    @BeforeAll void openOwnedFixture() throws Exception {
        byte[] document;
        try (var input = Objects.requireNonNull(getClass().getResourceAsStream(
                "/atf/ui/playwright/locator-readiness.html"))) {
            document = input.readAllBytes();
        }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, document.length);
                exchange.getResponseBody().write(document);
            }
        });
        server.start();
        origin = "http://127.0.0.1:" + server.getAddress().getPort();
        try {
            session = PlaywrightUiSession.open(new UiBrowserConfiguration(URI.create("about:blank"),
                    "chromium", true, Duration.ofMillis(850), Duration.ofSeconds(10), 1280, 800, 100, true));
        } catch (RuntimeException | Error failure) {
            server.stop(0);
            throw failure;
        }
    }

    @AfterAll void closeOwnedFixture() {
        try { if (session != null) session.close(); }
        finally { if (server != null) server.stop(0); }
    }

    @ParameterizedTest @EnumSource(Action.class)
    void absentTargetCanBecomeUniqueBeforeExactlyOneActivation(Action action) {
        load(action, "delayed");
        assertEquals(0, session.page().locator("#target").count());
        var actions = actions();
        activate(action, actions);
        assertSuccessfulActivation(action, actions);
    }

    @ParameterizedTest @EnumSource(Action.class)
    void permanentAbsencePreservesNativeTimeoutWithoutMutationRetry(Action action) {
        load(action, "missing");
        String document = session.page().url();
        var actions = actions();
        var failure = assertThrows(TimeoutError.class, () -> activate(action, actions));
        assertTrue(failure.getMessage().contains("850ms"), failure.getMessage());
        assertEquals(document, session.page().url());
        assertEquals("0", session.page().locator("#activations").innerText());
        assertFailedRecord(actions, "#target");
        assertTrue(actions.snapshot().getFirst().observed().contains("TimeoutError"));
    }

    @ParameterizedTest @EnumSource(Action.class)
    void ambiguousTargetIsNeverReducedToTheFirstMatch(Action action) {
        load(action, "ambiguous");
        assertEquals(2, session.page().locator("#target").count());
        var actions = actions();
        var failure = assertThrows(PlaywrightException.class, () -> activate(action, actions), this::diagnostic);
        assertTrue(failure.getMessage().contains("strict mode violation"), failure.getMessage());
        assertEquals("0", session.page().locator("#activations").innerText());
        assertFailedRecord(actions, "#target");
    }

    @ParameterizedTest @EnumSource(value = Action.class, names = {"CLICK", "FILL"})
    void disabledTargetCanBeReplacedBeforeTheSingleActivation(Action action) {
        load(action, "replacement");
        var actions = actions();
        activate(action, actions);
        assertSuccessfulActivation(action, actions);
        assertEquals(0, ((Number) session.page().evaluate("window.oldActivations")).intValue());
    }

    @ParameterizedTest @EnumSource(Action.class)
    void locatorResolvesTheNewDocumentBeforeActivation(Action action) {
        load(action, "document");
        assertEquals(0, session.page().locator("#target").count());
        var actions = actions();
        activate(action, actions);
        assertSuccessfulActivation(action, actions);
        assertTrue(session.page().url().startsWith(origin + "/ready?"));
    }

    @ParameterizedTest @EnumSource(value = Action.class, names = {"CLICK", "FILL"})
    void replacementThatBecomesAmbiguousStillDispatchesNothing(Action action) {
        load(action, "ambiguous-replacement");
        var actions = actions();
        var failure = assertThrows(PlaywrightException.class, () -> activate(action, actions), this::diagnostic);
        assertTrue(failure.getMessage().contains("strict mode violation"), failure.getMessage());
        assertEquals("0", session.page().locator("#activations").innerText());
        assertEquals(0, ((Number) session.page().evaluate("window.oldActivations")).intValue());
        assertFailedRecord(actions, "#target");
    }

    @ParameterizedTest @ValueSource(strings = {"replacement", "ambiguous-replacement"})
    void selectReplacementIsObservedBeforeOneActivationOrStrictRefusal(String mode) {
        load(Action.SELECT, mode);
        var actions = actions();
        if (mode.equals("replacement")) {
            activate(Action.SELECT, actions);
            assertSuccessfulActivation(Action.SELECT, actions);
        } else {
            var failure = assertThrows(PlaywrightException.class, () -> activate(Action.SELECT, actions));
            assertTrue(failure.getMessage().contains("strict mode violation"), failure.getMessage());
            assertEquals("0", session.page().locator("#activations").innerText());
            assertTrue(session.page().locator("#target").all().stream().allMatch(target -> target.inputValue().equals("red")));
            assertFailedRecord(actions, "#target");
        }
        assertEquals(0, ((Number) session.page().evaluate("window.oldActivations")).intValue());
    }

    private void load(Action action, String mode) {
        session.page().navigate(origin + "/initial?kind=" + action + "&mode=" + mode);
    }

    private PlaywrightActionSequence actions() {
        return new PlaywrightActionSequence(UiActionTrace.noOp(), Duration.ofMillis(850),
                UiRetryPolicy.readOnly(3, Duration.ZERO));
    }

    private void activate(Action action, PlaywrightActionSequence actions) {
        var target = new AtfLocator(session.page().locator("#target"), "#target");
        switch (action) {
            case CLICK -> actions.click(target, "one trusted activation");
            case FILL -> actions.fill(target, "configured value", "one input event");
            case SELECT -> actions.selectOption(target, "Blue", "one selection event");
        }
    }

    private void assertSuccessfulActivation(Action action, PlaywrightActionSequence actions) {
        assertEquals("1", session.page().locator("#activations").innerText(), this::diagnostic);
        assertEquals(1, actions.snapshot().size());
        assertEquals(AtfReportStatus.PASS, actions.snapshot().getFirst().status());
        assertEquals("#target", actions.snapshot().getFirst().target());
        switch (action) {
            case CLICK -> assertEquals(true, session.page().evaluate("window.trustedActivation"));
            case FILL -> assertEquals("configured value", session.page().locator("#target").inputValue());
            case SELECT -> assertEquals("blue", session.page().locator("#target").inputValue());
        }
    }

    private static void assertFailedRecord(PlaywrightActionSequence actions, String target) {
        assertEquals(1, actions.snapshot().size(), "no mutating action replay or synthetic retry rows");
        assertEquals(AtfReportStatus.FAIL, actions.snapshot().getFirst().status());
        assertEquals(target, actions.snapshot().getFirst().target());
    }

    private String diagnostic() {
        return String.valueOf(session.page().evaluate("""
                JSON.stringify({url:location.href,count:document.querySelector('#activations').textContent,
                  oldActivations:window.oldActivations,targets:[...document.querySelectorAll('#target')]
                    .map(e=>({tag:e.tagName,disabled:e.disabled,value:e.value}))})
                """));
    }
}
