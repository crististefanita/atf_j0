package atf.reporting.artifact;

import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;

/** Row/cardinality and containment checks; a sibling or script cannot satisfy another owner's inventory. */
final class ReportProfileEvidenceChecks {
    private ReportProfileEvidenceChecks() { }

    static void evaluate(ReportProfileDocument document, Element owner, ReportArtifactProfile.Evidence expected,
                         String prefix, Map<String, Boolean> checks) {
        checks.put(prefix + " identity", owner != null && document.title(owner).equals(expected.title()));
        if (expected instanceof ReportArtifactProfile.Assertions assertions) {
            assertions(document, owner, assertions, prefix, checks);
        } else if (expected instanceof ReportArtifactProfile.HttpExchange exchange) {
            checks.put(prefix + " verdict", owner != null && document.status(owner).equals(ReportProfileContractChecks.status(exchange.status())));
            checks.put(prefix + " request", !exchange.requestRequired() || owner != null
                    && !document.owned(owner, document.fallback ? ".request-details-content" : ".helm-request-details-content").isEmpty());
            checks.put(prefix + " response", !exchange.responseRequired() || owner != null
                    && !document.owned(owner, document.fallback ? ".response-details-content" : ".helm-response-details-content").isEmpty());
            var summaries = owner == null ? List.<Element>of() : document.owned(owner, ".helm-scroll-block-title");
            for (int i = 0; i < exchange.bodyLabels().size(); i++) {
                String label = exchange.bodyLabels().get(i);
                checks.put(prefix + " body[" + i + "]", summaries.stream().anyMatch(e -> e.text().trim().equals(label)));
            }
            var validations = owner == null ? List.<Element>of() : document.validations(owner);
            checks.put(prefix + " validations", validations.size() == exchange.validations().size());
            for (int i = 0; i < exchange.validations().size(); i++) {
                var validation = i < validations.size() ? validations.get(i) : null;
                evaluate(document, validation, exchange.validations().get(i), prefix + "/validation[" + i + "]", checks);
            }
        } else if (expected instanceof ReportArtifactProfile.Observation observation) {
            var tables = owner == null ? List.<Element>of() : document.owned(owner, "table.helm-evidence-table");
            var fields = tables.stream().flatMap(t -> t.select("tbody > tr").stream()).map(e -> ReportProfileDocument.text(e.selectFirst("td"))).toList();
            checks.put(prefix + " fields", fields.equals(observation.fieldNames().stream().map(v -> v.toUpperCase(java.util.Locale.ROOT)).toList()));
            var summaries = owner == null ? List.<Element>of() : document.owned(owner, "details.atf-observation-evidence > summary");
            var bodies = summaries.stream().skip(1).map(e -> e.text().trim()).toList();
            checks.put(prefix + " bodies", bodies.equals(observation.bodyLabels()));
        }
    }

    private static void assertions(ReportProfileDocument document, Element owner, ReportArtifactProfile.Assertions expected,
                                   String prefix, Map<String, Boolean> checks) {
        var tables = owner == null ? List.<Element>of() : document.owned(owner, document.fallback ? "table.assertions" : "table.helm-validation-table");
        checks.put(prefix + " verdict", owner != null && document.status(owner).equals(ReportProfileContractChecks.status(expected.status())));
        checks.put(prefix + " assertion table", tables.size() == 1);
        var table = tables.size() == 1 ? tables.getFirst() : null;
        var headers = table == null ? List.<String>of() : table.select("thead > tr > th").eachText();
        checks.put(prefix + " assertion columns", headers.equals(List.of("ASSERTION", "EXPECTED", "ACTUAL", "RESULT")));
        var rows = table == null ? List.<Element>of() : table.select("tbody > tr").stream().toList();
        checks.put(prefix + " assertion rows", rows.size() == expected.comparisons().size());
        for (int i = 0; i < expected.comparisons().size(); i++) {
            var cells = i < rows.size() ? rows.get(i).children().stream().filter(e -> e.tagName().equals("td")).toList() : List.<Element>of();
            var comparison = expected.comparisons().get(i);
            checks.put(prefix + "/row[" + i + "] identity", cells.size() == 4 && cells.getFirst().text().equals(comparison.name()));
            checks.put(prefix + "/row[" + i + "] verdict", cells.size() == 4 && cells.get(3).text().equals(
                    document.fallback ? comparison.status().displayLabel() : comparison.status().marker()));
        }
    }
}
