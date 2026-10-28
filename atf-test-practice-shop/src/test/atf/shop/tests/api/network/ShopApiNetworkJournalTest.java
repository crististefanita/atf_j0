package atf.shop.tests.api.network;

import atf.core.http.protocol.ApiUris;
import atf.core.http.request.HttpMethod;
import atf.shop.tests.support.ShopComponentTest;
import atf.reporting.contract.AtfAssertion;
import atf.shop.support.api.ShopApiNetworkJournal;
import atf.shop.support.api.ShopApiEvidenceMapper;
import atf.reporting.ui.network.ReportNetworkAggregateMarkup;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.network.UiNetworkInspector;
import atf.ui.network.UiNetworkObservation;
import atf.shop.api.ShopApiRoutes;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.config.api.ShopApiEvidenceBinding;
import atf.shop.config.ShopValidationProfile;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.time.Instant;

import static atf.shop.tests.api.network.ShopApiJournalFixture.capture;
import static atf.shop.tests.api.network.ShopApiJournalFixture.exchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ShopComponentTest
class ShopApiNetworkJournalTest {

    @Test
    void selectedEvidencePublishesOnlyConfiguredAndValidatedCheckpointsInCaptureOrder() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "product"));
        capture(journal, exchange(HttpMethod.GET.value(), "/session/status", "{}", "{}"));
        capture(journal, exchange(HttpMethod.GET.value(), "/session/status", "{}", "{}"));
        capture(journal, exchange(HttpMethod.GET.value(), "/products/search?q=Widget",
                "", "{\"data\":[{\"id\":\"p1\",\"name\":\"Widget\"}]}"));
        capture(journal, exchange(HttpMethod.GET.value(), ShopApiRoutes.product("p1"),
                "", "{\"id\":\"p1\",\"name\":\"Widget\"}"));

        journal.productReady("Widget", null);

        var selected = journal.selectedEvidence();
        assertEquals(List.of("E3", "E4"), selected.stream()
                .map(evidence -> evidence.trace().eventId()).toList());
        assertTrue(selected.stream().allMatch(evidence ->
                evidence.request().path().startsWith("/products")));
        assertEquals(List.of("/session/status", "/session/status", "/products/search?redacted-query",
                        "/products/p1"), journal.allEvidence().stream()
                .map(evidence -> evidence.request().path()).toList());
        assertEquals(4, journal.allEvidence().size());
    }

    @Test
    void observedValueMappingRetainsTheCapturedPathBeforeInspectorRedaction() {
        String capturedPath = "/products/search?q=Widget";
        var observed = exchange(HttpMethod.GET.value(), capturedPath, "", "{}");

        var evidence = ShopApiEvidenceMapper.toExecution(observed);

        assertEquals(capturedPath, evidence.request().path());
        assertEquals("Widget", evidence.request().query().get("q"));
        assertEquals(capturedPath, observed.path());
    }

    @Test
    void unboundRequestsAreRetainedButNotPublishedRegardlessOfTheirRoute() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "product"));
        capture(journal, exchange(HttpMethod.GET.value(), "/session/status", "{}", "{}"));
        capture(journal, exchange(HttpMethod.GET.value(), "/session/status", "{}", "{}"));

        assertTrue(journal.selectedEvidence().isEmpty());
        assertEquals(List.of("/session/status", "/session/status"), journal.allEvidence().stream()
                .map(evidence -> evidence.request().path()).toList());
        assertTrue(journal.inspectorEvidence().isEmpty());
    }

    @Test
    void orderEvidenceIsSelectedOnlyAtTheFinalOrderCheckpoint() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "order"));
        capture(journal, exchange(HttpMethod.POST.value(), ShopApiRoutes.GUEST_INVOICE_CREATE, "{}",
                "{\"id\":\"order-1\",\"invoice_number\":\"INV-1\",\"total\":24.02}"));

        var assertions = journal.orderCreated(0L, "Thanks for your order! Invoice INV-1");

        assertTrue(assertions.stream().allMatch(assertion -> assertion.passed()));
        assertEquals(List.of("/invoices/guest"), journal.evidence(ShopApiCheckpoint.ORDER).stream()
                .map(evidence -> evidence.request().path()).toList());
        assertEquals("checkout", journal.evidence(ShopApiCheckpoint.ORDER).getFirst()
                .trace().phase());
    }

    @Test
    void authenticatedOrderRouteIsSelectedByTheSameBusinessOperation() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "order"));
        capture(journal, exchange(HttpMethod.POST.value(), ShopApiRoutes.INVOICE_CREATE, "{}",
                "{\"id\":\"order-2\",\"invoice_number\":\"INV-2\",\"total\":24.02}"));

        var assertions = journal.orderCreated(0L, "Thanks for your order! Invoice INV-2");

        assertTrue(assertions.stream().allMatch(assertion -> assertion.passed()));
        assertEquals(List.of(ShopApiRoutes.INVOICE_CREATE), journal.evidence(ShopApiCheckpoint.ORDER).stream()
                .map(evidence -> evidence.request().path()).toList());
        assertEquals("checkout", journal.evidence(ShopApiCheckpoint.ORDER).getFirst()
                .trace().phase());
    }

    @Test
    void orderEvidenceCannotReuseAnOrderCapturedBeforeTheConfirmFence() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "order"));
        capture(journal, exchange(HttpMethod.POST.value(), ShopApiRoutes.GUEST_INVOICE_CREATE, "{}",
                "{\"id\":\"old\",\"invoice_number\":\"INV-OLD\",\"total\":24.02}"));
        long fence = journal.sequenceMarker();

        var assertions = journal.orderCreated(fence,
                "Thanks for your order! Your invoice number is INV-NEW.");

        assertFalse(assertions.stream().anyMatch(AtfAssertion::passed));
        assertTrue(journal.evidence(ShopApiCheckpoint.ORDER).isEmpty());
    }

    @Test
    void registrationCheckpointOwnsOnlyTheObservedPostAndRedactsIdentityBeforeAnyProjection() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "registration"));
        long fence = journal.sequenceMarker();
        capture(journal, exchange("api.practicesoftwaretesting.com", HttpMethod.POST.value(), "/users/register",
                "{\"first_name\":\"Ada\",\"last_name\":\"Lovelace\","
                        + "\"email\":\"ada@owned.example\",\"password\":\"Secret!123\","
                        + "\"street\":\"Secret Street\",\"phone\":\"0700000000\"}",
                "{\"id\":\"user-1\",\"first_name\":\"Ada\",\"email\":\"ada@owned.example\"}"));

        var assertions = journal.registrationConfirmed(fence,
                ApiUris.httpsEndpoint("api.practicesoftwaretesting.com"));
        var evidence = journal.evidence(ShopApiCheckpoint.REGISTRATION).getFirst();
        String published = evidence.request().bodyText() + evidence.response().body();

        assertTrue(assertions.stream().allMatch(AtfAssertion::passed));
        assertEquals("account-setup", evidence.trace().phase());
        assertEquals("/users/register", evidence.request().path());
        for (String personalValue : List.of("Ada", "Lovelace", "ada@owned.example", "Secret!123",
                "Secret Street", "0700000000")) {
            assertFalse(published.contains(personalValue), "leaked value: " + personalValue);
        }
        assertFalse(journal.allEvidence().getFirst().request().bodyText().contains("Ada"));
    }

    @Test
    void registrationThenLoginSelectsEachCheckpointExchangeExactlyOnce() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "registration,login"));
        long registrationFence = journal.sequenceMarker();
        capture(journal, exchange("api.practicesoftwaretesting.com", HttpMethod.POST.value(),
                "/users/register", "{}", "{\"id\":\"user-1\"}"));
        var registration = journal.registrationConfirmed(registrationFence,
                ApiUris.httpsEndpoint("api.practicesoftwaretesting.com"));
        capture(journal, exchange("api.practicesoftwaretesting.com", HttpMethod.POST.value(),
                ShopApiRoutes.LOGIN,
                "{\"email\":\"user@example.test\",\"password\":\"secret\"}",
                "{\"access_token\":\"redacted\"}"));
        var login = journal.loginConfirmed(null, null);

        assertTrue(registration.stream().allMatch(AtfAssertion::passed));
        assertTrue(login.stream().allMatch(AtfAssertion::passed));
        assertEquals(1, journal.evidence(ShopApiCheckpoint.REGISTRATION).size());
        assertEquals(1, journal.evidence(ShopApiCheckpoint.LOGIN).size());
        assertEquals(List.of("E1", "E2"), journal.selectedEvidence().stream()
                .map(evidence -> evidence.trace().eventId()).toList());
        assertEquals(List.of("/users/register", ShopApiRoutes.LOGIN), journal.selectedEvidence().stream()
                .map(evidence -> evidence.request().path()).toList());
    }

    @Test
    void eventIdsAreLocalToIndependentScenarioJournals() {
        var firstScenario = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "login"));
        var secondScenario = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "login"));
        for (var journal : List.of(firstScenario, secondScenario)) {
            capture(journal, exchange(HttpMethod.POST.value(), ShopApiRoutes.LOGIN,
                    "{}", "{\"access_token\":\"redacted\"}"));
            journal.loginConfirmed(null, null);
        }

        assertEquals("E1", firstScenario.selectedEvidence().getFirst().trace().eventId());
        assertEquals("E1", secondScenario.selectedEvidence().getFirst().trace().eventId());
        assertEquals(1, firstScenario.selectedEvidence().size());
        assertEquals(1, secondScenario.selectedEvidence().size());
    }

    @Test
    void ordinaryApiExchangesRedactIdentityFieldsBeforeEnteringTheJournal() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "order"));
        capture(journal, exchange(HttpMethod.POST.value(), ShopApiRoutes.GUEST_INVOICE_CREATE,
                "{\"firstName\":\"Ada\",\"email\":\"ada.private@example.test\","
                        + "\"address\":{\"city\":\"Hidden City\",\"postalCode\":\"10101\"}}",
                "{\"phone\":\"0700000000\",\"result\":\"accepted\"}"));

        var evidence = journal.allEvidence().getFirst();
        String publishedBodies = evidence.request().bodyText() + evidence.response().body();

        for (String personalValue : List.of("Ada", "ada.private@example.test", "Hidden City",
                "10101", "0700000000")) {
            assertFalse(publishedBodies.contains(personalValue), "exchange leaked value: " + personalValue);
        }
    }

    @Test
    void registrationCheckpointFailsClosedWhenMoreThanOnePostWasObserved() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "registration"));
        long fence = journal.sequenceMarker();
        capture(journal, exchange("api.practicesoftwaretesting.com", HttpMethod.POST.value(),
                "/users/register", "{}", "{}"));
        capture(journal, exchange("api.practicesoftwaretesting.com", HttpMethod.POST.value(),
                "/users/register", "{}", "{}"));

        var assertions = journal.registrationConfirmed(fence,
                ApiUris.httpsEndpoint("api.practicesoftwaretesting.com"));

        assertFalse(assertions.getFirst().passed());
        assertFalse(assertions.getLast().passed());
        assertEquals(2, journal.evidence(ShopApiCheckpoint.REGISTRATION).size());
    }

    @Test
    void registrationCheckpointIgnoresPostsToOtherObservedHosts() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "registration"));
        long fence = journal.sequenceMarker();
        capture(journal, exchange("telemetry.test", HttpMethod.POST.value(), "/events", "{}", "{}"));
        capture(journal, exchange("api.practicesoftwaretesting.com", HttpMethod.POST.value(),
                "/dynamic-registration-route", "{}", "{}"));

        var assertions = journal.registrationConfirmed(fence,
                URI.create(ApiUris.httpsUrl(
                        "api.practicesoftwaretesting.com", "/v1")));

        assertTrue(assertions.stream().allMatch(AtfAssertion::passed));
        assertEquals(1, journal.evidence(ShopApiCheckpoint.REGISTRATION).size());
        assertEquals("/dynamic-registration-route", journal.evidence(ShopApiCheckpoint.REGISTRATION)
                .getFirst().request().path());
    }

    @Test
    void genericInspectorAuthClassificationSurvivesTheConsumerEvidenceAdapter() {
        var exchange = exchange(HttpMethod.GET.value(), "/orders", "", "{}")
                .withObservedAuthMethod("bearer");

        var evidence = ShopApiEvidenceMapper.toExecution(exchange);

        assertEquals("bearer", evidence.request().authMethod().wireValue());
    }

    @Test
    void observedExchangeUsesReportingQueryDecoderAndPreservesDecodedValues() {
        var exchange = exchange(HttpMethod.GET.value(), "/orders?state=ready+to+ship&filter=a%2Bb&empty=", "", "{}")
                .withObservedAuthMethod("none");

        var evidence = ShopApiEvidenceMapper.toExecution(exchange);

        assertEquals("ready to ship", evidence.request().query().get("state"));
        assertEquals("a+b", evidence.request().query().get("filter"));
        assertEquals("", evidence.request().query().get("empty"));
    }

    @Test
    void consumerServiceAliasIsInjectedAtTheAdapterBoundaryAndRetainedInTrace() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "product"),
                host -> "shop.test".equals(host) ? "Practice Shop API" : host);
        capture(journal, exchange(HttpMethod.GET.value(), "/products/search?q=Widget",
                "", "{\"data\":[{\"id\":\"p1\",\"name\":\"Widget\"}]}") );
        capture(journal, exchange(HttpMethod.GET.value(), ShopApiRoutes.product("p1"),
                "", "{\"id\":\"p1\",\"name\":\"Widget\"}"));

        journal.productReady("Widget", null);

        assertEquals("Practice Shop API", journal.selectedEvidence().get(0).trace().service());
    }

    @Test
    void requestFailureSurvivesJournalAndRendersAsUnavailableDiagnostic() {
        var journal = new ShopApiNetworkJournal(ShopValidationProfile.UI_API,
                ShopApiEvidenceBinding.fromCsv(ShopValidationProfile.UI_API, "product"));
        var inspector = new UiNetworkInspector(UiNetworkInspectionConfiguration.forHosts(
                List.of("shop.test")));
        journal.attach(inspector);
        inspector.capture(new UiNetworkObservation(1, Instant.EPOCH, HttpMethod.GET.value(),
                ApiUris.httpsUrl("shop.test", ShopApiRoutes.PRODUCT_SEARCH),
                ShopApiRoutes.PRODUCT_SEARCH, -1,
                Map.of(), Map.of(), "", "<unavailable: request failed before response>",
                "none", "none", "shop.test", "UI_BROWSER_REQUEST_FAILED",
                "GET /products/search#body=empty", false, "net::ERR_CONNECTION_RESET"));

        assertTrue(journal.selectedEvidence().isEmpty());
        var evidence = journal.inspectorEvidence();
        assertEquals(1, evidence.size());
        assertEquals("Unavailable", evidence.get(0).response().statusLabel());
        assertEquals("Unavailable", evidence.get(0).attemptHistory().get(0).statusLabel());
        assertEquals("FAILURE", evidence.get(0).attemptHistory().get(0).outcome());
        assertEquals("net::ERR_CONNECTION_RESET", evidence.get(0).attemptHistory().get(0).reason());
        assertEquals("UI_BROWSER_REQUEST_FAILED", evidence.get(0).trace().evidenceSource());
        var aggregate = ReportNetworkInspector.aggregate(evidence, List.of());
        assertEquals(1, aggregate.transport().failed());
        assertEquals("net::ERR_CONNECTION_RESET", aggregate.diagnostics().get(0).signal());
        String markup = ReportNetworkAggregateMarkup.disclosure(aggregate);
        assertTrue(markup.contains("Status unavailable"));
        assertTrue(markup.contains("net::ERR_CONNECTION_RESET"));
    }

}
