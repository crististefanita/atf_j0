package atf.reporting.browser.contract;

import atf.reporting.browser.junit.JunitBrowserProbe;
import atf.reporting.browser.lifecycle.*;
import atf.testkit.AtfComponentTest;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
import static org.junit.jupiter.api.Assertions.*;

/** Compiled external fixtures prove actual JUnit semantics without executable failure fixtures in the repository. */
@AtfComponentTest
@Tag("report-browser")
class BrowserBoundaryContractTest {
    @TempDir(cleanup = CleanupMode.NEVER) Path directory;
    private static final String OWNER = "browser.control.OriginalCases";
    private static final String ROOT = "[engine:junit-jupiter]/[class:" + OWNER + "]";

    @Test void originalClassLifecycleRunsOnceAndAFailedSiblingCannotBecomePass() throws Exception {
        try (var fixture = compile()) {
            var result = fixture.run(selectClass(fixture.type));
            assertEquals(6, result.getTestsStartedCount());
            assertEquals(5, result.getTestsSucceededCount());
            assertEquals(1, result.getTestsFailedCount());
            assertEquals(0, result.getTestsSkippedCount());
            assertEquals(1, fixture.lines("beforeAll"));
            assertEquals(1, fixture.lines("afterAll"));
            assertEquals(1, fixture.lines("factory"));
            assertTrue(result.getFailures().getFirst().getException().toString().contains("originalFail"));
        }
    }

    @Test void individualMethodDoesNotExecuteItsSiblingOrItsDynamicFactory() throws Exception {
        try (var fixture = compile()) {
            var result = fixture.run(selectMethod(fixture.type, "originalPass"));
            assertEquals(1, result.getTestsSucceededCount());
            assertEquals(1, result.getTestsStartedCount());
            assertEquals(0, result.getTotalFailureCount());
            assertEquals(0, fixture.lines("fail"));
            assertEquals(0, fixture.lines("factory"));
            assertEquals(1, fixture.lines("beforeAll"));
            assertEquals(1, fixture.lines("afterAll"));
        }
    }

    @Test void anIndividualParameterizedIterationKeepsItsOriginalValue() throws Exception {
        try (var fixture = compile()) {
            var result = fixture.run(selectIteration(selectMethod(fixture.type, "originalArgument", int.class), 1));
            assertEquals(1, result.getTestsSucceededCount());
            assertEquals(1, result.getTestsStartedCount());
            assertEquals(0, result.getTotalFailureCount());
            assertEquals(0, fixture.lines("argument:7"));
            assertEquals(1, fixture.lines("argument:11"));
        }
    }

    @Test void anIndividualDynamicCaseKeepsItsOriginalContainerAndOrdinal() throws Exception {
        try (var fixture = compile()) {
            var result = fixture.run(selectUniqueId(ROOT + "/[test-factory:originalDynamic()]/[dynamic-container:#1]/[dynamic-test:#2]"));
            assertEquals(1, result.getTestsSucceededCount());
            assertEquals(1, result.getTestsStartedCount());
            assertEquals(0, result.getTotalFailureCount());
            assertEquals(0, fixture.lines("dynamic:7"));
            assertEquals(1, fixture.lines("dynamic:11"));
        }
    }

    @Test void theReportOutputUsesTheShortOwnedSdkScratchInsteadOfTheLongParentRoot() throws Exception {
        try (var fixture = compile(); var mode = new Property("atf.browser.control.mode", "short-output")) {
            var result = fixture.run(selectMethod(fixture.type, "originalPass"));
            assertEquals(1, result.getTestsSucceededCount());
            assertEquals(1, result.getTestsStartedCount());
            assertEquals(0, result.getTotalFailureCount());
        }
    }

    @Test void missingSelectionCannotCertifyAZeroCaseBrowserRun() throws Exception {
        try (var fixture = compile()) {
            assertThrows(BrowserProbeFailure.class, () -> BrowserProbeProcess.run(directory, JunitBrowserProbe.class,
                    negativeBudget(), List.of(fixture.classes), ROOT + "/[method:missing()]"));
        }
    }

