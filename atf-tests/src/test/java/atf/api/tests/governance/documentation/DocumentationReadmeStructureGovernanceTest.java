package atf.api.tests.governance.documentation;

import atf.api.tests.governance.documentation.links.DocumentationLocalLinks;

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

/** Checks the minimum readable shape shared by every owner README. */
@AtfComponentTest
public class DocumentationReadmeStructureGovernanceTest {
    private static final Pattern H1 = Pattern.compile("^#\\s+[^#].*$");
    private static final Pattern H2_ORIENTATION = Pattern.compile("^##\\s+(?:Overview|Pe scurt)\\b.*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ROOT_H2_ORIENTATION = Pattern.compile(
            "^##\\s+ATF Java\\s+—\\s+prezentare executivă\\s*$", Pattern.CASE_INSENSITIVE);
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
    void liveRootOverviewRoutesEveryWorkspacePromisedByTheLibraryIndex() throws Exception {
        Path root = locateProjectRoot();
        List<String> lines = Files.readAllLines(root.resolve("README.md"));
        for (int workspace = 0; workspace <= 7; workspace++) {
            String rowPrefix = "| J" + workspace;
            assertEquals(1, lines.stream().filter(line -> line.startsWith(rowPrefix + " |")
                            || line.startsWith(rowPrefix + " ·")).count(),
                    "The root workspace overview must have one row for J" + workspace);
        }
    }

    @Test
    void rootReadmeRequiresItsOwnExecutiveOrientation(@TempDir Path workspace) throws Exception {
        Path root = Files.createDirectories(workspace.resolve("project"));
        Path readme = root.resolve("README.md");
        Files.writeString(readme, "# ATF\n\n## Module map\n\nDetails.\n");
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        validate(readme, root, violations);
        assertEquals(List.of("DOC-README-OVERALL"), violations.stream()
                .map(DocumentationGovernanceSupport.Violation::code).toList());

        Files.writeString(readme, "# ATF\n\n## ATF Java — prezentare executivă\n\nPurpose.\n");
        violations.clear();
        validate(readme, root, violations);
        DocumentationGovernanceSupport.assertNoViolations("Root README orientation", violations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Overview", "Pe scurt"})
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
            inspectTitleAndOrientation(path, root, lines, violations);
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
        boolean rootReadme = path.toAbsolutePath().normalize()
                .equals(root.resolve("README.md").toAbsolutePath().normalize());
        Pattern orientation = rootReadme ? ROOT_H2_ORIENTATION : H2_ORIENTATION;
        if (matchingLines(lines, orientation).isEmpty()) {
            violations.add(DocumentationGovernanceSupport.violation(
                    "DOC-README-OVERALL", root, path, 1,
                    rootReadme
                            ? "README-ul rădăcină nu are prezentarea executivă a proiectului"
                            : "README-ul nu are secțiunea de orientare `## Overview` sau `## Pe scurt`",
                    rootReadme
                            ? "pagina rădăcină orientează cititorul asupra proiectului înaintea detaliilor"
                            : "README-urile modulelor și ale documentației prezintă scopul, proprietarul și limitele înaintea detaliilor",
                    rootReadme
                            ? "adaugă `## ATF Java — prezentare executivă` înaintea detaliilor"
                            : "adaugă `## Overview` în engleză sau `## Pe scurt` în română înaintea detaliilor, conform limbii proiectului"));
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
