package atf.reporting.support.publication;

import atf.reporting.extent.session.ExtentReportFinalizer;
import atf.reporting.support.ReportTestScreenshots;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/** Dedicated JVM entry point; the parent component test supplies the explicit heap limit. */
public final class LargeReportPublicationProbe {
    private static final int SCREENSHOTS = 1_500;
    private static final int UNICODE_REPETITIONS = 350_000;
    private static final int MEDIA_PROFILE_UNICODE_REPETITIONS = 80_000;
    private static final int DISTINCT_PNG_COUNT = 4;
    private static final int DISTINCT_PNG_SIZE = 256;

    private LargeReportPublicationProbe() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Expected output report path and profile");
        Path report = Path.of(args[0]);
        String profile = args[1];
        Files.createDirectories(report.toAbsolutePath().getParent());
        var html = new StringBuilder(14_000_000);
        html.append("<!doctype html><html><head></head><body class=\"spa -report dark\">")
                .append("<div class=\"test-wrapper row view test-view\">");
        switch (profile) {
            case "repeated-pixels" -> appendRepeatedPixelScreenshots(html);
            case "distinct-pngs" -> appendDistinctPngScreenshots(html);
            default -> throw new IllegalArgumentException("Unknown publication profile: " + profile);
        }
        int unicodeRepetitions = profile.equals("distinct-pngs")
                ? MEDIA_PROFILE_UNICODE_REPETITIONS : UNICODE_REPETITIONS;
        html.append("<p>").append("Audit acțiune ✓ {JSON} ".repeat(unicodeRepetitions))
                .append("evidence-end-marker</p></div>")
                .append("<div class=\"test-wrapper row view category-view attributes-view\"></div>")
                .append("<div class=\"container-fluid p-4 view dashboard-view\"></div>")
                .append("</body></html>");
        Files.writeString(report, html);
        html = null;
        System.gc();
        AtomicLong peakHeapBytes = new AtomicLong(
                ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        AtomicBoolean sampling = new AtomicBoolean(true);
        Thread sampler = new Thread(() -> sampleHeapUsage(peakHeapBytes, sampling),
                "atf-publication-heap-sampler");
        sampler.setDaemon(true);
        sampler.start();
        long started = System.nanoTime();
        try {
            ExtentReportFinalizer.finalizeReport(report);
        } finally {
            sampling.set(false);
            sampler.join();
        }
        long publishMillis = (System.nanoTime() - started) / 1_000_000;
        System.out.println("SAMPLED_PEAK_HEAP_BYTES=" + peakHeapBytes.get());
        System.out.println("PUBLISH_MILLIS=" + publishMillis);
        System.out.println("PROFILE=" + profile);
    }

    private static void sampleHeapUsage(AtomicLong peakHeapBytes, AtomicBoolean sampling) {
        while (sampling.get()) {
            long used = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            peakHeapBytes.accumulateAndGet(used, Math::max);
            LockSupport.parkNanos(2_000_000);
        }
    }

    private static void appendRepeatedPixelScreenshots(StringBuilder html) {
        for (int index = 0; index < SCREENSHOTS; index++) {
            String png = index % 2 == 0
                    ? ReportTestScreenshots.ONE_PIXEL_PNG : ReportTestScreenshots.SECOND_PIXEL_PNG;
            appendScreenshot(html, png, index);
        }
    }

    private static void appendDistinctPngScreenshots(StringBuilder html) throws Exception {
        String[] payloads = new String[DISTINCT_PNG_COUNT];
        for (int index = 0; index < DISTINCT_PNG_COUNT; index++) {
            payloads[index] = distinctPng(index);
        }
        for (int repeat = 0; repeat < 2; repeat++) {
            for (int index = 0; index < payloads.length; index++) {
                appendScreenshot(html, payloads[index], repeat * payloads.length + index);
            }
        }
    }

    private static void appendScreenshot(StringBuilder html, String png, int index) {
        html.append("<a data-atf-screenshot=\"base64\" href=\"data:image/png;base64,")
                .append(png)
                .append("\">📷 image ")
                .append(index)
                .append("</a>");
    }

    private static String distinctPng(int seed) throws Exception {
        BufferedImage image = new BufferedImage(DISTINCT_PNG_SIZE, DISTINCT_PNG_SIZE,
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < DISTINCT_PNG_SIZE; y++) {
            for (int x = 0; x < DISTINCT_PNG_SIZE; x++) {
                int noise = x * 0x45d9f3b ^ y * 0x119de1f3 ^ seed * 0x27d4eb2d;
                noise ^= noise >>> 16;
                noise *= 0x45d9f3b;
                noise ^= noise >>> 16;
                image.setRGB(x, y, noise & 0x00ffffff);
            }
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                throw new IllegalStateException("PNG writer is unavailable");
            }
            return Base64.getEncoder().encodeToString(output.toByteArray());
        }
    }
}
