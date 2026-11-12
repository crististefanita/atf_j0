package atf.reporting.distribution;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Portable identity of an HTML artifact accepted by the producer distribution gate. */
public final class ReportEvidenceReceipt {
    private static final Set<String> KEYS = Set.of(
            "schema", "runId", "package", "packageSha256", "verdict", "report", "reportSha256");

    private ReportEvidenceReceipt() { }

    enum Verdict { PASS, FAIL }

    enum Status {
        VERIFIED, WRONG_RUN, WRONG_PACKAGE, REPORT_MISSING, REPORT_UNREADABLE, REPORT_DIGEST_MISMATCH
    }

    record Run(UUID id, String coordinate, String packageSha256) {
        Run {
            java.util.Objects.requireNonNull(id, "run ID");
            if (!coordinate.matches("[a-zA-Z0-9_.+-]+:[a-zA-Z0-9_.+-]+:[a-zA-Z0-9_.+-]+")) {
                throw new IllegalArgumentException("Expected Maven group:artifact:version");
            }
            packageSha256 = digestValue(packageSha256);
        }
    }

    record Verification(Status status, Verdict historicalVerdict) { }

    static Path capture(Path report, Run run, Verdict verdict) throws IOException {
        String name = reportName(report.getFileName().toString());
        Path receipt = report.resolveSibling(name + ".receipt");
        String data = """
                schema=1
                runId=%s
                package=%s
                packageSha256=%s
                verdict=%s
                report=%s
                reportSha256=%s
                """.formatted(run.id(), run.coordinate(), run.packageSha256(), verdict, name, sha256(report));
        return Files.writeString(receipt, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    static Verification verify(Path receipt, Run expected) throws IOException {
        var fields = read(receipt);
        var recorded = new Run(UUID.fromString(fields.get("runId")), fields.get("package"),
                fields.get("packageSha256"));
        var verdict = Verdict.valueOf(fields.get("verdict"));
        String name = reportName(fields.get("report"));
        String digest = digestValue(fields.get("reportSha256"));
        if (!recorded.id().equals(expected.id())) return new Verification(Status.WRONG_RUN, verdict);
        if (!recorded.equals(expected)) return new Verification(Status.WRONG_PACKAGE, verdict);
        return new Verification(verifyReport(receipt.resolveSibling(name), digest), verdict);
    }

    private static Status verifyReport(Path report, String expectedDigest) {
        try {
            var attributes = Files.readAttributes(report, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile()) return Status.REPORT_UNREADABLE;
            return sha256(report).equals(expectedDigest) ? Status.VERIFIED : Status.REPORT_DIGEST_MISMATCH;
        } catch (NoSuchFileException missing) {
            return Status.REPORT_MISSING;
        } catch (IOException unreadable) {
            return Status.REPORT_UNREADABLE;
        }
    }

    private static Map<String, String> read(Path receipt) throws IOException {
        var fields = new HashMap<String, String>();
        for (String line : Files.readAllLines(receipt)) {
            String[] field = line.split("=", 2);
            if (field.length != 2 || fields.putIfAbsent(field[0], field[1]) != null) {
                throw new IllegalArgumentException("Invalid or duplicate receipt field");
            }
        }
        if (!fields.keySet().equals(KEYS) || !"1".equals(fields.get("schema"))) {
            throw new IllegalArgumentException("Unknown or incomplete evidence receipt");
        }
        return fields;
    }

    private static String reportName(String name) {
        if (!name.matches("[a-zA-Z0-9][a-zA-Z0-9_.-]*\\.html")) {
            throw new IllegalArgumentException("Expected an adjacent HTML filename");
        }
        return name;
    }

    private static String digestValue(String value) {
        if (!value.matches("[0-9a-fA-F]{64}")) throw new IllegalArgumentException("Expected SHA-256");
        return value.toLowerCase(Locale.ROOT);
    }

    static String sha256(Path file) throws IOException {
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS);
             var digest = new DigestInputStream(input, MessageDigest.getInstance("SHA-256"))) {
            digest.transferTo(OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(digest.getMessageDigest().digest());
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is required for report evidence", unavailable);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            throw new IllegalArgumentException("Usage: <receipt> <expected-run-id> <expected-coordinate> <expected-jar-sha256>");
        }
        var expected = new Run(UUID.fromString(args[1]), args[2], args[3]);
        var result = verify(Path.of(args[0]), expected);
        System.out.printf("evidence=%s historicalVerdict=%s%n", result.status(), result.historicalVerdict());
        if (result.status() != Status.VERIFIED) System.exit(1);
    }
}
