package atf.ui.playwright;
import atf.testkit.AtfComponentTest;

import atf.core.reporting.contract.AtfReportStatus;
import atf.core.ui.AtfUiEvidenceSource;
import atf.ui.config.UiRetryOperation;
import atf.ui.config.UiRetryPolicy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest

class PlaywrightActionSequenceTest {
    @Test
    void drainsConsecutiveObservationsInOrderAndStartsANewSequence() {
        var actions = new PlaywrightActionSequence();
        actions.observation("Get text", "role=heading[name=Quote]", "heading is visible", "Quote");
        actions.observation("Focus", "role=tab[name=Checklist]", "tab is focused", "focus completed");

        var first = actions.drain();
        var second = actions.drain();

        assertEquals(2, first.size());
        assertEquals(1, first.get(0).sequence());
        assertEquals("Get text", first.get(0).operation());
        assertEquals("Focus", first.get(1).operation());
        assertEquals(0, second.size());
        assertTrue(first.stream().allMatch(action -> action.status() == AtfReportStatus.PASS));
        assertTrue(first.stream().allMatch(action -> action.source() == AtfUiEvidenceSource.OBSERVE));
    }

    @Test
    void keepsActionOrdinalStableAcrossSetupAndBodyDrains() {
        var actions = new PlaywrightActionSequence();

        actions.observation("Navigate", "#app", "application is loaded", "loaded");
        var setup = actions.drain();
        actions.observation("Click", "#submit", "business body starts", "clicked");
        var body = actions.drain();
        actions.observation("Get text", "#confirmation", "confirmation is visible", "confirmed");
        var finalBody = actions.drain();

        assertEquals(1, setup.getFirst().sequence());
        assertEquals(2, body.getFirst().sequence());
        assertEquals(3, finalBody.getFirst().sequence());
    }

    @Test
    void rejectsBlankLocatorTargetsBeforeAnElementActionCanBeRecorded() {
        assertThrows(IllegalArgumentException.class,
                () -> PlaywrightActionSequence.requireLocatorTarget(null));
        assertThrows(IllegalArgumentException.class,
                () -> PlaywrightActionSequence.requireLocatorTarget("  "));
        assertEquals("button[data-filter-value='clear']",
                PlaywrightActionSequence.requireLocatorTarget(" button[data-filter-value='clear'] "));
    }

    @Test
    void rejectsNonEmptyProseBecauseActionsMustExposeTheEffectiveLocator() {
        assertThrows(IllegalArgumentException.class,
                () -> PlaywrightActionSequence.requireLocatorTarget("selected owner detail"));
        assertEquals("details:visible > summary",
                PlaywrightActionSequence.requireLocatorTarget(" details:visible > summary "));
    }

    @Test
    void typedLocatorCarriesTheValidatedSelectorForElementBoundOperations() {
        assertThrows(NullPointerException.class,
                () -> new AtfLocator(null, "#test-view"));
        assertThrows(IllegalArgumentException.class,
                () -> PlaywrightActionSequence.requireLocatorTarget("selected owner detail"));
    }

    @Test
    void configuredStateTimeoutIsValidatedBeforeASequenceCanRun() {
        assertThrows(NullPointerException.class,
                () -> new PlaywrightActionSequence(UiActionTrace.noOp(), null));
        assertThrows(IllegalArgumentException.class,
                () -> new PlaywrightActionSequence(UiActionTrace.noOp(), Duration.ZERO));
        assertTrue(new PlaywrightActionSequence(UiActionTrace.noOp(), Duration.ofSeconds(20))
                .snapshot().isEmpty());
    }

