package atf.core.http.response;

import atf.core.http.request.HttpMethod;

import java.util.Objects;

/**
 * Stable, typed vocabulary for one API operation shown in a report.
 * Runtime query values and identifiers stay outside this descriptor.
 */
public record ApiOperationDescriptor(String method, String relativePath, String description) {
    public ApiOperationDescriptor {
        method = required(method, "method");
        relativePath = required(relativePath, "relativePath");
        description = description == null ? "" : description.trim();
    }

    public static ApiOperationDescriptor of(HttpMethod method, String relativePath,
                                            String description) {
        return new ApiOperationDescriptor(Objects.requireNonNull(method, "method").value(),
                relativePath, description);
    }

    public String label() {
        return ApiOperationLabel.format(method, relativePath, description);
    }

    public boolean hasDescription() {
        return !description.isBlank();
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
