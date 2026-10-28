package atf.autoex.tests.component;

import atf.autoex.tests.extension.AutoExComponentTest;
import atf.autoex.support.catalog.AutoExCatalogAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static atf.autoex.config.AutoExSite.PUBLIC_BASE_URI;
import static atf.testkit.assertions.AuditedAssertions.assertEquals;
import static atf.testkit.assertions.AuditedAssertions.assertFalse;
import static atf.testkit.assertions.AuditedAssertions.assertTrue;

@AutoExComponentTest
class AutoExCatalogAssertionsComponentTest {
    @Test
    void configuredPriceMustMatchACompleteRenderedLine() {
        String details = "Blue Top\nCategory: Women > Tops\nRs. 500\nAvailability: In Stock";

        assertTrue(AutoExCatalogAssertions.containsExactLine(details, "Rs. 500"));
        assertFalse(AutoExCatalogAssertions.containsExactLine(details, "Rs. 50"));
        assertFalse(AutoExCatalogAssertions.containsExactLine(details, "Rs. 5000"));
        assertFalse(AutoExCatalogAssertions.containsExactLine(details, null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("searchLocations")
    void searchRouteUsesOriginPathAndOneDecodedConfiguredTerm(String name, String location,
                                                              boolean expectedPass) {
        String observed = location == null ? null : PUBLIC_BASE_URI.resolve(location).toString();
        var assertion = AutoExCatalogAssertions.searchLocation(PUBLIC_BASE_URI, "Blue Top", observed);
        assertEquals(expectedPass, assertion.passed(), name);
    }

    static Stream<Arguments> searchLocations() {
        return Stream.of(
                Arguments.of("form encoding", "/products?search=Blue+Top", true),
                Arguments.of("percent encoding", "/products?search=Blue%20Top", true),
                Arguments.of("query order", "/products?page=1&search=Blue+Top", true),
                Arguments.of("encoded key", "/products?se%61rch=Blue+Top", true),
                Arguments.of("wrong term", "/products?search=Green+Top", false),
                Arguments.of("term prefix", "/products?search=Blue+Top+2", false),
                Arguments.of("wrong path", "/products/archive?search=Blue+Top", false),
                Arguments.of("foreign origin", "//autoex-search.invalid/products?search=Blue+Top", false),
                Arguments.of("wrong scheme", PUBLIC_BASE_URI.resolve("/products?search=Blue+Top")
                        .toString().replace("https:", "http:"), false),
                Arguments.of("substring in redirect", "/redirect?next=/products?search=Blue+Top", false),
                Arguments.of("missing parameter", "/products?page=1", false),
                Arguments.of("empty parameter", "/products?search", false),
                Arguments.of("duplicate matching parameters", "/products?search=Blue+Top&search=Blue+Top", false),
                Arguments.of("ambiguous parameters", "/products?search=Green+Top&search=Blue+Top", false),
                Arguments.of("missing URL", null, false));
    }

    @Test
    void queryDecodingPreservesReservedCharactersAndConfiguredBasePaths() {
        String term = "Tools & + ü";
        var configuredBase = PUBLIC_BASE_URI.resolve("/store/");
        String observed = PUBLIC_BASE_URI.resolve(
                "/store/products?search=Tools+%26+%2B+%C3%BC").toString();

        assertTrue(AutoExCatalogAssertions.searchLocation(configuredBase, term, observed).passed());
        assertFalse(AutoExCatalogAssertions.searchLocation(PUBLIC_BASE_URI, term, observed).passed(),
                "a configured path prefix is part of the destination contract");
        assertFalse(AutoExCatalogAssertions.searchLocation(configuredBase, term,
                observed.replace("%2B", "+")).passed(), "a literal plus must not become a space");
    }

    @Test
    void malformedUrlsAndBlankSearchTermsFailWithoutMaskingTheObservedValue() {
        var malformed = AutoExCatalogAssertions.searchLocation(PUBLIC_BASE_URI, "Blue Top", "%invalid");
        assertFalse(malformed.passed());
        assertEquals("%invalid", malformed.actual());
        assertFalse(AutoExCatalogAssertions.searchLocation(PUBLIC_BASE_URI, " ",
                PUBLIC_BASE_URI.resolve("/products?search=Blue+Top").toString()).passed());
    }
}
