package atf.reporting.ui.model;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Immutable composition model for the fallback report shell.
 *
 * <p>The model is deliberately smaller than the semantic execution tree. It
 * describes the stable document contract (navigation and audited surfaces),
 * while scenario/request/assertion data remains runtime input. This gives
 * renderers and tests a single source of truth without turning every visible
 * sentence into an enum.</p>
 */
public record ReportDocumentModel(List<ReportTabModel> tabs,
                                  List<ReportSectionModel> sections) {
    public ReportDocumentModel {
        tabs = List.copyOf(Objects.requireNonNull(tabs, "tabs"));
        sections = List.copyOf(Objects.requireNonNull(sections, "sections"));
        if (tabs.isEmpty()) {
            throw new IllegalArgumentException("tabs must not be empty");
        }
        if (tabs.stream().map(ReportTabModel::panelId).distinct().count() != tabs.size()) {
            throw new IllegalArgumentException("tab panel ids must be unique");
        }
        var sectionNames = new HashSet<ReportSectionModel.Section>();
        for (var section : sections) {
            if (!sectionNames.add(section.section())) {
                throw new IllegalArgumentException("section must be unique: " + section.section());
            }
        }
    }

    /** Canonical order used by the standalone renderer and its browser tests. */
    public static ReportDocumentModel fallback() {
        return new ReportDocumentModel(
                List.of(
                        new ReportTabModel(AtfReportHtmlVocabulary.FallbackTab.TESTS),
                        new ReportTabModel(AtfReportHtmlVocabulary.FallbackTab.CATEGORY),
                        new ReportTabModel(AtfReportHtmlVocabulary.FallbackTab.FAILS),
                        new ReportTabModel(AtfReportHtmlVocabulary.FallbackTab.OVERVIEW)),
                ReportSectionModel.extentUiDefaults());
    }

    public ReportTabModel tab(AtfReportHtmlVocabulary.FallbackTab tab) {
        return tabs.stream()
                .filter(candidate -> candidate.tab() == Objects.requireNonNull(tab, "tab"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown tab: " + tab));
    }
}