    @Test void aNonCooperativeSetupIsStoppedAtItsOwnPhase() throws Exception {
        try (var fixture = compile(); var journal = new Property("atf.browser.control.journal", fixture.log.toString());
             var property = new Property("atf.browser.control.mode", "setup-hang")) {
            var failure = assertThrows(BrowserProbeFailure.class, () -> BrowserProbeProcess.run(directory, JunitBrowserProbe.class,
                    negativeBudget(), List.of(fixture.classes), ROOT + "/[method:originalPass()]"));
            assertEquals("SETUP", failure.phase());
            assertEquals(1, fixture.lines("beforeAll"), "The negative must enter the original setup, not merely time out during JVM startup");
            assertFalse(ProcessHandle.of(failure.pid()).map(ProcessHandle::isAlive).orElse(false));
        }
    }

    @Test void originalAssertionRemainsAuditableWhenAfterAllCleanupHangs() throws Exception {
        try (var fixture = compile(); var property = new Property("atf.browser.control.mode", "cleanup-hang")) {
            var failure = assertThrows(BrowserProbeFailure.class, () -> BrowserProbeProcess.run(directory, JunitBrowserProbe.class,
                    negativeBudget(), List.of(fixture.classes), ROOT + "/[method:originalFail()]"));
            assertEquals("CLEANUP", failure.phase());
            assertTrue(failure.getCause().getMessage().contains("ORIGINAL-SENTINEL"));
            assertFalse(ProcessHandle.of(failure.pid()).map(ProcessHandle::isAlive).orElse(false));
        }
    }

    @Test void aVerificationInsideFinallyIsRecordedBeforeANonCooperativeDisposer() throws Exception {
        try (var fixture = compile(); var journal = new Property("atf.browser.control.journal", fixture.log.toString());
             var mode = new Property("atf.browser.control.mode", "finish-hang")) {
            var failure = assertThrows(BrowserProbeFailure.class, () -> BrowserProbeProcess.run(directory, JunitBrowserProbe.class,
                    negativeBudget(), List.of(fixture.classes), ROOT + "/[method:originalPass()]"));
            assertEquals("CLEANUP", failure.phase());
            assertTrue(failure.getCause().getMessage().contains("FINALLY-SENTINEL"));
            assertEquals(1, fixture.lines("pass"));
        }
    }

    @Test void originalTryBodyIsRecordedBeforeItsResourceDisposalCanHang() throws Exception {
        try (var fixture = compile(); var journal = new Property("atf.browser.control.journal", fixture.log.toString());
             var mode = new Property("atf.browser.control.mode", "twr-hang")) {
            var failure = assertThrows(BrowserProbeFailure.class, () -> BrowserProbeProcess.run(directory, JunitBrowserProbe.class,
                    negativeBudget(), List.of(fixture.classes), ROOT + "/[method:originalPass()]"));
            assertEquals("CLEANUP", failure.phase());
            assertTrue(failure.getCause().getMessage().contains("TWR-SENTINEL"));
            assertEquals(1, fixture.lines("pass"));
        }
    }

    @Test void originalBodyFailureIsRecordedBeforeAfterEachCanHang() throws Exception {
        try (var fixture = compile(); var journal = new Property("atf.browser.control.journal", fixture.log.toString());
             var mode = new Property("atf.browser.control.mode", "after-each-hang")) {
            var failure = assertThrows(BrowserProbeFailure.class, () -> BrowserProbeProcess.run(directory, JunitBrowserProbe.class,
                    negativeBudget(), List.of(fixture.classes), ROOT + "/[method:originalPass()]"));
            assertEquals("CLEANUP", failure.phase());
            assertTrue(failure.getCause().getMessage().contains("AFTER-EACH-SENTINEL"));
            assertEquals(1, fixture.lines("pass"));
        }
    }

    @Test void theFirstCleanupExceptionSurvivesALaterNonCooperativeDisposer() throws Exception {
        try (var fixture = compile(); var journal = new Property("atf.browser.control.journal", fixture.log.toString());
             var mode = new Property("atf.browser.control.mode", "first-cleanup-then-hang")) {
            var failure = assertThrows(BrowserProbeFailure.class, () -> BrowserProbeProcess.run(directory, JunitBrowserProbe.class,
                    negativeBudget(), List.of(fixture.classes), ROOT + "/[method:originalPass()]"));
            assertEquals("CLEANUP", failure.phase());
            assertTrue(failure.getCause().getMessage().contains("FIRST-CLEANUP-SENTINEL"));
            assertEquals(1, fixture.lines("pass"));
        }
    }

