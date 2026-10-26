package atf.reporting.api.acceptance;

import atf.core.lifecycle.ApiScenarioPlan;

import java.util.Collection;
import java.util.List;

/** Test-scope DSL that keeps acceptance scenarios declarative. */
public final class ApiAcceptancePlanBuilder {
    private final String testClass;
    private final String testPackage;
    private final String scenario;
    private final List<String> tags;
    private ApiScenarioPlan.PhaseAction setup;
    private ApiScenarioPlan.PhaseAction body;
    private ApiScenarioPlan.PhaseAction teardown;

    private ApiAcceptancePlanBuilder(String testClass, String testPackage,
                                     String scenario, Collection<String> tags) {
        this.testClass = testClass;
        this.testPackage = testPackage;
        this.scenario = scenario;
        this.tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public static ApiAcceptancePlanBuilder scenario(String testClass, String scenario,
                                                     Collection<String> tags) {
        return new ApiAcceptancePlanBuilder(testClass, null, scenario, tags);
    }

    public static ApiAcceptancePlanBuilder scenario(String testClass, String testPackage,
                                                     String scenario, Collection<String> tags) {
        return new ApiAcceptancePlanBuilder(testClass, testPackage, scenario, tags);
    }

    public ApiAcceptancePlanBuilder setup(ApiScenarioPlan.PhaseAction action) {
        setup = action;
        return this;
    }

    public ApiAcceptancePlanBuilder body(ApiScenarioPlan.PhaseAction action) {
        body = action;
        return this;
    }

    public ApiAcceptancePlanBuilder teardown(ApiScenarioPlan.PhaseAction action) {
        teardown = action;
        return this;
    }

    public ApiScenarioPlan build() {
        var builder = ApiScenarioPlan.scenario(testClass, testPackage, scenario).tags(tags);
        if (setup != null) builder.setup(setup);
        if (body != null) builder.body(body);
        if (teardown != null) builder.teardown(teardown);
        return builder.build();
    }
}
