package atf.api.tests.governance;

import atf.core.config.module.AtfModuleIds;
import atf.core.files.AtfPathLocator;
import atf.testkit.AtfComponentTest;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static atf.api.tests.governance.AtfGovernanceTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards package size and keeps cohesion exceptions explicit. */
@AtfComponentTest
public class AtfPackageCohesionGovernanceTest {
    @Test
    void productionPackageCohesionUsesAnExplicitReviewedExceptionList() throws Exception {
        Path root = locateProjectRoot();
        Map<String, Integer> counts = productionPackageCounts(root);
        Map<String, List<String>> inventory = productionPackageClassInventory(root);
        Map<String, Integer> overLimit = new TreeMap<>();
        counts.forEach((packageName, count) -> {
            if (count > PACKAGE_COHESION_LIMIT) overLimit.put(packageName, count);
        });
        Map<String, Integer> unreviewed = withoutReviewedEntries(overLimit, REVIEWED_COHESION_EXCEPTIONS);

        assertTrue(!counts.isEmpty(), "Production package inventory must not be empty");
        assertTrue(unreviewed.isEmpty(),
                () -> "New package cohesion hotspot requires an explicit review: "
                        + describe(unreviewed, inventory));
        assertTrue(REVIEWED_COHESION_EXCEPTIONS.keySet().stream()
                        .filter(counts::containsKey)
                        .allMatch(overLimit::containsKey),
                () -> "Reviewed package allow-list is stale: " + overLimit);
    }

    @Test
    void automationExerciseProductionPackagesAreInTheGovernedInventory() throws Exception {
        assertTrue(AtfModuleIds.SOURCE_GOVERNED_MODULES.contains(AtfModuleIds.AUTOMATION_EXERCISE),
                "AutoEx has production sources and must participate in whole-ATF package governance");
        assertFalse(AtfModuleIds.TEST_ONLY_COMPOSITION_MODULES.contains(AtfModuleIds.AUTOMATION_EXERCISE),
                "AutoEx is not a test-only composition module");

        Map<String, List<String>> inventory = productionPackageClassInventory(locateProjectRoot());
        assertEquals(Set.of("AutoExCatalogApiPrecondition.java", "AutoExPurchaseJourney.java",
                        "AutoExAccountJourney.java", "AutoExAccountJourneyPort.java",
                        "AutoExCatalogSearchPort.java", "AutoExPurchaseSession.java",
                        "AutoExPurchaseTraceStep.java"),
                Set.copyOf(inventory.getOrDefault("atf.autoex.support.journey", List.of())));
        assertEquals(Set.of("AutoExCartJourney.java", "AutoExCartJourneyPort.java"),
                Set.copyOf(inventory.getOrDefault("atf.autoex.support.cart", List.of())));
        assertEquals(Set.of("AutoExAccountCleanupAction.java", "AutoExAccountCleanupDecision.java",
                        "AutoExAccountDeletionAttempt.java", "AutoExAccountDeletionProgress.java",
                        "AutoExAccountLifecycle.java",
                        "AutoExTestAccount.java"),
                Set.copyOf(inventory.getOrDefault("atf.autoex.support.account", List.of())));
        assertEquals(Set.of("AutoExCatalogAssertions.java", "AutoExCatalogSnapshot.java"),
                Set.copyOf(inventory.getOrDefault("atf.autoex.support.catalog", List.of())));
        assertEquals(Set.of("AutoExPrivacyConsentHandler.java", "AutoExVignetteHandler.java"),
                Set.copyOf(inventory.getOrDefault("atf.autoex.support.browser", List.of())));
    }