    @Test
    void fillRecordsSafeEvidenceWithoutExposingTheEnteredValue() {
        var entered = new ArrayList<String>();
        var locator = (com.microsoft.playwright.Locator) Proxy.newProxyInstance(
                com.microsoft.playwright.Locator.class.getClassLoader(),
                new Class<?>[]{com.microsoft.playwright.Locator.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "count" -> 1;
                    case "fill" -> {
                        entered.add((String) arguments[0]);
                        yield null;
                    }
                    case "toString" -> "#secret-field";
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        var actions = new PlaywrightActionSequence();
        actions.fill(new AtfLocator(locator, "#secret-field"), "sensitive-value", "field is filled");

        var record = actions.snapshot().getFirst();
        assertEquals("Fill", record.operation());
        assertEquals("value entered: [REDACTED]", record.observed());
        assertEquals("field is filled · value=[REDACTED]", record.expected());
        assertEquals(List.of("sensitive-value"), entered);
        assertTrue(record.observed().contains("value"));
        assertTrue(!record.observed().contains("sensitive-value"));
    }

    @Test
    void sensitiveTextRemainsUsableForBusinessAssertionsButIsRedactedFromEvidence() {
        String privateAddress = "Mr. Ada Lovelace\\n10 Private Street";
        var rows = (com.microsoft.playwright.Locator) Proxy.newProxyInstance(
                com.microsoft.playwright.Locator.class.getClassLoader(),
                new Class<?>[]{com.microsoft.playwright.Locator.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "all" -> List.of();
                    case "toString" -> "tr";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var locator = (com.microsoft.playwright.Locator) Proxy.newProxyInstance(
                com.microsoft.playwright.Locator.class.getClassLoader(),
                new Class<?>[]{com.microsoft.playwright.Locator.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "locator" -> rows;
                    case "innerText" -> privateAddress;
                    case "toString" -> "#address_delivery";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var actions = new PlaywrightActionSequence();

        String actual = actions.getText(new AtfLocator(locator, "#address_delivery"),
                "delivery address is read");

        assertEquals(privateAddress, actual);
        assertTrue(actual.contains("Ada Lovelace"));
        assertEquals("[REDACTED]", actions.snapshot().getFirst().observed());
        assertFalse(actions.snapshot().toString().contains("Ada Lovelace"));
    }

    @Test
    void fillKeepsReplayValueBesideItsTargetForNonSensitiveFields() {
        var locator = (com.microsoft.playwright.Locator) Proxy.newProxyInstance(
                com.microsoft.playwright.Locator.class.getClassLoader(),
                new Class<?>[]{com.microsoft.playwright.Locator.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "count" -> 1;
                    case "fill", "toString" -> method.getName().equals("toString")
                            ? "input[data-test='search-query']" : null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        var actions = new PlaywrightActionSequence();
        actions.fill(new AtfLocator(locator, "input[data-test='search-query']"),
                "pliers", "product search query is entered");

        var record = actions.snapshot().getFirst();
        assertEquals("input[data-test='search-query']", record.target());
        assertEquals("product search query is entered · value=pliers", record.expected());
        assertEquals("value entered: pliers", record.observed());
    }

    @Test
    void fillRedactsPersonalRegistrationFieldsButKeepsTheValuesInTheBrowser() {
        var entered = new ArrayList<String>();
        var actions = new PlaywrightActionSequence();
        var targetsAndValues = List.of(
                List.of("input[id='first_name']", "Ada"),
                List.of("input[id='last_name']", "Lovelace"),
                List.of("input[id='dob']", "1990-01-01"),
                List.of("input[id='postal_code']", "010101"),
                List.of("input[id='mobile_number']", "0700000001"),
                List.of("input[id='zipcode']", "90210"),
                List.of("input[id='house_number']", "10"),
                List.of("input[id='street']", "Secret Street"),
                List.of("input[id='city']", "Private City"),
                List.of("input[id='state']", "Private State"),
                List.of("input[id='phone']", "0700000000"),
                List.of("input[id='email']", "private.person@example.test"),
                List.of("input[id='password']", "Secret!123"),
                List.of("input[name='card-number']", "4111111111111111"),
                List.of("input[autocomplete='cc-number']", "5555555555554444"),
                List.of("input[id='cvc']", "987"),
                List.of("input[name='security-code']", "654"));

        for (List<String> targetAndValue : targetsAndValues) {
            String target = targetAndValue.getFirst();
            String value = targetAndValue.getLast();
            var locator = (com.microsoft.playwright.Locator) Proxy.newProxyInstance(
                    com.microsoft.playwright.Locator.class.getClassLoader(),
                    new Class<?>[]{com.microsoft.playwright.Locator.class},
                    (proxy, method, arguments) -> switch (method.getName()) {
                        case "count" -> 1;
                        case "fill" -> { entered.add((String) arguments[0]); yield null; }
                        case "toString" -> target;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            actions.fill(new AtfLocator(locator, target), value, "registration field is entered");
        }

        var records = actions.snapshot();
        assertEquals(targetsAndValues.stream().map(values -> values.get(1)).toList(), entered);
        assertEquals(targetsAndValues.size(), records.size());
        for (int index = 0; index < records.size(); index++) {
            assertEquals("value entered: [REDACTED]", records.get(index).observed());
            assertEquals("registration field is entered · value=[REDACTED]", records.get(index).expected());
            assertTrue(!records.get(index).observed().contains(entered.get(index)));
        }
    }

    @Test
    void explicitlySensitiveLocatorRedactsValuesEvenWhenSelectorHasNoSensitiveName() {
        String rawValue = "private-payment-value";
        var locator = (com.microsoft.playwright.Locator) Proxy.newProxyInstance(
                com.microsoft.playwright.Locator.class.getClassLoader(),
                new Class<?>[]{com.microsoft.playwright.Locator.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "count" -> 1;
                    case "fill" -> null;
                    case "inputValue" -> rawValue;
                    case "toString" -> "input[data-test='opaque-field']";
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        var actions = new PlaywrightActionSequence();
        var sensitive = new AtfLocator(locator, "input[data-test='opaque-field']").withSensitiveData();
        actions.fill(sensitive, rawValue, "private field is set to " + rawValue);
        actions.expectValue(sensitive, rawValue);

        var records = actions.snapshot();
        assertEquals("value entered: [REDACTED]", records.get(0).observed());
        assertEquals("private field is set to [REDACTED] · value=[REDACTED]",
                records.get(0).expected());
        assertEquals("value equals: [REDACTED]", records.get(1).expected());
        assertTrue(!records.toString().contains(rawValue));
        assertEquals("[REDACTED]", records.get(1).observed());
    }

    @Test
    void sensitiveFailureDoesNotLeakThroughRecordOrDiagnosticTrace() {
        String privateValue = "4111111111111111";
        var events = new ArrayList<UiActionTrace.UiActionEvent>();
        var locator = (com.microsoft.playwright.Locator) Proxy.newProxyInstance(
                com.microsoft.playwright.Locator.class.getClassLoader(),
                new Class<?>[]{com.microsoft.playwright.Locator.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "count" -> 1;
                    case "fill" -> throw new IllegalStateException("browser echoed " + privateValue);
                    case "toString" -> "input[id='card-number']";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var actions = new PlaywrightActionSequence(events::add);

        var failure = assertThrows(IllegalStateException.class, () -> actions.fill(
                new AtfLocator(locator, "input[id='card-number']"), privateValue,
                "payment card " + privateValue + " is entered"));

        var record = actions.snapshot().getFirst();
        assertTrue(record.expected().contains("[REDACTED]"));
        assertTrue(record.observed().contains("IllegalStateException"));
        assertFalse(record.observed().contains(privateValue));
        assertFalse(failure.getMessage().contains(privateValue));
        assertFalse(events.toString().contains(privateValue));
        assertTrue(events.toString().contains("Sensitive UI action failed"));
    }

    @Test
    void nativeOptionAndValueAssertionsRemainTraceable() {
        var selected = new ArrayList<String>();
        var locator = (com.microsoft.playwright.Locator) Proxy.newProxyInstance(
                com.microsoft.playwright.Locator.class.getClassLoader(),
                new Class<?>[]{com.microsoft.playwright.Locator.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "count" -> 1;
                    case "selectOption" -> {
                        selected.add(((com.microsoft.playwright.options.SelectOption) arguments[0]).label);
                        yield List.of("cash-on-delivery");
                    }
                    case "inputValue" -> "2";
                    case "toString" -> "select[data-test='payment-method']";
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        var actions = new PlaywrightActionSequence();
        actions.selectOption(new AtfLocator(locator, "select[data-test='payment-method']"),
                "Cash on Delivery", "demo payment method is selected");
        actions.expectValue(new AtfLocator(locator, "input[data-test='product-quantity']"), "2");

        assertEquals(2, actions.snapshot().size());
        assertEquals("Select option", actions.snapshot().get(0).operation());
        assertEquals("option selected: Cash on Delivery", actions.snapshot().get(0).observed());
        assertEquals("demo payment method is selected · value=Cash on Delivery",
                actions.snapshot().get(0).expected());
        assertEquals("Expect value", actions.snapshot().get(1).operation());
        assertEquals("2", actions.snapshot().get(1).observed());
        assertTrue(selected.getFirst().contains("Cash on Delivery"));
    }

    @Test
    void publishesUiTraceThroughAnInjectedPortWithoutTakingConsoleOwnership() {
        var events = new java.util.ArrayList<UiActionTrace.UiActionEvent>();
        var actions = new PlaywrightActionSequence(events::add);

        actions.observation("Click", "#nav-test", "Tests tab is visible", "Tests tab is visible");

        assertEquals(1, events.size());
        assertEquals(UiActionTrace.Kind.OBSERVATION, events.get(0).kind());
        assertEquals("Click", events.get(0).operation());
        assertEquals("#nav-test", events.get(0).target());
        assertEquals(AtfReportStatus.PASS, events.get(0).status());
    }

    @Test
    void retriesOnlyTheConfiguredReadOnlyActionAndPublishesAnInfoRow() {
        var recorder = new UiActionRecorder();
        recorder.setRetryPolicy(new UiRetryPolicy(2, Duration.ZERO,
                java.util.Set.of(UiRetryOperation.WAIT_FOR_VISIBLE), "DOM stabilization"));
        int[] attempts = {0};

        String result = recorder.run("Wait for visible", "#test-view", "state=Visible", () -> {
            if (attempts[0]++ == 0) throw new IllegalStateException("transient state");
            return "state reached";
        });

        assertEquals("state reached", result);
        assertEquals(3, recorder.snapshot().size());
        assertEquals("Retry attempt", recorder.snapshot().get(0).operation());
        assertEquals(AtfReportStatus.INFO, recorder.snapshot().get(0).status());
        assertTrue(recorder.snapshot().get(0).observed().contains("transient state"));
        assertEquals("Retry", recorder.snapshot().get(1).operation());
        assertEquals(AtfReportStatus.INFO, recorder.snapshot().get(1).status());
        assertEquals("Wait for visible", recorder.snapshot().get(2).operation());
        assertEquals(AtfReportStatus.PASS, recorder.snapshot().get(2).status());
    }

    @Test
    void retryPolicyDoesNotReplayMutatingActions() {
        var recorder = new UiActionRecorder();
        recorder.setRetryPolicy(UiRetryPolicy.readOnly(3, Duration.ZERO));

        assertThrows(IllegalStateException.class,
                () -> recorder.run("Click", "#submit", "dialog opens", () -> {
                    throw new IllegalStateException("click failed");
                }));

        assertEquals(1, recorder.snapshot().size());
        assertEquals("Click", recorder.snapshot().getFirst().operation());
    }
}
