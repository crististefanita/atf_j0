package atf.reporting.evidence.ui;

import atf.reporting.contract.AtfReportStatus;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.ui.model.AtfUiActionRecord;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.Markup;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Shared markup primitives for the UI action, observation and assertion views. */
public final class ExtentUiEvidenceMarkup {
    private ExtentUiEvidenceMarkup() {
    }

    public static String open(AtfReportHtmlVocabulary.Tag tag, String... attributes) {
        return AtfReportHtmlVocabulary.openTag(tag, attributes);
    }

    public static String close(AtfReportHtmlVocabulary.Tag tag) {
        return AtfReportHtmlVocabulary.closeTag(tag);
    }

    public static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static String tableHeader(String... labels) {
        StringBuilder html = new StringBuilder(open(AtfReportHtmlVocabulary.Tag.THEAD))
                .append(open(AtfReportHtmlVocabulary.Tag.TR));
        for (String label : labels) {
            html.append(open(AtfReportHtmlVocabulary.Tag.TH))
                    .append(escape(label))
                    .append(close(AtfReportHtmlVocabulary.Tag.TH));
        }
        return html.append(close(AtfReportHtmlVocabulary.Tag.TR))
                .append(close(AtfReportHtmlVocabulary.Tag.THEAD)).toString();
    }

    public static String actionRowStart() {
        return open(AtfReportHtmlVocabulary.Tag.TR)
                + open(AtfReportHtmlVocabulary.Tag.TD,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_IDENTITY))
                + open(AtfReportHtmlVocabulary.Tag.SPAN,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_OPERATION));
    }

    public static String resultSpan(AtfUiActionRecord action) {
        String stateClass = "atf-ui-action-result-" + action.status().displayValue();
        return open(AtfReportHtmlVocabulary.Tag.SPAN,
                AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.ATF_UI_ACTION_RESULT.value(), stateClass),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.ROLE, "img"),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.ARIA_LABEL,
                        action.resultLabel()),
                AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.TITLE,
                        action.resultLabel()))
                + action.resultEmoji() + close(AtfReportHtmlVocabulary.Tag.SPAN);
    }

    public static String actionSummary(List<AtfUiActionRecord> actions, boolean failed) {
        if (!failed) return AtfReportLabels.actionSequence(actions.size());
        long failures = actions.stream().filter(ExtentUiEvidenceMarkup::isFailure).count();
        return ExtentReportNodeSupport.resultMarker(AtfReportStatus.FAIL)
                + " " + AtfReportLabels.failedActions(failures, actions.size());
    }

    public static String observationSummary(List<AtfUiActionRecord> observations, boolean failed) {
        String checkpointLabel = observations.size() == 1 ? "checkpoint" : "checkpoints";
        if (failed) {
            long failures = observations.stream().filter(ExtentUiEvidenceMarkup::isFailure).count();
            return ExtentReportNodeSupport.resultMarker(AtfReportStatus.FAIL)
                    + " Audit observations · " + failures + " failed of " + observations.size();
        }
        return AtfReportLabels.AUDIT_OBSERVATIONS + " · " + observations.size()
                + " " + checkpointLabel;
    }

    public static boolean hasFailure(Collection<AtfUiActionRecord> actions) {
        return actions != null && actions.stream().filter(Objects::nonNull)
                .anyMatch(action -> isFailure(action.status()));
    }

    public static boolean isFailure(AtfUiActionRecord action) {
        return action != null && isFailure(action.status());
    }

    public static boolean isFailure(AtfReportStatus status) {
        return AtfReportStatus.FAIL == status;
    }

    public static String statusKey(boolean failed) {
        return (failed ? AtfReportStatus.FAIL : AtfReportStatus.PASS).displayValue();
    }

    public static void publish(ExtentTest node, String markup, boolean failed) {
        Markup evidence = () -> markup;
        if (failed) node.fail(evidence);
        else node.info(evidence);
    }
}
