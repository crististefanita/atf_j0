package atf.reporting.artifact.contract;

import java.util.Map;

final class ReportArtifactStructuralContracts {
    private ReportArtifactStructuralContracts() { }

    static void evaluate(String html, Map<String, Boolean> checks) {
        checks.put("dom contract", html.contains("window.__atfDomContract")
                && html.contains("violations"));
        checks.put("runtime bootstrap placement", html.indexOf("id=\"atf-report-behaviour\"") >= 0
                && html.indexOf("id=\"atf-report-behaviour\"") < html.indexOf("</head>"));
    }
}
