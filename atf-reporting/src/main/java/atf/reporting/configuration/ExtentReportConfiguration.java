package atf.reporting.configuration;

import atf.reporting.evidence.ReportScreenshotPolicy;

import java.nio.file.Path;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** Immutable output contract for an Extent report created by a test run. */
public record ExtentReportConfiguration(Path outputDirectory, String fileName,
                                        boolean timestampEnabled, String timestampPattern,
                                        String timestampZone,
                                        ReportingSettings reportingSettings,
                                        boolean enabled,
                                        ReportScreenshotPolicy screenshotPolicy) {
    public static final String DEFAULT_FILE_NAME = "api-atf-extent-report.html";
    public static final String DEFAULT_TIMESTAMP_PATTERN = "yyyy-MM-dd_HH-mm-ss-SSS";
    public static final String DEFAULT_TIMESTAMP_ZONE = "system";

    public ExtentReportConfiguration(Path outputDirectory, String fileName) {
        this(outputDirectory, fileName, true, DEFAULT_TIMESTAMP_PATTERN, DEFAULT_TIMESTAMP_ZONE);
    }

    public ExtentReportConfiguration(Path outputDirectory, String fileName,
                                     boolean timestampEnabled, String timestampPattern,
                                     String timestampZone) {
        this(outputDirectory, fileName, timestampEnabled, timestampPattern, timestampZone,
                ReportingSettings.defaults(), true);
    }

    public ExtentReportConfiguration(Path outputDirectory, String fileName,
                                     boolean timestampEnabled, String timestampPattern,
                                     String timestampZone, ReportingSettings reportingSettings) {
        this(outputDirectory, fileName, timestampEnabled, timestampPattern, timestampZone,
                reportingSettings, true, ReportScreenshotPolicy.EACH_CHECKPOINT);
    }

    public ExtentReportConfiguration(Path outputDirectory, String fileName,
                                     boolean timestampEnabled, String timestampPattern,
                                     String timestampZone, ReportingSettings reportingSettings,
                                     boolean enabled) {
        this(outputDirectory, fileName, timestampEnabled, timestampPattern, timestampZone,
                reportingSettings, enabled, ReportScreenshotPolicy.EACH_CHECKPOINT);
    }

    public ExtentReportConfiguration {
        outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory")
                .toAbsolutePath().normalize();
        fileName = Objects.requireNonNull(fileName, "fileName").trim();
        if (fileName.isEmpty()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (Path.of(fileName).isAbsolute() || fileName.contains("\\") || fileName.contains("/")) {
            throw new IllegalArgumentException("fileName must be a file name, not a path: " + fileName);
        }
        timestampPattern = Objects.requireNonNull(timestampPattern, "timestampPattern").trim();
        if (timestampPattern.isEmpty()) {
            throw new IllegalArgumentException("timestampPattern must not be blank");
        }
        DateTimeFormatter formatter;
        try {
            formatter = DateTimeFormatter.ofPattern(timestampPattern, Locale.ROOT);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("timestampPattern is invalid: " + timestampPattern, error);
        }
        timestampZone = Objects.requireNonNull(timestampZone, "timestampZone").trim();
        if (timestampZone.isEmpty()) {
            throw new IllegalArgumentException("timestampZone must not be blank");
        }
        ZoneId zone = resolveZoneId(timestampZone);
        String sample = formatter.withZone(zone).format(Instant.EPOCH);
        if (containsUnsafeFileNameCharacter(sample)) {
            throw new IllegalArgumentException(
                    "timestampPattern produces characters that are unsafe in a file name: " + sample);
        }
        reportingSettings = Objects.requireNonNull(reportingSettings, "reportingSettings");
        screenshotPolicy = Objects.requireNonNull(screenshotPolicy, "screenshotPolicy");
    }

    public Path reportPath() {
        return outputDirectory.resolve(fileName).normalize();
    }

    /**
     * Resolves the public path once for a report session. Keeping the clock
     * explicit makes timestamped names deterministic in unit tests and avoids
     * a report changing destination if a run crosses a clock boundary.
     */
    public Path resolveReportPath(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        if (!timestampEnabled) {
            return reportPath();
        }

        ZoneId zone = resolveZoneId(timestampZone);
        String timestamp = DateTimeFormatter.ofPattern(timestampPattern, Locale.ROOT)
                .withZone(zone)
                .format(clock.instant());
        if (containsUnsafeFileNameCharacter(timestamp)) {
            throw new IllegalArgumentException(
                    "timestampPattern produces characters that are unsafe in a file name: " + timestamp);
        }
        return outputDirectory.resolve(insertBeforeExtension(fileName, "_" + timestamp)).normalize();
    }

    public static ExtentReportConfiguration defaults(Path baseDirectory) {
        return new ExtentReportConfiguration(baseDirectory.resolve("target"), DEFAULT_FILE_NAME,
                true, DEFAULT_TIMESTAMP_PATTERN, DEFAULT_TIMESTAMP_ZONE, ReportingSettings.defaults(), true,
                ReportScreenshotPolicy.EACH_CHECKPOINT);
    }

    private static ZoneId resolveZoneId(String value) {
        if (DEFAULT_TIMESTAMP_ZONE.equalsIgnoreCase(value)) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(value);
        } catch (DateTimeException error) {
            throw new IllegalArgumentException("timestampZone is invalid: " + value, error);
        }
    }

    private static String insertBeforeExtension(String name, String suffix) {
        int extensionIndex = name.lastIndexOf('.');
        if (extensionIndex <= 0) {
            return name + suffix;
        }
        return name.substring(0, extensionIndex) + suffix + name.substring(extensionIndex);
    }

    private static boolean containsUnsafeFileNameCharacter(String value) {
        return value.chars().anyMatch(character -> "\\/:*?\"<>|".indexOf(character) >= 0);
    }
}
