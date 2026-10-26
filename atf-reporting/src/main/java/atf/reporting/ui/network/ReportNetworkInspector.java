package atf.reporting.ui.network;

import atf.reporting.api.ReportTraceMetadata;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.api.ReportAuthObservation;
import atf.reporting.api.ReportRequestSignature;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.model.ReportField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Builds the compact, renderer-neutral network summary for a UI journey.
 * Detailed request/response bodies remain owned by their UI checkpoints; this
 * projection only answers which exchange happened, when, and why.
 */
public final class ReportNetworkInspector {
    private ReportNetworkInspector() {
    }

    public static List<ReportField> fields(Collection<ReportExecutionEvidence> evidence) {
        return fields(evidence, List.of());
    }

    /** Projects only the compact index attached to the inspector request. */
    public static List<ReportField> fields(Collection<ReportExecutionEvidence> evidence,
                                           Collection<Step> steps) {
        return fields(evidence, steps, null);
    }

    public static List<ReportField> fields(Collection<ReportExecutionEvidence> evidence,
                                           Collection<Step> steps,
                                           ReportNetworkCaptureStatus capture) {
        List<ReportExecutionEvidence> exchanges = evidence == null
                ? List.of()
                : evidence.stream().filter(Objects::nonNull).toList();
        Set<String> signatures = new LinkedHashSet<>();
        List<ReportField> fields = new java.util.ArrayList<>();
        fields.add(ReportField.of("network.observations",
                exchanges.size() + " canonical exchanges"));
        List<Step> journey = steps == null ? List.of() : steps.stream()
                .filter(Objects::nonNull).toList();
        fields.add(ReportField.of("network.gherkin.correlation",
                journey.size() + " Gherkin steps; details are shown in Scenario correlation"));
        for (ReportExecutionEvidence exchange : exchanges) {
            if (exchange.request() != null) {
                signatures.add(signature(exchange));
            }
        }
        fields.add(ReportField.of("network.unique.signatures",
                signatures.size() + " unique request signatures; details are shown once below"));
        if (capture != null) {
            fields.add(ReportField.of("network.capture.scope", capture.scope()));
            fields.add(ReportField.of("network.capture.completeness", capture.completeness()));
            fields.add(ReportField.of("network.capture.accepted", Long.toString(capture.accepted())));
            fields.add(ReportField.of("network.capture.retained", Long.toString(capture.retained())));
            fields.add(ReportField.of("network.capture.dropped", Long.toString(capture.dropped())));
            fields.add(ReportField.of("network.capture.setup", Long.toString(capture.setupRetained())));
            fields.add(ReportField.of("network.capture.causal", Long.toString(capture.causalRetained())));
        }
        return List.copyOf(fields);
    }

    /** Builds the typed journey projection consumed by all report renderers. */
    public static JourneyAggregate aggregate(Collection<ReportExecutionEvidence> evidence,
                                             Collection<Step> steps) {
        List<ReportExecutionEvidence> exchanges = evidence == null
                ? List.of() : evidence.stream().filter(Objects::nonNull).toList();
        List<Step> journey = steps == null ? List.of() : steps.stream()
                .filter(Objects::nonNull).toList();
        List<NetworkFailure> technicalDiagnostics = new ArrayList<>();

        Map<AuthKey, Integer> authCounts = new LinkedHashMap<>();
        Map<SignatureKey, SignatureAccumulator> signatures = new LinkedHashMap<>();
        int successful = 0;
        int failed = 0;
        int attempts = 0;
        int retries = 0;
        int live = 0;
        int mock = 0;
        int unknown = 0;
        for (ReportExecutionEvidence exchange : exchanges) {
            if (exchange.request() != null) {
                ReportAuthObservation auth = exchange.request().authObservation();
                int status = exchange.response() == null ? -1 : exchange.response().status();
                AuthKey key = new AuthKey(auth.method().wireValue(), auth.source().wireValue(), status);
                authCounts.merge(key, 1, Integer::sum);
                String requestSignature = signature(exchange);
                SignatureKey signatureKey = new SignatureKey(requestSignature);
                signatures.computeIfAbsent(signatureKey, ignored -> new SignatureAccumulator())
                        .add(exchange);
            }
            if (exchange.response() != null) {
                if (exchange.response().status() >= 200 && exchange.response().status() < 400) successful++;
                else failed++;
                if (exchange.response().status() < 0 && exchange.request() != null) {
                    String reason = exchange.attemptHistory().stream()
                            .map(attempt -> attempt.reason())
                            .filter(value -> value != null && !value.isBlank())
                            .findFirst().orElse("request failed before response");
                    String source = exchange.trace() == null ? "UI_BROWSER_REQUEST_FAILED"
                            : exchange.trace().evidenceSource();
                    technicalDiagnostics.add(new NetworkFailure(source, exchange.request().method(),
                            exchange.request().path(), -1, reason));
                }
            }
            attempts += Math.max(0, exchange.attempts());
            retries += exchange.retryEvents().size();
            switch (exchange.provenance()) {
                case LIVE_HTTP -> live++;
                case ACCEPTANCE_MOCK -> mock++;
                case UNKNOWN -> unknown++;
            }
        }
        List<AuthenticationObservation> authentication = authCounts.entrySet().stream()
                .map(entry -> new AuthenticationObservation(entry.getKey().method(),
                        entry.getKey().source(), entry.getKey().status(), entry.getValue()))
                .toList();
        List<SignatureObservation> signatureObservations = signatures.entrySet().stream()
                .map(entry -> entry.getValue().toObservation(entry.getKey().signature()))
                .toList();
        List<ScenarioCorrelation> correlation = journey.stream()
                .map(step -> new ScenarioCorrelation(step.keyword(), step.title(), step.exchanges().size(),
                        step.assertionCount(), step.passed()))
                .toList();
        List<DiagnosticObservation> diagnosticObservations = deduplicateNetworkFailures(technicalDiagnostics);
        return new JourneyAggregate(authentication, correlation,
                new TransportCounters(exchanges.size(), successful, failed, attempts, retries,
                        live, mock, unknown), signatureObservations, diagnosticObservations);
    }

