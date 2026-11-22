package atf.api.tests.governance.documentation;

import atf.api.tests.governance.documentation.links.DocumentationLocalLinks;
import atf.api.tests.governance.documentation.links.MarkdownAnchorIndex;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the minimum readable shape shared by every owner README. */
@AtfComponentTest
public class DocumentationReadmeStructureGovernanceTest {
    private static final Pattern H1 = Pattern.compile("^#\\s+[^#].*$");
    private static final Pattern H2_ORIENTATION = Pattern.compile("^##\\s+(?:Overview|At a glance|Pe scurt)\\b.*$",
            Pattern.CASE_INSENSITIVE);
    private static final String WORKSPACE_INDEX = "docs/workspaces/README.md";
    private static final Pattern EXECUTIVE_OVERVIEW = Pattern.compile(
            "^##\\s+ATF Java\\s+—\\s+(?:prezentare executivă|executive overview)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORKSPACE_ROW = Pattern.compile("^\\|\\s*J[0-7](?:\\s|\\s*·).*");
    private static final Pattern INDEX_LINK = Pattern.compile("\\[[^\\]]+]\\(" + Pattern.quote(WORKSPACE_INDEX) + "\\)");
    private static final Pattern DETAILS_OPEN = Pattern.compile("^\\s*<details(?:\\s|>)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DETAILS_CLOSE = Pattern.compile("^\\s*</details>\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SUMMARY = Pattern.compile("^\\s*<summary(?:\\s|>)",
            Pattern.CASE_INSENSITIVE);

    @Test
    void ownerReadmesKeepAReadableStableShape() throws Exception {
        Path root = locateProjectRoot();
        List<DocumentationGovernanceSupport.Violation> violations = new ArrayList<>();
        try (var files = Files.walk(root)) {
            files.filter(DocumentationGovernanceSupport::isReadme)
                    .forEach(path -> validate(path, root, violations));
        }
        DocumentationGovernanceSupport.assertNoViolations("README structure governance", violations);
    }

    @Test
    void dedicatedIndexRoutesEveryWorkspacePromisedByTheLibraryIndex() throws Exception {
        Path root = locateProjectRoot();
        List<String> lines = Files.readAllLines(root.resolve(WORKSPACE_INDEX));
        for (int workspace = 0; workspace <= 7; workspace++) {
            String rowPrefix = "| J" + workspace;
            assertEquals(1, lines.stream().filter(line -> line.startsWith(rowPrefix + " |")
                            || line.startsWith(rowPrefix + " ·")).count(),
                    "The dedicated workspace index must have one row for J" + workspace);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"At a glance", "Overview", "Pe scurt"})
    void rootReadmeRequiresLocalOrientationAndAFinalIndexLink(String heading, @TempDir Path workspace) throws Exception {
        Path root = Files.createDirectories(workspace.resolve("project"));
        Path readme = root.resolve("README.md");
        writeRoot(root, "## Module map\n\nDetails.");
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        validate(readme, root, violations);
        assertEquals(List.of("DOC-README-OVERALL"), violations.stream()
                .map(DocumentationGovernanceSupport.Violation::code).toList());

        writeRoot(root, "## " + heading + "\n\nPurpose.");
        violations.clear();
        validate(readme, root, violations);
        DocumentationGovernanceSupport.assertNoViolations("Root README orientation", violations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"## ATF Java — executive overview", "## ATF Java — prezentare executivă",
            "| J1 | consumer |"})
    void rejectsWorkspaceCatalogContentInTheProjectOverview(String catalog, @TempDir Path root) throws Exception {
        Path readme = writeRoot(root, "## At a glance\n\nPurpose.\n\n" + catalog);
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        validate(readme, root, violations);
        assertTrue(violations.stream().anyMatch(item -> item.code().equals("DOC-README-WORKSPACE-CATALOG")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"early", "duplicate"})
    void indexNavigationMustBeUniqueAndInTheFinalSection(String mutation, @TempDir Path root) throws Exception {
        Path readme = writeRoot(root, "## At a glance\n\nPurpose.");
        String source = Files.readString(readme);
        source = mutation.equals("early") ? source + "\n## Later section\n\nDetails.\n"
                : source + "\n[Duplicate](" + WORKSPACE_INDEX + ")\n";
        Files.writeString(readme, source);
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        validate(readme, root, violations);
        assertTrue(violations.stream().anyMatch(item -> item.code().equals("DOC-README-WORKSPACE-ROUTE")));
    }

    @Test
    void rootOrientationPrecedesTheModuleDetails(@TempDir Path root) throws Exception {
        Path readme = writeRoot(root, "## Modules\n\nDetails.\n\n## At a glance\n\nPurpose.");
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        validate(readme, root, violations);
        assertTrue(violations.stream().anyMatch(item -> item.code().equals("DOC-README-ORIENTATION-ORDER")));
    }

    @Test
    void fencedCatalogExamplesDoNotBecomeRootContent(@TempDir Path root) throws Exception {
        Path readme = writeRoot(root, "## At a glance\n\nPurpose.\n\n~~~markdown\n"
                + "## ATF Java — executive overview\n| J1 | consumer |\n[Index](" + WORKSPACE_INDEX + ")\n~~~");
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        validate(readme, root, violations);
        DocumentationGovernanceSupport.assertNoViolations("Literal catalog example", violations);
    }

    private static Path writeRoot(Path root, String content) throws Exception {
        Path index = root.resolve(WORKSPACE_INDEX);
        Files.createDirectories(index.getParent());
        Files.writeString(index, "# Workspace index\n\n## At a glance\n\nProject routes.\n");
        Path readme = root.resolve("README.md");
        Files.writeString(readme, "# ATF\n\n" + content
                + "\n\n## Workspace connections\n\n[Index](" + WORKSPACE_INDEX + ")\n");
        return readme;
    }

    @ParameterizedTest
    @ValueSource(strings = {"Overview", "At a glance", "Pe scurt"})
    void localizedOrientationPreservesTheSameStructure(String heading, @TempDir Path root) throws Exception {
        Path file = Files.createDirectories(root.resolve("owner")).resolve("README.md");
        Files.writeString(file, "# Owner\n\n## " + heading + "\n\nContract.\n");
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        validate(file, root, violations);
        DocumentationGovernanceSupport.assertNoViolations("Localized orientation", violations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Summary", "Overviewish", "Pe scurtish"})
    void unrelatedOrPartialHeadingsStillFail(String heading, @TempDir Path root) throws Exception {
        Path file = Files.createDirectories(root.resolve("owner")).resolve("README.md");
        Files.writeString(file, "# Owner\n\n## " + heading + "\n\nContract.\n");
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        validate(file, root, violations);
        assertEquals(List.of("DOC-README-OVERALL"), violations.stream()
                .map(DocumentationGovernanceSupport.Violation::code).toList());
    }

    static void validate(Path path, Path root,
                         List<DocumentationGovernanceSupport.Violation> violations) {
        try {
            List<String> lines = Files.readAllLines(path);
            List<String> visible = MarkdownAnchorIndex.withoutFences(lines);
            inspectTitleAndOrientation(path, root, visible, violations);
            if (path.toAbsolutePath().normalize().equals(root.resolve("README.md").toAbsolutePath().normalize())) {
                inspectRootNavigation(path, root, visible, violations);
            }
            inspectDetails(path, root, lines, violations);
            inspectLocalLinks(path, root, lines, violations);
        } catch (Exception error) {
            violations.add(DocumentationGovernanceSupport.violation(
                    "DOC-READ", root, path, 1,
                    "README could not be read: " + error.getMessage(),
                    "every owner README must be readable by the governance scan",
                    "fix the file encoding or filesystem access before changing content"));
        }
    }

    private static void inspectTitleAndOrientation(Path path, Path root, List<String> lines,
                                                   List<DocumentationGovernanceSupport.Violation> violations) {
        List<Integer> titles = matchingLines(lines, H1);
        if (titles.size() != 1) {
            violations.add(DocumentationGovernanceSupport.violation(
                    "DOC-README-TITLE", root, path, titles.isEmpty() ? 1 : titles.getFirst(),
                    "README has " + titles.size() + " level-one title(s)",
                    "an owner README has one stable title before its sections",
                    "keep one `# ...` title and convert additional top-level titles to `## ...`"));
        }
        if (matchingLines(lines, H2_ORIENTATION).isEmpty()) {
            violations.add(DocumentationGovernanceSupport.violation(
                    "DOC-README-OVERALL", root, path, 1,
                    "README has no local project orientation",
                    "owner READMEs explain purpose, ownership and limits before details",
                    "add `## At a glance` in English or `## Pe scurt` in Romanian before details"));
        }
    }

    private static void inspectRootNavigation(Path path, Path root, List<String> lines,
                                               List<DocumentationGovernanceSupport.Violation> violations) {
        List<Integer> orientation = matchingLines(lines, H2_ORIENTATION);
        List<Integer> sections = matchingLines(lines, Pattern.compile("^##\\s+.+"));
        if (!orientation.isEmpty() && !sections.isEmpty() && !orientation.getFirst().equals(sections.getFirst())) {
            violations.add(DocumentationGovernanceSupport.violation("DOC-README-ORIENTATION-ORDER", root, path,
                    orientation.getFirst(), "local orientation follows other sections",
                    "the root introduces this project before its details", "move local orientation to the first level-two section"));
        }
        var links = new ArrayList<Integer>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (EXECUTIVE_OVERVIEW.matcher(line).matches() || WORKSPACE_ROW.matcher(line).matches()) {
                violations.add(DocumentationGovernanceSupport.violation("DOC-README-WORKSPACE-CATALOG", root, path,
                        index + 1, "cross-workspace overview in the project README",
                        "the root explains its own project; workspace routes have a dedicated owner",
                        "move the catalog to " + WORKSPACE_INDEX + " and retain its final navigation link"));
            }
            var matcher = INDEX_LINK.matcher(line);
            while (matcher.find()) links.add(index + 1);
        }
        if (links.size() != 1 || sections.isEmpty() || links.getFirst() <= sections.getLast()) {
            violations.add(DocumentationGovernanceSupport.violation("DOC-README-WORKSPACE-ROUTE", root, path,
                    links.isEmpty() ? 1 : links.getFirst(), "workspace index is missing, duplicated or outside the final section",
                    "one final root link routes cross-workspace readers to the dedicated index",
                    "link once to " + WORKSPACE_INDEX + " in the final level-two section"));
        }
    }

    private static List<Integer> matchingLines(List<String> lines, Pattern pattern) {
        List<Integer> matches = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            if (pattern.matcher(lines.get(index)).matches()) matches.add(index + 1);
        }
        return matches;
    }

    private static void inspectDetails(Path path, Path root, List<String> lines,
                                       List<DocumentationGovernanceSupport.Violation> violations) {
        Deque<Integer> openings = new ArrayDeque<>();
        Deque<Boolean> summaries = new ArrayDeque<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (DETAILS_OPEN.matcher(line).find()) {
                openings.push(index + 1);
                summaries.push(false);
            } else if (SUMMARY.matcher(line).find() && !summaries.isEmpty()) {
                summaries.pop();
                summaries.push(true);
            } else if (DETAILS_CLOSE.matcher(line).matches()) {
                if (openings.isEmpty()) {
                    violations.add(DocumentationGovernanceSupport.violation(
                            "DOC-README-DETAILS", root, path, index + 1,
                            "closing `</details>` has no matching opening tag",
                            "collapsible sections must be structurally balanced",
                            "remove the orphan closing tag or add the matching `<details>` owner"));
                    continue;
                }
                int opening = openings.pop();
                if (!summaries.pop()) {
                    violations.add(DocumentationGovernanceSupport.violation(
                            "DOC-README-SUMMARY", root, path, opening,
                            "collapsible section has no `<summary>`",
                            "every collapsed block needs a visible purpose before its details",
                            "add a concise `<summary>` that names the hidden rationale, table or exception"));
                }
            }
        }
        while (!openings.isEmpty()) {
            int opening = openings.pop();
            summaries.pop();
            violations.add(DocumentationGovernanceSupport.violation(
                    "DOC-README-DETAILS", root, path, opening,
                    "`<details>` is not closed",
                    "collapsible sections must be structurally balanced",
                    "close the block with `</details>` before the next Markdown section"));
        }
    }

    static void inspectLocalLinks(Path path, Path root, List<String> lines,
                                  List<DocumentationGovernanceSupport.Violation> violations) {
        DocumentationLocalLinks.validate(path, root, lines, violations);
    }
}
