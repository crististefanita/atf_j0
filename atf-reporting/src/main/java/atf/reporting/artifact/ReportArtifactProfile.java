package atf.reporting.artifact;

import atf.reporting.contract.AtfReportStatus;
import java.util.List;
import java.util.Objects;

/** Caller-authored inventory, independent of the delivered HTML and of a business transport. */
public record ReportArtifactProfile(Renderer renderer, List<Scenario> scenarios) {
    public enum Renderer { EXTENT, FALLBACK }

    public ReportArtifactProfile {
        Objects.requireNonNull(renderer, "renderer");
        scenarios = nonempty(scenarios, "scenarios");
    }

    public record Scenario(String testClass, String name, AtfReportStatus status, List<Phase> phases) {
        public Scenario {
            testClass = required(testClass, "testClass"); name = required(name, "scenario name");
            Objects.requireNonNull(status, "status"); phases = nonempty(phases, "phases");
            for (var phase : phases) requireParent(status, phase.status());
        }
    }

    public record Phase(String name, AtfReportStatus status, List<Evidence> evidence) {
        public Phase {
            name = required(name, "phase name"); Objects.requireNonNull(status, "status");
            evidence = List.copyOf(evidence);
            for (var node : evidence) {
                if (node instanceof Assertions assertions) requireParent(status, assertions.status());
                else if (node instanceof HttpExchange exchange) requireParent(status, exchange.status());
            }
        }
    }

    public sealed interface Evidence permits Assertions, HttpExchange, Observation { String title(); }

    /** Original comparison names and independently evaluated verdicts; no secret values enter the profile. */
    public record Comparison(String name, AtfReportStatus status) {
        public Comparison(String name, boolean passed) { this(name, passed ? AtfReportStatus.PASS : AtfReportStatus.FAIL); }
        public Comparison { name = required(name, "comparison name"); Objects.requireNonNull(status, "status"); }
    }

    public record Assertions(String title, List<Comparison> comparisons, AtfReportStatus status) implements Evidence {
        public Assertions(String title, List<Comparison> comparisons) { this(title, comparisons, worst(comparisons)); }
        public Assertions {
            title = required(title, "assertion title"); comparisons = nonempty(comparisons, "comparisons");
            Objects.requireNonNull(status, "status"); for (var row : comparisons) requireParent(status, row.status());
        }
    }

    /** Presence is an explicit requirement; absent HTTP never selects a more permissive profile. */
    public record HttpExchange(String title, boolean requestRequired, boolean responseRequired,
                               List<String> bodyLabels, List<Assertions> validations, AtfReportStatus status) implements Evidence {
        public HttpExchange {
            title = required(title, "exchange title"); bodyLabels = names(bodyLabels); validations = List.copyOf(validations);
            Objects.requireNonNull(status, "status"); for (var validation : validations) requireParent(status, validation.status());
        }
    }

    public record Observation(String title, List<String> fieldNames, List<String> bodyLabels) implements Evidence {
        public Observation { title = required(title, "observation title"); fieldNames = names(fieldNames); bodyLabels = names(bodyLabels); }
    }

    private static <T> List<T> nonempty(List<T> input, String name) {
        var copy = List.copyOf(input);
        if (copy.isEmpty()) throw new IllegalArgumentException(name + " must not be empty");
        return copy;
    }
    private static List<String> names(List<String> input) { return input.stream().map(value -> required(value, "evidence name")).toList(); }
    private static AtfReportStatus worst(List<Comparison> rows) {
        return rows.stream().map(Comparison::status).reduce(AtfReportStatus.INFO, AtfReportStatus::worst);
    }
    private static void requireParent(AtfReportStatus parent, AtfReportStatus child) {
        if (parent.severity() < child.severity()) throw new IllegalArgumentException("Parent verdict cannot hide a more severe required outcome");
    }
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
