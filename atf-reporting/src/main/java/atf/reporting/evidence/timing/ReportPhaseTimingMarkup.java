package atf.reporting.evidence.timing;

import atf.reporting.duration.AtfDurationFormatter;
import atf.reporting.model.timing.ReportPhaseTiming;
import static atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary.*;

/** Shared, typed metadata and presentation for an execution measured before its publication. */
public final class ReportPhaseTimingMarkup {
    private ReportPhaseTimingMarkup() { }
    public static String metadata(ReportPhaseTiming timing, long publicationMillis) {
        return element(Tag.SPAN, "", attribute(Attribute.HIDDEN, "hidden"),
                attribute(Attribute.EXECUTION_DURATION, Long.toString(timing.durationMillis())),
                attribute(Attribute.EXECUTION_NANOS, Long.toString(timing.execution().toNanos())),
                attribute(Attribute.EXECUTION_INTERVALS, Integer.toString(timing.intervals())),
                attribute(Attribute.PUBLICATION_DURATION, Long.toString(publicationMillis)));
    }
    public static String badge(long millis, String explanation) {
        return badge(millis, explanation, "aggregated");
    }
    private static String badge(long millis, String explanation, String source) {
        return element(Tag.SPAN, AtfDurationFormatter.formatMillis(millis), classAttribute(CssClass.BADGE),
                attribute(Attribute.DURATION, Long.toString(millis)), attribute(Attribute.TITLE, explanation),
                attribute(Attribute.DURATION_SOURCE, source));
    }
    public static String phase(ReportPhaseTiming timing, long publicationMillis) {
        return metadata(timing, publicationMillis) + badge(timing.durationMillis(),
                "Execution of " + timing.intervals() + " callbacks; snapshot publication: "
                        + AtfDurationFormatter.formatMillis(publicationMillis), "execution");
    }
}
