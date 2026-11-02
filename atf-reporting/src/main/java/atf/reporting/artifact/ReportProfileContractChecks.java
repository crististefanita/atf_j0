package atf.reporting.artifact;

import atf.reporting.artifact.contract.ReportArtifactContractEvaluation;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;

/** Applies a declared inventory to original published owners, never to runtime templates. */
final class ReportProfileContractChecks {
    static final Set<String> REQUIRED = Set.of("html exists", "doctype", "profile renderer", "profile navigation",
            "autonomous artifact", "scenario inventory", "profile runtime", "profile theme");

    private ReportProfileContractChecks() { }

    static ReportArtifactContractEvaluation evaluate(String html, ReportArtifactProfile profile) {
        var parsed = new ReportProfileDocument(html, profile.renderer());
        var checks = new LinkedHashMap<String, Boolean>();
        checks.put("html exists", !html.isBlank());
        checks.put("doctype", parsed.document.childNodes().stream().anyMatch(DocumentType.class::isInstance));
        boolean extent = parsed.document.selectFirst("#test-view") != null;
        boolean fallback = parsed.document.selectFirst("section#tests") != null;
        checks.put("profile renderer", parsed.fallback ? fallback && !extent : extent && !fallback);
        checks.put("profile navigation", navigation(parsed));
        // Parsing never fetches URLs. Only embedded executable/stylesheet resources satisfy autonomy.
        var original = parsed.document;
        checks.put("autonomous artifact", original.select("script[src],link[rel=stylesheet][href]").stream()
                .allMatch(e -> (e.hasAttr("src") ? e.attr("src") : e.attr("href")).startsWith("data:")));
        checks.put("profile runtime", original.select(parsed.fallback ? "body > script" : "head > script#atf-report-behaviour").stream()
                .anyMatch(e -> e.data().contains(parsed.fallback ? "synchronizeFallbackDisclosureAria" : "window.atfActivate")));
        checks.put("profile theme", original.select("style").stream().anyMatch(e ->
                e.data().contains("--atf-dark") && e.data().contains(".atf-disclosure-glyph")));
        var actual = parsed.scenarios();
        checks.put("scenario inventory", actual.size() == profile.scenarios().size());
        var occurrences = new HashMap<List<String>, Integer>();
        for (int index = 0; index < profile.scenarios().size(); index++) {
            var expected = profile.scenarios().get(index);
            var key = List.of(expected.testClass(), expected.name());
            int ordinal = occurrences.merge(key, 1, Integer::sum) - 1;
            var matches = actual.stream().filter(s -> s.testClass().equals(expected.testClass()) && s.title().equals(expected.name())).toList();
            var scenario = ordinal < matches.size() ? matches.get(ordinal).element() : null;
            String prefix = "scenario[" + index + "]";
            checks.put(prefix + " identity", scenario != null);
            checks.put(prefix + " verdict", scenario != null && parsed.status(scenario).equals(status(expected.status())));
            var phases = scenario == null ? List.<Element>of() : parsed.phases(scenario);
            checks.put(prefix + " phases", phases.size() == expected.phases().size());
            for (int p = 0; p < expected.phases().size(); p++) {
                var phase = p < phases.size() ? phases.get(p) : null;
                var required = expected.phases().get(p);
                String owner = prefix + "/phase[" + p + "]";
                checks.put(owner + " identity", phase != null && parsed.title(phase).equals(required.name()));
                checks.put(owner + " verdict", phase != null && parsed.status(phase).equals(status(required.status())));
                var nodes = phase == null ? List.<Element>of() : parsed.evidence(phase);
                checks.put(owner + " evidence inventory", nodes.size() == required.evidence().size());
                for (int n = 0; n < required.evidence().size(); n++)
                    ReportProfileEvidenceChecks.evaluate(parsed, n < nodes.size() ? nodes.get(n) : null,
                            required.evidence().get(n), owner + "/evidence[" + n + "]", checks);
            }
        }
        var owners = new LinkedHashMap<String, String>(); checks.keySet().forEach(name -> owners.put(name, "declared-profile"));
        return new ReportArtifactContractEvaluation(checks, owners);
    }

    private static boolean navigation(ReportProfileDocument parsed) {
        var elements = parsed.document.select(parsed.fallback ? "nav.tabs > button[data-tab]" : "#nav-test,#nav-category,#nav-fails,#nav-overview");
        var names = parsed.fallback ? elements.stream().map(e -> e.attr("data-tab")).toList() : elements.stream().map(Element::id).toList();
        return names.equals(parsed.fallback ? List.of("tests", "category", "fails", "overview")
                : List.of("nav-test", "nav-category", "nav-fails", "nav-overview"));
    }
    static String status(atf.reporting.contract.AtfReportStatus status) { return status.name().toLowerCase(java.util.Locale.ROOT); }
}
