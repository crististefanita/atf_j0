package atf.reporting.security;

import atf.reporting.contract.AtfReportPorts;
import atf.reporting.contract.AtfReportStatus;
import atf.reporting.configuration.ReportingSettings;
import atf.reporting.extent.session.ExtentReportSession;
import atf.reporting.html.HtmlReportSession;
import atf.testkit.AtfComponentTest;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the real renderer boundary with synthetic secrets and independent canaries. */
@AtfComponentTest
class JsonEvidenceRedactionComponentTest {
    private static final List<String> SECRETS = List.of("unicode-canary", "816943027154608219", "nested-canary",
            "array-canary", "first-duplicate", "last-duplicate", "escaped-canary");
    private static final String BODY = "{\"to\\u006ben\":\"unicode-canary\",\"password\":816943027154608219,"
            + "\"client_secret\":{\"nested\":\"nested-canary\"},\"api_key\":[\"array-canary\"],"
            + "\"cookie\":true,\"authorization\":null,\"token\":\"first-duplicate\","
            + "\"token\":\"last-duplicate\",\"nested\":[{\"password\":\"escaped-\\u0063anary\"}],"
            + "\"keep\":\"visible\",\"count\":7}";
    @TempDir Path directory;

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void validJsonIsRedactedSemanticallyBeforeFormattingInTheEntireArtifact(boolean extent) throws Exception {
        Path output=directory.resolve(extent?"extent.html":"fallback.html");
        var settings=new ReportingSettings(240,0);
        try (AtfReportPorts.Session session=extent?new ExtentReportSession(output,settings):new HtmlReportSession(output,settings)) {
            try (var scenario=session.startScenario("JsonBoundary","redaction",List.of("component"));
                 var phase=scenario.startPhase("TEST BODY");
                 var request=phase.startRequest("Observed body","GET","/fixture")) {
                request.json("RESPONSE JSON",BODY);
                request.complete(AtfReportStatus.PASS);phase.complete(AtfReportStatus.PASS);scenario.complete(AtfReportStatus.PASS);
            }
        }
        String html=Files.readString(output);
        for(String canary:SECRETS) assertFalse(html.contains(canary),"Artifact leaks "+canary);
        assertTrue(html.contains("visible"));
        assertTrue(html.contains("REDACTED"));
        String safe=new SensitiveDataRedactor().redactJson(BODY);
        assertEquals(3,safe.split("\\\"token\\\"",-1).length-1,"Unicode and duplicate fields stay inspectable");
        assertFalse(safe.contains("816943027154608219"));
        assertTrue(safe.contains("\"count\":7"));
    }
}
