package atf.autoex.runtime;
import atf.reporting.ui.timing.UiPhaseExecution;

import atf.core.http.request.HttpHeader;
import atf.core.http.response.ApiExecutionResult;
import atf.reporting.api.ReportModelAdapters;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfGherkinKeyword;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.failure.UiCheckpointFailure;
import atf.reporting.ui.network.ReportNetworkCaptureStatus;
import atf.reporting.ui.network.ReportNetworkInspector;
import atf.reporting.ui.validation.UiApiValidationEvidence;
import atf.ui.audit.UiScreenAudit;
import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiNetworkSettlementPolicy;
import atf.ui.config.UiScreenValidationConfiguration;
import atf.ui.network.UiNetworkObservation;
import atf.ui.playwright.AtfLocator;
import atf.ui.playwright.PlaywrightActionSequence;
import atf.ui.playwright.PlaywrightUiSession;
import atf.ui.playwright.UiScreenValidation;
import atf.autoex.config.AutoExConfiguration;
import atf.autoex.support.account.AutoExAccountLifecycle;
import atf.autoex.runtime.navigation.AutoExReadOnlyNavigation;
import atf.autoex.runtime.catalog.AutoExCatalogSearch;
import atf.autoex.locator.AutoExRoutes;
import atf.autoex.locator.AutoExSelectors;
import atf.autoex.support.browser.AutoExPrivacyConsentHandler;
import atf.autoex.support.browser.AutoExVignetteHandler;
import atf.autoex.support.catalog.AutoExCatalogSnapshot;
import atf.autoex.support.journey.AutoExPurchaseSession;
import atf.autoex.ui.AutoExAccountPagePort;
import atf.autoex.support.trace.AutoExTraceStep;
import com.microsoft.playwright.Page;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static atf.core.http.request.HttpMethod.GET;
import static atf.core.http.request.MediaTypes.APPLICATION_JSON;
import static atf.ui.network.UiNetworkCapturePolicy.ResourceKind.XHR;
import static atf.ui.network.UiNetworkCapturePolicy.ResourceKind.FETCH;
import static atf.autoex.api.AutoExApiJsonField.NAME;
import static atf.autoex.api.AutoExApiJsonField.PRICE;
import static atf.autoex.api.AutoExApiJsonField.PRODUCTS;
import static java.util.stream.Collectors.joining;

/** Caller-owned AutoEx browser/report runtime; it has no test framework dependency. */
public final class AutoExE2eRun implements AutoExPurchaseSession {
    private static final String NETWORK_SUMMARY_TITLE = "API network inspector — journey summary";
    private final AutoExConfiguration configuration;
    private final PlaywrightUiSession browser;
    private final UiExtentReportSession.UiScenario scenario;
    private final UiExtentReportSession.UiPhase phase;
    private final UiPhaseExecution execution;
    private final ReportEvidenceProfile evidenceProfile;
    private final boolean apiEnabled;
    private final AutoExRunLifecycle lifecycle;
    private final atf.ui.playwright.recovery.UiBackendRecovery backendRecovery;
    private final AutoExReadOnlyNavigation storeNavigation;
    private final AutoExAccountLifecycle accountLifecycle = new AutoExAccountLifecycle();
    private final List<ReportNetworkInspector.Step> networkSteps = new ArrayList<>();
    private final PlaywrightActionSequence actions;
    private int networkCursor;
    private int causalNetworkBaseline;
    private final AutoExVignetteHandler vignetteHandler = new AutoExVignetteHandler();
    private final AutoExPrivacyConsentHandler privacyConsentHandler = new AutoExPrivacyConsentHandler();
    private AutoExAccountPagePort.AccountCleanup failureCleanup;
    private boolean finished;