    private static List<DiagnosticObservation> deduplicateNetworkFailures(
            Collection<NetworkFailure> diagnostics) {
        Map<DiagnosticKey, DiagnosticAccumulator> grouped = new LinkedHashMap<>();
        for (NetworkFailure diagnostic : diagnostics) {
            DiagnosticKey key = DiagnosticKey.from(diagnostic);
            grouped.computeIfAbsent(key, ignored -> new DiagnosticAccumulator()).add(diagnostic.source());
        }
        List<DiagnosticObservation> observations = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            var key = entry.getKey();
            observations.add(new DiagnosticObservation(key.method(), key.route(), key.status(), key.message(),
                    entry.getValue().count, entry.getValue().sources()));
        }
        return List.copyOf(observations);
    }

    /** Adds capture coverage without conflating it with causal exchange or assertion counts. */
    public static JourneyAggregate aggregate(Collection<ReportExecutionEvidence> evidence,
                                             Collection<Step> steps, ReportNetworkCaptureStatus capture) {
        JourneyAggregate aggregate = aggregate(evidence, steps);
        return new JourneyAggregate(aggregate.authentication(), aggregate.correlation(),
                aggregate.transport(), aggregate.signatures(), aggregate.diagnostics(), capture);
    }

    /** Causal, renderer-neutral summary for one UI checkpoint. */
    public record Step(String keyword, String title, int assertionCount,
                       boolean passed, Collection<ReportExecutionEvidence> exchanges) {
        public Step {
            keyword = keyword == null || keyword.isBlank() ? "STEP" : keyword.trim().toUpperCase();
            title = title == null || title.isBlank() ? "UI checkpoint" : title.trim();
            if (assertionCount < 0) throw new IllegalArgumentException("assertionCount must not be negative");
            exchanges = exchanges == null ? List.of() : exchanges.stream()
                    .filter(Objects::nonNull).toList();
        }

        public static Step from(String keyword, String title, Collection<AtfAssertion> assertions,
                                Collection<ReportExecutionEvidence> exchanges) {
            List<AtfAssertion> safeAssertions = assertions == null ? List.of() : assertions.stream()
                    .filter(Objects::nonNull).toList();
            boolean passed = safeAssertions.stream().allMatch(AtfAssertion::passed);
            return new Step(keyword, title, safeAssertions.size(), passed, exchanges);
        }

        String summary() {
            String responses = exchanges.stream()
                    .filter(exchange -> exchange.response() != null)
                    .map(exchange -> Integer.toString(exchange.response().status()))
                    .toList().toString();
            return keyword + " " + title + " | exchanges=" + exchanges.size()
                    + " | responses=" + responses + " | assertions=" + assertionCount
                    + " " + (passed ? "PASS" : "FAIL");
        }
    }

    /** Complete renderer-neutral journey aggregate; no JSON or credentials. */
    public record JourneyAggregate(List<AuthenticationObservation> authentication,
                                   List<ScenarioCorrelation> correlation,
                                   TransportCounters transport,
                                   List<SignatureObservation> signatures,
                                   List<DiagnosticObservation> diagnostics,
                                   ReportNetworkCaptureStatus capture) {
        public JourneyAggregate(List<AuthenticationObservation> authentication,
                                List<ScenarioCorrelation> correlation, TransportCounters transport,
                                List<SignatureObservation> signatures, List<DiagnosticObservation> diagnostics) {
            this(authentication, correlation, transport, signatures, diagnostics, null);
        }
        public JourneyAggregate {
            authentication = List.copyOf(authentication == null ? List.of() : authentication);
            correlation = List.copyOf(correlation == null ? List.of() : correlation);
            transport = transport == null ? TransportCounters.empty() : transport;
            signatures = List.copyOf(signatures == null ? List.of() : signatures);
            diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
        }

        public int observations() { return transport.observations(); }
    }

    public record AuthenticationObservation(String method, String source, int status, int count) {
        public String summary() {
            return "method=" + method + "; source=" + source + "; status=" + status + "; count=" + count;
        }
    }

    public record ScenarioCorrelation(String keyword, String title, int exchangeCount,
                                      int assertionCount, boolean passed) {
        public String summary() {
            return keyword + " " + title + " | exchanges=" + exchangeCount
                    + " | assertions=" + assertionCount + " | " + (passed ? "PASS" : "FAIL");
        }
    }

    public record TransportCounters(int observations, int successful, int failed, int attempts,
                                    int retries, int live, int mock, int unknown) {
        public TransportCounters {
            if (observations < 0 || successful < 0 || failed < 0 || attempts < 0 || retries < 0
                    || live < 0 || mock < 0 || unknown < 0) {
                throw new IllegalArgumentException("network counters must not be negative");
            }
        }

        static TransportCounters empty() { return new TransportCounters(0, 0, 0, 0, 0, 0, 0, 0); }

        public String summary() {
            return "observations=" + observations + "; successful=" + successful
                    + "; failed=" + failed + "; attempts=" + attempts + "; retries=" + retries
                    + "; live=" + live + "; mock=" + mock + "; unknown=" + unknown;
        }
    }

    public record SignatureObservation(String signature, String method, String route,
                                       String service, String mechanism, int count,
                                       List<Integer> statuses,
                                       List<String> authentication, List<String> evidenceSources,
                                       List<String> phases) {
        public SignatureObservation(String signature, int count, List<Integer> statuses) {
            this(signature, "", "", "", "", count, statuses,
                    List.of(), List.of(), List.of());
        }

        public SignatureObservation {
            signature = signature == null ? "" : signature;
            method = normalize(method).toUpperCase(Locale.ROOT);
            route = normalize(route);
            service = normalize(service);
            mechanism = normalize(mechanism);
            statuses = List.copyOf(statuses == null ? List.of() : statuses);
            authentication = List.copyOf(authentication == null ? List.of() : authentication);
            evidenceSources = List.copyOf(evidenceSources == null ? List.of() : evidenceSources);
            phases = List.copyOf(phases == null ? List.of() : phases);
        }

        public String summary() {
            return "count=" + count + "; signature="
                    + ReportRequestSignature.displaySignature(signature)
                    + "; statuses=" + statuses;
        }
    }

    public record DiagnosticObservation(String method, String route, int status, String signal,
                                        int count, List<String> sources) {
        public DiagnosticObservation {
            method = normalize(method).toUpperCase(Locale.ROOT);
            route = normalize(route);
            signal = normalize(signal);
            sources = List.copyOf(sources == null ? List.of() : sources);
        }

        public String summary() {
            return "method=" + method + "; route=" + route + "; status=" + status
                    + "; count=" + count + "; sources=" + sources
                    + (signal.isBlank() ? "" : "; signal=" + signal);
        }
    }

    private record AuthKey(String method, String source, int status) { }
    private record SignatureKey(String signature) { }
    private record NetworkFailure(String source, String method, String route, int status, String message) {
        private NetworkFailure {
            source = normalize(source);
            method = normalize(method).toUpperCase(Locale.ROOT);
            route = normalize(route);
            message = normalize(message);
        }
    }

    private record DiagnosticKey(String method, String route, int status, String message) {
        private static DiagnosticKey from(NetworkFailure diagnostic) {
            return new DiagnosticKey(diagnostic.method(), diagnostic.route(), diagnostic.status(),
                    diagnostic.message());
        }
    }

    private static final class SignatureAccumulator {
        private int count;
        private final List<Integer> statuses = new ArrayList<>();
        private String method = "";
        private String route = "";
        private String service = "";
        private String mechanism = "";
        private final LinkedHashSet<String> authentication = new LinkedHashSet<>();
        private final LinkedHashSet<String> evidenceSources = new LinkedHashSet<>();
        private final LinkedHashSet<String> phases = new LinkedHashSet<>();

        void add(ReportExecutionEvidence evidence) {
            count++;
            if (evidence == null) return;
            if (evidence.request() != null) {
                method = evidence.request().method();
                route = evidence.request().path();
                authentication.add(evidence.request().authObservation().summary());
                if (service.isBlank()) service = service(evidence);
                if (mechanism.isBlank()) mechanism = mechanism(evidence);
            }
            if (evidence.response() != null && !statuses.contains(evidence.response().status())) {
                statuses.add(evidence.response().status());
            }
            var trace = evidence.trace();
            if (trace != null) {
                if (!trace.evidenceSource().isBlank()) evidenceSources.add(trace.evidenceSource());
                if (!trace.phase().isBlank()) phases.add(trace.phase());
            }
        }

        SignatureObservation toObservation(String signature) {
            return new SignatureObservation(signature, method, route, service, mechanism, count, statuses,
                    List.copyOf(authentication), List.copyOf(evidenceSources), List.copyOf(phases));
        }

        private static String service(ReportExecutionEvidence evidence) {
            if (evidence != null && evidence.trace() != null
                    && !evidence.trace().service().isBlank()) {
                return evidence.trace().service();
            }
            if (evidence == null || evidence.response() == null || evidence.response().url().isBlank()) {
                return "unknown";
            }
            try {
                String host = java.net.URI.create(evidence.response().url()).getHost();
                return host == null || host.isBlank() ? "unknown" : host;
            } catch (RuntimeException ignored) {
                return "unknown";
            }
        }

        private static String mechanism(ReportExecutionEvidence evidence) {
            String source = evidence.trace() == null ? "" : evidence.trace().evidenceSource();
            if (source.toUpperCase(Locale.ROOT).contains("UI_BROWSER")) {
                return "browser network / Playwright";
            }
            return switch (evidence.provenance()) {
                case LIVE_HTTP -> "API client / HTTP";
                case ACCEPTANCE_MOCK -> "acceptance mock";
                case UNKNOWN -> "unknown";
            };
        }
    }

    private static final class DiagnosticAccumulator {
        private int count;
        private final LinkedHashSet<String> sources = new LinkedHashSet<>();

        void add(String source) { count++; sources.add(normalize(source)); }

        List<String> sources() { return List.copyOf(sources); }
    }

    private static String normalize(String value) { return value == null ? "" : value.trim(); }

    public static String summary(ReportExecutionEvidence evidence) {
        if (evidence == null || evidence.request() == null || evidence.response() == null) {
            return "unavailable exchange";
        }
        var request = evidence.request();
        var response = evidence.response();
        var trace = evidence.trace();
        String event = trace != null && !trace.eventId().isBlank()
                ? trace.eventId() : "exchange";
        String phase = trace == null || trace.phase().isBlank() ? "unknown" : trace.phase();
        String contentType = header(response.headers(), "content-type");
        String transport = evidence.provenance().reportLabel();
        String operation = request.method() + " " + request.path() + " → "
                + response.statusLabel() + (contentType.isBlank() ? "" : " " + contentType);
        String signature = signature(evidence);
        String source = trace == null || trace.evidenceSource().isBlank()
                ? "unknown" : trace.evidenceSource();
        return event + " | " + traceFacts(trace) + "phase=" + phase + " | " + operation
                + " | auth=" + request.authObservation().summary()
                + " | attempts=" + evidence.attempts()
                + " | retries=" + evidence.retryEvents().size()
                + " | source=" + source + " | transport=" + transport
                + " | signature=" + ReportRequestSignature.displaySignature(signature);
    }

    private static String traceFacts(ReportTraceMetadata trace) {
        if (trace == null) return "";
        var facts = new StringJoiner(" | ");
        if (trace.sequence() > 0) facts.add("sequence=" + trace.sequence());
        if (!trace.stepId().isBlank()) facts.add("step=" + trace.stepId());
        if (!trace.role().isBlank()) facts.add("role=" + trace.role());
        if (!trace.initiator().isBlank()) facts.add("initiator=" + trace.initiator());
        if (!trace.correlationKey().isBlank()) facts.add("correlation=" + trace.correlationKey());
        String value = facts.toString();
        return value.isBlank() ? "" : value + " | ";
    }

    private static String signature(ReportExecutionEvidence evidence) {
        if (evidence == null || evidence.request() == null) return "";
        if (evidence.trace() != null && !evidence.trace().requestSignature().isBlank()) {
            return evidence.trace().requestSignature();
        }
        return ReportRequestSignature.forRequest(evidence.request());
    }

    private static String header(java.util.Map<String, String> headers, String name) {
        if (headers == null) return "";
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(java.util.Map.Entry::getValue)
                .findFirst().orElse("");
    }
}
