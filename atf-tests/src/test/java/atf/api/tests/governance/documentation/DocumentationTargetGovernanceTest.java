package atf.api.tests.governance.documentation;

import atf.api.tests.governance.documentation.links.DocumentationLinkGovernanceTest;

import atf.testkit.documentation.DocumentationTarget;
import atf.api.tests.governance.distribution.GovernanceDeployment;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;

/** Runs every documentation validator against the document selected by the annotation. */
@AtfComponentTest
@DocumentationTarget("README.md")
@DocumentationTarget("atf-console/README.md")
@DocumentationTarget("atf-core/README.md")
@DocumentationTarget("atf-debug-ui/README.md")
@DocumentationTarget("atf-mock/README.md")
@DocumentationTarget("atf-reporting-tests/README.md")
@DocumentationTarget("atf-reporting-tests/src/test/java/atf/ui/tests/extent/README.md")
@DocumentationTarget("atf-reporting/README.md")
@DocumentationTarget("atf-test-kit/README.md")
@DocumentationTarget("docs/ATF_Api_Test_Guide.md")
@DocumentationTarget("docs/ATF_Architecture.md")
@DocumentationTarget("docs/ATF_Context_Atlas.md")
@DocumentationTarget("docs/ATF_Build_And_Execution_Safety.md")
@DocumentationTarget("docs/ATF_Evidence_Capture_Contract.md")
@DocumentationTarget("docs/ATF_Reporting_Contract.md")
@DocumentationTarget("docs/ATF_Testing_Guide.md")
@DocumentationTarget("docs/ATF_Test_Changes_Baseline.md")
@DocumentationTarget("docs/ATF_Test_Infrastructure_Annotations.md")
@DocumentationTarget("docs/README_Atf_BestPractices.md")
@DocumentationTarget("docs/README_Atf_CodeBestPractices.md")
@DocumentationTarget("docs/README_Atf_DocumentationBestPractices.md")
@DocumentationTarget("docs/README.md")
@DocumentationTarget("docs/feedback/ATF_Reporting_Feedback.md")
@DocumentationTarget("docs/feedback/README.md")
@DocumentationTarget("docs/feedback/README_AI_Evaluation.md")
@DocumentationTarget("atf-tests/README.md")
@DocumentationTarget("atf-tests/src/test/java/atf/api/tests/governance/distribution/README.md")
@DocumentationTarget("atf-test-practice-shop/README.md")
@DocumentationTarget("atf-test-auto-ex/README.md")
@DocumentationTarget("atf-ui/README.md")
@DocumentationTarget("atf-validation/README.md")
public class DocumentationTargetGovernanceTest {
    @Test
    void annotatedDocumentsPassAllDocumentationGovernanceModules() throws Exception {
        Path root = locateProjectRoot();
        List<DocumentationGovernanceSupport.Violation> violations = new ArrayList<>();

        for (DocumentationTarget annotation : getClass().getAnnotationsByType(DocumentationTarget.class)) {
            for (String configuredPath : GovernanceDeployment.read(root).documentationTargets(annotation.value())) {
            Path target = root.resolve(configuredPath).normalize();
            if (!target.startsWith(root) || !Files.isRegularFile(target)) {
                violations.add(DocumentationGovernanceSupport.violation(
                        "DOC-TARGET-PATH", root, target, 1,
                        "configured documentation target does not resolve to a file: " + configuredPath,
                        "DocumentationTarget values are project-relative and must point to a governed document",
                        "set @DocumentationTarget to an existing README.md or governed Markdown path"));
                continue;
            }
            if (!DocumentationGovernanceSupport.isGovernedMarkdown(root, target)) {
                violations.add(DocumentationGovernanceSupport.violation(
                        "DOC-TARGET-SCOPE", root, target, 1,
                        "configured target is outside the governed documentation scope",
                        "the target smoke test must exercise the same document population as the whole-ATF suite",
                        "target an owner README or a designated normative Markdown document"));
                continue;
            }
            DocumentationDiagramGovernanceTest.validate(target, root, violations);
            if (DocumentationGovernanceSupport.isReadme(target)) {
                DocumentationReadmeStructureGovernanceTest.validate(target, root, violations);
            } else {
                DocumentationLinkGovernanceTest.validate(target, root, violations);
            }
            DocumentationStyleGovernanceTest.validate(target, root, violations);
            DocumentationProseGovernanceTest.validate(target, root, violations);
            DynamicPathDocumentationArchitectureTest.validatePaths(target, root, violations);
            DynamicPathDocumentationArchitectureTest.validateGeneratedReportNames(
                    target, root, violations);
            }
        }

        DocumentationGovernanceSupport.assertNoViolations(
                "targeted documentation governance [all @DocumentationTarget paths]", violations);
    }
}