    public AutoExE2eRun(AutoExConfiguration configuration, PlaywrightUiSession browser,
                         UiExtentReportSession.UiScenario scenario,
                         UiExtentReportSession.UiPhase phase, ReportEvidenceProfile evidenceProfile,
                         boolean apiEnabled, AutoExRunLifecycle lifecycle) {
        this.configuration = configuration;
        this.browser = browser;
        this.scenario = scenario;
        this.phase = phase;
        this.execution = phase.measureExecution();
        this.evidenceProfile = evidenceProfile;
        this.apiEnabled = apiEnabled;
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.actions = browser.newActionSequence();
        this.backendRecovery = atf.autoex.runtime.recovery.AutoExBackendRecovery.open(browser.page(), configuration, actions);
        this.storeNavigation = new AutoExReadOnlyNavigation(browser.page(), actions,
                configuration.baseUrl(), configuration.storeAvailabilityPolicy());
    }

    public static AutoExConfiguration withNetworkInspection(AutoExConfiguration source) {
        return withNetworkInspection(source, List.of());
    }

    /** Enables bounded browser inspection and optionally includes selected document routes. */
    public static AutoExConfiguration withNetworkInspection(AutoExConfiguration source,
                                                            Collection<String> documentRoutes) {
        UiBrowserConfiguration current = source.browser();
        var capturePolicy = current.networkInspection().capturePolicy().includingOtherRequests();
        if (documentRoutes != null && !documentRoutes.isEmpty()) {
            capturePolicy = capturePolicy.includingDocumentsFor(documentRoutes);
        }
        var network = current.networkInspection().withEnabled(true)
                .withCapturePolicy(capturePolicy)
                .withSettlementPolicy(UiNetworkSettlementPolicy.bounded(
                        Duration.ofSeconds(5), Duration.ofMillis(50)));
        var browser = new UiBrowserConfiguration(current.baseUrl(), current.browser(), current.headless(),
                current.timeout(), current.navigationTimeout(), current.viewportWidth(), current.viewportHeight(),
                current.zoomPercent(), current.screenshotOnFailure(), current.retryPolicy(), network, current.recovery());
        return new AutoExConfiguration(source.baseUrl(), source.e2eEnabled(), browser,
                source.uiReportBaseDirectory(), source.catalog(), source.registration(), source.payment(),
                source.storeAvailabilityPolicy(), source.evidence());
    }

    public AutoExConfiguration configuration() { return configuration; }
    public atf.ui.playwright.recovery.UiBackendRecovery backendRecovery() { return backendRecovery; }
    public AutoExAccountLifecycle accountLifecycle() { return accountLifecycle; }
    public Page page() { return browser.page(); }
    public String currentUrl() { return page().url(); }

    /** Reads the configured product through the site's real, read-only catalog API. */
    @SuppressWarnings("unchecked")
    public AutoExCatalogSnapshot inspectCatalogApi(String productName) {
        URI endpoint = AutoExRoutes.PRODUCTS_API.resolve(configuration.baseUrl());
        Object result = page().evaluate("""
                async request => {
                  const response = await fetch(request.url, { headers: request.headers });
                  return { status: response.status, body: await response.json() };
                }
                """, Map.of("url", endpoint.toString(),
                        "headers", Map.of(HttpHeader.ACCEPT.value(), APPLICATION_JSON)));
        if (!(result instanceof Map<?, ?> response)
                || !(response.get("status") instanceof Number status)
                || !(response.get("body") instanceof Map<?, ?> body)
                || !(body.get(PRODUCTS.path()) instanceof List<?> products)) {
            throw new IllegalStateException("Catalog API returned an unexpected response shape");
        }
        String price = products.stream().filter(Map.class::isInstance)
                .map(Map.class::cast)
                .filter(product -> productName.equals(product.get(NAME.path())))
                .map(product -> Objects.toString(product.get(PRICE.path()), ""))
                .findFirst().orElse("");
        return new AutoExCatalogSnapshot(status.intValue(), products.size(), price);
    }

    public void navigate(URI uri) {
        actions.navigate(page(), Objects.requireNonNull(uri, "uri"), "requested page is loaded");
        storeNavigation.recoverIfBusy();
    }

    public void click(String selector, String expected) {
        prepareUiInteraction();
        actions.click(locator(selector), expected);
    }

    @Override public void submitSearch(String query, String expectedProduct) {
        prepareUiInteraction();
        new AutoExCatalogSearch(page(), actions, configuration.baseUrl())
                .submit(query, expectedProduct);
    }

