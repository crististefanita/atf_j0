package atf.ui.network;

import atf.testkit.AtfComponentTest;
import atf.ui.config.UiNetworkInspectionConfiguration;
import atf.ui.config.UiNetworkSettlementPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/** Presentation compaction is neutral; raw status/order and repeated causal moments remain inspectable. */
@AtfComponentTest
class UiNetworkPresentationStreamComponentTest {
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void presentationPolicyNeverCompactsRawObservationsOrHidesTheirStatuses(boolean compact) {
        try (var inspector = inspector(compact)) {
            var first = inspector.capture(observation(1, "POST", 201)).orElseThrow();
            inspector.capture(observation(2, "POST", 500));
            var readBack = inspector.capture(observation(3, "GET", 200)).orElseThrow();

            assertEquals(List.of(1L, 2L, 3L), inspector.observations().stream().map(UiNetworkObservation::sequence).toList());
            assertEquals(List.of(201, 500, 200), inspector.observations().stream().map(UiNetworkObservation::status).toList());
            assertEquals(compact ? List.of(1L, 3L) : List.of(1L, 2L, 3L),
                    inspector.presentationObservations().stream().map(UiNetworkObservation::sequence).toList());
            assertEquals(first, inspector.presentationObservations().getFirst());
            assertEquals(readBack, inspector.presentationObservations().getLast());
            assertEquals(2, inspector.signatureCounts().get(first.requestSignature()));
        }
    }

    @Test void nonConsecutiveRepetitionsRetainTheirSeparateCausalPositions() {
        try (var inspector = inspector(true)) {
            var first = inspector.capture(observation(1, "POST", 201)).orElseThrow();
            inspector.capture(observation(2, "POST", 500));
            inspector.capture(observation(3, "GET", 200));
            inspector.capture(observation(4, "POST", 500));
            assertEquals(List.of(1L, 2L, 3L, 4L), inspector.observations().stream().map(UiNetworkObservation::sequence).toList());
            assertEquals(List.of(1L, 3L, 4L), inspector.presentationObservations().stream().map(UiNetworkObservation::sequence).toList());
            assertEquals(3, inspector.signatureCounts().get(first.requestSignature()));
        }
    }

    private static UiNetworkInspector inspector(boolean compact) {
        return new UiNetworkInspector(new UiNetworkInspectionConfiguration(true, Set.of("presentation.test"),
                8, 2000, compact, UiNetworkSettlementPolicy.disabled()));
    }
    private static UiNetworkObservation observation(long sequence, String method, int status) {
        return new UiNetworkObservation(sequence, Instant.EPOCH, method, "https://presentation.test/records", "/records", status,
                Map.of(), Map.of(), method.equals("POST") ? "{}" : "", "{}", "none", "none", "presentation.test", "fixture", "", true);
    }
}
