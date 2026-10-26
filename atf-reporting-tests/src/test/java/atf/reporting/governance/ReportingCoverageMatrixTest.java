package atf.reporting.governance;

import atf.testkit.AtfComponentTest;
import atf.testkit.AtfE2eTest;
import atf.testkit.AtfIntegrationTest;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.ParameterizedTest;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the reporting seam-to-test-level matrix executable and reviewable. */
@AtfComponentTest
class ReportingCoverageMatrixTest {
    private static final Entry NETWORK_INSPECTOR_PROJECTION = entry(
            "Network Inspector projection and aggregate markup",
            "atf.reporting.ui.network.ReportNetworkInspectorTest",
            AtfUnitTest.class);
    private static final Entry NETWORK_INSPECTOR_RENDERING = entry(
            "Network Inspector rendered summary role",
            "atf.reporting.ui.network.NetworkInspectorSummaryRoleTest",
            AtfComponentTest.class);
    private static final Entry NETWORK_INSPECTOR_PUBLICATION = entry(
            "Network Inspector aggregate publication",
            "atf.reporting.ui.ExtentReportUiEvidencePublishingTest",
            AtfUnitTest.class);

    private static final List<Entry> MATRIX = List.of(
            entry("execution projections", "atf.reporting.model.ReportExecutionProjectionTest",
                    AtfUnitTest.class),
            entry("fallback view shell", "atf.reporting.html.HtmlReportViewsTest",
                    AtfUnitTest.class),
            entry("fallback/Extent renderer parity",
                    "atf.reporting.html.ReportRendererParityTest",
                    AtfComponentTest.class),
            entry("typed environment snapshot", "atf.reporting.ui.UiExtentReportSessionCompositionTest",
                    AtfUnitTest.class),
            entry("operational System projection",
                    "atf.reporting.configuration.ReportEnvironmentSnapshotTest",
                    AtfUnitTest.class),
            entry("safe manual replay", "atf.reporting.api.ApiManualReplayProjectionTest",
                    AtfUnitTest.class),
            entry("legacy/core model compatibility boundary",
                    "atf.reporting.api.ReportModelAdaptersTest", AtfUnitTest.class),
            entry("semantic lifecycle boundary",
                    "atf.reporting.semantic.AtfReportSemanticSnapshotTest",
                    AtfUnitTest.class),
            entry("semantic snapshot", "atf.reporting.html.HtmlReportSemanticSnapshotMapperTest",
                    AtfUnitTest.class),
            entry("evidence markup", "atf.reporting.evidence.markup.ReportEvidenceMarkupTest",
                    AtfUnitTest.class),
            entry("asset publication", "atf.reporting.asset.ReportAssetContractTest",
                    AtfUnitTest.class),
            entry("asset size budget", "atf.reporting.asset.ReportAssetSizeBudgetTest",
                    AtfUnitTest.class),
            entry("typed screenshot capture provider",
                    "atf.reporting.evidence.profile.ReportEvidenceProfileTest",
                    AtfUnitTest.class),
            entry("capture provider publication",
                    "atf.reporting.ui.profile.ReportEvidenceProfileIntegrationTest",
                    AtfIntegrationTest.class),
            entry("runtime performance snapshot and budget",
                    "atf.reporting.runtime.performance.ReportRuntimePerformanceComponentTest",
                    AtfComponentTest.class),
            entry("UI component artifact", "atf.reporting.ui.contract.UiReportArtifactContractTest",
                    AtfUnitTest.class),
            entry("section component seam",
                    "atf.ui.tests.extent.component.audit.OverviewSecondaryContractValidatorTest",
                    AtfComponentTest.class),
            entry("configurable exact API selection projection",
                    "atf.ui.tests.extent.component.plan.ApiScenarioManifestTest",
                    AtfComponentTest.class),
            entry("Overview audit rules",
                    "atf.reporting.ui.audit.section.OverviewSectionValidatorTest",
                    AtfUnitTest.class),
            entry("Categories and request evidence rules",
                    "atf.reporting.ui.audit.section.CategoriesAndEvidenceSectionValidatorTest",
                    AtfUnitTest.class),
            entry("Fails projection rules",
                    "atf.reporting.ui.audit.section.FailsSectionValidatorTest",
                    AtfUnitTest.class),
            entry("Screen validation rules",
                    "atf.reporting.ui.audit.section.ScreenValidationSectionValidatorTest",
                    AtfUnitTest.class),
            entry("JSON and visual evidence rules",
                    "atf.ui.tests.extent.component.section.architecture.ExtractionValidationConvergenceTest",
                    AtfComponentTest.class),
            entry("Report projection rules",
                    "atf.reporting.ui.audit.section.ReportProjectionValidatorTest",
                    AtfUnitTest.class),
            entry("mock API integration", "atf.reporting.api.contract.MockApiReportIntegrationTest",
                    AtfIntegrationTest.class),
            entry("API report contract and negative drift probes",
                    "atf.reporting.api.contract.ApiReportContractAuditorTest",
                    AtfComponentTest.class),
            entry("same API exchange across direct, fallback and UI publishers",
                    "atf.ui.tests.extent.component.inspection.ApiExchangeParityComponentTest",
                    AtfComponentTest.class),
            entry("rendered API ownership, query and phase reconciliation",
                    "atf.ui.tests.extent.component.audit.ApiReportRenderedEvidenceComponentTest",
                    AtfComponentTest.class),
            entry("mixed UI/API verdict ownership and Fails replay",
                    "atf.ui.tests.extent.component.audit.UiApiVerdictProjectionComponentTest",
                    AtfComponentTest.class),
            entry("API table viewport and computed overflow behavior",
                    "atf.ui.tests.extent.component.inspection.ReportApiViewportParityComponentTest",
                    AtfComponentTest.class),
            entry("evidence disclosure keyboard and ARIA behavior",
                    "atf.ui.tests.extent.component.inspection.EvidenceDisclosureAriaComponentTest",
                    AtfComponentTest.class),
            NETWORK_INSPECTOR_PROJECTION,
            NETWORK_INSPECTOR_RENDERING,
            NETWORK_INSPECTOR_PUBLICATION,
            entry("static UI browser proof",
                    "atf.ui.tests.extent.e2e.uireport.StaticUiExtentReportContractTest",
                    AtfE2eTest.class));

