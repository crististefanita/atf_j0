package atf.reporting.artifact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable, reviewer-friendly identity of one published HTML artifact. */
public record ReportArtifactIdentity(Path path, String fileName, String sha256) {
    public ReportArtifactIdentity {
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        fileName = Objects.requireNonNull(fileName, "fileName");
        sha256 = Objects.requireNonNull(sha256, "sha256");
    }

    public static ReportArtifactIdentity read(Path report) {
        Objects.requireNonNull(report, "report");
        Path normalized = report.toAbsolutePath().normalize();
        try {
            byte[] bytes = Files.readAllBytes(normalized);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return new ReportArtifactIdentity(normalized, normalized.getFileName().toString(),
                    HexFormat.of().formatHex(digest).toUpperCase(java.util.Locale.ROOT));
        } catch (IOException error) {
            throw new IllegalStateException("Cannot fingerprint report artifact: " + normalized, error);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is required for report identity", error);
        }
    }

    public String shortSha256() {
        return sha256.substring(0, Math.min(12, sha256.length()));
    }

    /**
     * Human-facing label for Extent steps. The digest remains available to
     * technical gates, but it must not leak into the readable report copy.
     */
    public String display() {
        return fileName;
    }
}
