package atf.testkit.governance;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies language findings without treating localized literal data as source prose. */
@AtfUnitTest
class AtfSourceLanguageScannerTest {
    @Test
    void findsAccentedAndUnaccentedRomanianCommentsAndIdentifiersWithLines() {
        String source = "class Sample {\n"
                + "  // Verifică alegerea culorii.\n"
                + "  void adaugaProdus() { }\n"
                + "  String label = \"Numele jucătorului\";\n"
                + "}\n";

        List<AtfSourceLanguageScanner.Finding> findings =
                AtfSourceLanguageScanner.inspect("Sample.java", source);

        assertTrue(findings.contains(new AtfSourceLanguageScanner.Finding(
                2, AtfSourceLanguageScanner.Construct.COMMENT, "Verifică")));
        assertTrue(findings.contains(new AtfSourceLanguageScanner.Finding(
                3, AtfSourceLanguageScanner.Construct.IDENTIFIER, "adauga")));
        assertTrue(findings.stream().noneMatch(finding -> finding.token().equals("jucătorului")));
    }

    @Test
    void ignoresLocalizedStringsRegexesAndTemplateTextButChecksTemplateExpressions() {
        char tick = 96;
        String source = "const label = " + tick + "Meniu pentru jucător" + tick + ";\n"
                + "const route = /jucator/;\n"
                + "const title = " + tick + "Player: ${jucatorName}" + tick + ";\n";

        List<AtfSourceLanguageScanner.Finding> findings =
                AtfSourceLanguageScanner.inspect("fixture.mjs", source);

        assertEquals(1, findings.size());
        assertEquals(new AtfSourceLanguageScanner.Finding(
                3, AtfSourceLanguageScanner.Construct.IDENTIFIER, "jucator"), findings.getFirst());
    }

    @Test
    void checksDeveloperDiagnosticsButLeavesLocalizedAssertionFixturesAlone() {
        String source = "class Sample {\n"
                + "  void verify() {\n"
                + "    assertEquals(\"Jucătorul mută\", actual);\n"
                + "    throw new IllegalArgumentException(\"Culoare invalidă\");\n"
                + "    assertEquals(expected, actual, \"Poziție invalidă\");\n"
                + "  }\n"
                + "}\n";

        List<AtfSourceLanguageScanner.Finding> findings =
                AtfSourceLanguageScanner.inspect("Sample.java", source);

        assertTrue(findings.stream().anyMatch(finding -> finding.line() == 4
                && finding.construct() == AtfSourceLanguageScanner.Construct.DEVELOPER_MESSAGE));
        assertTrue(findings.stream().anyMatch(finding -> finding.line() == 5
                && finding.construct() == AtfSourceLanguageScanner.Construct.DEVELOPER_MESSAGE));
        assertTrue(findings.stream().noneMatch(finding -> finding.token().equals("Jucătorul")));

        List<AtfSourceLanguageScanner.Finding> testAssertions = AtfSourceLanguageScanner.inspect(
                "module/src/test/java/SampleTest.java", source);
        assertTrue(testAssertions.stream().anyMatch(finding -> finding.line() == 5
                && finding.construct() == AtfSourceLanguageScanner.Construct.DEVELOPER_MESSAGE));
        assertTrue(AtfSourceLanguageScanner.inspect("module/src/test/java/OutputTest.java",
                "class OutputTest { void run() { System.out.println(\"path with spaces Ș\"); } }").isEmpty());
    }

    @Test
    void rejectsUnsupportedSourceExtensions() {
        assertThrows(IllegalArgumentException.class,
                () -> AtfSourceLanguageScanner.inspect("fixture.yaml", "key: value"));
    }
}
