package atf.reporting.support.navigation;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Test-only identity parser for native/fallback hashes and serialized HTML links. */
public record FailureLinkIdentity(String scenarioAnchor, Map<String, String> parameters) {
    public FailureLinkIdentity {
        Objects.requireNonNull(scenarioAnchor, "scenarioAnchor");
        parameters = Map.copyOf(parameters);
    }

    public static FailureLinkIdentity parse(String href) {
        String fragment = URI.create(Objects.requireNonNull(href, "href")
                .replace("&amp;", "&")).getRawFragment();
        if (fragment == null || fragment.isBlank()) {
            throw new IllegalArgumentException("Failure link has no fragment");
        }
        String[] tokens = fragment.split("&", -1);
        var parameters = new LinkedHashMap<String, String>();
        for (String token : tokens) {
            String[] pair = token.split("=", 2);
            String key = decode(pair[0]);
            String value = pair.length == 2 ? decode(pair[1]) : "";
            if (key.isBlank() || parameters.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Ambiguous failure-link parameter: " + key);
            }
        }
        return new FailureLinkIdentity(decode(tokens[0]), parameters);
    }

    public boolean hasParameter(String name, String expected) {
        return expected != null && expected.equals(parameters.get(name));
    }
    public boolean failureOf(String scenario, int ordinal) {
        return ordinal > 0 && scenarioAnchor.equals(scenario)
                && hasParameter("failure-ordinal", Integer.toString(ordinal));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
