package atf.autoex.tests.component.lifecycle;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.runtime.AutoExRunLifecycle.Completion;
import atf.autoex.tests.extension.AutoExRunLifecycleAdapter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@AutoExComponentTest
class AutoExRunLifecycleAdapterComponentTest {
    @Test
    void originalFailureAndEverySecondaryFailureSurviveAllStages() {
        var events = new ArrayList<String>();
        var primary = new IllegalStateException("test");
        Throwable result = AutoExRunLifecycleAdapter.INSTANCE.finish(primary, new Completion(
                () -> fail(events, "cleanup"), () -> fail(events, "diagnostics"),
                failure -> { events.add("evidence"); throw new IllegalArgumentException("evidence"); },
                () -> fail(events, "close"),
                failed -> { events.add("phase:" + failed); throw new IllegalStateException("phase"); },
                failed -> { events.add("scenario:" + failed); throw new IllegalStateException("scenario"); }));
        assertSame(primary, result);
        assertEquals(List.of("evidence", "cleanup", "diagnostics", "close", "phase:true", "scenario:true"), events);
        assertEquals(List.of("evidence", "cleanup", "diagnostics", "close", "phase", "scenario"),
                java.util.Arrays.stream(result.getSuppressed()).map(Throwable::getMessage).toList());
    }

    @Test
    void firstCleanupFailureIsCapturedBeforeTheBrowserCloses() {
        var events = new ArrayList<String>();
        Throwable result = AutoExRunLifecycleAdapter.INSTANCE.finish(null, new Completion(
                () -> fail(events, "cleanup"), () -> events.add("diagnostics"),
                failure -> events.add("evidence:" + failure.getMessage()), () -> events.add("close"),
                failed -> events.add("phase:" + failed), failed -> events.add("scenario:" + failed)));
        assertEquals("cleanup", result.getMessage());
        assertEquals(List.of("cleanup", "diagnostics", "evidence:cleanup", "close", "phase:true", "scenario:true"), events);
    }

    @Test
    void phasePublicationFailureStillCompletesTheScenarioAsFailed() {
        var events = new ArrayList<String>();
        Throwable result = AutoExRunLifecycleAdapter.INSTANCE.finish(null, new Completion(
                () -> events.add("cleanup"), () -> events.add("diagnostics"),
                failure -> events.add("unexpected evidence"), () -> events.add("close"),
                failed -> { events.add("phase:" + failed); throw new IllegalStateException("phase"); },
                failed -> events.add("scenario:" + failed)));
        assertEquals("phase", result.getMessage());
        assertEquals(List.of("cleanup", "diagnostics", "close", "phase:false", "scenario:true"), events);
    }

    private static void fail(List<String> events, String name) throws Exception {
        events.add(name);
        throw new Exception(name);
    }
}
