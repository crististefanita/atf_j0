package atf.reporting.theme.css;
import atf.testkit.AtfUnitTest;

import atf.reporting.theme.tokens.AtfReportCssProperty;
import atf.reporting.theme.tokens.AtfReportCssSelectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class AtfReportCssBuilderTest {
    @Test
    void rendersTypedDeclarationsDeterministicallyWithoutTrailingNoise() {
        var builder = new AtfReportCssBuilder();
        builder.rule(".card")
                .important("display", "flex")
                .declaration("gap", "4px");
        builder.rule(".card.active")
                .declaration("color", "var(--atf-text)");

        String css = builder.build();

        assertEquals(".card{display:flex!important;gap:4px}.card.active{color:var(--atf-text)}", css);
    }

    @Test
    void rejectsIncompleteRuleModel() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> new AtfReportCssBuilder().rule(null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AtfReportCssBuilder().rule(".card").important("", "flex")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new AtfReportCssBuilder().rule(".card").declaration("display", "")));
    }

    @Test
    void acceptsTypedSelectorsAndPropertiesWithoutChangingSerialization() {
        var builder = new AtfReportCssBuilder();
        builder.ruleTyped(AtfReportCssSelectors.Component.CARD)
                .important(AtfReportCssProperty.DISPLAY, "flex")
                .declaration(AtfReportCssProperty.GAP, "4px");

        assertEquals(".card{display:flex!important;gap:4px}", builder.build());
    }
}
