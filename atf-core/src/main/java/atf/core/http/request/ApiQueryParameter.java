package atf.core.http.request;

import java.util.Objects;

/** One decoded query parameter occurrence; duplicate names are intentional and preserved. */
public record ApiQueryParameter(String name, String value) {
    public ApiQueryParameter {
        name = Objects.requireNonNull(name, "query parameter name");
        value = Objects.requireNonNull(value, "query parameter value");
        if (name.isBlank()) throw new IllegalArgumentException("query parameter name must not be blank");
    }

    @Override
    public String toString() {
        return name + "=" + value;
    }
}
