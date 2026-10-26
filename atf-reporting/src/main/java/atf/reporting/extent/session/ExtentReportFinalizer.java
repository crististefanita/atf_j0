package atf.reporting.extent.session;

/**
 * Public facade for Extent publication.
 *
 * <p>The facade owns the stable API; the publication pipeline owns the
 * ordering of asset inlining, ATF markup/runtime injection, atomic write and
 * cleanup. Keeping those responsibilities separate makes the risky finalizer
 * behavior directly testable without exposing implementation details.</p>
 */
public final class ExtentReportFinalizer {
    private ExtentReportFinalizer() {
    }

    public static void finalizeReport(java.nio.file.Path reportPath) throws java.io.IOException {
        ExtentReportPublicationPipeline.publish(reportPath);
    }
}

