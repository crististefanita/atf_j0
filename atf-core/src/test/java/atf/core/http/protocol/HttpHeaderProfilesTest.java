package atf.core.http.protocol;

import atf.core.http.request.HttpHeader;
import atf.core.http.request.MediaTypes;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AtfUnitTest
class HttpHeaderProfilesTest {
    @Test
    void exposesImmutableCanonicalJsonProfiles() {
        assertEquals(MediaTypes.APPLICATION_JSON,
                HttpHeaderProfiles.JSON_CONTENT.get(HttpHeader.CONTENT_TYPE.value()));
        assertEquals(MediaTypes.APPLICATION_JSON,
                HttpHeaderProfiles.JSON_REQUEST.get(HttpHeader.ACCEPT.value()));
        assertEquals(MediaTypes.APPLICATION_JSON,
                HttpHeaderProfiles.JSON_REQUEST.get(HttpHeader.CONTENT_TYPE.value()));
        assertThrows(UnsupportedOperationException.class,
                () -> HttpHeaderProfiles.JSON_REQUEST.put("X-Test", "value"));
    }
}
