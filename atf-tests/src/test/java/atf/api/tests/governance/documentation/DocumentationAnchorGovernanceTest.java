package atf.api.tests.governance.documentation;

import atf.api.tests.governance.documentation.links.MarkdownAnchorIndex;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class DocumentationAnchorGovernanceTest {
    @Test
    void indexesUnicodeMarkupDuplicatesSetextAndExplicitAnchors() {
        assertEquals(Set.of("validări-și-api", "validări-și-api-1", "validări-și-api-1-1", "compat", "legacy", "subtitle", "a-useful-pre_commit"),
                MarkdownAnchorIndex.anchors(List.of("## Validări și `API`", "## Validări și API!",
                        "## Validări și API-1", "<a id='compat' name=\"legacy\"></a>", "Subtitle", "---",
                        "## A _useful_ pre_commit", "<div data-id='not-an-anchor'></div>")));
    }

    @Test
    void fencesIgnoreLinksAndHeadingsUntilAMatchingLongEnoughClosingMarker() {
        assertEquals(Set.of("real"), MarkdownAnchorIndex.anchors(List.of("````md", "## fake", "```",
                "## still fake", "````", "~~~", "<a id='fake'></a>", "~~~", "## Real")));
    }

    @Test
    void acceptsLocalCrossFileEncodedAndExplicitFragments(@TempDir Path root) throws Exception {
        Path source = root.resolve("README.md");
        Files.writeString(root.resolve("other file.md"), "## Validări și API\n## Validări și API\n<a id='compat+v1'></a>\n");
        var lines = List.of("# Owner", "## Pe scurt", "[local](#pe-scurt)",
                "[encoded](other%20file.md#valid%C4%83ri-%C8%99i-api)",
                "[duplicate](other%20file.md#validări-și-api-1)", "[explicit](<other%20file.md#compat+v1>)",
                "```md", "[example](missing.md#missing)", "```", "~~~md", "[example](#missing)", "~~~");
        Files.write(source, lines);
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        DocumentationReadmeStructureGovernanceTest.inspectLocalLinks(source, root, lines, violations);
        assertTrue(violations.isEmpty(), violations::toString);
    }

    @Test
    void aRemovedHeadingFailsAtTheOriginalLinkLineWithRemediation(@TempDir Path root) throws Exception {
        Path source = root.resolve("README.md");
        Path target = root.resolve("owner.md");
        var lines = List.of("# Owner", "## Pe scurt", "[contract](owner.md#stable-heading)");
        Files.write(source, lines);
        Files.writeString(target, "## Stable heading\n");
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        DocumentationReadmeStructureGovernanceTest.inspectLocalLinks(source, root, lines, violations);
        assertTrue(violations.isEmpty());
        Files.writeString(target, "```md\n## Stable heading\n```\n## Renamed heading\n");
        DocumentationReadmeStructureGovernanceTest.inspectLocalLinks(source, root, lines, violations);
        assertEquals(1, violations.size());
        String diagnostic = violations.getFirst().toString();
        assertTrue(diagnostic.contains("DOC-README-ANCHOR"), diagnostic);
        assertTrue(diagnostic.contains("README.md") && diagnostic.contains("3"), diagnostic);
        assertTrue(diagnostic.contains("restore the destination heading"), diagnostic);
    }

    @Test
    void missingFilesInvalidEscapesAndCaseMismatchesDoNotPass(@TempDir Path root) throws Exception {
        Path source = root.resolve("README.md");
        var lines = List.of("# Owner", "[missing](absent.md#heading)", "[bad](#%xx)", "[case](#Owner)");
        Files.write(source, lines);
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        DocumentationReadmeStructureGovernanceTest.inspectLocalLinks(source, root, lines, violations);
        assertEquals(3, violations.size(), violations::toString);
    }
}
