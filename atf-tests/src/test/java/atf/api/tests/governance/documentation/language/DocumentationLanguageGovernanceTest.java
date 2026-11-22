package atf.api.tests.governance.documentation.language;

import atf.api.tests.governance.documentation.DocumentationGovernanceSupport;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static atf.api.tests.governance.AtfGovernanceTestSupport.locateProjectRoot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Enforces the language declared by the current bilingual documentation branch. */
@AtfComponentTest
public class DocumentationLanguageGovernanceTest {
    @Test
    void everyMarkdownDocumentUsesTheConfiguredBranchLanguage() throws Exception {
        Path root = locateProjectRoot();
        String language = DocumentationLanguagePolicy.read(root);
        List<DocumentationGovernanceSupport.Violation> violations = new ArrayList<>();

        try (var files = Files.walk(root)) {
            files.filter(path -> isEditionDocument(path, root))
                    .forEach(path -> validateLanguage(path, root, language, violations));
        }

        DocumentationGovernanceSupport.assertNoViolations(
                "branch documentation language governance [" + language + "]", violations);
    }

    @Test
    void detectsForeignHeadingsProseDiagramLabelsAndYamlReasons() {
        assertTrue(DocumentationLanguageRules.firstForeignLine("## Overview", "ro") > 0);
        assertTrue(DocumentationLanguageRules.firstForeignLine("## Scope and cause", "ro") > 0);
        assertTrue(DocumentationLanguageRules.firstForeignLine(
                "## Pe scurt\n\nÎnainte de rulare, fiecare modul păstrează dovezile.", "en") > 0);
        String mermaid = String.join("\n", "```mermaid", "flowchart LR",
                "A[Validarea contractului] --> B[Raportul]", "```");
        assertTrue(DocumentationLanguageRules.firstForeignLine(mermaid, "en") > 0);
        String romanianReason = String.join("\n", "```yaml",
                "reason: Clarifică proprietatea fără schimbarea verdictului.", "```");
        assertTrue(DocumentationLanguageRules.firstForeignLine(romanianReason, "en") > 0);
        String englishReason = String.join("\n", "```yaml",
                "reason: The policy states that the module must remain stable after an owner change.", "```");
        assertTrue(DocumentationLanguageRules.firstForeignLine(englishReason, "ro") > 0);
    }

    @Test
    void detectsCommonUntranslatedEnglishDocumentationHeadings() {
        for (String heading : List.of("Reporting library and audit contracts", "Screenshot context",
                "Explicit fixture preparation")) {
            assertTrue(DocumentationLanguageRules.firstForeignLine("## " + heading, "ro") > 0,
                    "the Romanian edition must translate the visible heading: " + heading);
        }
    }

    @Test
    void detectsShortEnglishDocumentsAcrossConciseLines() {
        String shortEnglish = String.join("\n", "## Owner and references",
                "The verification record owns the setup.",
                "Assertions remain stable after each run.",
                "Links preserve the canonical target.");
        assertTrue(DocumentationLanguageRules.firstForeignLine(shortEnglish, "ro") > 0);
    }

    @Test
    void doesNotAccumulateEnglishTermsAcrossSeparateMarkdownBlocks() {
        String romanian = String.join("\n\n",
                "Configurația and modulul păstrează contextul.",
                "Schema before publicare rămâne verificabilă.",
                "Contractul for test descrie limita.",
                "Rezultatele remain în raport.",
                "Verificarea through JUnit păstrează dovada.",
                "Modulul keeps proprietatea explicită.");
        assertEquals(0, DocumentationLanguageRules.firstForeignLine(romanian, "ro"));
    }

    @Test
    void ignoresCodeIdentifiersUrlsAndExamplesInsideNonYamlFences() {
        String english = String.join("\n", "Use `Pe scurt` in the example at https://example.test/path.",
                "```java", "String descriere = \"înainte și după\";", "```",
                "The API keeps the response and its contract stable.");
        String romanian = String.join("\n", "Folosește `At a glance` în exemplul de la https://example.test/path.",
                "```text", "The API and its response must stay unchanged.", "```",
                "Contractul păstrează sursa și rezultatul verificabil.");
        assertEquals(0, DocumentationLanguageRules.firstForeignLine(english, "en"));
        assertEquals(0, DocumentationLanguageRules.firstForeignLine(romanian, "ro"));
    }

    @Test
    void policyRequiresARecognizedLanguageAndVersionOne(@TempDir Path root) throws Exception {
        Path policy = Files.createDirectories(root.resolve("config"))
                .resolve("atf-governance-policy.json");
        Files.writeString(policy, "{\"schemaVersion\":1,\"documentationLanguage\":\"en\"}");
        assertEquals("en", DocumentationLanguagePolicy.read(root));
        Files.writeString(policy, "{\"schemaVersion\":1,\"documentationLanguage\":\"ro\"}");
        assertEquals("ro", DocumentationLanguagePolicy.read(root));
        Files.writeString(policy, "{\"schemaVersion\":1,\"documentationLanguage\":\"fr\"}");
        assertThrows(IOException.class, () -> DocumentationLanguagePolicy.read(root));
        Files.writeString(policy, "{\"schemaVersion\":2,\"documentationLanguage\":\"en\"}");
        assertThrows(IOException.class, () -> DocumentationLanguagePolicy.read(root));
    }

    private static boolean isEditionDocument(Path path, Path root) {
        return path.getFileName().toString().toLowerCase().endsWith(".md")
                && (DocumentationGovernanceSupport.isGovernedMarkdown(root, path)
                || path.toAbsolutePath().normalize().equals(root.resolve("AGENTS.md").toAbsolutePath().normalize()));
    }

    private static void validateLanguage(Path path, Path root, String language,
                                         List<DocumentationGovernanceSupport.Violation> violations) {
        try {
            String source = Files.readString(path);
            int line = DocumentationLanguageRules.firstForeignLine(source, language);
            if (line == 0) return;
            String excerpt = source.split("\\R", -1)[line - 1].strip();
            if (excerpt.length() > 120) excerpt = excerpt.substring(0, 119) + "…";
            String required = language.equals("en") ? "English" : "Romanian";
            violations.add(DocumentationGovernanceSupport.violation(
                    "DOC-LANGUAGE", root, path, line,
                    "visible prose is outside the " + required + " edition: `" + excerpt + "`",
                    "all Markdown documents use the language declared by the current branch policy",
                    "translate visible explanations and labels; preserve API names, commands and code examples"));
        } catch (Exception error) {
            violations.add(DocumentationGovernanceSupport.violation(
                    "DOC-READ", root, path, 1,
                    "Markdown could not be checked for language: " + error.getMessage(),
                    "every documentation edition file is readable by governance",
                    "fix the encoding or policy before changing document text"));
        }
    }
}
