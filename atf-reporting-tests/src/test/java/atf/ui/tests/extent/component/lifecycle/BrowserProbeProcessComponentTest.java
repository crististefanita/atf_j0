package atf.ui.tests.extent.component.lifecycle;

import org.junit.jupiter.api.Tag;

import atf.reporting.browser.lifecycle.BrowserProbe;
import atf.reporting.browser.lifecycle.BrowserProbeBudget;
import atf.reporting.browser.lifecycle.BrowserProbeFailure;
import atf.reporting.browser.lifecycle.BrowserProbeProcess;

import atf.testkit.AtfComponentTest;
import com.microsoft.playwright.Page;
import atf.ui.tests.extent.support.lifecycle.SinglePageBrowserProbe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.CleanupMode;
import static org.junit.jupiter.api.Assertions.*;

/** Real process containment plus non-cooperative factories; no service or application dependency. */
@AtfComponentTest
@Tag("report-browser")
public class BrowserProbeProcessComponentTest {
    private static final BrowserProbeBudget NEGATIVE = new BrowserProbeBudget(
            Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(2));
    @TempDir(cleanup = CleanupMode.NEVER) Path directory;

    @Test void allThreePhasesMustCompleteBeforePass() throws Exception {
        Path evidence = BrowserProbeProcess.run(directory, Recording.class);
        assertEquals("open\nverify\nclose\n", Files.readString(evidence.resolve("calls")));
        Path scratch = Path.of(Files.readString(evidence.resolve("scratch.path")));
        assertEquals(evidence.toString(), Files.readString(scratch.resolve("ATF_OWNER")));
        assertEquals(scratch.toString(), Files.readString(evidence.resolve("native-temp")));
        assertTrue(Files.readString(evidence.resolve("outcome")).startsWith("PASS"));
        assertTrue(Files.isRegularFile(evidence.resolve("phase-DONE")));
    }

