package atf.reporting.semantic;
import atf.reporting.contract.evidence.ReportEvidenceOwner;
import atf.reporting.model.ReportExecutionModel;
import atf.reporting.contract.AtfReportStatus;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable semantic population shared by every ATF report renderer.
 *
 * <p>The snapshot contains report facts only. It deliberately has no HTML,
 * CSS, Extent or browser types, so a renderer can be compared with another
 * renderer without scraping its output.</p>
 */
public record AtfReportSemanticSnapshot(List<ClassSnapshot> classes) {
    public AtfReportSemanticSnapshot {
        classes = List.copyOf(Objects.requireNonNull(classes, "classes"));
    }

    public int scenarioCount() {
        return classes.stream().mapToInt(value -> value.scenarios().size()).sum();
    }

    public int phaseCount() {
        return classes.stream().flatMap(value -> value.scenarios().stream())
                .mapToInt(value -> value.phases().size()).sum();
    }

    public int requestCount() {
        return classes.stream().flatMap(value -> value.scenarios().stream())
                .flatMap(value -> value.phases().stream())
                .mapToInt(value -> (int) value.nodes().stream()
                        .filter(node -> node instanceof RequestSnapshot).count()).sum();
    }

    public int validationCount() {
        return classes.stream().flatMap(value -> value.scenarios().stream())
                .flatMap(value -> value.phases().stream())
                .flatMap(value -> value.nodes().stream())
                .filter(node -> node instanceof RequestSnapshot)
                .mapToInt(node -> ((RequestSnapshot) node).validations().size()).sum();
    }

    /** Projects the fallback execution tree into the common semantic contract. */
    public static AtfReportSemanticSnapshot from(ReportExecutionModel model) {
        Objects.requireNonNull(model, "model");
        Map<String, ClassAccumulator> classes = new LinkedHashMap<>();
        for (var scenario : model.scenarios()) {
            String className = scenario.testClass().isBlank() ? "API tests" : scenario.testClass();
            classes.computeIfAbsent(className,
                    key -> new ClassAccumulator(key, scenario.packageName()))
                    .add(scenarioSnapshot(scenario));
        }
        return new AtfReportSemanticSnapshot(classes.values().stream()
                .map(ClassAccumulator::snapshot).toList());
    }

    private static ScenarioSnapshot scenarioSnapshot(ReportExecutionModel.Scenario scenario) {
        return new ScenarioSnapshot(scenario.id(), scenario.scenario(), scenario.tags(),
                scenario.phases().stream().map(AtfReportSemanticSnapshot::phaseSnapshot).toList(),
                scenario.status());
    }

    private static PhaseSnapshot phaseSnapshot(ReportExecutionModel.Phase phase) {
        return new PhaseSnapshot(phase.name(),
                phase.children().stream().map(AtfReportSemanticSnapshot::nodeSnapshot).toList(),
                phase.details().stream().map(field -> field.name()).toList(), phase.status(), phase.timing());
    }

    private static NodeSnapshot nodeSnapshot(ReportExecutionModel.Node node) {
        if (node instanceof ReportExecutionModel.AssertionStep step) {
            return new AssertionStepSnapshot(step.title(),
                    step.assertions().stream().map(assertion -> assertion.name()).toList(), step.status());
        }
        if (node instanceof ReportExecutionModel.Request request) {
            return new RequestSnapshot(request.title(), request.method(), request.path(),
                    requestDetailNames(request),
                    request.json().stream().map(body -> body.label()).toList(),
                    request.execution() != null, request.uiActionCount(), request.screenshotCount(),
                    validationSnapshots(request.children()), request.status());
        }
        var validation = (ReportExecutionModel.Validation) node;
        return new ValidationSnapshot(validation.title(),
                validation.assertions().stream().map(assertion -> assertion.name()).toList(),
                validation.uiActionCount(), validation.screenshotCount(), validation.status(),
                validation.children().stream().filter(ReportExecutionModel.Request.class::isInstance)
                        .map(AtfReportSemanticSnapshot::nodeSnapshot)
                        .map(RequestSnapshot.class::cast).toList(),
                validation.evidenceOwner());
    }

    private static List<ValidationSnapshot> validationSnapshots(List<ReportExecutionModel.Node> nodes) {
        var result = new ArrayList<ValidationSnapshot>();
        for (var node : nodes) {
            if (node instanceof ReportExecutionModel.Validation validation) {
                result.add((ValidationSnapshot) nodeSnapshot(validation));
            } else if (node instanceof ReportExecutionModel.Request request) {
                result.addAll(validationSnapshots(request.children()));
            }
        }
        return List.copyOf(result);
    }

    private static List<String> requestDetailNames(ReportExecutionModel.Request request) {
        var names = new ArrayList<>(request.details().stream().map(field -> field.name()).toList());
        if (request.networkAggregate() != null) names.add("network.aggregate");
        return List.copyOf(names);
    }

