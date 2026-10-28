package atf.core.files;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class AtfJavaSourceReferencesTest {
    @Test
    void exposesOnlyExecutableTokensForAnnotationAndTypeGuards() {
        String code = AtfJavaSourceReferences.codeOnly("// @Suite abstract class Hidden {}\n"
                + "/* @interface Hidden {} */\n@" + "\\" + "u0041lias class Actual { "
                + "String example = \"@Suite\"; char quote = '\\''; }");
        assertTrue(code.contains("@Alias class Actual"));
        assertFalse(code.contains("@Suite"));
        assertFalse(code.contains("@interface"));
        assertFalse(code.contains("abstract class"));
    }

    @Test
    void recognizesQualifiedNamesAcrossWhitespaceAndComments() {
        for (String reference : List.of("import atf.ui.Page;", "import static atf.ui.Page.VALUE;",
                "import atf.ui.*;", "package atf.ui;", "atf.ui.Page page;",
                "atf\n. /* boundary */ ui . Page page;")) {
            assertTrue(AtfJavaSourceReferences.containsNamespace(reference, "atf.ui"), reference);
        }
    }

    @Test
    void skipsCommentsStringsCharactersAndTextBlocks() {
        String examples = "// atf.ui.Page\n/* import atf.ui.Page; */\n"
                + "String key = \"atf.ui.timeout\"; char quote = '\\'';"
                + "String doc = \"\"\"\nimport atf.ui.Page;\n\"\"\";"
                + "String escaped = \"a \\\" atf.ui.Page\";";
        assertFalse(AtfJavaSourceReferences.containsNamespace(examples, "atf.ui"));
        assertTrue(AtfJavaSourceReferences.containsNamespace(examples + " atf.ui.Page actual;", "atf.ui"));
    }

    @Test
    void distinguishesJavaIdentifierBoundariesAndRejectsInvalidNamespaces() {
        for (String similar : List.of("atf.uikit.Page", "other.atf.ui.Page", "other . atf.ui.Page", "$atf.ui.Page",
                "atf.uiExtra.Page", "atf.uiλ.Page")) {
            assertFalse(AtfJavaSourceReferences.containsNamespace(similar, "atf.ui"), similar);
        }
        assertThrows(IllegalArgumentException.class,
                () -> AtfJavaSourceReferences.containsNamespace("anything", "atf.ui.*"));
    }

    @Test
    void unicodeEscapesCannotHideCodeOrPrematurelyEndAComment() {
        String escapedA = "\\" + "u0061";
        String escapedLineFeed = "\\" + "u000a";
        assertTrue(AtfJavaSourceReferences.containsNamespace(
                "import " + escapedA + "tf.ui.Page;", "atf.ui"));
        assertTrue(AtfJavaSourceReferences.containsNamespace(
                "import " + "\\" + "uuuu0061" + "tf.ui.Page;", "atf.ui"));
        assertTrue(AtfJavaSourceReferences.containsNamespace(
                "// documented example " + escapedLineFeed + " import atf.ui.Page;", "atf.ui"));
        assertTrue(AtfJavaSourceReferences.containsNamespace(
                "// documented example " + "\\" + "u000d" + "import atf.ui.Page;", "atf.ui"));
        assertTrue(AtfJavaSourceReferences.containsNamespace(
                "// documented example\rimport atf.ui.Page;", "atf.ui"));
        assertFalse(AtfJavaSourceReferences.containsNamespace(
                "String key = \"" + escapedA + "tf.ui.timeout\";", "atf.ui"));
        assertFalse(AtfJavaSourceReferences.containsNamespace(
                "// " + escapedA + "tf.ui.Page example", "atf.ui"));
        assertFalse(AtfJavaSourceReferences.containsNamespace(
                "\\" + "u002f" + "\\" + "u002a" + " import atf.ui.Page; "
                        + "\\" + "u002a" + "\\" + "u002f", "atf.ui"));
        assertFalse(AtfJavaSourceReferences.containsNamespace(
                "\\" + "u005c" + "u0061tf.ui.Page", "atf.ui"),
                "a translated backslash must not recursively start another escape");
        assertThrows(IllegalArgumentException.class,
                () -> AtfJavaSourceReferences.containsNamespace("class Bad { " + "\\" + "u12GG }", "atf.ui"));
    }
}
