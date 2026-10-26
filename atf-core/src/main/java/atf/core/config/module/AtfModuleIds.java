package atf.core.config.module;

import java.util.List;

/** Canonical Maven module identifiers shared by ATF governance and tooling. */
public final class AtfModuleIds {
    public static final String TEST_KIT = "atf-test-kit";
    public static final String CORE = "atf-core";
    public static final String MOCK = "atf-mock";
    public static final String VALIDATION = "atf-validation";
    public static final String CONSOLE = "atf-console";
    public static final String REPORTING = "atf-reporting";
    public static final String UI = "atf-ui";
    public static final String DEBUG_UI = "atf-debug-ui";
    public static final String PRACTICE_SHOP = "atf-test-practice-shop";
    public static final String AUTOMATION_EXERCISE = "atf-test-auto-ex";
    public static final String TESTS = "atf-tests";
    public static final String REPORTING_TESTS = "atf-reporting-tests";

    /** Every Maven module in the ATF reactor, in the root POM order. */
    public static final List<String> REACTOR_MODULES = List.of(
            TEST_KIT, CORE, MOCK, VALIDATION, CONSOLE, REPORTING, UI, DEBUG_UI, PRACTICE_SHOP,
            AUTOMATION_EXERCISE, TESTS,
            REPORTING_TESTS);

    /** Composition roots without production source; their ATF dependencies stay test-only. */
    public static final List<String> TEST_ONLY_COMPOSITION_MODULES =
            List.of(TESTS, REPORTING_TESTS);

    /** Modules whose production/test source boundaries are governed by the whole-ATF guard. */
    public static final List<String> SOURCE_GOVERNED_MODULES = List.of(
            CORE, MOCK, VALIDATION, CONSOLE, REPORTING, UI, DEBUG_UI, PRACTICE_SHOP,
            AUTOMATION_EXERCISE, TESTS);

    private AtfModuleIds() {
    }
}
