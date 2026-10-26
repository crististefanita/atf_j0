package atf.api.tests.governance;

import atf.core.config.module.AtfModuleIds;

import java.util.List;

/** Test-only policy catalog for whole-ATF source and Maven boundaries. */
final class AtfGovernanceRules {
    static final List<SourceRule> PRODUCTION_SOURCE_RULES = List.of(
            source(AtfModuleIds.CORE, "com.aventstack", "org.junit"),
            source(AtfModuleIds.MOCK, "com.aventstack", "org.junit",
                    "atf.validation", "atf.reporting"),
            source(AtfModuleIds.VALIDATION, "com.aventstack", "org.junit", "<style", "<script",
                    "atf.reporting"),
            source(AtfModuleIds.UI, "atf.validation", "atf.reporting",
                    "atf.mock", "org.junit", "<style", "<script"),
            source(AtfModuleIds.DEBUG_UI, "atf.validation", "atf.reporting",
                    "atf.mock", "org.junit", "<style", "<script"),
            source(AtfModuleIds.AUTOMATION_EXERCISE, "org.junit", "atf.testkit"),
            source(AtfModuleIds.REPORTING, "atf.core",
                    "atf.validation", "atf.console", "org.junit"));

    static final List<DependencyRule> MODULE_DEPENDENCY_RULES = List.of(
            dependency(AtfModuleIds.CORE, AtfModuleIds.VALIDATION, false),
            dependency(AtfModuleIds.CORE, AtfModuleIds.REPORTING, false),
            dependency(AtfModuleIds.MOCK, AtfModuleIds.REPORTING, false),
            dependency(AtfModuleIds.MOCK, AtfModuleIds.CORE, true),
            dependency(AtfModuleIds.REPORTING, AtfModuleIds.CONSOLE, false),
            dependency(AtfModuleIds.VALIDATION, AtfModuleIds.REPORTING, false),
            dependency(AtfModuleIds.VALIDATION, AtfModuleIds.CORE, true),
            dependency(AtfModuleIds.CONSOLE, AtfModuleIds.CORE, true),
            dependency(AtfModuleIds.REPORTING, AtfModuleIds.CORE, false),
            dependency(AtfModuleIds.REPORTING, AtfModuleIds.VALIDATION, false),
            dependency(AtfModuleIds.UI, AtfModuleIds.CORE, true),
            dependency(AtfModuleIds.TESTS, AtfModuleIds.REPORTING, true),
            dependency(AtfModuleIds.TESTS, AtfModuleIds.UI, true));

    /** Every ATF module dependency declared by reporting must remain test-only. */
    static final List<String> REPORTING_FORBIDDEN_PRODUCTION_PACKAGES = List.of(
            "atf.core", "atf.validation", "atf.mock",
            "atf.console", "atf.ui", "atf.debugui", "atf.api.tests",
            "atf.testkit", "org.junit");

    private AtfGovernanceRules() {
    }

    private static SourceRule source(String module, String... forbidden) {
        return new SourceRule(module, List.of(forbidden));
    }

    private static DependencyRule dependency(String module, String dependency, boolean required) {
        return new DependencyRule(module, dependency, required);
    }

    record SourceRule(String module, List<String> forbiddenTokens) {
    }

    record DependencyRule(String module, String dependency, boolean required) {
    }
}
