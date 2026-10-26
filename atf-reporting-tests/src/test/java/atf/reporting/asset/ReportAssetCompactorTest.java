package atf.reporting.asset;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest
class ReportAssetCompactorTest {
    private final ReportAssetCompactor compactor = new ReportAssetCompactor();

    @Test
    void compactsJavaScriptWithoutChangingQuotedTextOrOperatorMeaning() {
        String source = """
                function evaluate ( value ) {
                  // publication-only comment
                  return "keep  two spaces" + +value;
                }
                """;

        String compact = compactor.compact(ReportAsset.RUNTIME_BOOT_JS, source);

        assertAll(
                () -> assertEquals(
                        "function evaluate(value){return\"keep  two spaces\"+ +value;}",
                        compact),
                () -> assertFalse(compact.contains("\n")),
                () -> assertFalse(compact.contains("publication-only")));
    }

    @Test
    void compactsCssWithoutJoiningDescendantSelectorsOrCalcOperators() {
        String source = """
                .panel .item {
                  content: "keep  two spaces";
                  margin: calc(100% - 2px);
                }
                """;

        String compact = compactor.compact(ReportAsset.FALLBACK_CSS, source);

        assertAll(
                () -> assertEquals(
                        ".panel .item{content:\"keep  two spaces\";margin:calc(100% - 2px)}",
                        compact),
                () -> assertTrue(compact.contains(".panel .item{")),
                () -> assertFalse(compact.contains("\n")));
    }

    @Test
    void compactsCssWithoutChangingSelectorOrMediaSemantics() {
        String source = "[data-role=validation] .card { margin: 0; }\n"
                + "@media (min-width: 701px) and (max-width: 1300px) { .card { margin: 0; } }";

        String compact = compactor.compact(ReportAsset.FALLBACK_CSS, source);

        assertEquals(
                "[data-role=validation] .card{margin:0}@media (min-width:701px) and (max-width:1300px){.card{margin:0}}",
                compact);
    }

    @Test
    void preservesDescendantBeforePseudoClassDuringCssPublication() {
        String source = ".card[data-atf-node-role='validation'] :is(.wrap,.audit) "
                + "{ max-height: var(--atf-table-scroll-height); }";

        assertEquals(
                ".card[data-atf-node-role='validation'] :is(.wrap,.audit)"
                        + "{max-height:var(--atf-table-scroll-height)}",
                compactor.compact(ReportAsset.FALLBACK_CSS, source));
    }

    @Test
    void loaderSeparatesReadableSourceFromPublicationCopy() {
        ReportAssetLoader loader = new ReportAssetLoader();

        String source = loader.load(ReportAsset.EXTENT_NAVIGATION_CONTROLLER_JS);
        String publication = loader.loadForPublication(ReportAsset.EXTENT_NAVIGATION_CONTROLLER_JS);

        assertAll(
                () -> assertTrue(source.contains("\n")),
                () -> assertFalse(publication.contains("\n")),
                () -> assertTrue(publication.length() < source.length()),
                () -> assertTrue(publication.contains("window.atfActivate")));
    }
}
