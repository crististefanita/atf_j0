package atf.shop.support.api;

import atf.shop.support.validation.ShopOrderExpectation;

import atf.core.config.ApiTransportMode;
import atf.core.http.response.ApiBodyEvidence;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportRequestSignature;
import atf.reporting.api.ReportAuthMethod;
import atf.reporting.model.ReportExecutionEvidence;
import atf.core.http.request.HttpHeader;
import atf.core.http.request.HttpMethod;
import atf.core.http.request.MediaTypes;
import atf.core.http.protocol.ApiUris;
import atf.shop.api.ShopApiOperation;
import atf.shop.api.contract.ShopApiHttpStatus;
import atf.shop.api.contract.ShopApiJsonField;
import atf.shop.api.ShopApiRoutes;
import atf.shop.config.api.ShopApiConfiguration;
import atf.shop.config.model.ShopCredentials;
import atf.shop.config.ShopValidationProfile;
import atf.shop.config.api.ShopApiCheckpoint;
import atf.shop.config.api.ShopApiEvidenceBinding;
import atf.shop.support.ShopJsonEvidence;
import atf.shop.support.ShopLoginBodyRecovery;
import atf.ui.network.UiNetworkInspector;
import atf.ui.network.UiNetworkObservation;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;

import java.util.ArrayList;
import java.util.Arrays;
import java.time.Instant;
import java.net.URI;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.EnumMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Captures only the Practice Shop API traffic produced by the same UI session.
 * It is deliberately a consumer-side adapter: production modules do not depend on
 * the external API and no second browser or independent cart is created.
 */
public final class ShopApiNetworkJournal {
    private final ShopValidationProfile profile;
    private final ShopApiEvidenceBinding evidenceBinding;
    private final Function<String, String> serviceAlias;
    private final ShopApiTrafficCapture trafficCapture = new ShopApiTrafficCapture();
    private final List<Exchange> exchanges = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong();
    private final EnumMap<ShopApiCheckpoint, List<ReportExecutionEvidence>> evidenceByCheckpoint =
            new EnumMap<>(ShopApiCheckpoint.class);
    private String productId;

    public ShopApiNetworkJournal(ShopValidationProfile profile) {
        this(profile, ShopApiEvidenceBinding.forProfile(profile, null));
    }

    public ShopApiNetworkJournal(ShopValidationProfile profile,
                                         ShopApiEvidenceBinding evidenceBinding) {
        this(profile, evidenceBinding, Function.identity());
    }

