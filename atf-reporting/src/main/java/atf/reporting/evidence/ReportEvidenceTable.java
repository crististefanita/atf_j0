package atf.reporting.evidence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Typed, renderer-neutral table model for report evidence.
 *
 * <p>The report can have more than one output adapter, but the table
 * semantics must not be reimplemented in each adapter. This model keeps
 * headers and cells as data; escaping, duration formatting and CSS remain
 * responsibilities of the selected renderer.</p>
 */
public final class ReportEvidenceTable {
    private final List<String> headers;
    private final List<List<String>> rows;
    private final boolean firstColumnHeader;

    private ReportEvidenceTable(Collection<String> headers, Collection<? extends Collection<String>> rows) {
        this(headers, rows, false);
    }

    private ReportEvidenceTable(Collection<String> headers, Collection<? extends Collection<String>> rows,
                                boolean firstColumnHeader) {
        this.headers = immutableRow(headers);
        this.rows = rows == null ? List.of() : rows.stream().map(ReportEvidenceTable::immutableRow).toList();
        this.firstColumnHeader = firstColumnHeader;
    }

    public static ReportEvidenceTable of(Collection<String> headers,
                                         Collection<? extends Collection<String>> rows) {
        return new ReportEvidenceTable(headers, rows);
    }

    public static ReportEvidenceTable from(String[][] legacyRows) {
        if (legacyRows == null || legacyRows.length == 0) {
            return new ReportEvidenceTable(List.of(), List.of());
        }
        List<String> headers = legacyRows[0] == null
                ? List.of()
                : Arrays.asList(legacyRows[0]);
        List<List<String>> rows = new ArrayList<>();
        for (int index = 1; index < legacyRows.length; index++) {
            rows.add(legacyRows[index] == null ? List.of() : Arrays.asList(legacyRows[index]));
        }
        return new ReportEvidenceTable(headers, rows);
    }

    public List<String> headers() {
        return headers;
    }

    public List<List<String>> rows() {
        return rows;
    }

    /** Returns a copy whose first cell in each data row is rendered as a row header. */
    public ReportEvidenceTable withFirstColumnHeaders() {
        return new ReportEvidenceTable(headers, rows, true);
    }

    public boolean firstColumnHeader() {
        return firstColumnHeader;
    }

    public String[][] toArray() {
        String[][] result = new String[rows.size() + 1][];
        result[0] = headers.toArray(String[]::new);
        for (int index = 0; index < rows.size(); index++) {
            result[index + 1] = rows.get(index).toArray(String[]::new);
        }
        return result;
    }

    private static List<String> immutableRow(Collection<String> values) {
        if (values == null) return List.of();
        return values.stream().map(value -> Objects.requireNonNullElse(value, "")).toList();
    }
}
