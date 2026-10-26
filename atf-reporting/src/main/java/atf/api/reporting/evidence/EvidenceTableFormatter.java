package atf.api.reporting.evidence;
import atf.api.reporting.duration.AtfDurationFormatter;
import atf.api.reporting.api.ApiEvidenceField;
import atf.api.reporting.api.contract.ApiReportEvidenceField;
import atf.api.reporting.markup.AtfReportLabels;
import atf.api.reporting.model.ReportField;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static atf.api.reporting.markup.AtfReportEvidenceKeys.REQUEST_ACTION;
import static atf.api.reporting.markup.AtfReportEvidenceKeys.REQUEST_TARGET;
import static atf.api.reporting.markup.AtfReportEvidenceKeys.EVIDENCE_TYPE;
import static atf.api.reporting.markup.AtfReportEvidenceKeys.EVIDENCE_POLICY;
import static atf.api.reporting.markup.AtfReportEvidenceKeys.SCREEN_VALIDATION;
import static atf.api.reporting.markup.AtfReportEvidenceKeys.UI_FAILURE;
import static atf.api.reporting.markup.AtfReportEvidenceKeys.UNIFIED_UI_FLOW;
import static atf.api.reporting.markup.AtfReportEvidenceKeys.FLOW_PRESENTATION;
import static atf.api.reporting.markup.AtfReportEvidenceKeys.COMPACT_UI_ONLY;

/** Gives request evidence one stable, human-readable vocabulary. */
public final class EvidenceTableFormatter {
    private EvidenceTableFormatter() {
    }

    public static String[][] toExtentRows(List<String[]> details) {
        var rows = new ArrayList<String[]>(details.size() + 1);
        rows.add(new String[]{AtfReportLabels.FIELD, AtfReportLabels.VALUE});
        for (String[] detail : details) {
            rows.add(new String[]{displayName(detail[0]), displayValue(detail[0], detail[1])});
        }
        return rows.toArray(String[][]::new);
    }

    public static String[][] toExtentRowsModel(List<ReportField> details) {
        var rows = new ArrayList<String[]>(details.size() + 1);
        rows.add(new String[]{AtfReportLabels.FIELD, AtfReportLabels.VALUE});
        for (ReportField detail : details) {
            rows.add(new String[]{displayName(detail.name()), displayValue(detail.name(), detail.value())});
        }
        return rows.toArray(String[][]::new);
    }

    /** Canonical typed table model shared by Extent and fallback HTML. */
    public static ReportEvidenceTable toTable(List<ReportField> details) {
        var rows = new ArrayList<List<String>>(details == null ? 1 : details.size() + 1);
        rows.add(List.of(AtfReportLabels.FIELD, AtfReportLabels.VALUE));
        if (details != null) {
            for (ReportField detail : details) {
                if (detail != null) {
                    rows.add(List.of(displayName(detail.name()), displayValue(detail.name(), detail.value())));
                }
            }
        }
        return ReportEvidenceTable.of(rows.remove(0), rows);
    }

    /** Canonical typed serializer for Extent evidence tables. */
    public static List<ReportField> technicalTraceFields(List<ReportField> details) {
        if (details == null || details.isEmpty()) return List.of();
        if (isCompactUiFlow(details)) return List.of();
        boolean checkpoint = details.stream().anyMatch(detail -> detail != null
                && isUiEvidenceType(detail.name(), detail.value()));
        return details.stream()
                .filter(detail -> detail != null)
                .filter(detail -> !isInternalUiContext(detail.name()))
                .filter(detail -> !checkpoint || !isUiCheckpointPolicy(detail.name()))
                .filter(detail -> !isRequestDetail(detail.name()))
                .filter(detail -> !isExecutionSummaryField(detail.name()))
                .toList();
    }

    /** Cross-phase UI provenance is rendered by its own disclosure, never as an HTTP response. */
    public static boolean isCompactUiFlow(List<ReportField> details) {
        return details != null && details.stream().anyMatch(detail -> detail != null
                && EVIDENCE_TYPE.equalsIgnoreCase(detail.name())
                && UNIFIED_UI_FLOW.equalsIgnoreCase(detail.value()))
                && details.stream().anyMatch(detail -> detail != null
                && FLOW_PRESENTATION.equalsIgnoreCase(detail.name())
                && COMPACT_UI_ONLY.equalsIgnoreCase(detail.value()));
    }

