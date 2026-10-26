package atf.reporting.evidence.markup;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.markup.AtfReportLabels;
import atf.reporting.markup.ExtentReportNodeSupport;
import atf.reporting.model.ReportJsonBody;
import atf.reporting.model.ReportJsonBodyFormatter;
import atf.reporting.security.SensitiveDataRedactor;

import java.util.Objects;

/** Shared JSON-evidence policy used by Extent and fallback HTML adapters. */
public final class ReportEvidenceJsonFormatter {
    private ReportEvidenceJsonFormatter() {
    }

    public static String label(ReportJsonBody body) {
        return body == null || body.label().isBlank()
                ? AtfReportLabels.JSON_BODY
                : body.label();
    }

    public static String displayBody(ReportJsonBody body,
                                    SensitiveDataRedactor redactor,
                                    ReportingSettings settings) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(redactor, "redactor");
        Objects.requireNonNull(settings, "settings");
        String redacted = redactor.redactJson(body.body());
        return ExtentReportNodeSupport.displayBody(body.format() == ReportJsonBody.Format.JSON
                ? ReportJsonBodyFormatter.format(redacted) : redacted, settings);
    }
}