    @Test
    void aiValidationUsesSemanticSubpackagesWithoutAnAllowListException() throws Exception {
        Map<String, List<String>> inventory = productionPackageClassInventory(locateProjectRoot());

        assertEquals(Set.of(
                        "AiEvaluationCase.java",
                        "AiEvaluationEvaluator.java",
                        "AiEvaluationPolicy.java",
                        "AiEvaluationSnapshot.java",
                        "AiEvaluationValues.java",
                        "AiFieldComparison.java",
                        "AiFieldExpectation.java"),
                Set.copyOf(inventory.getOrDefault("atf.validation.ai", List.of())));
        assertEquals(Set.of("AiGroundingObservation.java", "AiGroundingRequirement.java",
                        "AiSourceReference.java"),
                Set.copyOf(inventory.getOrDefault("atf.validation.ai.grounding", List.of())));
        assertEquals(List.of("AiInvocationEvidence.java"),
                inventory.getOrDefault("atf.validation.ai.invocation", List.of()));
        assertFalse(inventory.containsKey("atf.validation.ai.support"),
                "internal AI defensive-copy helpers must not become a public support package");
    }

    @Test
    void reportingTestPackagesKeepCohesionBoundaries() throws Exception {
        Path tests = AtfPathLocator.resolveModuleSourceRoot(
                locateProjectRoot(), AtfModuleIds.REPORTING_TESTS, true);
        Map<String, Integer> counts = testPackageCounts(tests);
        Map<String, List<String>> inventory = packageClassInventory(tests);
        Map<String, Integer> overLimit = new TreeMap<>();
        counts.forEach((packageName, count) -> {
            if (count > PACKAGE_COHESION_LIMIT) overLimit.put(packageName, count);
        });

        assertTrue(overLimit.isEmpty(), () ->
                "Reporting-test package exceeds the eight-class cohesion limit: "
                        + describe(overLimit, inventory));
        assertFalse(counts.containsKey("atf.ui.tests.extent.support.section"),
                "section root must not become a mixed test package again");
        assertTrue(counts.getOrDefault("atf.ui.tests.extent.e2e.section", 0) <= 6);
        assertTrue(counts.getOrDefault("atf.ui.tests.extent.component.section.contract", 0) <= 6);
        assertTrue(counts.getOrDefault("atf.ui.tests.extent.support.section.validation", 0) <= 6);
        assertTrue(counts.getOrDefault("atf.ui.tests.extent.component.section.architecture", 0) <= 6);
    }

    @Test
    void everyReactorModuleTestSourceSetRespectsTheEightClassCohesionLimitUnlessReviewed()
            throws Exception {
        Path root = locateProjectRoot();
        Map<String, Integer> overLimit = new TreeMap<>();
        Map<String, List<String>> inventory = new TreeMap<>();
        for (String module : AtfModuleIds.REACTOR_MODULES) {
            Path sourceRoot = AtfPathLocator.resolveModuleSourceRoot(root, module, true);
            testPackageCounts(sourceRoot).forEach((packageName, count) -> {
                String modulePackage = module + ":" + packageName;
                if (count > PACKAGE_COHESION_LIMIT) overLimit.put(modulePackage, count);
            });
            packageClassInventory(sourceRoot).forEach((packageName, classes) ->
                    inventory.put(module + ":" + packageName, classes));
        }
        Map<String, Integer> unreviewed = withoutReviewedEntries(overLimit, REVIEWED_TEST_COHESION_EXCEPTIONS);
        assertTrue(unreviewed.isEmpty(),
                () -> "New test-package cohesion hotspot requires an explicit review: "
                        + describe(unreviewed, inventory));
        assertTrue(REVIEWED_TEST_COHESION_EXCEPTIONS.keySet().stream()
                        .allMatch(overLimit::containsKey),
                () -> "Reviewed test-package allow-list is stale: " + overLimit);
    }

    private static Map<String, Integer> withoutReviewedEntries(Map<String, Integer> counts,
                                                                 Map<String, String> reviewed) {
        Map<String, Integer> unreviewed = new TreeMap<>(counts);
        unreviewed.keySet().removeAll(reviewed.keySet());
        return unreviewed;
    }

    private static String describe(Map<String, Integer> counts,
                                   Map<String, List<String>> inventory) {
        return counts.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()
                        + " [" + String.join(", ", inventory.getOrDefault(entry.getKey(), List.of())) + "]")
                .toList()
                .toString();
    }
}
