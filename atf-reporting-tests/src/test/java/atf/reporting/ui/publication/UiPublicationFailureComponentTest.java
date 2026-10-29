package atf.reporting.ui.publication;

import atf.reporting.api.ReportRequest;
import atf.reporting.api.ReportResponse;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.evidence.ReportScreenshotPolicy;
import atf.reporting.evidence.profile.ReportEvidenceFlowMode;
import atf.reporting.evidence.profile.ReportEvidenceProfile;
import atf.reporting.model.ReportExecutionEvidence;
import atf.reporting.ui.UiExtentReportSession;
import atf.reporting.ui.validation.UiApiValidationEvidence;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import static atf.reporting.contract.AtfGherkinKeyword.THEN;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Faults are injected at public ports; the facade must preserve identities, order and scope ownership. */
@AtfComponentTest
class UiPublicationFailureComponentTest {
    @Test void nestedApiCloseFailuresStaySuppressedOnTheOriginalAssertionPublicationFailure() {
        var ports = new ControlledPorts("assertions", Fault.BODY_CLOSE);
        ports.failEveryClose = true;
        Throwable observed = assertThrows(Throwable.class, () -> publish(Mode.API_TRACE, ports));
        assertSame(ports.primary, observed);
        assertEquals(List.of("assertions", "exchange", "validation", "request"), new ArrayList<>(ports.closeFailures.keySet()));
        assertEquals(new ArrayList<>(ports.closeFailures.values()), List.of(observed.getSuppressed()));
        for (String acquired : ports.acquired) {
            assertEquals(1L, ports.events.stream().filter(e -> e.equals(acquired + ".close")).count());
        }
    }

    @ParameterizedTest(name = "{0} · {1} · {2}")
    @MethodSource("publications")
    void ownedScopesCloseOnceAndPreserveTheFirstFailure(Mode mode, String owner, Fault fault) {
        var ports = new ControlledPorts(owner, fault);
        Runnable publication = () -> publish(mode, ports);
        if (fault == Fault.NONE) assertDoesNotThrow(publication::run);
        else {
            Throwable observed = assertThrows(Throwable.class, publication::run);
            assertSame(fault == Fault.CLOSE ? ports.cleanup : ports.primary, observed, ports.events.toString());
            assertEquals(fault == Fault.BODY_CLOSE || fault == Fault.COMPLETE_CLOSE || fault == Fault.ERROR_CLOSE
                    ? List.of(ports.cleanup) : List.of(), List.of(observed.getSuppressed()));
        }
        for (String acquired : ports.acquired) {
            assertEquals(1L, ports.events.stream().filter(e -> e.equals(acquired + ".close")).count(),
                    "Exactly one close per acquired scope: " + ports.events);
        }
        assertEquals(1L, ports.events.stream().filter(e -> e.equals("phase.startRequest")).count(), "Publication is never replayed");
    }

    private static void publish(Mode mode, ControlledPorts ports) {
        var profile = ReportEvidenceProfile.builder().screenshotPolicy(ReportScreenshotPolicy.NONE)
                .flowMode(mode == Mode.UNIFIED || mode == Mode.CROSS_PHASE
                        ? ReportEvidenceFlowMode.UNIFIED_FLOW : ReportEvidenceFlowMode.SCREEN_CHECKPOINTS).build();
        var scenario = UiExtentReportSession.adaptScenario(ports.scenario(), profile, "controlled publication");
        if (mode == Mode.CROSS_PHASE) scenario.unifyAcrossPhases();
        var phase = scenario.startPhase("TEST BODY");
        var checks = List.of(new AtfAssertion("Independent comparison", "ready", "ready", true));
        switch (mode) {
            case OUTCOME -> phase.outcome(THEN, "the artifact is published", "local artifact", AtfReportStatus.PASS);
            case SETUP -> phase.setup(THEN, "the input is ready", "local target", checks, AtfReportStatus.PASS);
            case API_TRACE -> phase.apiTrace(THEN, "the exchange is observed", "local target", api(), AtfReportStatus.PASS);
            case MEDIA -> phase.uiValidationAsMedia("the state is confirmed", "local target", "", checks, List.of(), AtfReportStatus.PASS);
            case ACTION, UNIFIED, CROSS_PHASE -> {
                phase.step(THEN, "the state is confirmed", "local target", "", checks, AtfReportStatus.PASS);
                if (mode == Mode.UNIFIED) phase.complete(AtfReportStatus.PASS);
                if (mode == Mode.CROSS_PHASE) phase.completeFinal(AtfReportStatus.PASS);
                if (mode == Mode.CROSS_PHASE) scenario.complete(AtfReportStatus.PASS);
            }
        }
    }

