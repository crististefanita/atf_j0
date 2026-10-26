package atf.reporting.semantic;
import atf.reporting.contract.evidence.ReportEvidenceOwner;

import atf.reporting.contract.AtfAssertion;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.model.ReportTagNormalizer;
import atf.reporting.ui.model.AtfUiActionRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable aggregate behind the semantic lifecycle boundary.
 *
 * <p>The state types stay package-private and renderer-neutral. Keeping the
 * mutation graph here makes the lifecycle adapter small while preserving the
 * typed, immutable snapshot as the only read model crossing the boundary.</p>
 */
final class AtfReportSemanticState {
    private final Map<String, ClassState> classes = new LinkedHashMap<>();
    private int nextScenarioId;

    ScenarioState startScenario(String testClass, String testPackage,
                                String scenario, Collection<String> tags) {
        String className = testClass == null || testClass.isBlank() ? "API tests" : testClass;
        ClassState owner = classes.computeIfAbsent(className,
                key -> new ClassState(key, testPackage));
        var state = new ScenarioState("test-id=" + ++nextScenarioId, scenario, tags);
        owner.scenarios.add(state);
        return state;
    }

    AtfReportSemanticSnapshot snapshot() {
        return new AtfReportSemanticSnapshot(classes.values().stream()
                .map(ClassState::snapshot).toList());
    }

    static final class ClassState {
        private final String name;
        private final String packageName;
        private final List<ScenarioState> scenarios = new ArrayList<>();
        private AtfReportStatus status = AtfReportStatus.INFO;

        private ClassState(String name, String packageName) {
            this.name = name;
            this.packageName = packageName == null ? "" : packageName;
        }

        private AtfReportSemanticSnapshot.ClassSnapshot snapshot() {
            AtfReportStatus aggregate = status;
            for (var scenario : scenarios) {
                aggregate = AtfReportStatus.worst(aggregate, scenario.status);
            }
            return new AtfReportSemanticSnapshot.ClassSnapshot(name, packageName,
                    scenarios.stream().map(ScenarioState::snapshot).toList(), aggregate);
        }
    }

    static final class ScenarioState {
        private final String id;
        private final String name;
        private final List<String> tags;
        private final List<PhaseState> phases = new ArrayList<>();
        private AtfReportStatus status = AtfReportStatus.INFO;
        private boolean completed;

        private ScenarioState(String id, String name, Collection<String> tags) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.tags = ReportTagNormalizer.normalize(tags);
        }

        String id() { return id; }

        PhaseState startPhase(String name) {
            ensureOpen("scenario");
            var phase = new PhaseState(name);
            phases.add(phase);
            return phase;
        }

        void complete(AtfReportStatus status) {
            ensureOpen("scenario");
            if (phases.stream().anyMatch(phase -> !phase.completed())) {
                throw new IllegalStateException("Cannot complete scenario before all phases are complete");
            }
            this.status = Objects.requireNonNull(status, "status");
            completed = true;
        }

        private void ensureOpen(String scope) {
            if (completed) throw new IllegalStateException(scope + " is already complete");
        }

