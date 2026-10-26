package atf.reporting.html.scope;

/** Shared monotonic timing policy for console lifecycle events. */
final class HtmlReportScopeTiming {
    private HtmlReportScopeTiming() {
    }

    static long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }
}
