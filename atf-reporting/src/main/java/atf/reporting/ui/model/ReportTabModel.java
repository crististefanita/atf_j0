package atf.reporting.ui.model;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

import java.util.Objects;

/** Immutable navigation model for one renderer-neutral fallback tab. */
public record ReportTabModel(AtfReportHtmlVocabulary.FallbackTab tab) {
    public ReportTabModel {
        tab = Objects.requireNonNull(tab, "tab");
    }

    public String panelId() {
        return tab.value();
    }

    public String label() {
        return tab.label();
    }
}
