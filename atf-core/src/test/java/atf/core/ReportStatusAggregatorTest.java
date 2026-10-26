package atf.core;
import atf.testkit.AtfUnitTest;

import atf.core.reporting.contract.AtfReportStatus;
import atf.core.reporting.contract.ReportStatusAggregator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class ReportStatusAggregatorTest {
    @Test
    void failureWinsOverSkipWarningAndPassRegardlessOfObservationOrder() {
        var aggregator = new ReportStatusAggregator();
        aggregator.record(AtfReportStatus.WARNING);
        aggregator.record(AtfReportStatus.PASS);
        aggregator.record(AtfReportStatus.SKIP);
        aggregator.record(AtfReportStatus.FAIL);

        assertEquals(AtfReportStatus.FAIL, aggregator.aggregate());
        assertEquals(1, aggregator.count(AtfReportStatus.WARNING));
        assertTrue(aggregator.hasWarnings());
    }

    @Test
    void skippedLifecycleRemainsStrongerThanWarningButWeakerThanFailure() {
        var aggregator = new ReportStatusAggregator();
        aggregator.record(AtfReportStatus.WARNING);
        aggregator.record(AtfReportStatus.SKIP);
        assertEquals(AtfReportStatus.SKIP, aggregator.aggregate());
    }
}