    @Test void aHandledExpectedFailureCannotBecomeTheLaterCleanupCause() throws Exception {
        try (var fixture = compile(); var journal = new Property("atf.browser.control.journal", fixture.log.toString());
             var mode = new Property("atf.browser.control.mode", "handled-cleanup-hang")) {
            var failure = assertThrows(BrowserProbeFailure.class, () -> BrowserProbeProcess.run(directory, JunitBrowserProbe.class,
                    negativeBudget(), List.of(fixture.classes), ROOT + "/[method:originalPass()]"));
            assertEquals("CLEANUP", failure.phase());
            assertFalse(failure.getCause().getMessage().contains("HANDLED-SENTINEL"));
            assertEquals(1, fixture.lines("pass"));
        }
    }

    @Test void nativeLogBytesCannotReplaceTheOriginalFailureWithADecodingException() throws Exception {
        try (var fixture = compile(); var mode = new Property("atf.browser.control.mode", "non-utf8-log")) {
            var failure = assertThrows(BrowserProbeFailure.class, () -> BrowserProbeProcess.run(directory, JunitBrowserProbe.class,
                    negativeBudget(), List.of(fixture.classes), ROOT + "/[method:originalPass()]"));
            assertTrue(failure.getCause().getMessage().contains("ORIGINAL-LOG-SENTINEL"));
            byte[] raw = Files.readAllBytes(failure.directory().resolve("process.log"));
            assertTrue(java.util.stream.IntStream.range(0, raw.length).anyMatch(index -> raw[index] == (byte) 0x96),
                    "The unmodified raw native diagnostic remains available");
        }
    }

    @Test void ownedJavaDiagnosticsAreEncodedAsUtf8IndependentlyOfTheWindowsConsole() throws Exception {
        try (var fixture = compile(); var mode = new Property("atf.browser.control.mode", "utf8-log")) {
            Path result = BrowserProbeProcess.run(directory, JunitBrowserProbe.class, negativeBudget(),
                    List.of(fixture.classes), ROOT + "/[method:originalPass()]");
            assertTrue(Files.readString(result.resolve("process.log")).contains("café – săgeată →"));
        }
    }

    private static BrowserProbeBudget negativeBudget() {
        return new BrowserProbeBudget(Duration.ofSeconds(3), Duration.ofSeconds(15), Duration.ofSeconds(2));
    }

