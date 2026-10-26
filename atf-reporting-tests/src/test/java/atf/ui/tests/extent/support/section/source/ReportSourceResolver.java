package atf.ui.tests.extent.support.section.source;
import atf.reporting.artifact.ReportArtifactIdentity;
import atf.reporting.artifact.ReportArtifactInspector;
import atf.ui.tests.extent.support.config.UiConfiguration;
import atf.reporting.ui.audit.plan.SectionAuditSpec;
import atf.reporting.ui.audit.plan.UiExtentReportSource;
import atf.reporting.ui.audit.input.ReportAuditInput;
import atf.reporting.ui.audit.source.ResolvedReportSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Resolves static report evidence without opening a browser or mutating it. */
public final class ReportSourceResolver {
    private ReportSourceResolver() { }

    public static ReportAuditInput resolveStatic(Class<?> owner, SectionAuditSpec spec) {
        return resolveStaticSource(owner, null, spec).input();
    }

    public static ResolvedReportSource resolveStaticSource(Class<?> owner,
                                                           UiConfiguration configuration,
                                                           SectionAuditSpec spec) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(spec, "spec");
        Path report = switch (spec.source()) {
            case STATIC_PATH -> resolvePath(configuration, spec.reportProperty());
            case STATIC_RESOURCE -> resolveResource(owner, spec.reportResource());
            case GENERATED_API -> throw new IllegalArgumentException(
                    "GENERATED_API is resolved by the API setup strategy");
        };
        validateHtml(report, spec.source());
        return new ResolvedReportSource(spec.source(), ReportAuditInput.existing(report),
                ReportArtifactIdentity.read(report));
    }

    public static ResolvedReportSource fingerprintGenerated(Path report) {
        Objects.requireNonNull(report, "report");
        Path normalized = report.toAbsolutePath().normalize();
        validateHtml(normalized, UiExtentReportSource.GENERATED_API);
        return new ResolvedReportSource(UiExtentReportSource.GENERATED_API,
                ReportAuditInput.apiGenerated(normalized), ReportArtifactIdentity.read(normalized));
    }

    private static Path resolvePath(UiConfiguration configuration, String property) {
        String value = System.getProperty(property);
        if ((value == null || value.isBlank()) && configuration != null
                && "atf.ui.report".equals(property)) {
            value = configuration.auditInputReport().map(Path::toString).orElse(null);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing static Extent report property -D" + property);
        }
        Path path = Path.of(value).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Static Extent report does not exist: " + path);
        }
        return path;
    }

    private static Path resolveResource(Class<?> owner, String resource) {
        String normalized = resource.startsWith("/") ? resource.substring(1) : resource;
        URL url = owner.getClassLoader().getResource(normalized);
        if (url == null) throw new IllegalArgumentException(
                "Static Extent report resource was not found: " + resource);
        try {
            if ("file".equalsIgnoreCase(url.getProtocol())) {
                return Path.of(url.toURI()).toAbsolutePath().normalize();
            }
            Path copy = Files.createTempFile("atf-extent-static-", ".html");
            try (InputStream input = url.openStream()) {
                Files.copy(input, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return copy;
        } catch (IOException | URISyntaxException failure) {
            throw new IllegalStateException("Cannot resolve static Extent report resource: " + resource,
                    failure);
        }
    }

    private static void validateHtml(Path report, UiExtentReportSource source) {
        if (!Files.isRegularFile(report)) {
            throw new IllegalArgumentException("Resolved " + source + " report does not exist: " + report);
        }
        var inspection = ReportArtifactInspector.inspect(report);
        if (!inspection.readError().isEmpty()
                || !inspection.satisfies("html exists")
                || !inspection.satisfies("doctype")) {
            throw new IllegalArgumentException("Resolved " + source
                    + " report is not readable HTML: " + inspection.violations());
        }
        try {
            if (Files.size(report) == 0L) {
                throw new IllegalArgumentException("Resolved report is empty: " + report);
            }
        } catch (IOException error) {
            throw new IllegalArgumentException("Cannot read resolved report: " + report, error);
        }
    }
}

