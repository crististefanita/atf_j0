package atf.reporting.ui.contract;

import atf.reporting.ui.model.ReportSectionModel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure contract for the published UI Extent artifact.
 *
 * <p>This is intentionally source-based rather than browser-based: the
 * reporting module owns the HTML/CSS/JavaScript and can prove the complete
 * evidence surface without a running browser. Playwright remains a thin
 * end-to-end smoke adapter in {@code atf-tests}.</p>
 */
public final class UiReportArtifactContract {
    private UiReportArtifactContract() {
    }

    public static Evaluation evaluate(String html) {
        String source = html == null ? "" : html;
        String lower = source.toLowerCase(Locale.ROOT);
        Map<String, Boolean> checks = new LinkedHashMap<>();
        checks.put("ui doctype", lower.contains("<!doctype html>"));
        checks.put("typed report section model", ReportSectionModel.extentUiDefaults().stream()
                .allMatch(section -> section.isPresentIn(source)));
        checks.put("four navigation tabs", (hasAll(source,
                "data-tab=\"overview\"", "data-tab=\"tests\"",
                "data-tab=\"category\"", "data-tab=\"fails\"")
                || hasAll(source, "id=\"test-view\"", "id=\"dashboard-view\"",
                "id=\"category-view\"", "id=\"atf-fails-view\"")));
        checks.put("autonomous navigation", hasAll(source,
                "data-atf-view=\"test-view\"", "data-atf-view=\"category-view\"",
                "data-atf-view=\"atf-fails-view\"", "data-atf-view=\"dashboard-view\"",
                "function bindNavigationSelectionLinks()", "function activateFromHash()",
                "window.addEventListener('hashchange',activateFromHash)", "aria-current", "location.hash"));
        checks.put("semantic phase hierarchy", hasAll(source,
                "data-atf-node-role=\"phase\"", "data-atf-node-role=\"request\"",
                "data-atf-node-role=\"validation\""));
        checks.put("setup evidence is separate", setupEvidenceIsSeparate(source));
        checks.put("UI validation evidence", hasAll(source,
                "UI validation", "class=\"atf-ui-action-sequence\""));
        checks.put("legacy validation title absent", !source.contains("<span>Screen validation</span>")
                && !source.contains(" · Screen validation"));
        checks.put("UI validation sibling layout", hasAll(source,
                ".test-view .test-content-detail .card-body>.table>tbody>tr.event-row:has(details.atf-ui-action-sequence)>td",
                ".test-view .test-content-detail .card-body>.table>tbody>tr.event-row:has(details.atf-ui-observations)>td",
                ".test-view .test-content-detail .card-body>.table>tbody>tr.event-row:has(table.helm-validation-table)>td",
                "border-top:0!important;border-bottom:0!important"));
        checks.put("Playwright action table", hasAll(source,
                "UI action sequence", "<th>ACTION</th>", "<th>TARGET</th>",
                "<th>EXPECTED</th>", "<th>RESULT</th>"));
        checks.put("audit observations", hasAll(source,
                "class=\"atf-ui-observations\"", "Audit observations"));
        checks.put("assertion table", hasAll(source,
                "helm-validation-table", "ASSERTION", "EXPECTED", "ACTUAL", "RESULT"));
        checks.put("inline screenshot link", hasAll(source,
                "class=\"atf-ui-action-screenshot-link\"",
                "href=\"data:image/png;base64,", "data-featherlight=\"image\"",
                "target=\"_blank\"", "rel=\"noopener\""));
        checks.put("closed disclosures", !lower.contains("<details open"));
        checks.put("inline plus glyph", glyphRulesPresent(source));
        checks.put("long proposition wrapping", hasAll(source,
                "overflow-wrap:anywhere!important", "word-break:break-word!important"));
        return new Evaluation(checks);
    }

    private static boolean setupEvidenceIsSeparate(String source) {
        String marker = "data-atf-evidence-type=\"report-setup\"";
        int start = source.indexOf(marker);
        if (start < 0) return false;
        int end = source.indexOf("</details>", start);
        if (end < 0) return false;
        String setup = source.substring(start, end);
        return !setup.contains("UI validation")
                && !setup.contains("atf-ui-action-sequence")
                && !setup.contains("atf-ui-observations");
    }

    private static boolean hasAll(String source, String... markers) {
        for (String marker : markers) {
            if (!source.contains(marker)) return false;
        }
        return true;
    }

    private static boolean glyphRulesPresent(String source) {
        return hasAll(source,
                ".atf-ui-action-sequence>summary:before{content:'＋'",
                ".atf-ui-observations>summary:before{content:'＋'",
                ".atf-ui-action-sequence[open]>summary:before{content:'−'")
                || hasAll(source,
                ".atf-ui-action-sequence>summary:before{content:\"＋\"",
                ".atf-ui-observations>summary:before{content:\"＋\"",
                ".atf-ui-action-sequence[open]>summary:before{content:\"−\"");
    }

    public record Evaluation(Map<String, Boolean> checks) {
        public Evaluation {
            checks = Map.copyOf(new LinkedHashMap<>(checks == null ? Map.of() : checks));
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
