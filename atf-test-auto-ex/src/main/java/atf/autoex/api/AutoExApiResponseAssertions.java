package atf.autoex.api;

import atf.reporting.contract.AtfAssertion;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;

/** Reusable semantic assertions for live Automation Exercise API responses. */
public final class AutoExApiResponseAssertions {
    private static final String SEARCH_ASSERTION = "JSON results contain the configured search term";
    private static final String MISSING_VALUE = "<missing>";
    private static final String NO_RESULTS = "<no matching product>";

    private AutoExApiResponseAssertions() { }

    public static AtfAssertion searchResultsContain(JsonNode response, String searchTerm) {
        JsonNode products = response == null ? null : response.get(AutoExApiJsonField.PRODUCTS.path());
        List<String> names = products == null || !products.isArray()
                ? List.of()
                : StreamSupport.stream(products.spliterator(), false)
                        .map(product -> product.path(AutoExApiJsonField.NAME.path()).asText(""))
                        .filter(name -> !name.isBlank())
                        .toList();
        String expected = searchTerm == null ? "" : searchTerm.trim();
        String matchingName = names.stream()
                .filter(name -> name.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT)))
                .findFirst().orElse(NO_RESULTS);
        boolean passed = !expected.isBlank() && !NO_RESULTS.equals(matchingName);
        String actual = names.isEmpty() ? MISSING_VALUE : matchingName;
        return new AtfAssertion(SEARCH_ASSERTION, expected, actual, passed);
    }
}