    @Test void sameSdkCreatesANewPageAndClosesItsResources() throws Exception {
        Path evidence = BrowserProbeProcess.run(directory, Sdk.class);
        byte[] png = Files.readAllBytes(evidence.resolve("viewport.png"));
        assertArrayEquals(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}, java.util.Arrays.copyOf(png, 8));
        assertTrue(Files.isRegularFile(evidence.resolve("phase-DONE")));
    }

    @Test void hungSetupFailsAndOnlyItsOwnProcessTreeIsStopped() throws Exception {
        Process unrelated = sleeper(directory.resolve("unrelated.pid"));
        try {
            var failure = assertThrows(BrowserProbeFailure.class,
                    () -> BrowserProbeProcess.run(directory, HungSetup.class, NEGATIVE));
            assertEquals("SETUP", failure.phase());
            assertFalse(ProcessHandle.of(failure.pid()).map(ProcessHandle::isAlive).orElse(false));
            long child = Long.parseLong(Files.readString(failure.directory().resolve("child.pid")));
            assertFalse(ProcessHandle.of(child).map(ProcessHandle::isAlive).orElse(false));
            assertTrue(unrelated.isAlive(), "an independently owned process must survive");
            assertTrue(Files.readString(failure.directory().resolve("threads.log")).contains("HungSetup.open"));
            assertTrue(Files.readString(failure.directory().resolve("processes.log")).contains("pid=" + child));
            assertTrue(Files.readString(failure.directory().resolve("outcome")).startsWith("FAIL"));
        } finally { unrelated.destroyForcibly(); assertTrue(unrelated.waitFor(5, TimeUnit.SECONDS)); }
    }

    @Test void hungCleanupHasItsOwnDeadlineAndCannotBecomePass() throws Exception {
        var failure = assertThrows(BrowserProbeFailure.class,
                () -> BrowserProbeProcess.run(directory, HungCleanup.class, NEGATIVE));
        assertEquals("CLEANUP", failure.phase());
        assertTrue(Files.readString(failure.directory().resolve("calls")).contains("verify"));
        assertFalse(ProcessHandle.of(failure.pid()).map(ProcessHandle::isAlive).orElse(false));
    }

    @Test void setupCausePrecedesTheSuppressedCleanupCause() {
        var failure = assertThrows(BrowserProbeFailure.class,
                () -> BrowserProbeProcess.run(directory, BrokenSetupAndCleanup.class));
        assertEquals("SETUP", failure.phase());
        String diagnostic = failure.getCause().getMessage();
        assertTrue(diagnostic.contains("IllegalArgumentException: original setup 153"));
        assertTrue(diagnostic.contains("Suppressed: java.lang.IllegalStateException: cleanup 153"));
        assertFalse(ProcessHandle.of(failure.pid()).map(ProcessHandle::isAlive).orElse(false));
    }

    @Test void cleanupFailureAloneCannotBeReportedAsPass() {
        var failure = assertThrows(BrowserProbeFailure.class,
                () -> BrowserProbeProcess.run(directory, BrokenCleanup.class));
        assertEquals("CLEANUP", failure.phase());
        assertTrue(failure.getCause().getMessage().contains("IllegalStateException: cleanup alone 153"));
    }

    @Test void hungVerificationCannotBorrowSetupOrCleanupTime() {
        var failure = assertThrows(BrowserProbeFailure.class,
                () -> BrowserProbeProcess.run(directory, HungVerification.class, NEGATIVE));
        assertEquals("VERIFY", failure.phase());
        assertTrue(Files.exists(failure.directory().resolve("threads.log")));
        assertFalse(ProcessHandle.of(failure.pid()).map(ProcessHandle::isAlive).orElse(false));
    }

    @Test void originalAssertionSurvivesAHungCleanup() {
        var failure = assertThrows(BrowserProbeFailure.class,
                () -> BrowserProbeProcess.run(directory, FailedVerificationHungCleanup.class, NEGATIVE));
        assertEquals("CLEANUP", failure.phase());
        assertTrue(failure.getCause().getMessage().contains("AssertionError: original verification 153"));
        assertFalse(ProcessHandle.of(failure.pid()).map(ProcessHandle::isAlive).orElse(false));
    }

    @Test void budgetsRejectNonPositiveDurations() {
        assertEquals(Duration.ofSeconds(30), BrowserProbeBudget.DEFAULT.setup());
        assertEquals(Duration.ofSeconds(60), BrowserProbeBudget.DEFAULT.verification());
        assertEquals(Duration.ofSeconds(5), BrowserProbeBudget.DEFAULT.cleanup());
        assertThrows(IllegalArgumentException.class, () -> new BrowserProbeBudget(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new BrowserProbeBudget(Duration.ofSeconds(1), Duration.ofSeconds(-1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new BrowserProbeBudget(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ZERO));
    }

    @Test void parentInterruptionStillReapsItsOwnedChildAndRemainsAFailure() throws Exception {
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedException.class, () -> BrowserProbeProcess.run(directory, HungSetup.class));
            assertTrue(Thread.currentThread().isInterrupted(), "cleanup must preserve the interrupt flag");
        } finally { Thread.interrupted(); }
        try (var probes = Files.list(directory)) {
            Path probe = probes.findFirst().orElseThrow();
            String outcome = Files.readString(probe.resolve("outcome"));
            assertTrue(outcome.startsWith("FAIL"));
            long pid = Long.parseLong(outcome.lines().filter(line -> line.startsWith("pid=")).findFirst().orElseThrow().substring(4));
            assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
        }
    }

    public static class Recording implements BrowserProbe {
        protected Path directory;
        @Override public void open(Path value) throws Exception {
            directory = value;
            String scratch = System.getProperty("java.io.tmpdir");
            for (String key : java.util.List.of("TEMP", "TMP", "TMPDIR")) assertEquals(scratch, System.getenv(key));
            Files.writeString(directory.resolve("native-temp"), scratch);
            call("open");
        }
        @Override public void verify(String[] arguments) throws Exception { call("verify"); }
        @Override public void close() throws Exception { call("close"); }
        private void call(String value) throws Exception {
            Files.writeString(directory.resolve("calls"), value + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        }
    }

    public static final class HungSetup extends Recording {
        @Override public void open(Path value) throws Exception { super.open(value); sleeper(value.resolve("child.pid")); hang(); }
    }

    public static class HungCleanup extends Recording { @Override public void close() { hang(); } }

    public static final class BrokenCleanup extends Recording {
        @Override public void close() { throw new IllegalStateException("cleanup alone 153"); }
    }

    public static final class HungVerification extends Recording { @Override public void verify(String[] arguments) { hang(); } }

    public static final class FailedVerificationHungCleanup extends HungCleanup {
        @Override public void verify(String[] arguments) { throw new AssertionError("original verification 153"); }
    }

    public static final class BrokenSetupAndCleanup extends Recording {
        @Override public void open(Path directory) { throw new IllegalArgumentException("original setup 153"); }
        @Override public void close() { throw new IllegalStateException("cleanup 153"); }
    }

    public static final class Sdk extends SinglePageBrowserProbe {
        @Override public void verify(String[] arguments) {
            page().navigate("data:text/html,<h1>Browser setup 153</h1>");
            assertEquals("Browser setup 153", page().locator("h1").innerText());
            page().screenshot(new Page.ScreenshotOptions().setPath(directory().resolve("viewport.png")));
        }
    }

    public static final class Sleeper {
        public static void main(String[] arguments) throws Exception {
            Files.writeString(Path.of(arguments[0]), Long.toString(ProcessHandle.current().pid()));
            while (true) { Thread.interrupted(); LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1)); }
        }
    }

    private static Process sleeper(Path pidFile) throws Exception {
        String classes = Path.of(Sleeper.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        return new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", classes, Sleeper.class.getName(), pidFile.toString())
                .redirectErrorStream(true).redirectOutput(pidFile.resolveSibling(pidFile.getFileName() + ".log").toFile()).start();
    }

    private static void hang() { while (true) { Thread.interrupted(); LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1)); } }
}
