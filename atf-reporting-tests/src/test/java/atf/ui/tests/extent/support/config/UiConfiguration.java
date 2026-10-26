package atf.ui.tests.extent.support.config;

import atf.ui.config.UiBrowserConfiguration;
import atf.ui.config.UiScreenValidationConfiguration;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.io.IOException;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** UI execution policy plus the report discovery policy used by UI checks. */
public record UiConfiguration(UiBrowserConfiguration browser, Path reportDirectory,
                              String reportFilePrefix,
                              List<String> apiScenarioFamilies,
                              UiScreenValidationConfiguration screenValidation,
                              Optional<Path> auditInputReport,
                              Map<String, UiAuditSuiteConfiguration> auditSuites) {
    private static final DateTimeFormatter REPORT_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");
    static List<String> defaultScenarioFamilies() {
        return List.of("posts", "users", "requests", "headers", "lifecycle");
    }

    public UiConfiguration {
        browser = Objects.requireNonNull(browser, "browser");
        reportDirectory = Objects.requireNonNull(reportDirectory, "reportDirectory")
                .toAbsolutePath().normalize();
        reportFilePrefix = Objects.requireNonNull(reportFilePrefix, "reportFilePrefix").trim();
        if (reportFilePrefix.isEmpty()) throw new IllegalArgumentException("reportFilePrefix must not be blank");
        apiScenarioFamilies = List.copyOf(Objects.requireNonNull(apiScenarioFamilies, "apiScenarioFamilies"));
        if (apiScenarioFamilies.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("apiScenarioFamilies must not contain blank values");
        }
        screenValidation = Objects.requireNonNull(screenValidation, "screenValidation");
        auditInputReport = Objects.requireNonNull(auditInputReport, "auditInputReport")
                .map(path -> path.toAbsolutePath().normalize());
        auditSuites = Map.copyOf(Objects.requireNonNull(auditSuites, "auditSuites"));
    }

    public UiConfiguration(UiBrowserConfiguration browser, Path reportDirectory, String reportFilePrefix) {
        this(browser, reportDirectory, reportFilePrefix,
                defaultScenarioFamilies(),
                UiScreenValidationConfiguration.defaults(), Optional.empty());
    }

    public UiConfiguration(UiBrowserConfiguration browser, Path reportDirectory, String reportFilePrefix,
                           List<String> apiScenarioFamilies) {
        this(browser, reportDirectory, reportFilePrefix, apiScenarioFamilies,
                UiScreenValidationConfiguration.defaults(), Optional.empty());
    }

    public UiConfiguration(UiBrowserConfiguration browser, Path reportDirectory,
                           String reportFilePrefix, List<String> apiScenarioFamilies,
                           UiScreenValidationConfiguration screenValidation,
                           Optional<Path> auditInputReport) {
        this(browser, reportDirectory, reportFilePrefix, apiScenarioFamilies,
                screenValidation, auditInputReport, Map.of());
    }

    public UiConfiguration withAuditInputReport(Path report) {
        return new UiConfiguration(browser, reportDirectory, reportFilePrefix,
                apiScenarioFamilies, screenValidation, Optional.ofNullable(report), auditSuites);
    }

    public UiConfiguration withReportDirectory(Path directory) {
        return new UiConfiguration(browser, directory, reportFilePrefix,
                apiScenarioFamilies, screenValidation, auditInputReport, auditSuites);
    }

    public Optional<UiAuditSuiteConfiguration> auditSuite(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(auditSuites.get(normalizeSuiteId(id)));
    }

    private static String normalizeSuiteId(String value) {
        return value.trim().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }

    /** Resolves a collision-resistant artifact name for one audit role. */
    public String timestampedReportFileName(String logicalSuffix, Clock clock) {
        Objects.requireNonNull(clock, "clock");
        String suffix = logicalSuffix == null ? "" : logicalSuffix.trim();
        if (!suffix.isEmpty() && !suffix.endsWith("_")) suffix += "_";
        return reportFilePrefix + suffix
                + REPORT_TIMESTAMP.withZone(clock.getZone()).format(clock.instant()) + ".html";
    }

    /**
     * Atomically reserves a unique report path, including across parallel JVMs
     * sharing the same output directory and clock tick.
     */
    public Path reserveTimestampedReportPath(String logicalSuffix, Clock clock) {
        Objects.requireNonNull(clock, "clock");
        String baseName = timestampedReportFileName(logicalSuffix, clock);
        try {
            Files.createDirectories(reportDirectory);
            for (int collision = 1; ; collision++) {
                String candidateName = collision == 1 ? baseName : disambiguate(baseName, collision);
                Path candidate = reportDirectory.resolve(candidateName);
                try {
                    return Files.createFile(candidate);
                } catch (FileAlreadyExistsException collisionOnDisk) {
                    // CREATE_NEW is the cross-process arbitration boundary.
                }
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot reserve a unique UI report path under "
                    + reportDirectory, failure);
        }
    }

    private static String disambiguate(String fileName, int collision) {
        int extension = fileName.lastIndexOf('.');
        return fileName.substring(0, extension) + "_" + collision + fileName.substring(extension);
    }

    public static UiConfiguration defaults(Path baseDirectory) {
        return new UiConfiguration(UiBrowserConfiguration.defaults(),
                baseDirectory.resolve("artifacts/reports/ui"), "ui-atf-extent-report_",
                defaultScenarioFamilies(),
                UiScreenValidationConfiguration.defaults(), Optional.empty());
    }
}
