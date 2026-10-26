package atf.ui.dom;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class HtmlElementTest {
    @Test
    void buildsCanonicalElementAndAttributeSelectors() {
        assertEquals("input[id='email']",
                HtmlElement.INPUT.attributeEquals(HtmlAttribute.ID, "email"));
        assertEquals("select[data-test='country']",
                HtmlElement.SELECT.attributeEquals(HtmlAttribute.DATA_TEST, "country"));
        assertEquals("a[data-test^='product-']",
                HtmlElement.ANCHOR.attributeStartsWith(HtmlAttribute.DATA_TEST, "product-"));
        assertEquals("[contenteditable='true']",
                HtmlAttribute.CONTENTEDITABLE.equalsSelector("true"));
        assertEquals("input, textarea, select", HtmlElement.selectorList(
                HtmlElement.INPUT, HtmlElement.TEXTAREA, HtmlElement.SELECT));
        assertEquals("select option:checked", CssPseudoClass.CHECKED.applyTo(
                HtmlElement.SELECT.selector() + " " + HtmlElement.OPTION.selector()));
    }

    @Test
    void escapesValuesAndRejectsInvalidAttributeNames() {
        assertEquals("input[value='customer\\'s']",
                HtmlElement.INPUT.attributeEquals(HtmlAttribute.VALUE, "customer's"));
        assertThrows(IllegalArgumentException.class,
                () -> HtmlElement.INPUT.attributeEquals("data test", "value"));
        assertThrows(IllegalArgumentException.class, HtmlElement::selectorList);
        assertThrows(IllegalArgumentException.class,
                () -> CssPseudoClass.CHECKED.applyTo(" "));
    }
}
