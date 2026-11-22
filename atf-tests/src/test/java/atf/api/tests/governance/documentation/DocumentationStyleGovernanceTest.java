package atf.api.tests.governance.documentation;

import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects the renderer-neutral ATF README visual language. */
@AtfComponentTest
public class DocumentationStyleGovernanceTest {
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+.+$");
    private static final Pattern FENCE = Pattern.compile("^\\s*(`{3,}|~{3,})(.*)$");
    private static final Pattern PRESENTATIONAL_HTML = Pattern.compile(
            "(?i)<(?:font|h[1-6]|span|div)\\b|\\bstyle\\s*=");
    private static final Pattern NON_CANONICAL_BULLET = Pattern.compile("^\\s*[+*]\\s+.+$");
    private static final Pattern LANGUAGE = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]*$");
    private static final Pattern EDITORIAL_ARTIFACT = Pattern.compile(
            "\\bZ[XQ]{2,}\\d*(?:Z[XQ\\d]*)?\\b|\\u2581|(?i:\\bfucking\\b)");
    private static final Pattern PROSE_EXCLUSIONS = Pattern.compile(
            "`+[^`]*`+|<[^>]+>|\\]\\((?:[^()]|\\([^()]*\\))*\\)|https?://\\S+");

    @Test
    void everyOwnerReadmeUsesTheRendererNeutralAtfStyle() throws Exception {
        Path root = locateProjectRoot();
        List<DocumentationGovernanceSupport.Violation> violations = new ArrayList<>();
        try (var files = Files.walk(root)) {
            files.filter(path -> DocumentationGovernanceSupport.isGovernedMarkdown(root, path))
                    .forEach(path -> validate(path, root, violations));
        }
        DocumentationGovernanceSupport.assertNoViolations("README ATF style governance", violations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"A remaining ZXQ005ZXQ token.", "A damaged ZXQ005ZQ token.",
            "A stray ZXXQ token.", "A broken ▁word.", "Cancel the fucking pending."})
    void rejectsObservedTranslationArtifactsInVisibleProse(String prose, @TempDir Path root) throws Exception {
        Path readme = root.resolve("README.md");
        Files.writeString(readme, "# Contract\n\n" + prose + "\n");
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        validate(readme, root, violations);
        assertTrue(violations.stream().anyMatch(item -> item.code().equals("DOC-STYLE-EDITORIAL")
                && item.line() == 3), "the diagnostic must identify the visible artifact and its line");
    }

    @Test
    void retainsLiteralExamplesAndMatchesFenceKindAndLength(@TempDir Path root) throws Exception {
        Path readme = root.resolve("README.md");
        Files.writeString(readme, String.join("\n", "# Contract", "",
                "Use `ZXQ005ZQ` as an example at https://example.test/ZXQ005ZQ.",
                "````java", "```", "String literal = \"ZXQ005ZQ\";", "````",
                "~~~text", "A ▁literal example.", "~~~", ""));
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        validate(readme, root, violations);
        assertTrue(violations.isEmpty(), () -> "literal examples must stay valid: " + violations);
    }

    @Test
    void checksDiagramLabelsAndRejectsAnUnclosedTildeFence(@TempDir Path root) throws Exception {
        Path readme = root.resolve("README.md");
        Files.writeString(readme, "# Contract\n\n~~~mermaid\nA[ZXQ005ZQ]\n````\n");
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        validate(readme, root, violations);
        assertTrue(violations.stream().anyMatch(item -> item.code().equals("DOC-STYLE-EDITORIAL")
                && item.line() == 4));
        assertTrue(violations.stream().anyMatch(item -> item.code().equals("DOC-STYLE-FENCE")),
                "a backtick fence must not close a tilde fence");
    }

    @Test
    void checksYamlReasonsWithoutTreatingDataAsProse(@TempDir Path root) throws Exception {
        Path readme = root.resolve("README.md");
        Files.writeString(readme, "# Contract\n\n~~~YAML\ncode: ZXQ005ZQ\nreason: ZXQ005ZQ\n~~~\n");
        var violations = new ArrayList<DocumentationGovernanceSupport.Violation>();
        validate(readme, root, violations);
        assertTrue(violations.size() == 1 && violations.getFirst().code().equals("DOC-STYLE-EDITORIAL")
                && violations.getFirst().line() == 5, "only the human-readable reason is editorial prose");
    }

    static void validate(Path path, Path root,
                         List<DocumentationGovernanceSupport.Violation> violations) {
        try {
            List<String> lines = Files.readAllLines(path);
            MatcherState fence = null;
            int previousHeadingLevel = 1;
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                MatcherState state = fenceState(line);
                if (fence != null) {
                    if (state.closing() && state.marker() == fence.marker() && state.length() >= fence.length()) {
                        fence = null;
                    } else if (fence.language().equalsIgnoreCase("mermaid")
                            || fence.language().equalsIgnoreCase("yaml") && line.matches("^\\s*(?:-\\s+)?reason:.*")) {
                        validateEditorialProse(path, root, line, index + 1, violations);
                    }
                    continue;
                }
                if (state.opening()) {
                    fence = state;
                    if (state.language().isBlank()) {
                        violations.add(DocumentationGovernanceSupport.violation(
                                "DOC-STYLE-FENCE", root, path, index + 1,
                                "code fence has no language identifier",
                                "every executable or schematic block uses a typed Markdown fence",
                                "write ` ```text `, ` ```mermaid `, ` ```java ` or the appropriate language"));
                    } else if (!LANGUAGE.matcher(state.language()).matches()) {
                        violations.add(DocumentationGovernanceSupport.violation(
                                "DOC-STYLE-FENCE", root, path, index + 1,
                                "code fence language is not a simple renderer token: " + state.language(),
                                "fence labels must be portable across Markdown renderers",
                                "use one identifier such as `mermaid`, `text`, `java`, `xml` or `yaml`"));
                    }
                    continue;
                }
                validateEditorialProse(path, root, line, index + 1, violations);
                if (PRESENTATIONAL_HTML.matcher(line).find()) {
                    violations.add(DocumentationGovernanceSupport.violation(
                            "DOC-STYLE-PRESENTATION", root, path, index + 1,
                            "README contains renderer-specific HTML/CSS presentation",
                            "ATF style is semantic Markdown; the host renderer owns font, size and color",
                            "replace font/style tags with headings, bullets, tables, code fences or `<details>`"));
                }
                if (NON_CANONICAL_BULLET.matcher(line).matches()) {
                    violations.add(DocumentationGovernanceSupport.violation(
                            "DOC-STYLE-BULLET", root, path, index + 1,
                            "README uses `*` or `+` as a list marker",
                            "ATF prose uses `-` bullets for one stable scan pattern",
                            "replace the marker with `-` and keep the item concise"));
                }
                var heading = HEADING.matcher(line);
                if (heading.matches()) {
                    int level = heading.group(1).length();
                    if (level > previousHeadingLevel + 1) {
                        violations.add(DocumentationGovernanceSupport.violation(
                                "DOC-STYLE-HEADING", root, path, index + 1,
                                "heading jumps from level " + previousHeadingLevel + " to level " + level,
                                "the outline must remain scannable and hierarchical",
                                "insert the missing intermediate heading or lower this heading one level"));
                    }
                    previousHeadingLevel = level;
                }
            }
            if (fence != null) {
                violations.add(DocumentationGovernanceSupport.violation(
                        "DOC-STYLE-FENCE", root, path, lines.size(),
                        "code fence is not closed",
                        "every fenced block must have a matching closing fence",
                        "close the block before the next Markdown section"));
            }
        } catch (Exception error) {
            violations.add(DocumentationGovernanceSupport.violation(
                    "DOC-READ", root, path, 1,
                    "README could not be read: " + error.getMessage(),
                    "every owner README must be readable by the governance scan",
                    "fix the file encoding or filesystem access before changing content"));
        }
    }

    private static void validateEditorialProse(Path path, Path root, String line, int number,
                                              List<DocumentationGovernanceSupport.Violation> violations) {
        String visible = PROSE_EXCLUSIONS.matcher(line).replaceAll(" ");
        if (EDITORIAL_ARTIFACT.matcher(visible).find()) {
            violations.add(DocumentationGovernanceSupport.violation(
                    "DOC-STYLE-EDITORIAL", root, path, number,
                    "visible prose contains an unresolved translation or drafting artifact",
                    "published contracts contain reviewed technical prose, not generation markers",
                    "compare the passage with its language peer and live owner code; repair the wording"));
        }
    }

    private static MatcherState fenceState(String line) {
        var matcher = FENCE.matcher(line);
        if (!matcher.matches()) return new MatcherState(false, false, '\0', 0, "");
        String delimiter = matcher.group(1);
        String suffix = matcher.group(2).trim();
        return new MatcherState(true, suffix.isBlank(), delimiter.charAt(0), delimiter.length(), suffix);
    }

    private record MatcherState(boolean opening, boolean closing, char marker, int length, String language) {
    }
}
