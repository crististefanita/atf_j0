package atf.api.tests.governance.documentation.links;

import atf.api.tests.governance.documentation.DocumentationGovernanceSupport;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Local destinations and fragments are governed for owner READMEs and every project document. */
@AtfComponentTest
public class DocumentationLinkGovernanceTest {
    @Test
    void governedMarkdownKeepsResolvableLocalDestinationsAndFragments() throws Exception {
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        inspect(locateProjectRoot(), violations);
        DocumentationGovernanceSupport.assertNoViolations("documentation links and fragments", violations);
    }

    public static void inspect(Path root, List<DocumentationGovernanceSupport.Violation> violations) throws Exception {
        try (var files = Files.walk(root)) {
            files.filter(path -> DocumentationGovernanceSupport.isGovernedMarkdown(root, path))
                    .forEach(path -> validate(path, root, violations));
        }
    }

    public static void validate(Path path, Path root, List<DocumentationGovernanceSupport.Violation> violations) {
        try {
            List<String> lines = Files.readAllLines(path);
            DocumentationLocalLinks.validate(path, root, lines, violations);
            WorkspaceDocumentationLinks.validate(path, root, lines, violations);
        } catch (java.io.IOException unreadable) {
            violations.add(DocumentationGovernanceSupport.violation("DOC-READ", root, path, 1,
                    "documentation could not be read: " + unreadable.getMessage(),
                    "local destinations and fragments require readable Markdown",
                    "fix the document encoding or filesystem access"));
        }
    }

    @Test
    void ownedGithubRoutesRetainOfflinePathAndFragmentCoverage(@TempDir Path workspace) throws Exception {
        Path root = Files.createDirectories(workspace.resolve("atf_j0"));
        Path source = root.resolve("README.md");
        for (String project : List.of("atf_j1", "atf_j4", "atf_j5", "atf_j7")) {
            Path target = Files.createDirectories(workspace.resolve(project)).resolve("README.md");
            Files.writeString(target, "# Owner\n\n## Validări și API\n<a id='compat+v1'></a>\n");
        }
        List<String> lines = List.of("[J1](https://github.com/crististefanita/atf_j1/blob/HEAD/README.md#valid%C4%83ri-%C8%99i-api)",
                "[Whist](https://github.com/crististefanita/whist/blob/main/README.md#compat+v1)",
                "[Rentz](https://github.com/crististefanita/rentz)",
                "[Chess](https://github.com/crististefanita/chess/blob/main_RO/README.md)",
                "~~~markdown", "[literal](../../atf_j1/missing.md)", "~~~");
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        WorkspaceDocumentationLinks.validate(source, root, lines, violations);
        assertTrue(violations.isEmpty(), violations::toString);
        Files.writeString(workspace.resolve("atf_j1/README.md"), "# Renamed\n");
        WorkspaceDocumentationLinks.validate(source, root, lines, violations);
        assertEquals(1, violations.size());
        assertEquals("DOC-WORKSPACE-ANCHOR", violations.getFirst().code());
        assertTrue(violations.getFirst().toString().contains("README.md:1")
                && violations.getFirst().toString().contains("https://github.com/"));
    }

    @Test
    void rejectsCheckoutOnlyLinksMissingTargetsAndIncorrectPublishedNames(@TempDir Path workspace) throws Exception {
        Path root = Files.createDirectories(workspace.resolve("atf_j0"));
        Files.createDirectories(workspace.resolve("atf_j1"));
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        WorkspaceDocumentationLinks.validate(root.resolve("README.md"), root, List.of(
                "[local](../atf_j1/README.md)",
                "[missing](https://github.com/crististefanita/atf_j1/blob/HEAD/absent.md)",
                "[wrong repository](https://github.com/crististefanita/atf_j4/blob/HEAD/README.md)",
                "[unknown ref](https://github.com/crististefanita/atf_j1/blob/stale/README.md)"), violations);
        assertEquals(List.of("DOC-WORKSPACE-LINK", "DOC-WORKSPACE-TARGET", "DOC-WORKSPACE-REPOSITORY",
                "DOC-WORKSPACE-ROUTE"), violations.stream().map(DocumentationGovernanceSupport.Violation::code).toList());
    }
}