        private AtfReportSemanticSnapshot.ScenarioSnapshot snapshot() {
            return new AtfReportSemanticSnapshot.ScenarioSnapshot(id, name, tags,
                    phases.stream().map(PhaseState::snapshot).toList(), status);
        }
    }

    static final class PhaseState {
        private final String name;
        private final List<NodeState> nodes = new ArrayList<>();
        private final List<String> detailNames = new ArrayList<>();
        private AtfReportStatus status = AtfReportStatus.INFO;
        private boolean completed;

        private PhaseState(String name) {
            this.name = name == null ? "" : name;
        }

        RequestState startRequest(String title, String method, String path) {
            ensureOpen("phase");
            var request = new RequestState(title, method, path);
            nodes.add(request);
            return request;
        }

        void detail(String name) {
            ensureOpen("phase");
            detailNames.add(normalizeLabel(name, "DETAIL"));
        }

        void complete(AtfReportStatus status) {
            ensureOpen("phase");
            if (nodes.stream().anyMatch(node -> !node.completed())) {
                throw new IllegalStateException("Cannot complete phase before all children are complete");
            }
            this.status = Objects.requireNonNull(status, "status");
            completed = true;
        }

        boolean completed() {
            return completed;
        }

        private void ensureOpen(String scope) {
            if (completed) throw new IllegalStateException(scope + " is already complete");
        }

        private AtfReportSemanticSnapshot.PhaseSnapshot snapshot() {
            return new AtfReportSemanticSnapshot.PhaseSnapshot(name,
                    nodes.stream().map(NodeState::snapshot).toList(), detailNames, status);
        }
    }

    sealed interface NodeState permits RequestState, ValidationState {
        AtfReportSemanticSnapshot.NodeSnapshot snapshot();

        boolean completed();
    }

    static final class RequestState implements NodeState {
        private final String title;
        private final String method;
        private final String path;
        private final List<String> detailNames = new ArrayList<>();
        private final List<String> jsonLabels = new ArrayList<>();
        private final List<ValidationState> validations = new ArrayList<>();
        private boolean executionPresent;
        private int uiActionCount;
        private int screenshotCount;
        private AtfReportStatus status = AtfReportStatus.INFO;
        private boolean completed;

        private RequestState(String title, String method, String path) {
            this.title = title == null ? "" : title;
            this.method = method == null ? "" : method;
            this.path = path == null ? "" : path;
        }

        void detail(String name) {
            ensureOpen();
            detailNames.add(normalizeLabel(name, "DETAIL"));
        }

        void json(String label) {
            ensureOpen();
            jsonLabels.add(normalizeLabel(label, "JSON"));
        }

        void execution() {
            ensureOpen();
            executionPresent = true;
        }

        void uiActions(Collection<AtfUiActionRecord> actions) {
            ensureOpen();
            uiActionCount += sizeOf(actions);
        }

        void screenshot() {
            ensureOpen();
            screenshotCount++;
        }

        ValidationState startValidation(String title) {
            ensureOpen();
            var validation = new ValidationState(title);
            validations.add(validation);
            return validation;
        }

        void complete(AtfReportStatus status) {
            ensureOpen();
            if (validations.stream().anyMatch(validation -> !validation.completed())) {
                throw new IllegalStateException("Cannot complete request before all validations are complete");
            }
            this.status = Objects.requireNonNull(status, "status");
            completed = true;
        }

        @Override
        public boolean completed() {
            return completed;
        }

        private void ensureOpen() {
            if (completed) throw new IllegalStateException("request is already complete");
        }

        @Override
        public AtfReportSemanticSnapshot.RequestSnapshot snapshot() {
            return new AtfReportSemanticSnapshot.RequestSnapshot(title, method, path,
                    detailNames, jsonLabels, executionPresent, uiActionCount,
                    screenshotCount, validations.stream().map(ValidationState::snapshot).toList(), status);
        }
    }

    static final class ValidationState implements NodeState {
        private ReportEvidenceOwner evidenceOwner = ReportEvidenceOwner.UNSPECIFIED;
        private final String title;
        private final List<String> assertionNames = new ArrayList<>();
        private final List<RequestState> requests = new ArrayList<>();
        private int uiActionCount;
        private int screenshotCount;
        private AtfReportStatus status = AtfReportStatus.INFO;
        private boolean completed;

        private ValidationState(String title) {
            this.title = title == null ? "" : title;
        }

        void evidenceOwner(ReportEvidenceOwner owner) {
            ensureOpen();
            evidenceOwner = Objects.requireNonNull(owner, "evidenceOwner");
        }

        RequestState startRequest(String title, String method, String path) {
            ensureOpen();
            var request = new RequestState(title, method, path);
            requests.add(request);
            return request;
        }

        void assertions(Collection<AtfAssertion> assertions) {
            ensureOpen();
            if (assertions != null) {
                assertionNames.addAll(assertions.stream()
                        .filter(Objects::nonNull)
                        .map(AtfAssertion::name)
                        .toList());
            }
        }

        void uiActions(Collection<AtfUiActionRecord> actions) {
            ensureOpen();
            uiActionCount += sizeOf(actions);
        }

        void screenshot() {
            ensureOpen();
            screenshotCount++;
        }

        void complete(AtfReportStatus status) {
            ensureOpen();
            if (requests.stream().anyMatch(request -> !request.completed())) {
                throw new IllegalStateException("Cannot complete validation before all nested requests are complete");
            }
            this.status = Objects.requireNonNull(status, "status");
            completed = true;
        }

        @Override
        public boolean completed() {
            return completed;
        }

        private void ensureOpen() {
            if (completed) throw new IllegalStateException("validation is already complete");
        }

        @Override
        public AtfReportSemanticSnapshot.ValidationSnapshot snapshot() {
            return new AtfReportSemanticSnapshot.ValidationSnapshot(title, assertionNames,
                    uiActionCount, screenshotCount, status,
                    requests.stream().map(RequestState::snapshot).toList(), evidenceOwner);
        }
    }

    private static int sizeOf(Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    private static String normalizeLabel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
