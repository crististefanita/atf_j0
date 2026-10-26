package atf.reporting.ui;

import atf.reporting.console.AtfConsoleSink;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.configuration.ReportEnvironmentSnapshot;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/** Typed composition options for the canonical UI report session entry point. */
public record UiExtentReportOptions(Path outputDirectory,
                                    String fileName,
                                    AtfConsoleSink console,
                                    ReportEvidenceProfile evidenceProfile,
                                    boolean timestampEnabled,
                                    Map<String, String> systemInfo,
                                    ReportEnvironmentSnapshot environment) {
    public UiExtentReportOptions(Path outputDirectory, String fileName,
                                 AtfConsoleSink console,
                                 ReportEvidenceProfile evidenceProfile,
                                 boolean timestampEnabled) {
        this(outputDirectory, fileName, console, evidenceProfile, timestampEnabled,
                Map.of(), ReportEnvironmentSnapshot.empty());
    }

    public UiExtentReportOptions(Path outputDirectory, String fileName,
                                 AtfConsoleSink console,
                                 ReportEvidenceProfile evidenceProfile,
                                 boolean timestampEnabled,
                                 Map<String, String> systemInfo) {
        this(outputDirectory, fileName, console, evidenceProfile, timestampEnabled,
                systemInfo, ReportEnvironmentSnapshot.empty());
    }

    public UiExtentReportOptions {
        outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory");
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        console = Objects.requireNonNull(console, "console");
        evidenceProfile = Objects.requireNonNull(evidenceProfile, "evidenceProfile");
        systemInfo = systemInfo == null ? Map.of() : Map.copyOf(systemInfo);
        environment = environment == null ? ReportEnvironmentSnapshot.empty() : environment;
    }

    public static Builder builder(Path outputDirectory) {
        return new Builder(outputDirectory);
    }

    /** Small builder for consumers that need only one or two overrides. */
    public static final class Builder {
        private final Path outputDirectory;
        private String fileName = UiExtentReportSession.DEFAULT_FILE_NAME;
        private AtfConsoleSink console = AtfConsoleSink.noOp();
        private ReportEvidenceProfile evidenceProfile = ReportEvidenceProfile.defaults();
        private boolean timestampEnabled = true;
        private Map<String, String> systemInfo = Map.of();
        private ReportEnvironmentSnapshot environment = ReportEnvironmentSnapshot.empty();

        private Builder(Path outputDirectory) {
            this.outputDirectory = Objects.requireNonNull(outputDirectory, "outputDirectory");
        }

        public Builder fileName(String value) {
            fileName = value;
            return this;
        }

        public Builder console(AtfConsoleSink value) {
            console = value;
            return this;
        }

        public Builder evidenceProfile(ReportEvidenceProfile value) {
            evidenceProfile = value;
            return this;
        }

        public Builder timestampEnabled(boolean value) {
            timestampEnabled = value;
            return this;
        }

        public Builder systemInfo(Map<String, String> value) {
            systemInfo = value == null ? Map.of() : Map.copyOf(value);
            return this;
        }

        /** Adds semantic, allowlisted fields to Overview/System metadata. */
        public Builder environment(ReportEnvironmentSnapshot value) {
            environment = value == null ? ReportEnvironmentSnapshot.empty() : value;
            return this;
        }

        public UiExtentReportOptions build() {
            return new UiExtentReportOptions(outputDirectory, fileName, console,
                    evidenceProfile, timestampEnabled, systemInfo, environment);
        }
    }
}
