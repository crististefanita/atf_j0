package atf.reporting.html.contract;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static, renderer-specific contract for the autonomous fallback HTML.
 * Extent artifacts use {@code ReportArtifactContractChecks}; this contract
 * validates the fallback surface and shares the DOM-aware resource policy.
 */
public final class HtmlReportStaticContract {
    private HtmlReportStaticContract() { }

    public static Evaluation evaluate(String html) {
        String source = html == null ? "" : html;
        Map<String, Boolean> checks = new LinkedHashMap<>();
        checks.put("doctype", source.toLowerCase().contains("<!doctype html>"));
        checks.put("semantic phase shell", source.contains("data-atf-node-role=\"phase\""));
        checks.put("request validation roles", source.contains("data-atf-node-role=\"request\"")
                && source.contains("data-atf-node-role=\"validation\""));
        checks.put("autonomous tabs", source.contains("<nav class=\"tabs\">")
                && source.contains("data-tab=\"tests\"")
                && source.contains("data-tab=\"fails\""));
        checks.put("collapsed evidence", !source.contains("class=\"collapse show\""));
        checks.put("json evidence channels", bodyChannel(source, "request") && bodyChannel(source, "response"));
        checks.put("aggregate assertions", source.contains("ASSERTION")
                && source.contains("EXPECTED") && source.contains("ACTUAL")
                && source.contains("RESULT"));
        checks.put("runtime asset", source.contains("<script")
                && source.contains("activate('overview')"));
        checks.put("hex-only theme", !source.matches("(?s).*rgba?\\s*\\(.*"));
        // Historical key retained; literal paths are provenance, operative references must be autonomous.
        checks.put("no project absolute paths", atf.reporting.artifact.resources.ReportArtifactResources.inspect(source).isAutonomous());
        return new Evaluation(checks);
    }

    private static boolean bodyChannel(String source, String side) {
        String label = side.toUpperCase(java.util.Locale.ROOT);
        return ((source.contains(label + " JSON") || source.contains(label + " FORM BODY")
                || source.contains(label + " BODY")) && source.contains("class=\"json-code\""))
                || source.contains("data-atf-" + side + "-body-availability=\"ABSENT\"")
                || source.contains("data-atf-" + side + "-body-availability=\"NOT_CAPTURED\"");
    }

    public record Evaluation(Map<String, Boolean> checks) {
        public Evaluation {
            checks = checks == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(checks));
        }

        public boolean isCompliant() {
            return checks.values().stream().allMatch(Boolean.TRUE::equals);
        }

        public List<String> violations() {
            return checks.entrySet().stream()
                    .filter(entry -> !Boolean.TRUE.equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .toList();
        }

        public boolean satisfies(String name) {
            return Boolean.TRUE.equals(checks.get(name));
        }
    }
}