    public void clickFirst(String selector, String expected) {
        prepareUiInteraction();
        actions.click(new AtfLocator(page().locator(selector).first(), selector), expected);
    }

    public void clickLast(String selector, String expected) {
        prepareUiInteraction();
        actions.click(new AtfLocator(page().locator(selector).last(), selector), expected);
    }

    public void fill(String selector, String value, String expected) {
        prepareUiInteraction();
        actions.fill(locator(selector), value, expected);
    }

    public String text(String selector, String expected) {
        prepareUiInteraction();
        return actions.getText(locator(selector), expected);
    }

    public void waitForText(String selector, String expectedText, String expected) {
        prepareUiInteraction();
        actions.waitForText(page(), locator(selector), expectedText, expected);
    }

    public void waitForVisible(String selector) {
        prepareUiInteraction();
        actions.waitForVisible(locator(selector));
    }

    public String textFirst(String selector, String expected) {
        prepareUiInteraction();
        return actions.getText(new AtfLocator(page().locator(selector).first(), selector + " (first match)"), expected);
    }

    public void select(String selector, String label, String expected) {
        prepareUiInteraction();
        actions.selectOption(locator(selector), label, expected);
    }

    public boolean visible(String selector) {
        prepareUiInteraction();
        return page().locator(selector).isVisible();
    }

    public int count(String selector) {
        prepareUiInteraction();
        return page().locator(selector).count();
    }

    public String firstAttribute(String selector, String attribute) {
        prepareUiInteraction();
        return page().locator(selector).first().getAttribute(attribute);
    }

    private void checkpoint(AtfGherkinKeyword keyword, String title, List<AtfAssertion> assertions) {
        prepareUiInteraction();
        boolean passed = assertions.stream().allMatch(AtfAssertion::passed);
        String screenshot = evidenceProfile.captures(ReportScreenshotPolicy.CapturePoint.CHECKPOINT)
                ? capture(title) : "";
        UiApiValidationEvidence apiValidation = apiEnabled
                ? networkValidationSinceCursor() : UiApiValidationEvidence.empty();
        var actionEvidence = ReportModelAdapters.uiActions(actions.drain());
        AtfReportStatus status = passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL;
        phase.step(keyword, title, currentUrl(), screenshot, assertions, actionEvidence,
                apiValidation, ReportScreenshotPolicy.CapturePoint.CHECKPOINT, status);
        if (apiEnabled) {
            networkSteps.add(ReportNetworkInspector.Step.from(keyword.text(), title, assertions, exchangeEvidence(apiValidation)));
        }
        if (!passed) throw new UiCheckpointFailure("UI checkpoint failed: " + title
                + ". Failed assertions: " + failedAssertionSummary(assertions), assertions);
    }

    public void checkpoint(AutoExTraceStep step, List<AtfAssertion> assertions) {
        checkpoint(step.keyword(), step.title(), assertions);
    }

    /** API checks own their actual catalog exchange; they are never UI assertions. */
    public void apiCheckpoint(AutoExTraceStep step, List<AtfAssertion> assertions) {
        String catalogPath = AutoExRoutes.PRODUCTS_API.path();
        browser.networkInspector().awaitAfter(networkCursor,
                observation -> GET.value().equals(observation.method())
                        && catalogPath.equals(observation.path()), null);
        UiApiValidationEvidence captured = networkValidationSinceCursor();
        boolean found = captured.exchanges().stream()
                .anyMatch(exchange -> catalogPath.equals(exchange.evidence().request().path()));
        List<AtfAssertion> correlations = found ? List.of() : List.of(
                new AtfAssertion("Catalog API evidence", catalogPath, "not observed", false));
        var apiValidation = new UiApiValidationEvidence(correlations, captured.exchanges().stream()
                .map(exchange -> catalogPath.equals(exchange.evidence().request().path())
                        ? new UiApiValidationEvidence.Exchange(exchange.evidence(), assertions)
                        : exchange).toList());
        phase.step(step.keyword(), step.title(), currentUrl(), "", List.of(), List.of(),
                apiValidation, ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.INFO);
        if (apiValidation.status() == AtfReportStatus.FAIL) {
            throw new AssertionError("API checkpoint failed: " + step.title()
                    + ". Failed assertions: " + failedAssertionSummary(assertions));
        }
    }

