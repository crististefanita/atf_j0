package atf.core.lifecycle;
import atf.core.reporting.contract.AtfReportPorts;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Immutable description of one scenario and its optional lifecycle phases. */
public record ApiScenarioPlan(
        String testClass,
        String testPackage,
        String scenario,
        List<String> tags,
        PhaseAction setup,
        PhaseAction body,
        PhaseAction teardown) {

    public ApiScenarioPlan {
        testClass = requireText(testClass, "testClass");
        scenario = requireText(scenario, "scenario");
        tags = tags == null ? List.of() : List.copyOf(tags);
        if (body == null) throw new IllegalArgumentException("body must not be null");
    }

    public static Builder scenario(String testClass, String scenario) {
        return new Builder(testClass, null, scenario);
    }

    public static Builder scenario(String testClass, String testPackage, String scenario) {
        return new Builder(testClass, testPackage, scenario);
    }

    @FunctionalInterface
    public interface PhaseAction {
        void execute(AtfReportPorts.Phase phase) throws Exception;
    }

    public static final class Builder {
        private final String testClass;
        private String testPackage;
        private final String scenario;
        private List<String> tags = List.of();
        private PhaseAction setup;
        private PhaseAction body;
        private PhaseAction teardown;

        private Builder(String testClass, String testPackage, String scenario) {
            this.testClass = testClass;
            this.testPackage = testPackage;
            this.scenario = scenario;
        }

        public Builder testPackage(String testPackage) {
            this.testPackage = testPackage;
            return this;
        }

        public Builder tags(Collection<String> tags) {
            this.tags = tags == null ? List.of() : List.copyOf(tags);
            return this;
        }

        public Builder setup(PhaseAction action) {
            this.setup = Objects.requireNonNull(action, "setup");
            return this;
        }

        public Builder body(PhaseAction action) {
            this.body = Objects.requireNonNull(action, "body");
            return this;
        }

        public Builder teardown(PhaseAction action) {
            this.teardown = Objects.requireNonNull(action, "teardown");
            return this;
        }

        public ApiScenarioPlan build() {
            return new ApiScenarioPlan(testClass, testPackage, scenario, tags, setup, body, teardown);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
}
