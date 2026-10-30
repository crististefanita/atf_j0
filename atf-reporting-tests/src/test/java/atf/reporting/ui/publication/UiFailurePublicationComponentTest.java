package atf.reporting.ui.publication;

import atf.reporting.contract.*;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.evidence.profile.ReportUiActionDetailsMode;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.failure.UiFailureEvidencePublisher;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Non-cascading ports independently prove ownership for direct and facade-delegated failure publication. */
@AtfComponentTest
class UiFailurePublicationComponentTest {
    @ParameterizedTest(name = "delegated={0}; fault={1}; Error={2}; same={3}") @MethodSource("faults")
    void eachOwnedScopeClosesOnceWithoutReplacingThePrimaryCause(boolean delegated, String fault,
                                                                 boolean error, boolean same) {
        var probe = new Ports(fault, error, same);
        Runnable publish = () -> {
            if (delegated) {
                var profile = ReportEvidenceProfile.builder().screenshotPolicy(ReportScreenshotPolicy.NONE).build();
                UiExtentReportSession.adaptScenario(probe.port(AtfReportPorts.Scenario.class, "scenario"), profile, "fixture")
                        .startPhase("TEST BODY").failureEvidence("the original operation failed", "target", List.of(), probe.business);
            } else new UiFailureEvidencePublisher(probe.port(AtfReportPorts.Phase.class, "phase"),
                    ReportScreenshotPolicy.NONE, ReportUiActionDetailsMode.INCLUDED_COLLAPSED)
                    .publish("the original operation failed", "target", List.of(), probe.business, null, false);
        };
        if (fault.equals("none")) assertDoesNotThrow(publish::run);
        else assertSame(probe.primary, assertThrows(Throwable.class, publish::run), "Publication preserves throwable identity");
        assertEquals(probe.acquired.reversed(), probe.closed, "Children close before their owners, once per acquisition");
        assertEquals(1, Collections.frequency(probe.calls, "phase.startRequest"), "No publication replay");
        if (fault.equals("none")) {
            assertTrue(probe.calls.contains("validation.completeFailure"));
            assertTrue(probe.calls.contains("request.complete"));
            assertEquals(List.of(AtfReportStatus.FAIL, AtfReportStatus.FAIL), probe.statuses);
        }
        if (fault.equals("assertions+close")) assertEquals(same ? List.of() : List.of(probe.cleanup), List.of(probe.primary.getSuppressed()));
        assertEquals(0, probe.business.getSuppressed().length, "Publication errors do not mutate the reported business cause");
    }

    static Stream<Arguments> faults() {
        return Stream.of(false, true).flatMap(delegated -> Stream.of("none", "phase.startRequest", "request.detail",
                "request.startValidation", "validation.assertions", "validation.completeFailure", "request.complete",
                "validation.close", "request.close", "assertions+close").flatMap(fault -> Stream.of(false, true)
                .flatMap(error -> Stream.of(false, true).map(same -> Arguments.of(delegated, fault, error, same)))));
    }
    private static final class Ports {
        final String fault; final boolean same;
        final Throwable primary, cleanup = new IllegalStateException("cleanup");
        final Throwable business = new IllegalArgumentException("original business failure");
        final List<String> acquired = new ArrayList<>(), closed = new ArrayList<>(), calls = new ArrayList<>();
        final List<AtfReportStatus> statuses = new ArrayList<>();
        Ports(String fault, boolean error, boolean same) {
            this.fault = fault; this.same = same;
            primary = error ? new AssertionError("publication") : new IllegalStateException("publication");
        }
        <T> T port(Class<T> type, String owner) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
                String call = owner + "." + method.getName(); calls.add(call);
                if (method.getName().equals("close")) closed.add(owner);
                if (fault.equals(call) || fault.equals("assertions+close") && call.equals("validation.assertions")) throw primary;
                if (fault.equals("assertions+close") && call.equals("validation.close")) throw same ? primary : cleanup;
                if (method.getName().equals("complete") || method.getName().equals("completeFailure")) statuses.add((AtfReportStatus) args[0]);
                return switch (method.getName()) {
                    case "id" -> "fixture";
                    case "hasWarnings" -> false;
                    case "startPhase" -> port(AtfReportPorts.Phase.class, "phase");
                    case "startRequest" -> { acquired.add("request"); yield port(AtfReportPorts.Request.class, "request"); }
                    case "startValidation" -> { acquired.add("validation"); yield port(AtfReportPorts.Validation.class, "validation"); }
                    default -> null;
                };
            }));
        }
    }
}