    @Test
    void everyPublishedBoundaryHasAnExplicitLowerLevelProof() {
        for (Entry entry : MATRIX) {
            Class<?> testClass = load(entry.className());
            assertTrue(hasExecutableTestMethod(testClass),
                    () -> entry.seam() + " must retain an executable JUnit test method: "
                            + entry.className());
            assertTrue(entry.annotation().isAssignableFrom(annotationType(testClass)),
                    () -> entry.seam() + " must be marked with " + entry.annotation().getSimpleName()
                            + ": " + entry.className());
        }
    }

    @Test
    void networkInspectorProofsCannotBeRemovedFromTheCoverageMatrix() {
        assertTrue(MATRIX.contains(NETWORK_INSPECTOR_PROJECTION),
                "Network Inspector projection/markup needs its unit proof in the matrix");
        assertTrue(MATRIX.contains(NETWORK_INSPECTOR_RENDERING),
                "Network Inspector summary rendering needs its component proof in the matrix");
        assertTrue(MATRIX.contains(NETWORK_INSPECTOR_PUBLICATION),
                "Network Inspector aggregate publication must remain a separate proof");
    }

    @Test
    void eachExecutableLayerHasAStableLauncherBoundary() {
        assertEquals("atf.reporting",
                AtfReportingUnitSuite.class.getAnnotation(org.junit.platform.suite.api.SelectPackages.class)
                        .value()[0]);
        assertEquals("atf.reporting",
                AtfReportingComponentSuite.class.getAnnotation(org.junit.platform.suite.api.SelectPackages.class)
                        .value()[0]);
        assertEquals("atf.reporting",
                AtfReportingIntegrationSuite.class.getAnnotation(org.junit.platform.suite.api.SelectPackages.class)
                        .value()[0]);
        assertEquals("atf.reporting.api.tests.api",
                AtfReportingApiFixtureSuite.class.getAnnotation(org.junit.platform.suite.api.SelectPackages.class)
                        .value()[0]);
        assertEquals("atf.ui.tests.extent.e2e",
                AtfReportingUiAuditSuite.class.getAnnotation(org.junit.platform.suite.api.SelectPackages.class)
                        .value()[0]);
    }

