package atf.reporting.api.evidence;

import atf.reporting.api.ApiManualReplay;
import atf.reporting.api.ApiEvidenceField;
import atf.reporting.api.ReportRequestSignature;
import atf.reporting.api.auth.ApiAuthenticationEvidence;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.model.ReportJsonBody;
import atf.reporting.model.ReportJsonBodyFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** One renderer-neutral projection; consumers retain ownership of titles and verdicts. */
public final class ApiExchangeEvidence {
    private ApiExchangeEvidence() { }

    /** Publishes captured facts, including terminal exchanges without a response or a known resolved URL. */
    public static void publish(AtfReportPorts.Request node, ReportExecutionEvidence evidence) {
        Objects.requireNonNull(node, "node");
        project(evidence, node::detail, node::body);
        node.execution(evidence);
    }

    /** Terminal evidence never fabricates a response; the caller retains ownership of the logical verdict. */
    public static String publishFailure(AtfReportPorts.Request node, ReportExecutionEvidence evidence,
                                        String resolvedUrl, String termination, String diagnostic) {
        Objects.requireNonNull(node, "node");
        String safe = new atf.reporting.security.SensitiveDataRedactor().redactText(diagnostic);
        safe = atf.reporting.markup.ExtentReportNodeSupport.display(safe, atf.reporting.configuration.ReportingSettings.defaults());
        var terminal = evidence.withTermination(termination, safe);
        if (terminal.response() != null) publish(node, terminal);
        else {
            projectRequest(terminal, resolvedUrl, node::detail, node::body);
            node.execution(terminal);
        }
        return safe;
    }

    /** Narrow write seam for legacy ports; no producer dependency leaks into Reporting. */
    public static void publishFields(ReportExecutionEvidence evidence,
                                     BiConsumer<String, String> detail, BiConsumer<String, String> json) {
        project(evidence, detail, body -> json.accept(body.label(), body.body()));
    }

    private static void project(ReportExecutionEvidence evidence,
                                BiConsumer<String, String> detail, Consumer<ReportJsonBody> bodies) {
        var response = evidence.response();
        projectRequest(evidence, response == null ? "" : response.url(), detail, bodies);
        if (response == null) return;
        detail.accept(ApiEvidenceField.HTTP_STATUS.label(), response.statusLabel());
        detail.accept("response.content.type", header(response.headers(), "content-type"));
        String headers = ApiManualReplay.relevantResponseHeaders(response.headers());
        detail.accept("response.headers", headers);
        body(detail, bodies, ReportJsonBody.Direction.RESPONSE, response.body(),
                response.bodyAvailability(), header(response.headers(), "content-type"));
    }

    private static void projectRequest(ReportExecutionEvidence evidence, String url,
                                        BiConsumer<String, String> detail, Consumer<ReportJsonBody> bodies) {
        var request = Objects.requireNonNull(evidence.request(), "captured request");
        detail.accept("method", request.method());
        detail.accept("path", request.path());
        detail.accept("url", url);
        detail.accept("request.signature", ReportRequestSignature.displaySignature(
                ReportRequestSignature.forRequest(request)));
        ApiAuthenticationEvidence.publishTo(detail, request);
        if (!request.query().isEmpty()) detail.accept("query.params", request.query().toString());
        detail.accept("request.headers", ApiManualReplay.redactedHeaders(request));
        detail.accept("transport.provenance", evidence.attempts() == 0
                ? "No request dispatched; selected transport: " + evidence.provenance().name()
                : evidence.provenance().reportLabel());
        detail.accept("manual.replay", evidence.attempts() == 0 ? "NOT VERIFIED — no request was dispatched"
                : ApiManualReplay.describe(evidence.provenance(), request, url, request.bodyText()));
        body(detail, bodies, ReportJsonBody.Direction.REQUEST, request.bodyText(),
                request.bodyAvailability(), header(request.headerEvidence().values(), "content-type"));
    }

    private static void body(BiConsumer<String, String> detail, Consumer<ReportJsonBody> bodies,
                             ReportJsonBody.Direction direction, String body,
                             ReportBodyAvailability availability, String contentType) {
        String side = direction.name().toLowerCase(Locale.ROOT);
        detail.accept(side + ".body.capture", switch (availability) {
            case AVAILABLE -> "Captured";
            case ABSENT -> "Absent — confirmed no HTTP body";
            case NOT_CAPTURED -> "Not captured — body evidence unavailable"
                    + (body.isBlank() ? "" : ": " + body);
        });
        if (availability == ReportBodyAvailability.AVAILABLE) {
            ReportJsonBody.Format format = format(body, contentType);
            String suffix = switch (format) {
                case JSON -> "JSON";
                case FORM -> "FORM BODY";
                case TEXT -> "BODY";
            };
            bodies.accept(new ReportJsonBody(direction.name() + " " + suffix,
                    body,
                    direction, format));
        }
    }

    private static ReportJsonBody.Format format(String body, String contentType) {
        String mime = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (mime.equals("application/x-www-form-urlencoded") || mime.equals("multipart/form-data")) {
            return ReportJsonBody.Format.FORM;
        }
        return ReportJsonBodyFormatter.isJsonDocument(body)
                ? ReportJsonBody.Format.JSON : ReportJsonBody.Format.TEXT;
    }

    private static String header(Map<String, String> headers, String name) {
        return headers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue).findFirst().orElse("");
    }
}