    public static List<ReportField> uiFlowProvenanceFields(List<ReportField> details) {
        if (!isCompactUiFlow(details)) return List.of();
        return details.stream().filter(detail -> detail != null
                && !FLOW_PRESENTATION.equalsIgnoreCase(detail.name()))
                .toList();
    }

    public static List<ReportField> requestFields(List<ReportField> details) {
        if (details == null || details.isEmpty()) return List.of();
        return details.stream()
                .filter(detail -> detail != null)
                .filter(detail -> !isInternalUiContext(detail.name()))
                .filter(detail -> isRequestDetail(detail.name()))
                .toList();
    }

    /**
     * Keeps the technical trace focused on request context and response
     * metadata. HTTP status is intentionally visible here as the primary
     * response verdict; the execution-evidence disclosure keeps the complete
     * attempt history and timing details. The request URL remains visible here
     * because it identifies the operation a reader is inspecting.
     */
    public static List<String[]> technicalTraceDetails(List<String[]> details) {
        if (details == null || details.isEmpty()) return List.of();
        boolean checkpoint = details.stream().anyMatch(detail -> detail != null && detail.length > 1
                && isUiEvidenceType(detail[0], detail[1]));
        return details.stream()
                .filter(detail -> detail != null && detail.length > 0)
                .filter(detail -> !isInternalUiContext(detail[0]))
                .filter(detail -> !checkpoint || !isUiCheckpointPolicy(detail[0]))
                .filter(detail -> !isRequestDetail(detail[0]))
                .filter(detail -> !isExecutionSummaryField(detail[0]))
                .toList();
    }

    public static List<String[]> requestDetails(List<String[]> details) {
        if (details == null || details.isEmpty()) return List.of();
        return details.stream()
                .filter(detail -> detail != null && detail.length > 0)
                .filter(detail -> !isInternalUiContext(detail[0]))
                .filter(detail -> isRequestDetail(detail[0]))
                .toList();
    }

    /** Checkpoint identity and evidence policy stay in the model, outside public HTTP evidence. */
    private static boolean isInternalUiContext(String key) {
        if (key == null) return false;
        String normalized = key.trim();
        return REQUEST_ACTION.equalsIgnoreCase(normalized)
                || REQUEST_TARGET.equalsIgnoreCase(normalized);
    }

    private static boolean isUiEvidenceType(String key, String value) {
        return key != null && value != null && EVIDENCE_TYPE.equalsIgnoreCase(key.trim())
                && (SCREEN_VALIDATION.equalsIgnoreCase(value.trim())
                || UI_FAILURE.equalsIgnoreCase(value.trim()));
    }

    private static boolean isUiCheckpointPolicy(String key) {
        return key != null && (EVIDENCE_TYPE.equalsIgnoreCase(key.trim())
                || EVIDENCE_POLICY.equalsIgnoreCase(key.trim()));
    }

    public static boolean isRequestDetail(String key) {
        if (key == null) return false;
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("method") || normalized.equals("path")
                || normalized.equals("url") || normalized.equals("route")
                || normalized.equals("route template") || normalized.equals("request.route")
                || normalized.startsWith("request.") || normalized.startsWith("header.")
                || normalized.startsWith("headers.") || normalized.startsWith("param.")
                || normalized.startsWith("params.") || normalized.startsWith("query.")
                || normalized.startsWith("auth.") || normalized.equals("authorization")
                || normalized.equals("auth profile") || normalized.equals("auth type")
                || normalized.equals("transport") || normalized.equals("transport.provenance")
                || normalized.equals("manual replay") || normalized.equals("manual.replay")
                || normalized.equals("query params") || normalized.equals("request headers")
                || normalized.equals("trace.parent.step") || normalized.equals("trace.position")
                || normalized.equals("trace.exchange")
                || normalized.equals("body") || normalized.equals("request body");
    }

    private static boolean isExecutionSummaryField(String key) {
        if (key == null) return false;
        return switch (key.trim().toLowerCase(Locale.ROOT)) {
            case "duration", "response duration", "response duration (ms)", "response.duration.ms",
                    "total duration", "total duration (ms)", "total.duration.ms", "exchange.duration.ms", "response status",
                    "response.status", "attempt", "attempts", "response.url" -> true;
            default -> false;
        };
    }