    private static final class ClassAccumulator {
        private final String name;
        private final String packageName;
        private final List<ScenarioSnapshot> scenarios = new ArrayList<>();
        private AtfReportStatus status = AtfReportStatus.INFO;

        private ClassAccumulator(String name, String packageName) {
            this.name = name;
            this.packageName = packageName == null ? "" : packageName;
        }

        private void add(ScenarioSnapshot scenario) {
            scenarios.add(scenario);
            status = AtfReportStatus.worst(status, scenario.status());
        }

        private ClassSnapshot snapshot() {
            return new ClassSnapshot(name, packageName, scenarios, status);
        }
    }

    public record ClassSnapshot(String name, String packageName,
                                List<ScenarioSnapshot> scenarios,
                                AtfReportStatus status) {
        public ClassSnapshot {
            name = required(name, "name");
            packageName = packageName == null ? "" : packageName;
            scenarios = List.copyOf(Objects.requireNonNull(scenarios, "scenarios"));
            status = Objects.requireNonNull(status, "status");
        }
    }

    public record ScenarioSnapshot(String id, String name, List<String> tags,
                                   List<PhaseSnapshot> phases, AtfReportStatus status) {
        public ScenarioSnapshot {
            id = required(id, "id");
            name = name == null ? "" : name;
            tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
            phases = List.copyOf(Objects.requireNonNull(phases, "phases"));
            status = Objects.requireNonNull(status, "status");
        }
    }

    public record PhaseSnapshot(String name, List<NodeSnapshot> nodes,
                                List<String> detailNames, AtfReportStatus status,
                                java.util.Optional<atf.reporting.model.timing.ReportPhaseTiming> timing) {
        public PhaseSnapshot(String name, List<NodeSnapshot> nodes, List<String> detailNames, AtfReportStatus status) {
            this(name, nodes, detailNames, status, java.util.Optional.empty());
        }
        public PhaseSnapshot {
            timing = Objects.requireNonNull(timing, "timing");
            name = name == null ? "" : name;
            nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
            detailNames = List.copyOf(Objects.requireNonNull(detailNames, "detailNames"));
            status = Objects.requireNonNull(status, "status");
        }
    }

    public sealed interface NodeSnapshot permits RequestSnapshot, ValidationSnapshot, AssertionStepSnapshot {
        String title();
        AtfReportStatus status();
    }

    public record AssertionStepSnapshot(String title, List<String> assertionNames,
                                        AtfReportStatus status) implements NodeSnapshot {
        public AssertionStepSnapshot {
            title = required(title, "title");
            assertionNames = List.copyOf(Objects.requireNonNull(assertionNames, "assertionNames"));
            status = Objects.requireNonNull(status, "status");
        }
    }

    public record RequestSnapshot(String title, String method, String path,
                                  List<String> detailNames, List<String> jsonLabels,
                                  boolean executionPresent, int uiActionCount,
                                  int screenshotCount, List<ValidationSnapshot> validations,
                                  AtfReportStatus status) implements NodeSnapshot {
        public RequestSnapshot {
            title = title == null ? "" : title;
            method = method == null ? "" : method;
            path = path == null ? "" : path;
            detailNames = List.copyOf(Objects.requireNonNull(detailNames, "detailNames"));
            jsonLabels = List.copyOf(Objects.requireNonNull(jsonLabels, "jsonLabels"));
            if (uiActionCount < 0 || screenshotCount < 0) {
                throw new IllegalArgumentException("Evidence counts must be non-negative");
            }
            validations = List.copyOf(Objects.requireNonNull(validations, "validations"));
            status = Objects.requireNonNull(status, "status");
        }
    }

    public record ValidationSnapshot(String title, List<String> assertionNames,
                                     int uiActionCount, int screenshotCount,
                                     AtfReportStatus status,
                                     List<RequestSnapshot> requests, ReportEvidenceOwner evidenceOwner) implements NodeSnapshot {
        public ValidationSnapshot(String title, List<String> assertionNames,
                                  int uiActionCount, int screenshotCount,
                                  AtfReportStatus status, List<RequestSnapshot> requests) {
            this(title, assertionNames, uiActionCount, screenshotCount, status, requests,
                    ReportEvidenceOwner.UNSPECIFIED);
        }
        public ValidationSnapshot(String title, List<String> assertionNames,
                                  int uiActionCount, int screenshotCount,
                                  AtfReportStatus status) {
            this(title, assertionNames, uiActionCount, screenshotCount, status, List.of());
        }

        public ValidationSnapshot {
            evidenceOwner = Objects.requireNonNull(evidenceOwner, "evidenceOwner");
            title = title == null ? "" : title;
            assertionNames = List.copyOf(Objects.requireNonNull(assertionNames, "assertionNames"));
            if (uiActionCount < 0 || screenshotCount < 0) {
                throw new IllegalArgumentException("Evidence counts must be non-negative");
            }
            status = Objects.requireNonNull(status, "status");
            requests = List.copyOf(Objects.requireNonNull(requests, "requests"));
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
