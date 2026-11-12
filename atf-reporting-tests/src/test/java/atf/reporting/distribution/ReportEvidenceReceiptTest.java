package atf.reporting.distribution;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static atf.reporting.distribution.ReportEvidenceReceipt.Status.*;
import static atf.reporting.distribution.ReportEvidenceReceipt.Verdict.FAIL;
import static atf.reporting.distribution.ReportEvidenceReceipt.Verdict.PASS;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ReportEvidenceReceiptTest {
    private static final String COORDINATE = "atf.api:atf-reporting:1.0.0";
    private static final String PACKAGE_SHA256 = "a".repeat(64);
    private static final ReportEvidenceReceipt.Run RUN =
            new ReportEvidenceReceipt.Run(UUID.fromString("11111111-2222-3333-4444-555555555555"), COORDINATE, PACKAGE_SHA256);
    private static final ReportEvidenceReceipt.Run OTHER_RUN =
            new ReportEvidenceReceipt.Run(UUID.fromString("21111111-2222-3333-4444-555555555555"), COORDINATE, PACKAGE_SHA256);
    private static final String REPORT = "extent.html";

    @TempDir Path directory;

    @Test void relocationPreservesReceiptBytesAndTheOriginalFailVerdict() throws Exception {
        Path report = publish("original", RUN);
        Path receipt = ReportEvidenceReceipt.capture(report, RUN, FAIL);
        byte[] original = Files.readAllBytes(receipt);
        Path moved = Files.move(report.getParent(), directory.resolve("relocated"));
        Files.createDirectories(report.getParent());
        Files.writeString(report, "<p>decoy at original location</p>");

        Path relocatedReceipt = moved.resolve(receipt.getFileName());
        var verification = ReportEvidenceReceipt.verify(relocatedReceipt, RUN);
        assertEquals(VERIFIED, verification.status());
        assertEquals(FAIL, verification.historicalVerdict());
        assertArrayEquals(original, Files.readAllBytes(relocatedReceipt));
    }

    @Test void missingReportDoesNotBorrowAnIdenticalFileFromAnotherDirectory() throws Exception {
        Path report = publish("run", RUN);
        Path receipt = ReportEvidenceReceipt.capture(report, RUN, PASS);
        Path neighbor = Files.createDirectory(directory.resolve("neighbor")).resolve(REPORT);
        Files.move(report, neighbor);

        assertEquals(new ReportEvidenceReceipt.Verification(REPORT_MISSING, PASS),
                ReportEvidenceReceipt.verify(receipt, RUN));
    }

    @Test void reportFromAnotherRunWithTheSameNameFailsTheDigestCheck() throws Exception {
        Path original = publish("run", RUN);
        Path receipt = ReportEvidenceReceipt.capture(original, RUN, FAIL);
        Path foreign = publish("other-run", OTHER_RUN);
        assertNotEquals(ReportEvidenceReceipt.sha256(original), ReportEvidenceReceipt.sha256(foreign));
        Files.copy(foreign, original, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertEquals(new ReportEvidenceReceipt.Verification(REPORT_DIGEST_MISMATCH, FAIL),
                ReportEvidenceReceipt.verify(receipt, RUN));
    }

    @Test void matchingReportAndReceiptFromAnotherRunStillFailTheExpectedRunCheck() throws Exception {
        Path report = publish("other-run", OTHER_RUN);
        Path receipt = ReportEvidenceReceipt.capture(report, OTHER_RUN, PASS);

        assertEquals(VERIFIED, ReportEvidenceReceipt.verify(receipt, OTHER_RUN).status());
        assertEquals(new ReportEvidenceReceipt.Verification(WRONG_RUN, PASS),
                ReportEvidenceReceipt.verify(receipt, RUN));
    }

    @Test void matchingRunWithAnotherPackageFailsTheExpectedPackageCheck() throws Exception {
        Path receipt = ReportEvidenceReceipt.capture(publish("run", RUN), RUN, PASS);
        var otherPackage = new ReportEvidenceReceipt.Run(RUN.id(), COORDINATE, "b".repeat(64));

        assertEquals(new ReportEvidenceReceipt.Verification(WRONG_PACKAGE, PASS),
                ReportEvidenceReceipt.verify(receipt, otherPackage));
    }

    @Test void receiptCannotEscapeItsDirectoryOrOverrideARecordedField() throws Exception {
        Path receipt = ReportEvidenceReceipt.capture(publish("run", RUN), RUN, PASS);
        String original = Files.readString(receipt);
        Files.writeString(receipt, original.replace("report=" + REPORT, "report=../" + REPORT));
        assertThrows(IllegalArgumentException.class, () -> ReportEvidenceReceipt.verify(receipt, RUN));
        Files.writeString(receipt, original + "verdict=FAIL\n");
        assertThrows(IllegalArgumentException.class, () -> ReportEvidenceReceipt.verify(receipt, RUN));
    }

    private Path publish(String folder, ReportEvidenceReceipt.Run run) throws Exception {
        Path report = Files.createDirectory(directory.resolve(folder)).resolve(REPORT);
        return Files.writeString(report, "<!doctype html><p>Publication run: " + run.id() + "</p>");
    }
}
