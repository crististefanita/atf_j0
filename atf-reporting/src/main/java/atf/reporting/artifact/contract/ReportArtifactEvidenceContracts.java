package atf.reporting.artifact.contract;

import java.util.Map;

final class ReportArtifactEvidenceContracts {
    private ReportArtifactEvidenceContracts() { }

    static void evaluate(String html, Map<String, Boolean> checks) {
        checks.put("autonomous artifact", !html.contains("spark/")
                && !html.contains("fontawesome-webfont.woff2"));
        checks.put("collapsed evidence", !html.contains("class=\"collapse show\""));
        checks.put("scrollable JSON", html.contains("code-block") && html.contains("overflow:auto"));
        checks.put("compact technical trace", html.contains("TECHNICAL TRACE")
                && html.toUpperCase(java.util.Locale.ROOT).contains("RESPONSE DURATION"));
        checks.put("evidence visual contract", html.contains(".helm-scroll-block")
                && html.contains("max-height:360px!important;overflow:auto!important")
                && html.contains(".helm-validation-table")
                && html.contains(".step-detail>summary"));
        checks.put("collapsible evidence runtime", html.contains("function normalizeEvidenceDisclosures()")
                && html.contains("response-evidence") && html.contains("json-evidence")
                && html.contains("assertion-evidence") && html.contains("execution-evidence")
                && html.contains("helm-scroll-block"));
        checks.put("request response evidence split", html.contains("Request details")
                && html.contains("Response details")
                && html.contains("helm-request-details-content")
                && html.contains("helm-response-details-content")
                && html.contains("response-json-body"));
        checks.put("execution evidence disclosure", html.contains("Execution evidence")
                && html.contains("execution-table")
                && html.contains("execution-evidence-body")
                && html.contains("retry-history-table"));
        checks.put("technical trace", html.contains("TECHNICAL TRACE"));
        checks.put("response duration", html.toUpperCase(java.util.Locale.ROOT)
                .contains("RESPONSE DURATION"));
        checks.put("aggregate assertion table", html.contains("helm-validation-table")
                && html.contains("ASSERTION") && html.contains("EXPECTED")
                && html.contains("ACTUAL") && html.contains("RESULT"));
        checks.put("request-validation roles", html.contains("data-atf-node-role=\"request\"")
                && html.contains("data-atf-node-role=\"validation\""));
        checks.put("scenario-only tags", html.contains("data-atf-node-role=\"phase\"]>.card-header .node-attr")
                && html.contains("data-atf-node-role=\"request\"")
                && html.contains("data-atf-node-role=\"validation\""));
        checks.put("numeric duration normalization", html.contains("function normalizeDurations()")
                && html.toUpperCase(java.util.Locale.ROOT).contains("RESPONSE DURATION"));
        checks.put("duration coverage", html.contains(".test-list-item .text-sm span")
                && html.contains(".detail-head .info > .badge-default")
                && (html.contains("function atfDurationValue(raw)")
                && html.contains("function atfNormalizeDuration(e,source)")
                || html.contains("function atfParseDuration(raw)"))
                && html.contains("long=value.match")
                && html.contains("short=value.match")
                && html.contains("function aggregateClassDurations()")
                && html.contains("data-duration-ms")
                && html.contains("data-duration-source"));
        checks.put("failure evidence projection", html.contains("atf-failure-evidence")
                && html.contains("data-atf-failure-phase")
                && html.contains("REQUEST") && html.contains("VALIDATION")
                && html.contains("ASSERTION")
                && html.contains("Failure recorded; see exact test for full evidence")
                && html.contains("data-atf-test-id")
                && html.contains("data-atf-scenario-index")
                && html.contains("data-atf-failure-ordinal")
                && html.contains("data-atf-failure-kind")
                && html.contains("data-atf-evidence-owner")
                && html.contains("atf-failure-open")
                && html.contains("Open in Tests")
                && !html.contains("atf-failure-test-detail"));
        checks.put("evidence cardinality contract", html.contains("traceTables")
                && html.contains("jsonBlocks") && html.contains("assertionTables")
                && html.contains("categoryLinks"));
        checks.put("result markers", html.contains("✅") || html.contains("⚠️") || html.contains("❌"));
    }
}
