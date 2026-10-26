package atf.reporting.ui.model;

import atf.reporting.markup.AtfReportEvidenceKeys;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

import java.util.List;
import java.util.Objects;

/** Immutable marker model for a rendered report surface or evidence area. */
public record ReportSectionModel(Section section, String title,
                                 List<List<ReportSectionMarker>> markerAlternatives) {
    public ReportSectionModel {
        section = Objects.requireNonNull(section, "section");
        title = requireText(title, "title");
        markerAlternatives = Objects.requireNonNull(markerAlternatives, "markerAlternatives").stream()
                .map(markers -> List.copyOf(Objects.requireNonNull(markers, "markers")))
                .toList();
        if (markerAlternatives.isEmpty() || markerAlternatives.stream().anyMatch(List::isEmpty)) {
            throw new IllegalArgumentException("markerAlternatives must contain non-empty marker groups");
        }
    }

    public boolean isPresentIn(String html) {
        String source = html == null ? "" : html;
        return markerAlternatives.stream()
                .anyMatch(markers -> markers.stream().map(ReportSectionMarker::token)
                        .allMatch(source::contains));
    }

    public enum Section {
        OVERVIEW("Overview", AtfReportHtmlVocabulary.ViewId.OVERVIEW),
        TESTS("Tests", AtfReportHtmlVocabulary.ViewId.TESTS),
        CATEGORIES("Categories", AtfReportHtmlVocabulary.ViewId.CATEGORIES),
        FAILS("Fails", AtfReportHtmlVocabulary.ViewId.FAILS),
        SETUP_EVIDENCE("Setup evidence", null),
        UI_VALIDATION("UI validation", null);

        private final String displayName;
        private final AtfReportHtmlVocabulary.ViewId viewId;

        Section(String displayName, AtfReportHtmlVocabulary.ViewId viewId) {
            this.displayName = displayName;
            this.viewId = viewId;
        }

        public String displayName() {
            return displayName;
        }

        public AtfReportHtmlVocabulary.ViewId viewId() {
            return viewId;
        }
    }

    public static List<ReportSectionModel> extentUiDefaults() {
        return List.of(
                single(Section.OVERVIEW, "Overview"),
                single(Section.TESTS, "Tests"),
                single(Section.CATEGORIES, "Categories"),
                single(Section.FAILS, "Fails"),
                new ReportSectionModel(Section.SETUP_EVIDENCE, "Setup evidence",
                        List.of(List.of(ReportSectionMarker.attribute(
                                AtfReportHtmlVocabulary.Attribute.EVIDENCE_TYPE,
                                AtfReportEvidenceKeys.REPORT_SETUP)))),
                new ReportSectionModel(Section.UI_VALIDATION, "UI validation",
                        List.of(markers(
                                ReportSectionMarker.text(Section.UI_VALIDATION.displayName()),
                                ReportSectionMarker.text("class=\"atf-ui-action-sequence\"")))));
    }

    private static ReportSectionModel single(Section section, String title) {
        return new ReportSectionModel(section, title,
                List.of(List.of(
                        ReportSectionMarker.id(
                                Objects.requireNonNull(section.viewId(), "viewId")))));
    }

    private static List<ReportSectionMarker> markers(ReportSectionMarker... markers) {
        return List.of(markers);
    }

    private static String requireText(String value, String field) {
        if (Objects.requireNonNull(value, field).isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