    private static UiApiValidationEvidence api() {
        var request = new ReportRequest("GET", "/observed", Map.of(), Map.of(), "none", "");
        var response = new ReportResponse(200, "{}", Map.of("Content-Type", "application/json"), Duration.ofMillis(2), "https://fixture.test/observed");
        var evidence = new ReportExecutionEvidence(response, 1, Duration.ofMillis(2), List.of(), List.of(), request, ReportTransportProvenance.LIVE_HTTP);
        return new UiApiValidationEvidence(List.of(), List.of(new UiApiValidationEvidence.Exchange(evidence,
                List.of(new AtfAssertion("API comparison", "200", "200", true)))));
    }

    static Stream<Arguments> publications() {
        return Stream.of(Mode.values()).flatMap(mode -> {
            List<String> owners = switch (mode) {
                case OUTCOME, SETUP -> List.of("request");
                case API_TRACE -> List.of("request", "validation", "exchange", "assertions");
                default -> List.of("request", "validation");
            };
            return owners.stream().flatMap(owner -> Stream.of(Fault.values()).map(fault -> Arguments.of(mode, owner, fault)));
        });
    }

    enum Mode { OUTCOME, SETUP, ACTION, MEDIA, API_TRACE, UNIFIED, CROSS_PHASE }
    enum Fault { NONE, BODY, COMPLETE, CLOSE, BODY_CLOSE, COMPLETE_CLOSE, SELF_CLOSE, ERROR_CLOSE }

    private static final class ControlledPorts {
        final String owner;
        final Fault fault;
        final Throwable primary;
        final RuntimeException cleanup = new IllegalStateException("controlled close failure");
        final List<String> events = new ArrayList<>();
        final List<String> acquired = new ArrayList<>();
        final Map<String, RuntimeException> closeFailures = new java.util.LinkedHashMap<>();
        boolean failEveryClose;
        ControlledPorts(String owner, Fault fault) {
            this.owner = owner; this.fault = fault;
            primary = fault == Fault.ERROR_CLOSE ? new AssertionError("controlled body error")
                    : new IllegalArgumentException("controlled publication failure");
        }
        AtfReportPorts.Scenario scenario() { return proxy(AtfReportPorts.Scenario.class, "scenario"); }

        private <T> T proxy(Class<T> type, String node) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
                String call = method.getName();
                if (call.equals("toString")) return node;
                if (call.equals("hashCode")) return System.identityHashCode(instance);
                if (call.equals("equals")) return instance == args[0];
                events.add(node + "." + call);
                if (call.equals("id")) return "controlled-scenario";
                if (call.equals("hasWarnings")) return false;
                if (call.equals("startPhase")) return proxy(AtfReportPorts.Phase.class, "phase");
                if (call.equals("startRequest")) {
                    String child = node.equals("phase") ? "request" : "exchange";
                    acquired.add(child);
                    return proxy(AtfReportPorts.Request.class, child);
                }
                if (call.equals("startValidation")) {
                    String child = node.equals("exchange") ? "assertions" : "validation";
                    acquired.add(child);
                    return proxy(AtfReportPorts.Validation.class, child);
                }
                if (call.equals("close") && failEveryClose && acquired.contains(node)) {
                    throw closeFailures.computeIfAbsent(node, key -> new IllegalStateException("close " + key));
                }
                if (node.equals(owner)) {
                    if (call.equals("close") && fault != Fault.NONE && fault != Fault.BODY && fault != Fault.COMPLETE) {
                        if (fault == Fault.SELF_CLOSE) throw primary;
                        throw cleanup;
                    }
                    if (call.equals("complete") && (fault == Fault.COMPLETE || fault == Fault.COMPLETE_CLOSE)) throw primary;
                    if (!call.equals("close") && !call.equals("complete")
                            && (fault == Fault.BODY || fault == Fault.BODY_CLOSE || fault == Fault.SELF_CLOSE || fault == Fault.ERROR_CLOSE)) throw primary;
                }
                return null;
            }));
        }
    }
}