    @Test
    void commonApiGateIsSelectedByTheReportingComponentSuite() {
        var suite = AtfReportingComponentSuite.class;
        var packages = suite.getAnnotation(org.junit.platform.suite.api.SelectPackages.class).value();
        var tags = suite.getAnnotation(org.junit.platform.suite.api.IncludeTags.class).value();
        var excludedSuites = suite.getAnnotation(org.junit.platform.suite.api.ExcludeClassNamePatterns.class).value();

        assertEquals(List.of("atf.reporting", "atf.ui.tests.extent.component"), List.of(packages),
                "the common gate's direct and browser proofs must both be selected");
        assertEquals(List.of("component"), List.of(tags),
                "the suite must not silently filter component proofs out");
        assertTrue(Arrays.asList(excludedSuites).contains(".*Suite"),
                "nested suite classes must not be recursively selected");

        List<Entry> apiGate = MATRIX.stream().filter(entry -> entry.annotation() == AtfComponentTest.class
                && (entry.className().equals("atf.reporting.api.contract.ApiReportContractAuditorTest")
                || entry.className().equals("atf.ui.tests.extent.component.inspection.ApiExchangeParityComponentTest")
                || entry.className().equals("atf.ui.tests.extent.component.audit.ApiReportRenderedEvidenceComponentTest")
                || entry.className().equals("atf.ui.tests.extent.component.inspection.ReportApiViewportParityComponentTest")
                || entry.className().equals("atf.ui.tests.extent.component.inspection.EvidenceDisclosureAriaComponentTest")))
                .toList();
        assertEquals(5, apiGate.size(), "all common API gate proofs must remain explicit in the matrix");
        for (Entry entry : apiGate) {
            String packageName = entry.className().substring(0, entry.className().lastIndexOf('.'));
            assertTrue(Arrays.stream(packages).anyMatch(packageName::startsWith),
                    () -> entry.className() + " is outside the selected component packages");
            assertTrue(entry.annotation().isAssignableFrom(annotationType(load(entry.className()))),
                    () -> entry.className() + " must retain its component tag");
        }
    }

    @Test
    void matrixHasOneReasonAndOneDeclaredProofPerBoundary() {
        assertEquals(MATRIX.size(), MATRIX.stream().map(Entry::seam).distinct().count());
        assertEquals(MATRIX.size(), MATRIX.stream().map(Entry::className).distinct().count());
        assertTrue(MATRIX.stream().allMatch(entry -> !entry.seam().isBlank()
                        && !entry.className().isBlank()),
                "every matrix row needs a readable reason and proof class");
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, ReportingCoverageMatrixTest.class.getClassLoader());
        } catch (ClassNotFoundException error) {
            throw new AssertionError("Missing coverage proof: " + name, error);
        }
    }

    private static Class<? extends Annotation> annotationType(Class<?> testClass) {
        if (testClass.isAnnotationPresent(AtfUnitTest.class)) return AtfUnitTest.class;
        if (testClass.isAnnotationPresent(AtfComponentTest.class)) return AtfComponentTest.class;
        if (testClass.isAnnotationPresent(AtfIntegrationTest.class)) return AtfIntegrationTest.class;
        if (testClass.isAnnotationPresent(AtfE2eTest.class)) return AtfE2eTest.class;
        throw new AssertionError("Coverage proof has no ATF layer annotation: " + testClass.getName());
    }

    private static boolean hasExecutableTestMethod(Class<?> testClass) {
        return Arrays.stream(testClass.getDeclaredMethods()).anyMatch(method ->
                method.isAnnotationPresent(Test.class)
                        || method.isAnnotationPresent(TestFactory.class)
                        || method.isAnnotationPresent(TestTemplate.class)
                        || method.isAnnotationPresent(RepeatedTest.class)
                        || method.isAnnotationPresent(ParameterizedTest.class));
    }

    private static Entry entry(String seam, String className,
                               Class<? extends Annotation> annotation) {
        return new Entry(seam, className, annotation);
    }

    private record Entry(String seam, String className,
                         Class<? extends Annotation> annotation) {
    }
}
