package atf.reporting.api.evidence;

import atf.reporting.api.ApiManualReplay;
import atf.reporting.api.ReportTransportProvenance;
import atf.reporting.security.SensitiveDataRedactor;
import atf.reporting.console.AtfConsoleEvent;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AtfComponentTest
class ApiMultipartBodyComponentTest {
    private static final String SECRET = "synthetic-multipart-secret";
    private static final String MIME = "multipart/form-data; boundary=audit-boundary";
    private static final String MASK = "***REDACTED***";
    private static final SensitiveDataRedactor REDACTOR = new SensitiveDataRedactor();

    private enum Sample {
        CRLF("\r\n", "name=\"password\"", true),
        LF("\n", "name=password", true),
        CASE_AND_HEADERS("\r\n", "name=\"Access_Token\"; filename=\"secret.txt\"", true),
        HYPHEN_ALIAS("\r\n", "name=\"access-token\"", true),
        QUOTED_FILENAME("\r\n", "filename=\"visible; name=description.txt\"; name=\"password\"", true),
        FOLDED("\r\n", "\r\n\tname=\"password\"", true),
        TRUNCATED("\r\n", "name=\"password\"", false),
        ESCAPED_NAME("\r\n", "name=\"pass\\word\"", true);

        final String newline;
        final String name;
        final boolean closed;
        Sample(String newline, String name, boolean closed) {
            this.newline = newline; this.name = name; this.closed = closed;
        }
        String body() {
            String nl = newline;
            return "--audit-boundary" + nl + "Content-Disposition: form-data; name=\"description\""
                    + nl + nl + "safe product" + nl + "--audit-boundary" + nl
                    + "Content-Disposition: form-data; " + name + nl + "Content-Type: text/plain" + nl + nl
                    + SECRET + nl + "second secret line" + nl + "--audit-boundary-not-a-delimiter"
                    + (closed ? nl + "--audit-boundary--" + nl : "");
        }
        String expected() {
            String body = body();
            int start = body.indexOf(SECRET);
            return body.substring(0, start) + MASK
                    + (closed ? newline + "--audit-boundary--" + newline : "");
        }
    }

    @ParameterizedTest
    @EnumSource(ApiBodyArtifactFixture.Projection.class)
    void allPublishersRedactSensitivePartsButKeepSafeFieldsAndFraming(ApiBodyArtifactFixture.Projection projection)
            throws Exception {
        var exchanges = Arrays.stream(Sample.values())
                .map(sample -> ApiBodyArtifactFixture.exchange(sample.body(), MIME, ReportBodyAvailability.AVAILABLE)).toList();
        var published = ApiBodyArtifactFixture.publish("multipart", projection, exchanges);
        List<String> expected = Arrays.stream(Sample.values()).map(Sample::expected).toList();
        assertEquals(expected, ApiBodyArtifactFixture.bodyText(published.html(), "request"));
        assertEquals(expected, ApiBodyArtifactFixture.bodyText(published.html(), "response"));
        assertFalse(published.html().contains(SECRET));
        assertFalse(published.console().toString().contains(SECRET));
        if (projection == ApiBodyArtifactFixture.Projection.EXTENT) {
            assertTrue(published.console().stream().anyMatch(event -> event.type() == AtfConsoleEvent.Type.CODE));
        }
        assertTrue(published.html().contains("REQUEST FORM BODY"));
        assertTrue(published.html().contains("RESPONSE FORM BODY"));
        assertTrue(published.html().contains(MIME));
        assertTrue(published.html().contains("ACCEPTANCE / MOCK"));
    }

    @ParameterizedTest
    @EnumSource(Sample.class)
    void replayAndRepeatedRedactionKeepTheSameSafeMultipart(Sample sample) {
        String safe = REDACTOR.redactJson(sample.body());
        assertEquals(sample.expected(), safe);
        assertEquals(safe, REDACTOR.redactJson(safe), "renderer passes must be idempotent");
        var request = ApiBodyArtifactFixture.exchange(sample.body(), MIME, ReportBodyAvailability.AVAILABLE).request();
        // LIVE_HTTP is only a captured-snapshot input here; this call never performs HTTP.
        String replay = ApiManualReplay.describe(ReportTransportProvenance.LIVE_HTTP, request,
                "https://audit.example.test/body-audit", request.bodyText());
        assertFalse(replay.contains(SECRET));
        assertFalse(replay.contains("second secret line"));
        assertTrue(replay.contains("safe product"));
        assertTrue(replay.contains(MIME));
        assertEquals(replay, REDACTOR.redactText(replay));
    }

    @Test
    void repeatedSensitivePartsAndSameLineContentKeepTheirFraming() {
        String one = Sample.CRLF.body();
        String multiple = one.replace("--audit-boundary--\r\n", "--audit-boundary\r\n"
                + "Content-Disposition: form-data; name=\"api-key\"\r\n\r\n"
                + SECRET + "\r\n--audit-boundary--\r\n");
        String safe = REDACTOR.redactText(multiple);
        assertFalse(safe.contains(SECRET));
        assertEquals(2, safe.split(Pattern.quote(MASK), -1).length - 1);
        assertTrue(safe.endsWith("\r\n--audit-boundary--\r\n"));
        assertTrue(safe.contains("safe product"));
    }

    @Test
    void replacementCharactersAreLiteralAndNonSecretPartsRemainUntouched() {
        String replacement = "$mask\\value";
        assertEquals(Sample.CRLF.expected().replace(MASK, replacement),
                new SensitiveDataRedactor(replacement).redactText(Sample.CRLF.body()));
        String publicPart = "--audit-boundary\r\nContent-Disposition: form-data; name=\"description\"\r\n\r\n"
                + "safe product\r\n--audit-boundary--\r\n";
        assertEquals(publicPart, REDACTOR.redactText(publicPart));
    }
}
