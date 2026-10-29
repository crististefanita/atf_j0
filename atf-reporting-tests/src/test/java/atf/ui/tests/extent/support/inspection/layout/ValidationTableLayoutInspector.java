package atf.ui.tests.extent.support.inspection.layout;

import com.microsoft.playwright.Locator;

import java.util.Objects;

/** Browser adapter that audits the shared runtime-generated evidence-table layout. */
public final class ValidationTableLayoutInspector {
    public Result inspect(Locator table) {
        Objects.requireNonNull(table, "table");
        String evidence = table.evaluate("""
                element => {
                  const rows = Array.from(element.rows || []);
                  const count = rows.length ? rows[0].cells.length : 0;
                  const maxPx = String(element.getAttribute('data-atf-content-column-max-px') || '')
                    .split('|').filter(Boolean).map(value => Number.parseFloat(value));
                  const headers = rows.length && rows[0].cells
                    ? Array.from(rows[0].cells).map(cell => String(cell.textContent || '').trim().toLowerCase())
                    : [];
                  const keyValue = count === 2 && headers[0] === 'field' && headers[1] === 'value';
                  const contentIndexes = Array.from({length: count}, (_, index) => index)
                    .filter(index => !/(^|\\b)(result|status|outcome|time|duration|attempt|attempts)(\\b|$)/.test(headers[index] || ''));
                  const widths = String(element.getAttribute('data-atf-content-column-widths')
                    || element.getAttribute('data-atf-validation-column-widths') || '')
                    .split('|').filter(Boolean).map(value => Number.parseFloat(value));
                  const longestIndex = contentIndexes.length
                    ? contentIndexes.reduce((best, index) => maxPx[index] > maxPx[best] ? index : best,
                        contentIndexes[0]) : -1;
                  const widestIndex = contentIndexes.length
                    ? contentIndexes.reduce((best, index) => widths[index] > widths[best] ? index : best,
                        contentIndexes[0]) : -1;
                  return [
                    element.getAttribute('data-atf-content-layout')
                      || element.getAttribute('data-atf-validation-layout') || '',
                    count,
                    widths.join(','),
                    widths.reduce((sum, value) => sum + value, 0),
                    maxPx.join(','),
                    longestIndex,
                    widestIndex,
                    keyValue ? 'key-value' : 'content',
                    element.scrollWidth,
                    element.clientWidth
                  ].join('|');
                }
                """).toString();
        String[] parts = evidence.split("\\|", -1);
        if (parts.length != 10) return new Result(false, evidence);
        try {
            int columns = Integer.parseInt(parts[1]);
            double widthSum = Double.parseDouble(parts[3]);
            int maxWidthColumns = parts[4].split(",", -1).length;
            int longestIndex = Integer.parseInt(parts[5]);
            int widestIndex = Integer.parseInt(parts[6]);
            double scrollWidth = Double.parseDouble(parts[8]);
            double clientWidth = Double.parseDouble(parts[9]);
            String[] measured = parts[4].split(",", -1);
            boolean semanticWidth = longestIndex >= 0 && widestIndex >= 0
                    && Double.parseDouble(measured[widestIndex]) >= Double.parseDouble(measured[longestIndex]) - 1;
            boolean valid = "dynamic-longest-value".equals(parts[0])
                    && columns >= 2
                    && parts[2].split(",", -1).length == columns
                    && maxWidthColumns == columns
                    && widthSum >= 99.0 && widthSum <= 101.0
                    && semanticWidth
                    && scrollWidth <= clientWidth + 1.0;
            return new Result(valid, evidence);
        } catch (NumberFormatException ignored) {
            return new Result(false, evidence);
        }
    }

    public record Result(boolean valid, String evidence) {
        public Result {
            Objects.requireNonNull(evidence, "evidence");
        }
    }

}
