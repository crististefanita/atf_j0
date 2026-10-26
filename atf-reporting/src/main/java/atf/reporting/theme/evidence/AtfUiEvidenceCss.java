package atf.reporting.theme.evidence;

import atf.reporting.asset.ReportAssetComposer;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;

/**
 * CSS contract for the inline exploratory UI evidence block.
 *
 * <p>The selectors are supplied by the typed HTML vocabulary and the finite
 * colours come from the reporting palette. Runtime capture titles, steps and
 * image data stay outside this class. The stylesheet is a native CSS asset;
 * this facade provides only the typed composition boundary.</p>
 */
public final class AtfUiEvidenceCss {
    private AtfUiEvidenceCss() {
    }

    public static String stylesheet(AtfReportHtmlVocabulary.ElementId sectionId,
                                    AtfReportHtmlVocabulary.CssClass screenshotLinkClass) {
        return ReportAssetComposer.uiEvidenceCss(sectionId, screenshotLinkClass);
    }
}