    /** Consumer adapter seam for an honest service alias; the generic inspector remains host-based. */
    public ShopApiNetworkJournal(ShopValidationProfile profile,
                                         ShopApiEvidenceBinding evidenceBinding,
                                         Function<String, String> serviceAlias) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.evidenceBinding = Objects.requireNonNull(evidenceBinding, "evidenceBinding");
        this.serviceAlias = Objects.requireNonNull(serviceAlias, "serviceAlias");
    }

    public void attach(Page page) {
        Objects.requireNonNull(page, "page");
        if (!hasEnabledCheckpoint()) return;
        page.onResponse(response -> trafficCapture.capture(response).ifPresent(this::recordCapturedExchange));
    }

    /**
     * Reuses the generic Playwright network boundary when the UI session owns
     * one. This is the plug-and-play path; the page-specific overload above is
     * retained as a compatibility seam for focused consumer tests.
     */
    public void attach(UiNetworkInspector inspector) {
        Objects.requireNonNull(inspector, "inspector");
        if (!hasEnabledCheckpoint()) return;
        inspector.subscribe(this::recordCapturedObservation);
    }

    /** Records a sanitized exchange supplied by one of the public capture subscriptions. */
    private void recordCapturedExchange(Exchange exchange) {
        Objects.requireNonNull(exchange, "exchange");
        exchanges.add(exchange.withTrace(sequence.incrementAndGet(), Instant.now(), phase(exchange.path())));
    }

    private void recordCapturedObservation(UiNetworkObservation observation) {
        Exchange exchange = new Exchange(
                observation.method(), observation.path(), observation.url(), observation.status(),
                observation.requestBody(), observation.responseBodyAvailable() ? observation.responseBody()
                        : ApiBodyEvidence.unavailable("Browser observation", observation.responseBody()), observation.requestHeaders(),
                observation.responseHeaders()).withObservedAuthMethod(observation.authMethod());
        recordCapturedExchange(observation.requestFailed()
                ? exchange.withFailureReason(observation.failureReason()) : exchange);
    }

    /** API precondition for the product-page purchase action. */
    public List<AtfAssertion> productReady(String productName) {
        return productReady(productName, null);
    }

    /** API confirmation for the login setup that precedes an authenticated UI flow. */
    /**
     * Confirms the browser login and keeps deterministic JSON in the report when
     * Chromium releases the compressed CORS login payload before it can be read.
     * Recovery is limited to the idempotent login exchange; the browser exchange
     * remains the canonical trace and no cart/payment request is replayed.
     */
    public List<AtfAssertion> loginConfirmed(ShopApiConfiguration apiConfiguration,
                                             ShopCredentials credentials) {
        if (!enabled(ShopApiCheckpoint.LOGIN)) return List.of();
        Exchange login = ShopApiExchangeSelector.latest(
                exchanges, HttpMethod.POST.value(), ShopApiRoutes.LOGIN,
                exchange -> exchange.status() == 200);
        login = recoverLoginBody(login, apiConfiguration, credentials);
        store(ShopApiCheckpoint.LOGIN, List.of(toEvidence(login)));
        return ShopApiAssertions.login(login);
    }

    /** Sequence fence used to associate only the browser submission that followed the registration form. */
    public long sequenceMarker() {
        return sequence.get();
    }

    /** Validates and owns the observed form POST; the route is discovered from this browser run, not guessed. */
    public List<AtfAssertion> registrationConfirmed(long afterSequence, URI apiEndpoint) {
        if (!enabled(ShopApiCheckpoint.REGISTRATION)) return List.of();
        URI endpoint = Objects.requireNonNull(apiEndpoint, "apiEndpoint");
        List<Exchange> submissions = exchanges.stream()
                .filter(exchange -> exchange.sequence() > afterSequence)
                .filter(exchange -> HttpMethod.POST.value().equalsIgnoreCase(exchange.method()))
                .filter(exchange -> sameHost(exchange.url(), endpoint))
                .toList();
        List<Exchange> safeSubmissions = submissions.stream()
                .map(exchange -> exchange.withPhase("account-setup"))
                .toList();
        if (!safeSubmissions.isEmpty()) {
            store(ShopApiCheckpoint.REGISTRATION,
                    safeSubmissions.stream().map(this::toEvidence).toList());
        }
        boolean singleSubmission = safeSubmissions.size() == 1;
        boolean successfulResponse = singleSubmission
                && safeSubmissions.getFirst().status() >= 200
                && safeSubmissions.getFirst().status() < 300;
        return List.of(
                new AtfAssertion("Registration browser exchange", "exactly one observed form POST",
                        safeSubmissions.size() + " observed form POST(s)", singleSubmission),
                new AtfAssertion("Registration response", "HTTP 2xx",
                        singleSubmission ? "HTTP " + safeSubmissions.getFirst().status() : "not uniquely observed",
                        successfulResponse));
    }

    private static boolean sameHost(String url, URI endpoint) {
        try {
            String observedHost = URI.create(url).getHost();
            String expectedHost = endpoint.getHost();
            return observedHost != null && expectedHost != null
                    && observedHost.equalsIgnoreCase(expectedHost);
        } catch (RuntimeException malformedUrl) {
            return false;
        }
    }

    private static Exchange recoverLoginBody(Exchange login,
                                             ShopApiConfiguration apiConfiguration,
                                             ShopCredentials credentials) {
        if (login == null || !bodyUnavailable(login.responseBody())
                || apiConfiguration == null || credentials == null
                || apiConfiguration.mode() != ApiTransportMode.LIVE) {
            return login;
        }
        ShopLoginBodyRecovery.Result recovery =
                ShopLoginBodyRecovery.recoverLive(apiConfiguration, credentials);
        // Browser evidence remains authoritative. A failed optional recovery is
        // still published as explicit evidence instead of being swallowed.
        return login.withResponseBody(recovery.effectiveBody());
    }

    private static boolean bodyUnavailable(String body) {
        return ApiBodyEvidence.isUnavailable(body);
    }

    /**
     * API precondition for the product selected by the UI. The route is the
     * source of truth when the external catalog rotates IDs or contains
     * several cards with the same display name.
     */
    public List<AtfAssertion> productReady(String productName, String selectedProductPath) {
        if (!enabled(ShopApiCheckpoint.PRODUCT)) return List.of();
        Exchange search = ShopApiExchangeSelector.latest(
                exchanges, ShopApiOperation.SEARCH,
                exchange -> exchange.status() == 200);
        productId = ShopApiExchangeSelector.productIdFromPath(selectedProductPath);
        if (productId == null) {
            productId = ShopApiAssertions.productId(search.responseBody(), productName);
        }
        String productPath = ShopApiRoutes.product(productId);
        Exchange detail = ShopApiExchangeSelector.latest(exchanges, HttpMethod.GET.value(), productPath,
                exchange -> exchange.status() == 200
                        && exchange.path().equals(productPath));
        store(ShopApiCheckpoint.PRODUCT, List.of(toEvidence(search), toEvidence(detail)));
        return ShopApiAssertions.product(search, detail, productName, productId);
    }

    /** Compatibility view across the session; action-bound journeys supply an explicit sequence marker. */
    public List<AtfAssertion> cartReady(int quantity) {
        return cartReady(0L, quantity);
    }

    /** Selects observed add/read responses after this UI action, before evaluating their verdicts. */
    public List<AtfAssertion> cartReady(long afterSequence, int quantity) {
        if (!enabled(ShopApiCheckpoint.CART)) return List.of();
        if (productId == null || productId.isBlank()) {
            throw new IllegalStateException("Product API precondition was not established before cart validation");
        }
        Exchange add = ShopApiExchangeSelector.latestOptional(exchanges, HttpMethod.POST.value(),
                ShopApiRoutes.CART_RESOURCE_PREFIX,
                exchange -> exchange.sequence() > afterSequence && hasProduct(exchange, productId));
        Exchange read = add == null ? null : ShopApiExchangeSelector.latestOptional(
                exchanges, HttpMethod.GET.value(), add.path(), exchange -> exchange.sequence() > add.sequence());
        storeObserved(ShopApiCheckpoint.CART, add, read);
        return ShopApiAssertions.cart(add, read, productId, quantity);
    }

    /** Compatibility view across the session; action-bound journeys supply an explicit sequence marker. */
    public List<AtfAssertion> paymentConfirmed(String paymentMethod) {
        return paymentConfirmed(0L, paymentMethod);
    }

    /** An absent or failed current response cannot be replaced by an earlier successful payment. */
    public List<AtfAssertion> paymentConfirmed(long afterSequence, String paymentMethod) {
        if (!enabled(ShopApiCheckpoint.PAYMENT)) return List.of();
        Exchange payment = paymentAfter(afterSequence);
        storeObserved(ShopApiCheckpoint.PAYMENT, payment);
        return ShopApiAssertions.payment(payment, paymentMethod);
    }

    /** Observes the current response within Page's configured timeout; a capture timeout produces missing-evidence FAIL checks. */
    public List<AtfAssertion> awaitPaymentConfirmed(Page page, long afterSequence, String paymentMethod) {
        if (!enabled(ShopApiCheckpoint.PAYMENT)) return List.of();
        try {
            Objects.requireNonNull(page, "page").waitForCondition(() -> paymentAfter(afterSequence) != null);
        } catch (TimeoutError missingCapture) {
            storeObserved(ShopApiCheckpoint.PAYMENT);
            return ShopApiAssertions.payment(null, paymentMethod);
        }
        return paymentConfirmed(afterSequence, paymentMethod);
    }

    private Exchange paymentAfter(long afterSequence) {
        return ShopApiExchangeSelector.latestOptional(exchanges, HttpMethod.POST.value(), ShopApiRoutes.PAYMENT_CHECK,
                exchange -> exchange.sequence() > afterSequence);
    }

    private static boolean hasProduct(Exchange exchange, String expectedProductId) {
        var body = ShopJsonEvidence.parse(exchange.requestBody());
        var product = body == null ? null : body.get(ShopApiJsonField.PRODUCT_ID.wireValue());
        return product != null && product.isTextual() && expectedProductId.equals(product.textValue());
    }

    private void storeObserved(ShopApiCheckpoint checkpoint, Exchange... observed) {
        store(checkpoint, Arrays.stream(observed).filter(Objects::nonNull).map(this::toEvidence).toList());
    }

    /** API confirmation for the order created by the final UI confirmation action. */
    /**
     * Binds order evidence to the exchange observed after the final Confirm
     * click. A stale order response from an earlier attempt can never satisfy
     * the current confirmation checkpoint.
     */
    public List<AtfAssertion> orderCreated(long afterSequence, String uiConfirmation,
                                          ShopOrderExpectation expectedOrder) {
        if (!enabled(ShopApiCheckpoint.ORDER)) return List.of();
        Exchange order = ShopApiExchangeSelector.latestOptional(
                exchanges, ShopApiOperation.ORDER_CREATE,
                exchange -> ShopApiHttpStatus.isSuccess(exchange.status())
                        && exchange.sequence() > afterSequence);
        if (order != null) store(ShopApiCheckpoint.ORDER, List.of(toEvidence(order)));
        return ShopApiAssertions.order(order, uiConfirmation, expectedOrder);
    }

    public List<Exchange> exchanges() { return List.copyOf(exchanges); }
    /** Returns the complete ordered browser network trace for the journey-level inspector. */
    public List<ReportExecutionEvidence> allEvidence() {
        return exchanges.stream()
                .map(this::toEvidence)
                .toList();
    }

    /**
     * Returns only exchanges owned by the configured, successfully evaluated
     * API checkpoints. The selection belongs to this consumer composition
     * boundary; the reporting renderer must remain unaware of Practice Shop
     * business concepts and must never receive unbound browser traffic.
     */
    public List<ReportExecutionEvidence> selectedEvidence() {
        if (!profile.apiEvidenceRequired() || evidenceBinding.checkpoints().isEmpty()) {
            return List.of();
        }
        List<ReportExecutionEvidence> selected = evidenceByCheckpoint.values().stream()
                .flatMap(List::stream)
                .toList();
        if (selected.isEmpty()) return List.of();

        Set<String> selectedEventIds = selected.stream()
                .map(ReportExecutionEvidence::trace)
                .filter(Objects::nonNull)
                .map(trace -> trace.eventId())
                .filter(eventId -> eventId != null && !eventId.isBlank())
                .collect(Collectors.toSet());
        return allEvidence().stream()
                .filter(evidence -> selectedEventIds.contains(evidence.trace().eventId())
                        || selected.contains(evidence))
                .toList();
    }

    /** Keeps failed allowed-host requests visible in the journey even without a successful checkpoint. */
    public List<ReportExecutionEvidence> inspectorEvidence() {
        if (!hasEnabledCheckpoint()) return List.of();
        Set<String> selectedEventIds = selectedEvidence().stream()
                .map(evidence -> evidence.trace().eventId())
                .collect(Collectors.toSet());
        return allEvidence().stream()
                .filter(evidence -> evidence.response().status() < 0
                        || selectedEventIds.contains(evidence.trace().eventId()))
                .toList();
    }

    public List<ReportExecutionEvidence> evidence(ShopApiCheckpoint checkpoint) {
        return evidenceByCheckpoint.getOrDefault(Objects.requireNonNull(checkpoint, "checkpoint"), List.of());
    }
    public String productId() { return productId; }

    private boolean enabled(ShopApiCheckpoint checkpoint) {
        return profile.apiEvidenceRequired() && evidenceBinding.includes(checkpoint);
    }

    private boolean hasEnabledCheckpoint() {
        return profile.apiEvidenceRequired() && !evidenceBinding.checkpoints().isEmpty();
    }

    private void store(ShopApiCheckpoint checkpoint, List<ReportExecutionEvidence> evidence) {
        if (enabled(checkpoint)) evidenceByCheckpoint.put(checkpoint, List.copyOf(evidence));
    }

    private ReportExecutionEvidence toEvidence(Exchange exchange) {
        String host = URI.create(exchange.url()).getHost();
        String alias = serviceAlias.apply(host == null ? "" : host);
        return ShopApiEvidenceMapper.toExecution(exchange, alias);
    }

    private static String phase(String path) {
        if (path == null) return "unknown";
        if (path.startsWith(ShopApiRoutes.LOGIN)) return "authentication";
        if (path.startsWith(ShopApiRoutes.PRODUCT_SEARCH)
                || path.startsWith(ShopApiRoutes.PRODUCT_COLLECTION)
                || path.startsWith(ShopApiRoutes.PRODUCTS)) return "catalog";
        if (path.startsWith(ShopApiRoutes.CARTS)) return "cart";
        if (path.startsWith(ShopApiRoutes.PAYMENT_CHECK)
                || path.startsWith(ShopApiRoutes.POSTCODE_LOOKUP)
                || path.equals(ShopApiRoutes.INVOICE_CREATE)
                || path.equals(ShopApiRoutes.GUEST_INVOICE_CREATE)) return "checkout";
        return "unknown";
    }

    public record Exchange(String method, String path, String url, int status,
                           String requestBody, String responseBody,
                           java.util.Map<String, String> requestHeaders,
                           java.util.Map<String, String> responseHeaders,
                           long sequence, Instant capturedAt, String phase,
                           String signature, String digest,
                           String role, String stepId, String initiator,
                           String evidenceSource, String expected, String observed,
                           String observedAuthMethod, String failureReason) {
        public Exchange(String method, String path, String url, int status,
                        String requestBody, String responseBody,
                        java.util.Map<String, String> requestHeaders,
                        java.util.Map<String, String> responseHeaders) {
            this(method, path, url, status, requestBody, responseBody, requestHeaders, responseHeaders,
                    0, Instant.EPOCH, "unknown", digest(method, path, status, requestBody, responseBody), "",
                    "", "", "", "", "", "", "", "");
        }

        /** Compatibility constructor for direct API fixture and test callers. */
        public Exchange(String method, String path, String url, int status,
                        String requestBody, String responseBody,
                        java.util.Map<String, String> requestHeaders,
                        java.util.Map<String, String> responseHeaders,
                        long sequence, Instant capturedAt, String phase,
                        String signature, String digest, String role, String stepId,
                        String initiator, String evidenceSource, String expected,
                        String observed) {
            this(method, path, url, status, requestBody, responseBody, requestHeaders,
                    responseHeaders, sequence, capturedAt, phase, signature, digest,
                    role, stepId, initiator, evidenceSource, expected, observed, "");
        }

        /** Compatibility constructor for response exchanges created before failure evidence. */
        public Exchange(String method, String path, String url, int status,
                        String requestBody, String responseBody,
                        java.util.Map<String, String> requestHeaders,
                        java.util.Map<String, String> responseHeaders,
                        long sequence, Instant capturedAt, String phase,
                        String signature, String digest, String role, String stepId,
                        String initiator, String evidenceSource, String expected,
                        String observed, String observedAuthMethod) {
            this(method, path, url, status, requestBody, responseBody, requestHeaders,
                    responseHeaders, sequence, capturedAt, phase, signature, digest,
                    role, stepId, initiator, evidenceSource, expected, observed,
                    observedAuthMethod, "");
        }

        public Exchange {
            requestHeaders = ShopJsonEvidence.redactHeaders(requestHeaders);
            responseHeaders = ShopJsonEvidence.redactHeaders(responseHeaders);
            requestBody = ShopApiEvidenceMapper.redactJson(requestBody);
            responseBody = ShopApiEvidenceMapper.redactJson(responseBody);
            capturedAt = capturedAt == null ? Instant.EPOCH : capturedAt;
            phase = phase == null || phase.isBlank() ? "unknown" : phase;
            signature = signature == null || signature.isBlank()
                    ? requestSignature(method, path, requestBody) : signature;
            digest = digest == null ? "" : digest;
            role = role == null ? "" : role.trim();
            stepId = stepId == null ? "" : stepId.trim();
            initiator = initiator == null ? "" : initiator.trim();
            evidenceSource = evidenceSource == null ? "" : evidenceSource.trim();
            expected = expected == null ? "" : expected.trim();
            observed = observed == null ? "" : observed.trim();
            observedAuthMethod = observedAuthMethod == null ? "" : observedAuthMethod.trim();
            failureReason = failureReason == null ? "" : failureReason.trim();
        }

        public Exchange withTrace(long sequence, Instant capturedAt, String phase) {
            return withTrace(sequence, capturedAt, phase, "ActionCaused", phase,
                    reportSummary(), failureReason.isBlank() ? "UI_BROWSER" : "UI_BROWSER_REQUEST_FAILED", "", "");
        }

        public Exchange withTrace(long sequence, Instant capturedAt, String phase,
                                  String role, String stepId, String initiator,
                                  String evidenceSource, String expected, String observed) {
            String digest = digest(method, path, status, requestBody, responseBody);
            String signature = requestSignature(method, path, requestBody);
            return new Exchange(method, path, url, status, requestBody, responseBody,
                    requestHeaders, responseHeaders, sequence, capturedAt, phase, signature, digest,
                    role, stepId, initiator, evidenceSource, expected, observed,
                    observedAuthMethod, failureReason);
        }

        public Exchange withResponseBody(String body) {
            return new Exchange(method, path, url, status, requestBody, body,
                    requestHeaders, responseHeaders, sequence, capturedAt, phase, signature,
                    digest(method, path, status, requestBody, body), role, stepId, initiator,
                    evidenceSource, expected, observed, observedAuthMethod, failureReason);
        }

        public Exchange withPhase(String selectedPhase) {
            return new Exchange(method, path, url, status, requestBody, responseBody,
                    requestHeaders, responseHeaders, sequence, capturedAt, selectedPhase,
                    signature, digest, role, stepId, initiator, evidenceSource,
                    expected, observed, observedAuthMethod, failureReason);
        }

        public Exchange withObservedAuthMethod(String authMethod) {
            return new Exchange(method, path, url, status, requestBody, responseBody,
                    requestHeaders, responseHeaders, sequence, capturedAt, phase, signature,
                    digest, role, stepId, initiator, evidenceSource, expected, observed,
                    authMethod, failureReason);
        }

        public Exchange withFailureReason(String reason) {
            return new Exchange(method, path, url, status, requestBody, responseBody,
                    requestHeaders, responseHeaders, sequence, capturedAt, phase, signature,
                    digest, role, stepId, initiator, evidenceSource, expected, observed,
                    observedAuthMethod, reason);
        }

        public String contentType() {
            return responseHeaders.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(HttpHeader.CONTENT_TYPE.value()))
                    .map(java.util.Map.Entry::getValue).findFirst().orElse("");
        }

        public boolean isJsonResponse() { return MediaTypes.isJson(contentType()); }
        public String authType() {
            if (!observedAuthMethod.isBlank()) return observedAuthMethod;
            return ReportAuthMethod.fromHeaders(requestHeaders).wireValue();
        }
        public String summary() { return method + " " + path + " → " + statusLabel() + " " + contentType(); }

        public String statusLabel() { return status < 0 ? "Unavailable" : Integer.toString(status); }

        public String eventId() { return sequence > 0 ? "E" + sequence : ""; }

        public String correlationKey() {
            return (method == null ? "" : method.toUpperCase()) + " "
                    + ShopApiExchangeSelector.normalize(path);
        }

        public String evidenceSource() { return evidenceSource; }

        /**
         * Human-facing summary retains the captured verb. Business operation
         * selection may recognize QUERY as search, but does not rewrite HTTP evidence.
         */
        public String reportSummary() {
            return summary();
        }

        private static String digest(String method, String path, int status,
                                     String requestBody, String responseBody) {
            try {
                byte[] bytes = (method + "\n" + path + "\n" + status + "\n"
                        + requestBody + "\n" + responseBody).getBytes(StandardCharsets.UTF_8);
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
                StringBuilder result = new StringBuilder(hash.length * 2);
                for (byte value : hash) result.append(String.format("%02x", value));
                return result.toString();
            } catch (java.security.NoSuchAlgorithmException error) {
                throw new IllegalStateException("SHA-256 is unavailable", error);
            }
        }

        /** Stable request identity: retries share it, but different JSON bodies do not. */
        private static String requestSignature(String method, String path, String requestBody) {
            String target = path == null ? "/" : path;
            URI uri = URI.create(ApiUris.httpsUrl("trace.invalid", target));
            Map<String, String> query = new java.util.LinkedHashMap<>();
            if (uri.getRawQuery() != null) {
                for (String pair : uri.getRawQuery().split("&")) {
                    String[] parts = pair.split("=", 2);
                    query.put(java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                            parts.length == 1 ? "" : java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                }
            }
            return ReportRequestSignature.forRequest(new ReportRequest(
                    method == null ? HttpMethod.GET.value() : method, uri.getRawPath(), query,
                    Map.of(), "none", requestBody));
        }
    }
}
