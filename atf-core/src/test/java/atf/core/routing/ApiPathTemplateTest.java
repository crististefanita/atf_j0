package atf.core.routing;

import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class ApiPathTemplateTest {
    @Test
    void joinsSegmentsAndResolvesEncodedValues() {
        var users = ApiPathTemplate.of("/api").child("users");

        assertEquals("/api/users/{id}", users.child("{id}").value());
        assertEquals("/api/users/alice%20smith", users.child("{id}").resolve("id", "alice smith"));
    }

    @Test
    void rejectsUnsafeTemplatesAndValues() {
        assertThrows(IllegalArgumentException.class, () -> ApiPathTemplate.of("api/users"));
        assertThrows(IllegalArgumentException.class, () -> ApiPathTemplate.of("/api//users"));
        assertThrows(IllegalArgumentException.class,
                () -> ApiPathTemplate.of("/users/{id}").resolve(Map.of("id", "1/2")));
        assertThrows(IllegalArgumentException.class,
                () -> ApiPathTemplate.of("/users/{id}").resolve(Map.of()));
    }
}
