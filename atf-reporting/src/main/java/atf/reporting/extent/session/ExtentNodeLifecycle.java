package atf.reporting.extent.session;
import atf.reporting.contract.AtfReportStatus;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

import java.util.Date;
import java.util.Objects;

/**
 * Owns the small but vendor-specific lifecycle translation used by the
 * Extent adapter.
 *
 * <p>The reporting session decides <em>what</em> the semantic outcome is;
 * this class decides only how that outcome and its timestamps are represented
 * by an Extent node. Keeping that boundary explicit prevents status mapping
 * and duration bookkeeping from leaking into scenario, phase and request
 * scopes.</p>
 */
public final class ExtentNodeLifecycle {
    private ExtentNodeLifecycle() {
    }

    public static void start(ExtentTest node) {
        Objects.requireNonNull(node, "node").getModel().setStartTime(new Date());
    }

    public static void complete(ExtentTest node, AtfReportStatus status) {
        Objects.requireNonNull(node, "node").getModel().setStatus(toExtentStatus(status));
    }

    public static void end(ExtentTest node) {
        var model = Objects.requireNonNull(node, "node").getModel();
        Date started = model.getStartTime();
        long end = System.currentTimeMillis();
        if (started != null) {
            end = Math.max(end, started.getTime() + 1L);
        }
        model.setEndTime(new Date(end));
    }

    public static long durationMillis(ExtentTest node) {
        var model = Objects.requireNonNull(node, "node").getModel();
        Date started = model.getStartTime();
        Date ended = model.getEndTime();
        if (started == null || ended == null) return 0L;
        return Math.max(0L, ended.getTime() - started.getTime());
    }

    public static Status toExtentStatus(AtfReportStatus status) {
        return switch (Objects.requireNonNull(status, "status")) {
            case PASS -> Status.PASS;
            case FAIL -> Status.FAIL;
            case SKIP -> Status.SKIP;
            case WARNING -> Status.WARNING;
            case INFO -> Status.INFO;
        };
    }
}
