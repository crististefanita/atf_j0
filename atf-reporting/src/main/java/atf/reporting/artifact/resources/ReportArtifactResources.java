package atf.reporting.artifact.resources;

import atf.reporting.artifact.ReportArtifactDiagnostic;
import atf.reporting.security.SensitiveDataRedactor;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** DOM-only autonomy policy shared by published Extent and fallback inspections; never fetches a resource. */
public final class ReportArtifactResources {
    private ReportArtifactResources() { }

    public static Inspection inspect(String html) { return inspect(Jsoup.parse(html == null ? "" : html)); }

    public static Inspection inspect(Document document) {
        var violations = new ArrayList<Violation>();
        var ids = document.getAllElements().stream().filter(e -> !e.id().isEmpty())
                .collect(java.util.stream.Collectors.groupingBy(Element::id));
        for (Element element : document.getAllElements()) {
            String tag = element.normalName();
            for (String attribute : List.of("src", "poster", "background", "srcset")) {
                if (!element.hasAttr(attribute)) continue;
                if (attribute.equals("srcset")) {
                    for (String reference : ResourceReferences.srcset(element.attr(attribute)))
                        resource(document, element, attribute, reference, violations);
                } else resource(document, element, attribute, element.attr(attribute), violations);
            }
            if (tag.equals("object") && element.hasAttr("data")) resource(document, element, "data", element.attr("data"), violations);
            for (String attribute : List.of("href", "xlink:href")) {
                if (!element.hasAttr(attribute)) continue;
                String reference = element.attr(attribute).strip();
                if (tag.equals("a")) {
                    if (ResourceReferences.externalEvidence(element, reference))
                        reject(element, attribute, reference, "Evidence must be embedded in this HTML", violations);
                    screenshot(ids, element, attribute, reference, violations);
                } else if (tag.equals("base")) {
                    if (!reference.isEmpty()) reject(element, attribute, reference, "External base changes artifact-relative navigation", violations);
                } else resource(document, element, attribute, reference, violations);
            }
            if (element.hasAttr("srcdoc")) {
                var nested = inspect(element.attr("srcdoc"));
                for (var issue : nested.violations())
                    violations.add(new Violation(element.cssSelector() + " / srcdoc / " + issue.owner(), issue.attribute(), issue.reference(), issue.reason()));
            }
            if (element.hasAttr("style")) css(document, element, "style", element.attr("style"), violations);
            if (tag.equals("style")) css(document, element, "text", element.data(), violations);
        }
        return new Inspection(violations);
    }

    private static void css(Document document, Element owner, String attribute, String text, List<Violation> violations) {
        for (String reference : ResourceReferences.css(text)) resource(document, owner, attribute, reference, violations);
    }

    private static void resource(Document document, Element element, String attribute, String reference, List<Violation> violations) {
        String value = reference.strip();
        if (value.isEmpty() || ResourceReferences.embedded(value)) return;
        if (value.startsWith("#") && document.getElementById(value.substring(1)) != null) return;
        reject(element, attribute, value, "Resource must be embedded or resolve inside this HTML", violations);
    }

    private static void screenshot(java.util.Map<String, List<Element>> ids, Element element, String attribute, String reference, List<Violation> violations) {
        if (!element.hasAttr("data-atf-screenshot-ref") || ResourceReferences.embedded(reference)) return;
        String ref = element.attr("data-atf-screenshot-ref"), id = "atf-screenshot-" + ref;
        var targets = ids.getOrDefault(id, List.of());
        boolean valid = reference.equals("#" + id) && targets.size() == 1
                && targets.getFirst().attr("data-atf-screenshot-ref").equals(ref)
                && targets.getFirst().attr("href").startsWith("data:image/png;base64,");
        if (!valid) reject(element, attribute, reference, "Screenshot reference needs one canonical inline PNG", violations);
    }

    private static void reject(Element element, String attribute, String reference, String reason, List<Violation> violations) {
        violations.add(new Violation(element.cssSelector(), attribute, reference, reason));
    }

    public record Violation(String owner, String attribute, String reference, String reason) { }

    public record Inspection(List<Violation> violations) {
        public Inspection { violations = List.copyOf(violations); }
        public boolean isAutonomous() { return violations.isEmpty(); }
        /** One check has one diagnostic, including every rejected operative reference. */
        public ReportArtifactDiagnostic diagnostic() {
            var redactor = new SensitiveDataRedactor();
            String evidence = violations.stream().map(v -> v.owner() + " [" + v.attribute() + "] "
                    + redactor.redactText(v.reference()) + " — " + v.reason()).collect(java.util.stream.Collectors.joining("\n"));
            return new ReportArtifactDiagnostic("artifact-resources", "autonomous artifact", isAutonomous(), evidence,
                    isAutonomous() ? "All operative resources are embedded" : "Artifact depends on external or unresolved evidence/resources");
        }
    }
}
