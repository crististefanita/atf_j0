package atf.reporting.ui.model;

import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

import java.util.Objects;

/**
 * Typed, renderer-neutral marker used to locate one report surface in HTML.
 *
 * <p>IDs and attributes reuse the HTML vocabulary. Text remains a value because
 * it is content, not a finite HTML token; this keeps the model strict without
 * turning dynamic report prose into an enum.</p>
 */
public sealed interface ReportSectionMarker
        permits ReportSectionMarker.HtmlId,
        ReportSectionMarker.HtmlAttribute,
        ReportSectionMarker.Text {

    String token();

    static ReportSectionMarker id(AtfReportHtmlVocabulary.ViewId id) {
        return new HtmlId(id);
    }

    static ReportSectionMarker attribute(AtfReportHtmlVocabulary.Attribute attribute,
                                         String value) {
        return new HtmlAttribute(attribute, value);
    }

    static ReportSectionMarker text(String value) {
        return new Text(value);
    }

    record HtmlId(AtfReportHtmlVocabulary.ViewId id) implements ReportSectionMarker {
        public HtmlId {
            id = Objects.requireNonNull(id, "id");
        }

        @Override
        public String token() {
            return AtfReportHtmlVocabulary.idAttribute(id);
        }
    }

    record HtmlAttribute(AtfReportHtmlVocabulary.Attribute attribute, String value)
            implements ReportSectionMarker {
        public HtmlAttribute {
            attribute = Objects.requireNonNull(attribute, "attribute");
            value = Objects.requireNonNull(value, "value");
        }

        @Override
        public String token() {
            return AtfReportHtmlVocabulary.attribute(attribute, value);
        }
    }

    record Text(String value) implements ReportSectionMarker {
        public Text {
            value = Objects.requireNonNull(value, "value");
        }

        @Override
        public String token() {
            return value;
        }
    }
}
