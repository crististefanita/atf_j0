package atf.reporting.ui.network;

import atf.reporting.evidence.ReportEvidenceTable;
import atf.reporting.evidence.markup.ReportEvidenceMarkup;
import atf.reporting.api.ReportRequestSignature;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.markup.AtfReportMarkup;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders the typed journey aggregate as one compact, nested disclosure.
 *
 * <p>The aggregate is an index, not a second request/response report. Each
 * section exposes only facts that help a reader navigate the canonical
 * exchange cards: authentication, correlation, transport, signatures and
 * diagnostics. Keeping this markup here gives Extent and fallback HTML the
 * same structure without moving HTML into the inspector model.</p>
 */
public final class ReportNetworkAggregateMarkup {
    private static final String NETWORK_AGGREGATE = "network-aggregate";
    private static final String NETWORK_AGGREGATE_SECTION = "network-aggregate-section";
    private static final String NETWORK_AGGREGATE_TITLE = "Network aggregate";
    private static final String AUTHENTICATION = "Authentication evidence";
    private static final String CORRELATION = "Scenario correlation";
    private static final String TRANSPORT = "Transport counters";
    private static final String SIGNATURES = "Unique request signatures";
    private static final String DIAGNOSTICS = "Technical diagnostics";

    private ReportNetworkAggregateMarkup() { }

    public static String disclosure(ReportNetworkInspector.JourneyAggregate aggregate) {
        List<String> sections = substepSections(aggregate);
        if (sections.isEmpty()) return "";
        return ReportEvidenceMarkup.disclosure(NETWORK_AGGREGATE,
                NETWORK_AGGREGATE_TITLE + " · " + aggregate.observations() + " exchanges",
                sectionTable(sections));
    }

    /**
     * Renders the aggregate content for the synthetic inspector substep.
     *
     * <p>The parent node owns the title and status in the report tree. Keeping
     * only the typed sections here prevents the title from being repeated as a
     * second disclosure while retaining the same renderer-neutral evidence.</p>
     */
    public static String substepBody(ReportNetworkInspector.JourneyAggregate aggregate) {
        List<String> sections = substepSections(aggregate);
        return sections.isEmpty() ? "" : sectionTable(sections);
    }

    /** One visibility rule shared by Extent and fallback HTML renderers. */
    public static boolean hasContent(ReportNetworkInspector.JourneyAggregate aggregate) {
        return aggregate != null && !substepSections(aggregate).isEmpty();
    }

    /**
     * One collapsed disclosure per inspector section. Extent emits each as
     * its own INFO event row; fallback HTML wraps the same disclosures in the
     * equivalent Status/Details table so each status aligns with its section.
     */
    public static List<String> substepSections(
            ReportNetworkInspector.JourneyAggregate aggregate) {
        if (aggregate == null) return List.of();
        List<String> sections = new ArrayList<>();
        if (aggregate.capture() != null) {
            var capture = aggregate.capture();
            var coverage = ReportEvidenceTable.of(List.of("METRIC", "VALUE"), List.of(
                    List.of("Scope", capture.scope()),
                    List.of("Capture", capture.complete() ? "Complete within scope" : "Incomplete within scope"),
                    List.of("Accepted", Long.toString(capture.accepted())),
                    List.of("Retained", Long.toString(capture.retained())),
                    List.of("Excluded", Long.toString(capture.excluded())),
                    List.of("Dropped", Long.toString(capture.dropped())),
                    List.of("In flight", Long.toString(capture.inFlight())),
                    List.of("Limit reached", Boolean.toString(capture.limitReached())),
                    List.of("Setup exchanges", Long.toString(capture.setupExchanges())),
                    List.of("Causal exchanges", Integer.toString(aggregate.observations()))));
            sections.add(section("Capture coverage", AtfReportMarkup.tableHtml(
                    AtfReportHtmlVocabulary.classList(
                            AtfReportHtmlVocabulary.CssClass.HELM_EVIDENCE_TABLE,
                            AtfReportHtmlVocabulary.CssClass.HELM_TRACE_TABLE), coverage)));
        }
        if (!aggregate.authentication().isEmpty()) sections.add(
                section(AUTHENTICATION, aggregate.authentication().size(),
                        ReportEvidenceTable.of(
                                List.of("METHOD", "SOURCE", "STATUS", "COUNT"),
                                aggregate.authentication().stream()
                                        .map(item -> List.of(item.method(), item.source(),
                                                item.status() < 0 ? "Unavailable" : Integer.toString(item.status()),
                                                Integer.toString(item.count())))
                                        .toList())));
        if (!aggregate.correlation().isEmpty()) sections.add(
                section(CORRELATION, aggregate.correlation().size(),
                        ReportEvidenceTable.of(
                                List.of("KEYWORD", "STEP", "EXCHANGES", "ASSERTIONS", "RESULT"),
                                aggregate.correlation().stream()
                                        .map(item -> List.of(item.keyword(), item.title(),
                                                Integer.toString(item.exchangeCount()),
                                                Integer.toString(item.assertionCount()),
                                                item.passed() ? "PASS" : "FAIL"))
                                        .toList())));
        if (hasTransportActivity(aggregate.transport())) {
            sections.add(section(TRANSPORT, transportMetrics(aggregate.transport())));
        }
        if (!aggregate.signatures().isEmpty()) sections.add(
                section(SIGNATURES, aggregate.signatures().size(),
                        signatureCards(aggregate.signatures()), "network-signatures"));
        if (!aggregate.diagnostics().isEmpty()) sections.add(
                section(DIAGNOSTICS, aggregate.diagnostics().size(),
                        diagnosticCards(aggregate.diagnostics()), "network-diagnostics"));
        return List.copyOf(sections);
    }

