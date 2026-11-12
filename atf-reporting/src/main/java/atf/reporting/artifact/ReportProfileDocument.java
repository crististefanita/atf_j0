package atf.reporting.artifact;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** Reads published owners only. Script/template strings and secondary-view copies are never evidence. */
final class ReportProfileDocument {
    record Scenario(Element element, String testClass, String title) { }
    final Document document;
    final boolean fallback;

    ReportProfileDocument(String html, ReportArtifactProfile.Renderer renderer) {
        document = Jsoup.parse(html); fallback = renderer == ReportArtifactProfile.Renderer.FALLBACK;
        // HTML5 raw-text script/style nodes are not elements and cannot satisfy evidence selectors.
        document.select("template,noscript").remove();
    }

    List<Scenario> scenarios() {
        var result = new ArrayList<Scenario>();
        if (fallback) {
            for (var scenario : document.select("#tests .test-content > article.scenario")) {
                String id = scenario.id();
                var link = document.select("#tests aside a.test-link").stream().filter(e -> e.attr("href").equals("#" + id)).toList();
                result.add(new Scenario(scenario, link.size() == 1 ? literalText(link.getFirst().selectFirst("small")) : "",
                        literalText(scenario.selectFirst(".scenario-title > h2"))));
            }
        } else {
            for (var item : document.select("#test-view .test-list-wrapper > .test-list-item > .test-item")) {
                String type = literalText(item.selectFirst(".test-detail > .name"));
                var contents = item.selectFirst(".test-contents");
                var accordion = contents == null ? null : contents.selectFirst(".mt-4 > .accordion");
                if (accordion != null) for (var scenario : accordion.children())
                    if (scenario.hasClass("card")) result.add(new Scenario(scenario, type, title(scenario)));
            }
        }
        return List.copyOf(result);
    }

    List<Element> phases(Element scenario) {
        return fallback ? scenario.children().stream().filter(e -> "phase".equals(e.attr("data-atf-node-role"))).toList() : cards(scenario);
    }

    List<Element> evidence(Element phase) {
        if (!fallback) return cards(phase);
        return phase.children().stream().filter(e -> List.of("assertion-step", "request", "observation")
                .contains(e.attr("data-atf-node-role"))).toList();
    }

    List<Element> validations(Element request) {
        return fallback ? request.children().stream().filter(e -> "validation".equals(e.attr("data-atf-node-role"))).toList() : cards(request);
    }

    String title(Element element) {
        if (!fallback) return literalText(element.selectFirst(".card-header > .card-title > a.node > span"));
        if ("phase".equals(element.attr("data-atf-node-role"))) return ownTitle(element.selectFirst("h3"));
        if (List.of("request", "validation").contains(element.attr("data-atf-node-role"))) return literalText(element.selectFirst(".node-title > span"));
        return disclosureTitle(element.selectFirst("summary"));
    }

    String status(Element element) {
        var badge = fallback ? element.selectFirst(".scenario-title > .status, h3 > .status, summary > .status, .node-title > .status")
                : element.selectFirst(".card-header > .card-toolbar .badge.log");
        return text(badge).toLowerCase(java.util.Locale.ROOT);
    }

    List<Element> owned(Element owner, String selector) {
        return owner.select(selector).stream().filter(e -> nearestOwner(e) == owner).toList();
    }

    private Element nearestOwner(Element element) {
        for (var current = element.parent(); current != null; current = current.parent()) {
            if (!fallback && current.hasClass("card")) return current;
            if (fallback && List.of("request", "validation", "assertion-step", "observation").contains(current.attr("data-atf-node-role"))) return current;
        }
        return null;
    }

    private static List<Element> cards(Element parent) {
        return parent.children().stream().filter(e -> e.hasClass("accordion"))
                .flatMap(e -> e.children().stream()).filter(e -> e.hasClass("card")).toList();
    }
    static String text(Element element) { return element == null ? "" : element.text().trim(); }
    static String literalText(Element element) {
        return element == null || element.childrenSize() != 0 ? "" : text(element);
    }
    static String disclosureTitle(Element summary) {
        if (summary == null) return "";
        var labels = summary.children().stream().filter(child -> child.hasClass("se-title")).toList();
        return labels.size() == 1 ? literalText(labels.getFirst()) : "";
    }
    private static String ownTitle(Element element) {
        if (element == null) return "";
        var copy = element.clone();
        copy.select(".status,.badge,[hidden][data-atf-execution-duration-ms]").remove();
        return literalText(copy);
    }
}
