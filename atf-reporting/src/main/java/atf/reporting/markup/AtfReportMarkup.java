package atf.reporting.markup;
import atf.reporting.evidence.EvidenceTableFormatter;
import atf.reporting.evidence.ReportEvidenceTable;
import atf.reporting.markup.vocabulary.AtfReportHtmlVocabulary;
import atf.reporting.markup.table.ReportTableViewport;
import atf.reporting.theme.tokens.AtfReportCssProperty;

import com.aventstack.extentreports.markuputils.Markup;

import java.util.List;

/** Builds the semantic evidence tables consumed by the shared report theme. */
public final class AtfReportMarkup {
    private AtfReportMarkup() {
    }

    public static Markup table(String cssClass, String[][] rows) {
        return () -> tableHtml(cssClass, rows);
    }

    public static Markup table(String cssClass, ReportEvidenceTable table) {
        return () -> tableHtml(cssClass, table);
    }

    public static String tableHtml(String cssClass, String[][] rows) {
        return tableHtml(cssClass, ReportEvidenceTable.from(rows));
    }

    public static String tableHtml(String cssClass, ReportEvidenceTable table) {
        ReportEvidenceTable safeTable = table == null
                ? ReportEvidenceTable.from(null)
                : table;
        var html = new StringBuilder();
        if (isTraceTable(cssClass)) {
            html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.DIV,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.HELM_API_CLASSIFICATION,
                                    AtfReportHtmlVocabulary.CssClass.TECHNICAL)))
                    .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.SPAN,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.HELM_API_CLASSIFICATION_LABEL)))
                    .append(AtfReportLabels.TECHNICAL_TRACE)
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.SPAN))
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV));
        }
        html.append(tableViewport(cssClass).open());
        html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.TABLE,
                AtfReportHtmlVocabulary.classAttribute(cssClass)));
        if (isKeyValueEvidenceTable(cssClass) && !safeTable.headers().isEmpty()) {
            html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.COLGROUP))
                    .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.COL,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.EVIDENCE_KEY_COLUMN),
                            AtfReportHtmlVocabulary.attribute(AtfReportHtmlVocabulary.Attribute.STYLE,
                                    AtfReportCssProperty.WIDTH.value()
                                            + ":var(--atf-evidence-key-column-width)")))
                    .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.COL,
                            AtfReportHtmlVocabulary.classAttribute(
                                    AtfReportHtmlVocabulary.CssClass.EVIDENCE_VALUE_COLUMN)))
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.COLGROUP));
        }
        html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.THEAD))
                .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.TR));
        if (safeTable.headers().isEmpty()) {
            html.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TR))
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.THEAD))
                    .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.TBODY))
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TBODY))
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TABLE));
            return closeTableViewport(html).toString();
        }
        for (String value : safeTable.headers()) {
                    html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.TH))
                    .append(AtfReportHtmlVocabulary.escapeHtml(value))
                    .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TH));
        }
        html.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TR))
                .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.THEAD))
                .append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.TBODY));
        for (List<String> row : safeTable.rows()) {
            html.append(AtfReportHtmlVocabulary.openTag(AtfReportHtmlVocabulary.Tag.TR));
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                String value = row.get(columnIndex);
                if (isKeyValueEvidenceTable(cssClass) && columnIndex == 1 && !row.isEmpty()) {
                    value = EvidenceTableFormatter.displayValue(row.get(0), value);
                }
                var cellTag = safeTable.firstColumnHeader() && columnIndex == 0
                        ? AtfReportHtmlVocabulary.Tag.TH
                        : AtfReportHtmlVocabulary.Tag.TD;
                String cellAttributes = isUrlValueCell(safeTable, row, columnIndex)
                        ? AtfReportHtmlVocabulary.classAttribute(
                        AtfReportHtmlVocabulary.CssClass.EVIDENCE_URL_VALUE)
                        : "";
                html.append(AtfReportHtmlVocabulary.openTag(cellTag, cellAttributes))
                        .append(AtfReportHtmlVocabulary.escapeHtml(value))
                        .append(AtfReportHtmlVocabulary.closeTag(cellTag));
            }
            html.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TR));
        }
        html.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TBODY))
                .append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.TABLE));
        return closeTableViewport(html).toString();
    }

    private static StringBuilder closeTableViewport(StringBuilder html) {
        return html.append(AtfReportHtmlVocabulary.closeTag(AtfReportHtmlVocabulary.Tag.DIV));
    }

    private static ReportTableViewport tableViewport(String cssClass) {
        if (isTraceTable(cssClass)) return ReportTableViewport.TRACE;
        if (cssClass != null && (cssClass.contains(AtfReportHtmlVocabulary.CssClass.HELM_REQUEST_TABLE.value())
                || cssClass.contains(AtfReportHtmlVocabulary.CssClass.RETRY_HISTORY_TABLE.value()))) {
            return ReportTableViewport.API;
        }
        if (cssClass != null && cssClass.contains(
                AtfReportHtmlVocabulary.CssClass.HELM_VALIDATION_TABLE.value())) {
            return ReportTableViewport.ASSERTIONS;
        }
        return ReportTableViewport.DATA;
    }

    private static boolean isTraceTable(String cssClass) {
        return cssClass != null && cssClass.contains(
                AtfReportHtmlVocabulary.CssClass.HELM_TRACE_TABLE.value());
    }

    private static boolean isKeyValueEvidenceTable(String cssClass) {
        return cssClass != null && (cssClass.contains(
                AtfReportHtmlVocabulary.CssClass.HELM_TRACE_TABLE.value())
                || cssClass.contains(AtfReportHtmlVocabulary.CssClass.HELM_REQUEST_TABLE.value())
                || cssClass.contains(AtfReportHtmlVocabulary.CssClass.EXECUTION_TABLE.value()));
    }

    private static boolean isUrlValueCell(ReportEvidenceTable table,
                                          List<String> row, int columnIndex) {
        if (columnIndex != 1 || row.isEmpty() || table.headers().size() != 2) return false;
        String field = row.get(0) == null ? "" : row.get(0).trim().toLowerCase(java.util.Locale.ROOT);
        return field.equals("url") || field.endsWith(" url") || field.endsWith(".url")
                || field.equals("target") || field.endsWith(" target") || field.endsWith(".target")
                || field.equals("response url") || field.equals("request target");
    }

}