    private static List<ReportExecutionEvidence> exchangeEvidence(UiApiValidationEvidence validation) {
        return validation.exchanges().stream().map(UiApiValidationEvidence.Exchange::evidence).toList();
    }

    /** Publishes the terminal state; FINAL_ONLY creates exactly this one screenshot. */
    public void finalScreenshot(String title, List<AtfAssertion> assertions) {
        prepareUiInteraction();
        boolean passed = assertions.stream().allMatch(AtfAssertion::passed);
        String screenshot = evidenceProfile.captures(ReportScreenshotPolicy.CapturePoint.FINAL)
                ? capture(title) : "";
        UiScreenAudit.record(actions, title, currentUrl(), assertions.size(),
                new UiScreenValidation.Evidence(title, screenshot, !screenshot.isBlank()), passed);
        UiApiValidationEvidence apiValidation = apiEnabled
                ? networkValidationSinceCursor() : UiApiValidationEvidence.empty();
        phase.uiValidation(title, currentUrl(), screenshot, assertions,
                ReportModelAdapters.uiActions(actions.drain()), apiValidation,
                ReportScreenshotPolicy.CapturePoint.FINAL,
                passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
        if (apiEnabled) {
            networkSteps.add(ReportNetworkInspector.Step.from(AtfGherkinKeyword.THEN.text(),
                    title, assertions, exchangeEvidence(apiValidation)));
        }
        if (!passed) throw new UiCheckpointFailure("Final screen validation failed: " + title
                + ". Failed assertions: " + failedAssertionSummary(assertions), assertions);
    }

    public void finalScreenshot(AutoExTraceStep step, List<AtfAssertion> assertions) {
        finalScreenshot(step.title(), assertions);
    }

    private String capture(String title) {
        return execution.capture(() -> UiScreenValidation.capture(page(), UiScreenValidationConfiguration.defaults(), title,
                List.of(page().locator(AutoExSelectors.SENSITIVE_FIELDS))).base64Png());
    }

    private UiApiValidationEvidence networkValidationSinceCursor() {
        var inspector = browser.networkInspector();
        var observations = inspector.observationsSince(networkCursor);
        networkCursor += observations.size();
        return new UiApiValidationEvidence(List.of(), observations.stream()
                .map(AutoExObservedExchangeMapper::map)
                .map(evidence -> new UiApiValidationEvidence.Exchange(evidence, List.of())).toList());
    }

    public void onFailureCleanup(AutoExAccountPagePort.AccountCleanup cleanup) {
        failureCleanup = Objects.requireNonNull(cleanup, "cleanup");
    }

    public void clearFailureCleanup() { failureCleanup = null; }

    public void finish(Throwable testFailure) throws Exception {
        if (finished) return;
        finished = true;
        execution.finishExecution();
        Throwable terminalFailure = testFailure;
        boolean cleanupWasPending = failureCleanup != null;
        if (testFailure == null && cleanupWasPending) {
            terminalFailure = new AssertionError(
                    "E2E completed without explicitly deleting its disposable account");
        }

        terminalFailure = lifecycle.finish(terminalFailure, new AutoExRunLifecycle.Completion(
                () -> {
                    if (failureCleanup != null) {
                        try { failureCleanup.run(); }
                        finally { failureCleanup = null; }
                    }
                },
                () -> {
                    if (apiEnabled) {
                        phase.networkInspector(NETWORK_SUMMARY_TITLE,
                                causalApiObservations().stream()
                                        .map(AutoExObservedExchangeMapper::map).toList(), networkSteps,
                                captureCoverage());
                    }
                }, this::publishFailure, () -> { try (browser) { backendRecovery.close(); } },
                failed -> phase.complete(failed ? AtfReportStatus.FAIL : AtfReportStatus.PASS),
                failed -> scenario.complete(failed ? AtfReportStatus.FAIL : AtfReportStatus.PASS)));
        if (testFailure == null && terminalFailure != null) {
            if (terminalFailure instanceof Exception exception) throw exception;
            if (terminalFailure instanceof Error error) throw error;
            throw new IllegalStateException("Automation Exercise E2E teardown failed", terminalFailure);
        }
    }

    private void publishFailure(Throwable failure) {
        String screenshot = "";
        if (evidenceProfile.captures(ReportScreenshotPolicy.CapturePoint.FAILURE)) {
            try {
                screenshot = execution.capture(() -> UiScreenValidation.capture(page(), UiScreenValidationConfiguration.defaults(),
                        "Failure state — sensitive fields masked",
                        List.of(page().locator(AutoExSelectors.SENSITIVE_FIELDS))).base64Png());
            } catch (RuntimeException | Error captureFailure) {
                failure.addSuppressed(captureFailure);
            }
        }
        try {
            phase.failureEvidence("Automation Exercise UI E2E failure", currentUrl(),
                    ReportModelAdapters.uiActions(actions.drain()), failure, screenshot);
        } catch (RuntimeException | Error reportFailure) {
            failure.addSuppressed(reportFailure);
        }
    }

    public boolean dismissGoogleVignetteIfPresent() {
        return vignetteHandler.dismissIfPresent(page(), actions);
    }

    public boolean dismissPrivacyPromptIfPresent() {
        return privacyConsentHandler.rejectOptionalProcessingIfPrompted(page(), actions);
    }

    public void markCausalApiTrafficBaseline() {
        causalNetworkBaseline = browser.networkInspector().observationCursor();
    }
    public AutoExPurchaseSession.CartMutationObservation awaitCartExchange(String productId) {
        if (!apiEnabled) throw new IllegalStateException("Cart exchange observation requires UI_API evidence");
        String route = AutoExRoutes.ADD_TO_CART.path() + productId;
        var observed = browser.networkInspector().awaitAfter(networkCursor,
                observation -> isCartExchange(observation, route), null);
        return observed.map(exchange -> new AutoExPurchaseSession.CartMutationObservation(
                        productId, true, exchange.status(), exchange.failureReason(), exchange.sequence()))
                .orElseGet(() -> new AutoExPurchaseSession.CartMutationObservation(productId, false,
                        -1, "no settled matching GET XHR/FETCH within the bounded wait", -1));
    }

    /** Publishes transport and modal outcomes together under the click's causal checkpoint. */
    public void cartAddCheckpoint(AutoExTraceStep step,
                                  AutoExPurchaseSession.CartMutationObservation mutation,
                                  List<AtfAssertion> uiAssertions) {
        if (!apiEnabled) throw new IllegalStateException("Cart API checkpoint requires UI_API evidence");
        Objects.requireNonNull(step, "step");
        Objects.requireNonNull(mutation, "mutation");
        List<AtfAssertion> safeUiAssertions = List.copyOf(uiAssertions == null ? List.of() : uiAssertions);
        prepareUiInteraction();

        String route = AutoExRoutes.ADD_TO_CART.path() + mutation.productId();
        List<UiNetworkObservation> observations = browser.networkInspector().observationsSince(networkCursor);
        networkCursor += observations.size();
        var matchingExchange = observations.stream()
                .filter(observation -> isCartExchange(observation, route))
                .filter(observation -> mutation.responseObserved()
                        && observation.sequence() == mutation.sequence())
                .findFirst();
        String actual = !mutation.responseObserved()
                ? mutation.diagnostic()
                : matchingExchange.map(exchange -> exchange.status() < 0
                        ? "request failed before response: " + exchange.failureReason()
                        : "HTTP " + exchange.status())
                .orElse("the observed cart exchange is not present in this checkpoint window");
        boolean transportPassed = matchingExchange.map(exchange -> exchange.status() >= 200
                && exchange.status() < 300).orElse(false);
        var transportAssertion = new AtfAssertion("Cart add response", "HTTP 2xx from GET " + route,
                actual, transportPassed);
        var apiValidation = new UiApiValidationEvidence(
                matchingExchange.isPresent() ? List.of() : List.of(transportAssertion),
                observations.stream().map(observation -> {
                    var evidence = AutoExObservedExchangeMapper.map(observation);
                    return new UiApiValidationEvidence.Exchange(evidence,
                            matchingExchange.map(exchange -> exchange.sequence() == observation.sequence())
                                    .orElse(false) ? List.of(transportAssertion) : List.of());
                }).toList());

        boolean uiPassed = safeUiAssertions.stream().allMatch(AtfAssertion::passed);
        AtfReportStatus status = AtfReportStatus.worst(
                uiPassed ? AtfReportStatus.PASS : AtfReportStatus.FAIL, apiValidation.status());
        String screenshot = evidenceProfile.captures(ReportScreenshotPolicy.CapturePoint.CHECKPOINT)
                ? capture(step.title()) : "";
        var actionEvidence = ReportModelAdapters.uiActions(actions.drain());
        phase.step(step.keyword(), step.title(), currentUrl(), screenshot, safeUiAssertions,
                actionEvidence, apiValidation, ReportScreenshotPolicy.CapturePoint.CHECKPOINT,
                uiPassed ? AtfReportStatus.PASS : AtfReportStatus.FAIL);
        var allAssertions = new ArrayList<AtfAssertion>(safeUiAssertions);
        allAssertions.add(transportAssertion);
        networkSteps.add(ReportNetworkInspector.Step.from(step.keyword().text(), step.title(),
                allAssertions, exchangeEvidence(apiValidation)));
        if (status == AtfReportStatus.FAIL) {
            throw new AssertionError("Cart add checkpoint failed: " + failedAssertionSummary(allAssertions));
        }
    }

    private static boolean isCartExchange(UiNetworkObservation observation, String route) {
        return GET.value().equals(observation.method())
                && route.equals(URI.create(observation.url()).getRawPath())
                && (observation.resourceKind() == XHR || observation.resourceKind() == FETCH);
    }
    public boolean observesApiTraffic() { return apiEnabled; }

    /** An API-only GIVEN; direct preparation is never attributed to a browser action. */
    public void apiSetup(String title, ApiExecutionResult execution, List<AtfAssertion> assertions) {
        boolean passed = assertions.stream().allMatch(AtfAssertion::passed);
        var exchange = new UiApiValidationEvidence.Exchange(ReportModelAdapters.execution(execution), assertions);
        var apiValidation = new UiApiValidationEvidence(List.of(), List.of(exchange));
        phase.step(AtfGherkinKeyword.GIVEN, title, execution.request().path(), "", List.of(), List.of(),
                apiValidation, ReportScreenshotPolicy.CapturePoint.CHECKPOINT, AtfReportStatus.INFO);
        markCausalApiTrafficBaseline();
        if (!passed) throw new AssertionError("API setup failed: " + failedAssertionSummary(assertions));
    }

    private List<UiNetworkObservation> causalApiObservations() {
        return browser.networkInspector().observationsSince(causalNetworkBaseline);
    }

    private ReportNetworkCaptureStatus captureCoverage() {
        var inspector = browser.networkInspector();
        var statistics = inspector.statistics();
        return new ReportNetworkCaptureStatus(inspector.configuration().capturePolicy().description(),
                statistics.accepted(), statistics.retained(), statistics.excluded(),
                statistics.dropped(), statistics.inFlight(), statistics.limitReached(), causalNetworkBaseline);
    }

    private void prepareUiInteraction() {
        storeNavigation.recoverIfBusy();
        dismissPrivacyPromptIfPresent();
        dismissGoogleVignetteIfPresent();
        storeNavigation.recoverIfBusy();
    }

    private static String failedAssertionSummary(List<AtfAssertion> assertions) {
        return assertions.stream().filter(assertion -> !assertion.passed())
                .map(assertion -> assertion.name() + " [expected=" + assertion.expected()
                        + "; actual=" + assertion.actual() + "]")
                .collect(joining("; "));
    }

    private AtfLocator locator(String selector) { return new AtfLocator(page().locator(selector), selector); }

}
