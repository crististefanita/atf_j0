package atf.autoex.support.catalog;

import atf.reporting.contract.AtfAssertion;
import atf.autoex.locator.AutoExRoutes;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Pure catalog checks against configured product values and the site's search route. */
public final class AutoExCatalogAssertions {
    private AutoExCatalogAssertions() { }

    public static boolean containsExactLine(String renderedDetails, String expectedValue) {
        if (renderedDetails == null || expectedValue == null) return false;
        return renderedDetails.lines().map(String::trim).anyMatch(expectedValue::equals);
    }

    public static AtfAssertion searchLocation(URI baseUri, String searchTerm, String observedUrl) {
        URI expectedLocation = AutoExRoutes.PRODUCTS.resolve(baseUri);
        String term = searchTerm == null ? "" : searchTerm.trim();
        String expected = "%s?%s=%s".formatted(expectedLocation, AutoExRoutes.SEARCH_PARAMETER,
                URLEncoder.encode(term, StandardCharsets.UTF_8));
        return new AtfAssertion("Search route", expected,
                observedUrl == null ? "<missing>" : observedUrl,
                matchesSearchLocation(expectedLocation, term, observedUrl));
    }

    private static boolean matchesSearchLocation(URI expectedLocation, String term, String observedUrl) {
        if (observedUrl == null || term.isBlank()) return false;
        try {
            URI actual = URI.create(observedUrl);
            if (!expectedLocation.getScheme().equalsIgnoreCase(actual.getScheme())
                    || !expectedLocation.getRawAuthority().equalsIgnoreCase(actual.getRawAuthority())
                    || !expectedLocation.getPath().equals(actual.getPath())) return false;
            List<String> values = searchValues(actual.getRawQuery());
            return values.size() == 1 && term.equals(values.getFirst());
        } catch (IllegalArgumentException invalidLocation) {
            return false;
        }
    }

    private static List<String> searchValues(String rawQuery) {
        if (rawQuery == null) return List.of();
        return Arrays.stream(rawQuery.split("&"))
                .map(parameter -> parameter.split("=", 2))
                .filter(pair -> AutoExRoutes.SEARCH_PARAMETER.equals(decode(pair[0])))
                .map(pair -> pair.length == 2 ? decode(pair[1]) : "")
                .toList();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
