package atf.console.junit;

import atf.core.reporting.console.AtfConsoleEvent;
import atf.testkit.AtfConsoleMessages;
import atf.testkit.AtfIntegrationTest;
import atf.testkit.reporting.AtfReportPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.testkit.engine.EngineTestKit;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.jupiter.api.Assertions.*;

/** Message verbosity cannot erase a separately opted-in final-path event. */
@AtfIntegrationTest
class AtfReportPathLifecycleIntegrationTest {
    @TempDir Path directory;

    @Test void ordinaryAndDynamicOwnersRespectTheirOwnPolicyAndGlobalConsoleSwitch() throws Exception {
        for (boolean enabled : new boolean[]{true, false}) {
            for (Class<?> type : new Class<?>[]{Ordinary.class, Dynamic.class, OptOut.class}) {
                Path config = directory.resolve("console.yaml");
                Files.writeString(config, "reporting:\n  console:\n    enabled: " + enabled + "\n    detailsEnabled: false\n");
                String previous = System.getProperty("atf.report.config");
                PrintStream original = System.out;
                var bytes = new ByteArrayOutputStream();
                try (var capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                    System.setOut(capture);
                    System.setProperty("atf.report.config", config.toString());
                    EngineTestKit.engine("junit-jupiter").selectors(selectClass(type)).execute()
                            .testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
                } finally {
                    System.setOut(original);
                    if (previous == null) System.clearProperty("atf.report.config"); else System.setProperty("atf.report.config", previous);
                }
                String output = bytes.toString(StandardCharsets.UTF_8);
                long count = output.lines().filter(line -> line.contains("Extent report: ")).count();
                assertEquals(enabled && type != OptOut.class ? 1 : 0, count, type.getSimpleName());
                assertFalse(output.contains("hidden lifecycle detail"));
                if (count == 1) assertTrue(output.contains(path().toString()), "the entire absolute path must survive");
                assertNull(AtfConsoleTestContext.current());
            }
        }
    }

    private static Path path() { return Path.of("target", "report with spaces Ș " + "x".repeat(250) + ".html").toAbsolutePath().normalize(); }
    private static void emit() {
        var sink = AtfConsoleTestContext.configuredSink();
        sink.publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST, "hidden lifecycle detail", "hidden"));
        sink.publish(AtfConsoleEvent.reportPublished(path()));
    }

    @AtfIntegrationTest @AtfReportPath @AtfConsoleMessages(AtfConsoleMessages.Policy.OFF)
    @ExtendWith(AtfConsoleTestExtension.class)
    static class Ordinary { @Test void finalPath() { emit(); } }
    @AtfIntegrationTest @AtfReportPath @AtfConsoleMessages(AtfConsoleMessages.Policy.OFF)
    @ExtendWith(AtfConsoleTestExtension.class)
    static class Dynamic {
        @TestFactory Stream<org.junit.jupiter.api.DynamicTest> children() {
            return Stream.of(AtfConsoleTestContext.dynamicTest(Dynamic.class, "final path", AtfReportPathLifecycleIntegrationTest::emit));
        }
    }
    @AtfIntegrationTest @AtfReportPath(AtfReportPath.Policy.DISABLED) @AtfConsoleMessages(AtfConsoleMessages.Policy.OFF)
    @ExtendWith(AtfConsoleTestExtension.class)
    static class OptOut { @Test void finalPath() { emit(); } }
}