    private static boolean hasTransportActivity(ReportNetworkInspector.TransportCounters transport) {
        return transport.observations() > 0 || transport.successful() > 0 || transport.failed() > 0
                || transport.attempts() > 0 || transport.retries() > 0 || transport.live() > 0
                || transport.mock() > 0 || transport.unknown() > 0;
    }

    private static String sectionTable(List<String> disclosures) {
        StringBuilder table = new StringBuilder(AtfReportHtmlVocabulary.openTag(
                AtfReportHtmlVocabulary.Tag.TABLE,
                AtfReportHtmlVocabulary.classAttribute("table", "table-sm",
                        "network-aggregate-sections")));
        table.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.THEAD))
                .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.TR))
                .append(headerCell("Status", "status-col"))
                .append(headerCell("Details", "details-col"))
                .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TR))
                .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.THEAD))
                .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.TBODY));
        for (String disclosure : disclosures) {
            table.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.TR,
                            AtfReportHtmlVocabulary.classAttribute("event-row",
                                    "network-aggregate-section-row")))
                    .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.TD,
                            AtfReportHtmlVocabulary.classAttribute("status-col")))
                    .append(ReportEvidenceMarkup.statusBadge(AtfReportStatus.INFO))
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TD))
                    .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.TD,
                            AtfReportHtmlVocabulary.classAttribute("details-col")))
                    .append(disclosure)
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TD))
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TR));
        }
        return table.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TBODY))
                .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TABLE))
                .toString();
    }

    private static String headerCell(String label, String cssClass) {
        return AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.TH,
                        AtfReportHtmlVocabulary.classAttribute(cssClass))
                + escape(label)
                + AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TH);
    }

    private static String section(String title, int count, String body) {
        return section(title, count, body, "");
    }

    private static String section(String title, int count, String body, String modifier) {
        String kind = modifier == null || modifier.isBlank()
                ? NETWORK_AGGREGATE_SECTION
                : NETWORK_AGGREGATE_SECTION + " " + modifier;
        return ReportEvidenceMarkup.disclosure(kind,
                title + " · " + count,
                body);
    }

    private static String section(String title, String body) {
        return ReportEvidenceMarkup.disclosure(NETWORK_AGGREGATE_SECTION, title, body);
    }

    private static String section(String title, int count, ReportEvidenceTable table) {
        return section(title, count, AtfReportMarkup.tableHtml(
                AtfReportHtmlVocabulary.classList(
                        AtfReportHtmlVocabulary.CssClass.HELM_EVIDENCE_TABLE,
                        AtfReportHtmlVocabulary.CssClass.HELM_TRACE_TABLE), table));
    }

    private static String transportMetrics(ReportNetworkInspector.TransportCounters transport) {
        StringBuilder html = new StringBuilder(open("atf-secondary-grid network-metric-strip"));
        metric(html, "EXCHANGES", transport.observations());
        metric(html, "SUCCESSFUL", transport.successful());
        metric(html, "FAILED", transport.failed());
        metric(html, "ATTEMPTS", transport.attempts());
        metric(html, "RETRIES", transport.retries());
        if (transport.live() > 0) metric(html, "LIVE HTTP", transport.live());
        if (transport.mock() > 0) metric(html, "ACCEPTANCE MOCK", transport.mock());
        if (transport.unknown() > 0) metric(html, "UNKNOWN", transport.unknown());
        return html.append(close()).toString();
    }

    private static String signatureCards(List<ReportNetworkInspector.SignatureObservation> signatures) {
        StringBuilder html = new StringBuilder(open(
                "atf-secondary-body network-aggregate-scroll network-signature-list"));
        for (var item : signatures) {
            html.append(open("atf-secondary-metric network-signature-card"));
            html.append(open("network-signature-heading"));
            html.append(span("network-signature-method", item.method()));
            html.append(span("network-signature-route", item.route().isBlank()
                    ? ReportRequestSignature.displaySignature(item.signature()) : item.route()));
            html.append(span("atf-secondary-caption network-signature-status", statusSummary(item.statuses())));
            html.append(span("atf-secondary-caption network-signature-count", "Count " + item.count()));
            html.append(close());
            html.append(open("atf-secondary-caption network-signature-id"))
                    .append(escape(ReportRequestSignature.displaySignature(item.signature())))
                    .append(close());
            html.append(open("atf-secondary-grid network-signature-meta"));
            meta(html, "SERVICE", item.service());
            meta(html, "MECHANISM", item.mechanism());
            meta(html, "AUTHENTICATION", join(item.authentication()));
            meta(html, "EVIDENCE SOURCE", join(item.evidenceSources()));
            meta(html, "PHASE", join(item.phases()));
            html.append(close());
            html.append(close());
        }
        return html.append(close()).toString();
    }

    private static String diagnosticCards(List<ReportNetworkInspector.DiagnosticObservation> diagnostics) {
        StringBuilder html = new StringBuilder(open(
                "atf-secondary-body network-aggregate-scroll network-diagnostic-list"));
        for (var item : diagnostics) {
            html.append(open("atf-secondary-metric network-diagnostic-card"));
            html.append(open("network-diagnostic-heading"));
            html.append(span("network-diagnostic-signal", item.method() + " " + item.route()));
            html.append(span("network-diagnostic-status", status(item.status())));
            html.append(span("atf-secondary-caption network-diagnostic-count", "Count " + item.count()));
            html.append(close());
            html.append(open("atf-secondary-grid network-diagnostic-meta"));
            meta(html, "SOURCES", join(item.sources()));
            if (item.status() < 0) {
                meta(html, "REASON", item.signal());
            }
            html.append(close());
            html.append(close());
        }
        return html.append(close()).toString();
    }

    private static void metric(StringBuilder html, String label, int value) {
        html.append(open("atf-secondary-metric network-metric"))
                .append(text(AtfReportHtmlVocabulary.Tag.SMALL,
                        "atf-secondary-metric-label network-metric-label", label))
                .append(text(AtfReportHtmlVocabulary.Tag.STRONG,
                        "atf-secondary-metric-value network-metric-value", Integer.toString(value)))
                .append(close());
    }

    private static void meta(StringBuilder html, String label, String value) {
        if (value == null || value.isBlank()) return;
        html.append(open("network-meta-item"))
                .append(text(AtfReportHtmlVocabulary.Tag.SMALL,
                        "atf-secondary-metric-label network-meta-label", label))
                .append(text(AtfReportHtmlVocabulary.Tag.STRONG,
                        "atf-secondary-metric-value network-meta-value", value))
                .append(close());
    }

    private static String statusSummary(List<Integer> statuses) {
        if (statuses == null || statuses.isEmpty()) return "Status unavailable";
        return statuses.size() == 1
                ? status(statuses.get(0))
                : "Statuses " + statuses.stream()
                        .map(value -> value < 0 ? "unavailable" : Integer.toString(value))
                        .collect(Collectors.joining(" · "));
    }

    private static String status(int status) {
        return status < 0 ? "Status unavailable" : "Status " + status;
    }

    private static String join(List<String> values) {
        return values == null ? "" : values.stream().filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" · "));
    }

    private static String open(String cssClass) {
        return AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                AtfReportHtmlVocabulary.classAttribute(cssClass));
    }

    private static String close() {
        return AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV);
    }

    private static String span(String cssClass, String value) {
        return AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.SPAN,
                        AtfReportHtmlVocabulary.classAttribute(cssClass))
                + escape(value) + AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.SPAN);
    }

    private static String text(AtfReportHtmlVocabulary.Tag tag, String cssClass, String value) {
        return AtfReportHtmlVocabulary.openTag(tag,
                        AtfReportHtmlVocabulary.classAttribute(cssClass))
                + escape(value) + AtfReportHtmlVocabulary.closeTag(tag);
    }

    private static String escape(String value) {
        return AtfReportHtmlVocabulary.escapeHtml(value == null ? "" : value);
    }

}
