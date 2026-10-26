package atf.ui.dom;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class CssStringEscaperTest {
    @Test
    void preservesUnicodeAndEscapesQuotesBackslashesAndControls() {
        String input = "Pliers 🔧 " + "'" + "\\" + "\n" + "\0";
        String expected = "Pliers 🔧 " + "\\'" + "\\\\" + "\\a " + "\\fffd ";

        assertEquals(expected, CssStringEscaper.escapeQuotedValue(input));
    }

    @Test
    void rejectsNullValuesBeforeSelectorConstruction() {
        assertThrows(NullPointerException.class, () -> CssStringEscaper.escapeQuotedValue(null));
    }
}
