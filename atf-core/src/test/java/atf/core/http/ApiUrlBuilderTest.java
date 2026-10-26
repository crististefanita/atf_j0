package atf.core.http;
import atf.testkit.AtfUnitTest;

import atf.core.http.request.ApiQueryParameter;
import atf.core.http.request.ApiQueryParameters;
import atf.core.http.request.ApiRequest;
import atf.core.http.request.ApiUrlBuilder;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfUnitTest

class ApiUrlBuilderTest {
    @Test
    void encodesQueryValuesAndProducesStableKeyOrder() {
        var builder = new ApiUrlBuilder(URI.create("https://atf.local"));
        var first = new LinkedHashMap<String, String>();
        first.put("include", "details & permissions");
        first.put("page", "2");
        var second = new LinkedHashMap<String, String>();
        second.put("page", "2");
        second.put("include", "details & permissions");

        String firstUrl = builder.build(new ApiRequest(null, "GET", "/search", first, Map.of(), "none", null));
        String secondUrl = builder.build(new ApiRequest(null, "GET", "/search", second, Map.of(), "none", null));

        assertAll(
                () -> assertEquals(firstUrl, secondUrl),
                () -> assertTrue(firstUrl.endsWith("?include=details%20%26%20permissions&page=2")),
                () -> assertTrue(!firstUrl.contains("+")));
    }

    @Test
    void preservesEmptyAndDuplicateQueryOccurrences() {
        var builder = new ApiUrlBuilder(URI.create("https://atf.local"));
        var query = ApiQueryParameters.of(
                new ApiQueryParameter("tag", "api"),
                new ApiQueryParameter("page", ""),
                new ApiQueryParameter("tag", "contract"));

        String url = builder.build(new ApiRequest(null, "GET", "/search", query,
                Map.of(), "none", null, ""));

        assertEquals("https://atf.local/search?page=&tag=api&tag=contract", url);
        assertEquals(2, query.count("tag", "api") + query.count("tag", "contract"));
        assertTrue(ApiQueryParameters.fromRawQuery("tag=api&page=&tag=contract")
                .containsAll(query));
    }
}
