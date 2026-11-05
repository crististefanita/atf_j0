package atf.api.tests.governance.documentation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Shared discovery and diagnostic vocabulary for documentation governance tests. */
public final class DocumentationGovernanceSupport {
    private static final List<String> IGNORED_DIRECTORY_NAMES = List.of(
            ".git", "target", "artifacts", ".idea", "migration-copy-rejected",
            "migration-quarantine", ".presentation-build");

    private DocumentationGovernanceSupport() {
    }

    public record Violation(String code, Path file, int line, String finding,
                     String rule, String fix) {
        @Override
        public String toString() {
            return "\n[" + code + "] " + file + ":" + line
                    + "\n  found: " + finding
                    + "\n  rule: " + rule
                    + "\n  fix: " + fix;
        }
    }

    public static Violation violation(String code, Path root, Path file, int line,
                               String finding, String rule, String fix) {
        Path relative = root.toAbsolutePath().normalize()
                .relativize(file.toAbsolutePath().normalize());
        return new Violation(code, relative, line, finding, rule, fix);
    }

    public static void assertNoViolations(String area, List<Violation> violations) {
        assertTrue(violations.isEmpty(), () -> {
            String details = violations.stream().map(Violation::toString).reduce("", String::concat);
            return area + " failed with " + violations.size()
                    + " actionable violation(s)." + details;
        });
    }

    static boolean isReadme(Path path) {
        if (!Files.isRegularFile(path)
                || !path.getFileName().toString().equalsIgnoreCase("README.md")) {
            return false;
        }
        for (Path part : path) {
            if (IGNORED_DIRECTORY_NAMES.contains(part.toString())) return false;
        }
        return true;
    }

    public static boolean isGovernedMarkdown(Path root, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        Path projectRoot = root.toAbsolutePath().normalize();
        if (!normalized.startsWith(projectRoot)) return false;
        for (Path part : projectRoot.relativize(normalized)) {
            if (IGNORED_DIRECTORY_NAMES.contains(part.toString())) return false;
        }
        Path docsRoot = root.resolve("docs").toAbsolutePath().normalize();
        return isReadme(path)
                || (Files.isRegularFile(normalized) && normalized.getParent().equals(projectRoot.resolve("lib"))
                && List.of("README.Core.md", "README.Reporting.md", "README.Console.md", "README.Mock.md")
                .contains(normalized.getFileName().toString()))
                || (Files.isRegularFile(normalized)
                && normalized.startsWith(docsRoot)
                && normalized.getFileName().toString().toLowerCase().endsWith(".md"));
    }
}
