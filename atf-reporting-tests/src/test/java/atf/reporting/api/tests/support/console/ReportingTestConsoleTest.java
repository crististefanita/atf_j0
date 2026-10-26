package atf.reporting.api.tests.support.console;

import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import atf.reporting.console.AtfConsoleEvent.Scope;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static atf.reporting.console.AtfConsoleEvent.code;

@AtfComponentTest
class ReportingTestConsoleTest {
    @Test
    void coreAndReportingDetailsReachOneSinkWithoutDroppingPayloads() {
        var events = new ArrayList<AtfConsoleEvent>();
        var console = ReportingTestConsole.using(events::add);
        console.core().publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST, "url", "https://example.test/a"));
        console.reporting().publish(code(Scope.REQUEST, "response.body", "{\"ok\":true}"));
        assertEquals(2, events.size());
        assertEquals(AtfConsoleEvent.Type.DETAIL, events.getFirst().type());
        assertEquals(AtfConsoleEvent.Type.CODE, events.getLast().type());
        assertEquals("response.body", events.getLast().detailName());
        assertEquals("{\"ok\":true}", events.getLast().detailValue());
    }

    @Test
    void explicitlySilentSinkRemainsSupported() {
        var console = ReportingTestConsole.using(AtfConsoleSink.noOp());
        assertDoesNotThrow(() -> console.core().publish(
                AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST, "status", "200")));
        assertThrows(NullPointerException.class, () -> ReportingTestConsole.using(null));
    }
}
