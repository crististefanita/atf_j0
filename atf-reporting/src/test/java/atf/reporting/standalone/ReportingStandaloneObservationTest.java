package atf.reporting.standalone;

import atf.reporting.model.*;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.evidence.markup.ReportObservationMarkup;
import atf.reporting.security.SensitiveDataRedactor;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReportingStandaloneObservationTest {
    @Test void observationIsDefensivelyCopiedAndHasNoVerdictInput() {
        var fields = new ArrayList<>(List.of(new ReportField("name", "value")));
        var value = new ReportObservation("Evidence", fields, List.of(), List.of());
        fields.clear();
        assertEquals(1, value.fields().size());
        assertThrows(UnsupportedOperationException.class, () -> value.fields().clear());
        assertThrows(NullPointerException.class, () -> new ReportObservation("x", null, List.of(), List.of()));
    }
    @Test void activeOrCredentialBearingArtifactLocationsAreRejected() {
        for (String uri : List.of("javascript:alert(1)", "data:text/html,hi", "relative.txt",
                "https://name:secret@example.com/a", "https://example.com/a?token=secret",
                "https://example.com/a#secret", "file:relative")) {
            assertThrows(IllegalArgumentException.class, () -> new ReportObservation.Artifact("artifact", URI.create(uri), ""), uri);
        }
    }
    @Test void redactionPrecedesPreviewAndNoMaskedLocationSurvivesInHiddenHref() {
        var value = new ReportObservation("Evidence", List.of(new ReportField("password", "secret-value"),
                new ReportField("long", "x".repeat(350))),
                List.of(new ReportObservation.Body("raw", "<script>alert(1)</script>", ReportObservation.Format.TEXT)),
                List.of(new ReportObservation.Artifact("password", URI.create("file:///private/secret-value.json"), "")));
        String html = ReportObservationMarkup.render(value, ReportingSettings.defaults(), new SensitiveDataRedactor());
        assertFalse(html.contains("secret-value"));
        assertFalse(html.contains("href="));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("x".repeat(239) + "…"));
        assertFalse(html.contains("x".repeat(240)));
        assertTrue(html.contains("tabindex=\"0\""));
    }
}
