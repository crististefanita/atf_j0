package atf.reporting.support.publication;

import atf.reporting.configuration.ReportingSettings;
import atf.reporting.contract.*;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.model.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.management.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.LockSupport;

/** Real model/Spark/ATF close path at the captured consumer volume, isolated in a 1 GiB JVM. */
public final class CapturedVolumeProbe {
    private CapturedVolumeProbe() { }
    public static void main(String[] args) throws Exception {
        Path report = Path.of(args[0]); Files.createDirectories(report.getParent());
        var hashes = new ArrayList<String>();
        var session = new ExtentReportSession(report, new ReportingSettings(240, 0));
        var profile = ReportEvidenceProfile.builder().screenshotPolicy(ReportScreenshotPolicy.EACH_CHECKPOINT).build();
        var scenario = UiExtentReportSession.adaptScenario(session.startScenario("VolumeFixture",
                "321 captured checkpoints", List.of("component", "synthetic")), profile, "321 captured checkpoints");
        long pngBytes = 0;
        try (var phase = scenario.startPhase("TEST BODY")) {
            for (int checkpoint = 0; checkpoint < 321; checkpoint++) {
                byte[] png = png(checkpoint); pngBytes += png.length;
                String payload = Base64.getEncoder().encodeToString(png);
                hashes.add(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.US_ASCII))));
                var rows = new ArrayList<AtfAssertion>();
                int count = 20 + (checkpoint < 214 ? 1 : 0);
                for (int row = 0; row < count; row++) {
                    boolean passed = checkpoint < 320 || row < count - 1;
                    rows.add(new AtfAssertion("Comparison " + checkpoint + "/" + row, "expected-" + row,
                            passed ? "expected-" + row : "controlled-different-suffix", passed));
                }
                var actions = new ArrayList<AtfUiActionRecord>();
                for (int action = 0; action < (checkpoint < 256 ? 2 : 1); action++) {
                    actions.add(new AtfUiActionRecord(action + 1, "Checkpoint " + checkpoint, "synthetic target",
                            "captured expected ♠ " + checkpoint, "captured observed ♣ " + checkpoint, 1, AtfReportStatus.INFO,
                            action == 0 ? AtfUiEvidenceSource.ACTION : AtfUiEvidenceSource.OBSERVE));
                }
                phase.step(AtfGherkinKeyword.THEN, "Checkpoint " + checkpoint, "synthetic target", payload, rows, actions,
                        ReportScreenshotPolicy.CapturePoint.CHECKPOINT, checkpoint == 320 ? AtfReportStatus.FAIL : AtfReportStatus.PASS);
            }
            phase.completeFinal(AtfReportStatus.FAIL);
        }
        scenario.complete(AtfReportStatus.FAIL); scenario.close();
        Files.write(report.resolveSibling("expected-png-sha256.txt"), hashes);
        System.out.println("PNG_BYTES=" + pngBytes);
        System.out.println("REPORTING_ORIGIN=" + ExtentReportSession.class.getProtectionDomain().getCodeSource().getLocation());
        AtomicLong peak = new AtomicLong(); AtomicBoolean sampling = new AtomicBoolean(true);
        var sampler = new Thread(() -> {
            while (sampling.get()) { peak.accumulateAndGet(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(), Math::max); LockSupport.parkNanos(2_000_000); }
        }, "volume-heap-sampler");
        sampler.setDaemon(true); sampler.start();
        long before = allocated(), started = System.nanoTime();
        try { session.close(); }
        finally { sampling.set(false); sampler.join(); }
        System.out.println("PUBLISH_MILLIS=" + (System.nanoTime() - started) / 1_000_000);
        System.out.println("SAMPLED_PEAK_HEAP_BYTES=" + peak.get());
        System.out.println("PUBLISH_ALLOCATED_BYTES=" + Math.max(0, allocated() - before));
        System.out.println("HTML_BYTES=" + Files.size(report));
    }
    private static long allocated() {
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long total = 0; for (long value : bean.getThreadAllocatedBytes(bean.getAllThreadIds())) if (value > 0) total += value;
        return total;
    }
    private static byte[] png(int seed) throws Exception {
        var image = new BufferedImage(320, 252, BufferedImage.TYPE_INT_RGB);
        var random = new Random(seed + 981L);
        for (int y = 0; y < 252; y++) for (int x = 0; x < 320; x++) image.setRGB(x, y, random.nextInt(0x1000000));
        try (var bytes = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", bytes)) throw new IllegalStateException("PNG writer unavailable");
            return bytes.toByteArray();
        }
    }
}