    public static String displayName(String key) {
        if (key == null || key.isBlank()) return "DETAIL";
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        String apiExchangeLabel = apiExchangeDisplayName(normalized);
        if (apiExchangeLabel != null) return apiExchangeLabel;
        return switch (normalized) {
            case "method" -> "METHOD";
            case "path" -> "PATH";
            case "request.operation.kind" -> "OPERATION KIND";
            case "request.route.template" -> "ROUTE TEMPLATE";
            case "url" -> "URL";
            case "auth.mode" -> "AUTHENTICATION";
            case "auth.type", "auth type" ->
                    ApiEvidenceField.AUTH_TYPE.label().toUpperCase(Locale.ROOT);
            case "auth.profile", "auth profile" ->
                    ApiEvidenceField.AUTH_PROFILE.label().toUpperCase(Locale.ROOT);
            case "transport", "transport.provenance" -> "TRANSPORT PROVENANCE";
            case "manual replay", "manual.replay" -> "MANUAL REPLAY";
            case "response.status" -> "RESPONSE STATUS";
            case "response.content.type" -> ApiReportEvidenceField.CONTENT_TYPE.key();
            case "response.duration.ms" -> "RESPONSE DURATION";
            case "exchange.duration.ms", "total.duration.ms" -> "TOTAL DURATION";
            case "trace.parent.step" -> "TRACE PARENT STEP";
            case "trace.position" -> "TRACE POSITION";
            case "trace.exchange" -> "TRACE EXCHANGE";
            case "response duration", "response duration (ms)" -> "RESPONSE DURATION";
            case "total duration", "total duration (ms)" -> "TOTAL DURATION";
            case "attempt" -> "ATTEMPT";
            default -> key.replace('.', ' ').replace('_', ' ').toUpperCase(Locale.ROOT);
        };
    }

    /**
     * Keeps a multi-exchange UI trace readable while reusing the direct API
     * report vocabulary. The exchange number is the only UI-specific context;
     * the field name stays the same semantic label a direct API request uses.
     */
    private static String apiExchangeDisplayName(String normalized) {
        String[] parts = normalized.split("\\.");
        if (parts.length < 4 || !("request".equals(parts[0]) || "response".equals(parts[0]))
                || !"api".equals(parts[1]) || !parts[2].matches("\\d+")) {
            return null;
        }
        String field = String.join(".", java.util.Arrays.copyOfRange(parts, 3, parts.length));
        String label = switch (field) {
            case "method" -> "METHOD";
            case "path" -> "PATH";
            case "url" -> "URL";
            case "auth.mode", "auth.type", "auth.profile" -> displayName(field);
            case "query.params" -> "QUERY PARAMS";
            case "request.headers" -> "REQUEST HEADERS";
            case "transport" -> "TRANSPORT PROVENANCE";
            case "manual.replay" -> "MANUAL REPLAY";
            case "status" -> "HTTP STATUS";
            case "content.type" -> "CONTENT-TYPE";
            case "response.headers" -> "RESPONSE HEADERS";
            default -> null;
        };
        return label == null ? null : "API " + parts[2] + " · " + label;
    }

    /** Applies the shared display policy while keeping source keys backward-compatible. */
    public static String displayValue(String key, String value) {
        if (value == null || !isDurationKey(key)) return value == null ? "" : value;
        String normalized = value.trim();
        if (normalized.matches("(?i)^\\d+(?:\\.\\d+)?\\s*s$")) return normalized;
        String millis = normalized.replaceFirst("(?i)\\s*ms$", "").trim();
        if (!millis.matches("\\d+")) return value;
        try {
            return AtfDurationFormatter.formatMillis(Long.parseLong(millis));
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private static boolean isDurationKey(String key) {
        if (key == null) return false;
        return switch (key.trim().toLowerCase(Locale.ROOT)) {
            case "duration", "duration.ms", "response duration", "response duration (ms)", "response.duration.ms",
                    "total duration", "total duration (ms)", "total.duration.ms", "exchange.duration.ms" -> true;
            default -> false;
        };
    }
}
