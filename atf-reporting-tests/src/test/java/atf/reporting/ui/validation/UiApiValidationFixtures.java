package atf.reporting.ui.validation;

import atf.reporting.model.ReportExecutionEvidence;

import java.util.Collection;
import java.util.List;

/** Fixture assembly only; the reporting entry points accept the typed model exclusively. */
public final class UiApiValidationFixtures {
    private UiApiValidationFixtures() { }

    public static UiApiValidationEvidence observedExchanges(
            Collection<? extends ReportExecutionEvidence> exchanges) {
        return new UiApiValidationEvidence(List.of(), exchanges.stream()
                .map(exchange -> new UiApiValidationEvidence.Exchange(exchange, List.of())).toList());
    }
}
