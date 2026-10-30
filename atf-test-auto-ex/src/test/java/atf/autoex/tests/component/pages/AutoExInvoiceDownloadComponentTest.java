package atf.autoex.tests.component.pages;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.tests.support.invoice.AutoExInvoiceDownloadFixture;
import atf.core.reporting.contract.AtfReportStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static atf.testkit.assertions.AuditedAssertions.*;

@AutoExComponentTest
class AutoExInvoiceDownloadComponentTest {
    @Test void aBusyPostDocumentNeverBecomesAReadRetry() {
        try (var fixture = new AutoExInvoiceDownloadFixture("post")) {
            var failure = assertThrows(IllegalStateException.class, fixture::invoice, "A queue-full POST is refused rather than replayed");
            assertTrue(failure.getMessage().contains("POST"), "The refusal identifies the unsafe method");
            assertEquals(1, fixture.clicks(), "The initial activation remains unique");
            assertEquals(1, fixture.posts(), "The fixture observes one POST and no replay");
            assertEquals(0, fixture.gets(), "An unsafe document is not silently substituted by a GET");
        }
    }
    @ParameterizedTest @CsvSource({"download,1", "queue-once,2"})
    void anOwnedInvoiceDownloadMayRecoverOnlyItsObservedBusyGet(String mode, int gets) {
        try (var fixture = new AutoExInvoiceDownloadFixture(mode)) {
            var invoice = fixture.invoice();
            assertEquals("invoice.txt", invoice.fileName(), "The actual browser download retains its filename");
            assertTrue(invoice.available(), "The downloaded file exists before business invoice validation");
            assertEquals("Invoice for owned customer; amount=1000", invoice.contents(), "Invoice contents come from the downloaded artifact");
            assertEquals(1, fixture.clicks(), "Recovery never repeats the invoice click");
            assertEquals(gets, fixture.gets(), "Only the exact queue-full GET is retried");
            assertEquals(0, fixture.posts(), "Invoice recovery dispatches no mutation");
            var record = fixture.records().stream().filter(r -> r.operation().equals("Download")).toList();
            assertEquals(1, record.size(), "The complete download observation has one timed action");
            assertEquals(AtfReportStatus.PASS, record.getFirst().status(), "The action passes only after an actual Download event");
        }
    }
    @ParameterizedTest @CsvSource({"always-busy,2", "no-event,1", "redirect,1", "backoff-budget,1"})
    void refusalPreservesTheDeadlineAndNeverReplaysTheActivation(String mode, int gets) {
        try (var fixture = new AutoExInvoiceDownloadFixture(mode)) {
            long start = System.nanoTime();
            assertThrows(RuntimeException.class, fixture::invoice, "An absent download or unsafe document cannot become an invoice PASS");
            long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
            assertTrue(elapsed < 3000, "The fixture's original short deadline is not renewed by recovery");
            assertEquals(1, fixture.clicks(), "Failure never retries the invoice click");
            assertEquals(gets, fixture.gets(), "The retry bound and document ownership remain effective");
            var record = fixture.records().stream().filter(r -> r.operation().equals("Download")).findFirst().orElseThrow();
            assertEquals(AtfReportStatus.FAIL, record.status(), "The waiting operation is recorded as FAIL");
            assertTrue(record.durationMillis() > 0, "Failed download timing is measured rather than inferred from the click");
        }
    }
}
