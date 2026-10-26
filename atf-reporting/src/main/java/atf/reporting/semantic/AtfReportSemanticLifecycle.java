package atf.reporting.semantic;

import java.util.Collection;

/**
 * Mutable lifecycle state for the renderer-neutral semantic population.
 *
 * <p>The public recorder owns the handle contract, the state aggregate owns
 * mutation, and the snapshot is the immutable read model. No renderer or
 * vendor type crosses this boundary.</p>
 */
final class AtfReportSemanticLifecycle {
    private final AtfReportSemanticState state = new AtfReportSemanticState();

    AtfReportSemanticState.ScenarioState startScenario(String testClass, String testPackage,
                                                       String scenario, Collection<String> tags) {
        return state.startScenario(testClass, testPackage, scenario, tags);
    }

    AtfReportSemanticSnapshot snapshot() {
        return state.snapshot();
    }
}
