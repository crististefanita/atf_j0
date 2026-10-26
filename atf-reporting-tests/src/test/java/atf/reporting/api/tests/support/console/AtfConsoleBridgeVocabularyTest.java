package atf.reporting.api.tests.support.console;

import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.contract.AtfReportStatus;
import atf.reporting.console.AtfConsoleEvent.Scope;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;

import static atf.reporting.console.AtfConsoleEvent.started;
import static atf.reporting.console.AtfConsoleEvent.detail;
import static atf.reporting.console.AtfConsoleEvent.code;
import static atf.reporting.console.AtfConsoleEvent.completed;
import static atf.reporting.console.AtfConsoleEvent.reportPublished;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Cross-module vocabulary compatibility belongs to the test composition owner. */
@AtfComponentTest
class AtfConsoleBridgeVocabularyTest {
    @Test
    void mapsAllScopesForStartDetailAndCode() {
        List<AtfConsoleEvent> actual = new ArrayList<>();
        var sink = AtfConsoleBridge.toReportingSink(actual::add);
        assertEquals(List.of(AtfConsoleEvent.Scope.values()).stream().map(Enum::name).toList(),
                List.of(Scope.values()).stream()
                        .map(Enum::name).toList());

        for (var scope : Scope.values()) {
            sink.publish(started(scope, "test"));
            sink.publish(detail(scope, "field", "value"));
            sink.publish(code(scope, "body", "{}"));
        }

        assertEquals(AtfConsoleEvent.Scope.values().length * 3, actual.size());
        int index = 0;
        for (var scope : AtfConsoleEvent.Scope.values()) {
            assertEquals(AtfConsoleEvent.started(scope, "test"), actual.get(index++));
            assertEquals(AtfConsoleEvent.detail(scope, "field", "value"), actual.get(index++));
            assertEquals(AtfConsoleEvent.code(scope, "body", "{}"), actual.get(index++));
        }
    }

    @Test
    void mapsEveryCompletionStatusAndDuration() {
        List<AtfConsoleEvent> actual = new ArrayList<>();
        var sink = AtfConsoleBridge.toReportingSink(actual::add);
        var reportingStatuses = EnumSet.allOf(atf.reporting.contract.AtfReportStatus.class);
        for (var status : reportingStatuses) {
            sink.publish(completed(Scope.VALIDATION,
                    "validation", status, 27));
        }

        assertEquals(AtfReportStatus.values().length, actual.size());
        assertEquals(reportingStatuses.size(), actual.size());
        for (int index = 0; index < actual.size(); index++) {
            assertEquals(AtfReportStatus.values()[index],
                    actual.get(index).status());
            assertEquals(AtfConsoleEvent.Scope.VALIDATION, actual.get(index).scope());
            assertEquals(27, actual.get(index).durationMillis());
        }
    }

    @Test
    void mapsPublishedReportAndRejectsNullInputs() {
        List<AtfConsoleEvent> actual = new ArrayList<>();
        var sink = AtfConsoleBridge.toReportingSink(actual::add);
        Path report = Path.of("build", "reports", "extent.html").toAbsolutePath().normalize();
        sink.publish(reportPublished(report));

        assertEquals(AtfConsoleEvent.reportPublished(report), actual.getFirst());
        assertThrows(NullPointerException.class, () -> AtfConsoleBridge.toReportingSink(null));
        assertThrows(NullPointerException.class, () -> sink.publish(null));
    }
}
