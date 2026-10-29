package atf.console.junit;

import atf.testkit.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.opentest4j.TestAbortedException;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@AtfIntegrationTest @AtfConsoleMessages(AtfConsoleMessages.Policy.OFF)
@ExtendWith(AtfConsoleTestExtension.class)
class NestedConsoleContextIntegrationTest {
    @ParameterizedTest @ValueSource(strings = {"PASS", "FAIL", "SKIP"})
    void nestedDynamicBodiesRestoreTheOuterLifecycleAndSink(String outcome) throws Throwable {
        var owner = AtfConsoleTestContext.current(); assertNotNull(owner);
        var child = AtfConsoleTestContext.dynamicTest(QuietOwner.class, "child", () -> {
            var inner = AtfConsoleTestContext.current(); assertNotSame(owner, inner);
            var grandchild = AtfConsoleTestContext.dynamicTest(QuietOwner.class, "grandchild", () -> {
                assertNotSame(inner, AtfConsoleTestContext.current());
            });
            grandchild.getExecutable().execute(); assertSame(inner, AtfConsoleTestContext.current());
            if (outcome.equals("FAIL")) throw new IllegalStateException("controlled failure");
            if (outcome.equals("SKIP")) throw new TestAbortedException("controlled abort");
        });
        if (outcome.equals("PASS")) child.getExecutable().execute();
        else if (outcome.equals("FAIL")) assertThrows(IllegalStateException.class, child.getExecutable());
        else assertThrows(TestAbortedException.class, child.getExecutable());
        assertSame(owner, AtfConsoleTestContext.current()); assertSame(owner.console(), AtfConsoleTestContext.configuredSink());
    }

    @Test void aNestedJupiterInvocationRestoresTheOrdinaryOuterExtension() {
        var owner = AtfConsoleTestContext.current();
        EngineTestKit.engine("junit-jupiter").selectors(selectClass(InnerJUnitFixture.class))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "false").execute()
                .testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
        assertSame(owner, AtfConsoleTestContext.current()); assertSame(owner.console(), AtfConsoleTestContext.configuredSink());
    }

    @Test void parallelNestedOwnersRestoreOnlyTheirOwnThreadBindingAcrossMessagePolicies() throws Exception {
        var outer = AtfConsoleTestContext.current();
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var barrier = new CyclicBarrier(2);
            Callable<Void> body = () -> {
                assertNull(AtfConsoleTestContext.current());
                var parent = new AtfConsoleTestLifecycle(event -> { }, "parent", "isolated"); parent.start();
                AtfConsoleTestContext.install(parent);
                try {
                    barrier.await(2, TimeUnit.SECONDS);
                    for (var owner : List.of(QuietOwner.class, SummaryOwner.class, DetailsOwner.class)) {
                        var child = AtfConsoleTestContext.dynamicTest(owner, "nested policy", () -> {
                            assertNotSame(parent, AtfConsoleTestContext.current());
                            throw new IllegalStateException("controlled child failure");
                        });
                        assertThrows(IllegalStateException.class, child.getExecutable());
                        assertSame(parent, AtfConsoleTestContext.current());
                        assertSame(parent.console(), AtfConsoleTestContext.configuredSink());
                    }
                } finally { AtfConsoleTestContext.clear(); }
                assertNull(AtfConsoleTestContext.current()); return null;
            };
            for (var result : workers.invokeAll(List.of(body, body), 5, TimeUnit.SECONDS)) result.get();
        }
        assertSame(outer, AtfConsoleTestContext.current());
    }

    @AtfConsoleMessages(AtfConsoleMessages.Policy.OFF) public static final class QuietOwner { }
    @AtfConsoleMessages(AtfConsoleMessages.Policy.SUMMARY) public static final class SummaryOwner { }
    @AtfConsoleMessages(AtfConsoleMessages.Policy.DETAILS) public static final class DetailsOwner { }
    @AtfUnitTest @AtfConsoleMessages(AtfConsoleMessages.Policy.OFF) @ExtendWith(AtfConsoleTestExtension.class)
    static class InnerJUnitFixture { @Test void hasItsOwnLifecycle() { assertNotNull(AtfConsoleTestContext.current()); } }
}