    private Fixture compile() throws Exception {
        Path classes = Files.createDirectory(directory.resolve("classes"));
        Path source = directory.resolve("OriginalCases.java"); Files.writeString(source, FIXTURE);
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-proc:none", "-encoding", "UTF-8", "-cp",
                System.getProperty("java.class.path"), "-d", classes.toString(), source.toString());
        assertEquals(0, exit, "The controlled client must compile against the real harness classpath");
        var loader = new URLClassLoader(new java.net.URL[] {classes.toUri().toURL()}, getClass().getClassLoader());
        return new Fixture(classes, loader, loader.loadClass(OWNER), directory.resolve("calls.log"));
    }

    private record Fixture(Path classes, URLClassLoader loader, Class<?> type, Path log) implements AutoCloseable {
        org.junit.platform.launcher.listeners.TestExecutionSummary run(DiscoverySelector selection) throws Exception {
            var previous = Thread.currentThread().getContextClassLoader();
            try (var property = new Property("atf.browser.control.journal", log.toString())) {
                Thread.currentThread().setContextClassLoader(loader);
                var summary = new SummaryGeneratingListener();
                LauncherFactory.create().execute(LauncherDiscoveryRequestBuilder.request().selectors(selection).build(), summary);
                return summary.getSummary();
            } finally { Thread.currentThread().setContextClassLoader(previous); }
        }
        long lines(String value) throws Exception { return Files.exists(log) ? Files.readAllLines(log).stream().filter(value::equals).count() : 0; }
        @Override public void close() throws Exception { loader.close(); }
    }

    private static final class Property implements AutoCloseable {
        private final String key;
        private final String previous;
        Property(String key, String value) { this.key = key; previous = System.getProperty(key); System.setProperty(key, value); }
        @Override public void close() { if (previous == null) System.clearProperty(key); else System.setProperty(key, previous); }
    }

    private static final String FIXTURE = """
            package browser.control;
            import atf.reporting.browser.junit.ReportBrowserTest;
            import atf.reporting.browser.lifecycle.BrowserProbeScope;
            import atf.ui.tests.extent.support.lifecycle.BrowserResources;
            import java.nio.file.*;
            import java.util.concurrent.locks.LockSupport;
            import java.util.stream.Stream;
            import org.junit.jupiter.api.*;
            import org.junit.jupiter.params.ParameterizedTest;
            import org.junit.jupiter.params.provider.ValueSource;
            import static org.junit.jupiter.api.Assertions.*;
            @ReportBrowserTest @TestInstance(TestInstance.Lifecycle.PER_CLASS)
            public class OriginalCases {
              private AutoCloseable resource;
              private static void log(String value) throws Exception {
                String file=System.getProperty("atf.browser.control.journal");
                if(file!=null)Files.writeString(Path.of(file),value+"\\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
              }
              private static void hang(){while(true){Thread.interrupted();LockSupport.parkNanos(10000000);}}
              @BeforeAll void beforeAll() throws Exception {
                assertTrue(BrowserProbeScope.active(),"Parent must not execute browser lifecycle");log("beforeAll");
                if("non-utf8-log".equals(System.getProperty("atf.browser.control.mode"))) {
                  System.out.write(0x96);System.out.flush();throw new IllegalStateException("ORIGINAL-LOG-SENTINEL");
                }
                resource=BrowserResources.create(()->{if("setup-hang".equals(System.getProperty("atf.browser.control.mode")))hang();
                  return ()->{if(java.util.Set.of("cleanup-hang","finish-hang","twr-hang","after-each-hang","handled-cleanup-hang","first-cleanup-then-hang").contains(System.getProperty("atf.browser.control.mode","")))hang();};});
              }
              @AfterAll void afterAll() throws Exception {log("afterAll");BrowserResources.close(resource);}
              @BeforeEach void beforeEach() throws Exception {log("beforeEach");}
              @AfterEach void afterEach() throws Exception {log("afterEach");
                if(java.util.Set.of("after-each-hang","handled-cleanup-hang").contains(System.getProperty("atf.browser.control.mode",""))) BrowserResources.close(resource);}
              @Test void originalPass() throws Exception {log("pass");assertTrue(BrowserProbeScope.active());
                if("utf8-log".equals(System.getProperty("atf.browser.control.mode"))) System.out.println("café – săgeată →");
                if("after-each-hang".equals(System.getProperty("atf.browser.control.mode"))) throw new AssertionError("AFTER-EACH-SENTINEL");
                if("handled-cleanup-hang".equals(System.getProperty("atf.browser.control.mode")))
                  assertThrows(AssertionError.class,()->BrowserResources.finish(()->{throw new AssertionError("HANDLED-SENTINEL");}));
                if("twr-hang".equals(System.getProperty("atf.browser.control.mode"))) {
                  try(var lease=BrowserResources.open(()->resource)) {
                    try {throw new AssertionError("TWR-SENTINEL");}
                    catch(Exception|Error original) {BrowserResources.recordFailure(original);throw original;}
                  }
                }
                if("short-output".equals(System.getProperty("atf.browser.control.mode"))) {
                  Path output=Path.of(System.getProperty("atf.test.reportDirectory"));
                  assertTrue(output.startsWith(Path.of(System.getProperty("java.io.tmpdir"))),"Report output must share the exclusively owned short SDK scratch");
                  assertEquals("reports",output.getFileName().toString());
                }
                if("finish-hang".equals(System.getProperty("atf.browser.control.mode")))
                  BrowserResources.finish(()->{throw new AssertionError("FINALLY-SENTINEL");},resource);
                if("first-cleanup-then-hang".equals(System.getProperty("atf.browser.control.mode"))) {
                  AutoCloseable first=BrowserResources.create(()->()->{throw new IllegalStateException("FIRST-CLEANUP-SENTINEL");});
                  BrowserResources.closeAll(first,resource);
                }
              }
              @Test void originalFail() throws Exception {log("fail");assertEquals("expected","wrong","ORIGINAL-SENTINEL");}
              @ParameterizedTest @ValueSource(ints={7,11}) void originalArgument(int value) throws Exception {log("argument:"+value);assertTrue(value==7||value==11);}
              @TestFactory Stream<DynamicNode> originalDynamic() throws Exception {
                assertNotNull(resource,"Original factory requires class setup");log("factory");
                return Stream.of(DynamicContainer.dynamicContainer("Original container",Stream.of(
                  DynamicTest.dynamicTest("Original seven",()->{log("dynamic:7");assertTrue(BrowserProbeScope.active());}),
                  DynamicTest.dynamicTest("Original eleven",()->{log("dynamic:11");assertTrue(BrowserProbeScope.active());}))));
              }
            }
            """;
}
