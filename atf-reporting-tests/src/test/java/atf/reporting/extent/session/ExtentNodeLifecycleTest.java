package atf.reporting.extent.session;
import atf.testkit.AtfUnitTest;
import atf.reporting.contract.AtfReportStatus;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.Status;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class ExtentNodeLifecycleTest {
    @Test
    void translatesEverySemanticOutcomeToTheMatchingExtentStatus() {
        Map<AtfReportStatus, Status> expected = new EnumMap<>(AtfReportStatus.class);
        expected.put(AtfReportStatus.PASS, Status.PASS);
        expected.put(AtfReportStatus.FAIL, Status.FAIL);
        expected.put(AtfReportStatus.SKIP, Status.SKIP);
        expected.put(AtfReportStatus.WARNING, Status.WARNING);
        expected.put(AtfReportStatus.INFO, Status.INFO);

        expected.forEach((semantic, extent) -> assertEquals(extent,
                ExtentNodeLifecycle.toExtentStatus(semantic), semantic.name()));
    }

    @Test
    void lifecycleAlwaysProducesAVisiblePositiveDuration() {
        ExtentTestFixture fixture = new ExtentTestFixture();
        try {
            ExtentNodeLifecycle.start(fixture.node());
            ExtentNodeLifecycle.complete(fixture.node(), AtfReportStatus.PASS);
            ExtentNodeLifecycle.end(fixture.node());

            assertEquals(Status.PASS, fixture.node().getModel().getStatus());
            assertNotNull(fixture.node().getModel().getStartTime());
            assertNotNull(fixture.node().getModel().getEndTime());
            assertTrue(fixture.node().getModel().getEndTime().getTime()
                    >= fixture.node().getModel().getStartTime().getTime() + 1L);
        } finally {
            fixture.close();
        }
    }

    @Test
    void rejectsNullVendorNodesAndSemanticStatusesAtTheBoundary() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> ExtentNodeLifecycle.start(null)),
                () -> assertThrows(NullPointerException.class,
                        () -> ExtentNodeLifecycle.complete(null, AtfReportStatus.PASS)),
                () -> assertThrows(NullPointerException.class,
                        () -> ExtentNodeLifecycle.toExtentStatus(null)));
    }

    private static final class ExtentTestFixture implements AutoCloseable {
        private final ExtentReports reports = new ExtentReports();
        private final com.aventstack.extentreports.ExtentTest node = reports.createTest("lifecycle");

        com.aventstack.extentreports.ExtentTest node() {
            return node;
        }

        @Override
        public void close() {
            reports.flush();
        }
    }
}
