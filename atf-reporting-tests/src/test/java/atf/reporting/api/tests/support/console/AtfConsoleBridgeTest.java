package atf.reporting.api.tests.support.console;

import atf.core.reporting.console.AtfConsoleEvent;
import atf.core.reporting.console.AtfConsoleSink;
import atf.core.reporting.contract.AtfReportStatus;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static atf.reporting.console.AtfConsoleEvent.completed;

@AtfComponentTest
class AtfConsoleBridgeTest {
    @Test
    void coreEventsReachTheStandaloneReportingPortWithoutLoss() {
        List<atf.reporting.console.AtfConsoleEvent> received = new ArrayList<>();
        var reportingSink = (atf.reporting.console.AtfConsoleSink) received::add;
        var coreSink = AtfConsoleBridge.toCoreSink(reportingSink);

        coreSink.publish(AtfConsoleEvent.started(AtfConsoleEvent.Scope.REQUEST, "POST /login"));
        coreSink.publish(AtfConsoleEvent.detail(AtfConsoleEvent.Scope.REQUEST, "status", "200"));
        coreSink.publish(AtfConsoleEvent.code(AtfConsoleEvent.Scope.REQUEST, "body", "{\"ok\":true}"));
        coreSink.publish(AtfConsoleEvent.completed(AtfConsoleEvent.Scope.REQUEST,
                "POST /login", AtfReportStatus.PASS, 12));

        assertEquals(4, received.size());
        assertEquals("POST /login", received.get(0).name());
        assertEquals("status", received.get(1).detailName());
        assertEquals("{\"ok\":true}", received.get(2).detailValue());
        assertEquals(AtfReportStatus.PASS.name(), received.get(3).status().name());
        assertEquals(12, received.get(3).durationMillis());
    }

    @Test
    void reportingEventsReachTheCorePortWithTheSameFiniteVocabulary() {
        List<AtfConsoleEvent> received = new ArrayList<>();
        AtfConsoleSink coreSink = received::add;
        var reportingSink = AtfConsoleBridge.toReportingSink(coreSink);

        reportingSink.publish(completed(
                atf.reporting.console.AtfConsoleEvent.Scope.VALIDATION,
                "Cart contract",
                Enum.valueOf(atf.reporting.contract.AtfReportStatus.class, AtfReportStatus.FAIL.name()),
                31));

        assertEquals(1, received.size());
        assertEquals(AtfConsoleEvent.Type.COMPLETE, received.get(0).type());
        assertEquals(AtfConsoleEvent.Scope.VALIDATION, received.get(0).scope());
        assertEquals(AtfReportStatus.FAIL, received.get(0).status());
        assertEquals("Cart contract", received.get(0).name());
    }

    @Test
    void everyStatusRoundTripsThroughTheExplicitAdapter() {
        List<AtfConsoleEvent> received = new ArrayList<>();
        var reportingSink = AtfConsoleBridge.toReportingSink(received::add);

        for (var status : AtfReportStatus.values()) {
            reportingSink.publish(completed(
                    atf.reporting.console.AtfConsoleEvent.Scope.SCENARIO,
                    status.name(),
                    Enum.valueOf(atf.reporting.contract.AtfReportStatus.class, status.name()),
                    status.severity()));
        }

        assertEquals(AtfReportStatus.values().length, received.size());
        for (int index = 0; index < received.size(); index++) {
            assertEquals(AtfReportStatus.values()[index],
                    received.get(index).status());
        }
    }

    @Test
    void bridgeRejectsMissingPortInsteadOfSilentlyDroppingLifecycle() {
        assertThrows(NullPointerException.class, () -> AtfConsoleBridge.toCoreSink(null));
        assertThrows(NullPointerException.class, () -> AtfConsoleBridge.toReportingSink(null));
        assertNotNull(AtfConsoleBridge.toReportingSink(AtfConsoleSink.noOp()));
    }
}
